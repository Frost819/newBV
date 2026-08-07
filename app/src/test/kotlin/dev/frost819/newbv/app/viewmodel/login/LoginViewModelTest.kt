package dev.frost819.newbv.app.viewmodel.login

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.data.AuthData
import dev.frost819.newbv.biliapi.entity.login.QrLoginData
import dev.frost819.newbv.biliapi.entity.login.QrLoginResult
import dev.frost819.newbv.biliapi.entity.login.QrLoginState
import dev.frost819.newbv.biliapi.entity.login.WebCookies
import dev.frost819.newbv.biliapi.repositories.LoginRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

/**
 * [LoginViewModel] 的单元测试。
 *
 * 验证 QR 登录状态机流转、轮询逻辑、成功后凭证持久化。
 * 使用 MockK mock [LoginRepository] 和 [AccountRepositoryImpl]。
 * 使用 [UnconfinedTestDispatcher] 使 viewModelScope 协程立即执行到首个 delay。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var loginRepository: LoginRepository
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var viewModel: LoginViewModel

    private val fakeQrData = QrLoginData(url = "https://pass.bilibili.com/qr?key=abc", key = "abc")
    private val fakeCookies = WebCookies(
        dedeUserId = 123456L,
        dedeUserIdCkMd5 = "md5hash",
        sid = "session-id",
        biliJct = "jct-token",
        sessData = "sess-data-token",
        expiredDate = Date(System.currentTimeMillis() + 86400000),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginRepository = mockk()
        accountRepository = mockk(relaxed = true)
        viewModel = LoginViewModel(loginRepository, accountRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `requestAppQrCode transitions to WaitingForScan`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        try {
            viewModel.requestAppQrCode()

            assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.WaitingForScan)
            assertThat(viewModel.uiState.value.qrUrl).isEqualTo(fakeQrData.url)
        } finally {
            viewModel.cancelPolling()
        }
    }

    @Test
    fun `requestAppQrCode on error sets Error state`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } throws RuntimeException("Network error")

        viewModel.requestAppQrCode()

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Error)
        assertThat(viewModel.uiState.value.errorMessage).contains("Network error")
    }

    @Test
    fun `polling transitions to Success after scan and confirm`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returnsMany listOf(
            QrLoginResult(state = QrLoginState.WaitingForScan),
            QrLoginResult(state = QrLoginState.WaitingForConfirm),
            QrLoginResult(
                state = QrLoginState.Success,
                cookies = fakeCookies,
                accessToken = "access-token",
                refreshToken = "refresh-token",
            ),
        )
        coEvery { accountRepository.addUser(any()) } just Runs

        viewModel.requestAppQrCode()
        advanceTimeBy(3500)

        coVerify(exactly = 3) { loginRepository.checkAppQrLoginState("abc") }
        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Success)
    }

    @Test
    fun `polling on Expired sets Expired state`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.Expired,
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Expired)
    }

    @Test
    fun `polling on Success calls addUser with correct AuthData`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.Success,
            cookies = fakeCookies,
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
        coEvery { accountRepository.addUser(any()) } just Runs

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        val authDataSlot = slot<AuthData>()
        coVerify { accountRepository.addUser(capture(authDataSlot)) }
        val captured = authDataSlot.captured
        assertThat(captured.uid).isEqualTo(123456L)
        assertThat(captured.sessData).isEqualTo("sess-data-token")
        assertThat(captured.accessToken).isEqualTo("access-token")
        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Success)
    }

    @Test
    fun `requestWebQrCode uses web API`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestWebQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkWebQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        try {
            viewModel.requestWebQrCode()

            assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.WaitingForScan)
            assertThat(viewModel.uiState.value.qrUrl).isEqualTo(fakeQrData.url)
        } finally {
            viewModel.cancelPolling()
        }
    }

    @Test
    fun `cancelPolling stops ongoing polling`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)
        viewModel.cancelPolling()
        advanceTimeBy(10000)

        coVerify(exactly = 1) { loginRepository.checkAppQrLoginState(any()) }
    }

    @Test
    fun `reset clears state and cancels polling`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        viewModel.requestAppQrCode()
        viewModel.reset()

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Ready)
        assertThat(viewModel.uiState.value.qrUrl).isEmpty()
    }

    @Test
    fun `requestWebQrCode on error sets Error state`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestWebQrLogin() } throws RuntimeException("web error")

        viewModel.requestWebQrCode()

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Error)
        assertThat(viewModel.uiState.value.errorMessage).contains("web error")
    }

    @Test
    fun `polling failure sets Error state and stops polling`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } throws RuntimeException("poll error")

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Error)
        assertThat(viewModel.uiState.value.errorMessage).contains("poll error")
    }

    @Test
    fun `polling transitions through WaitingForConfirm`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returnsMany listOf(
            QrLoginResult(state = QrLoginState.WaitingForScan),
            QrLoginResult(state = QrLoginState.WaitingForConfirm),
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(2500)

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.WaitingForConfirm)
        viewModel.cancelPolling()
    }

    @Test
    fun `handleLoginSuccess with null cookies sets Error`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.Success,
            cookies = null,
            accessToken = null,
            refreshToken = null,
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Error)
        assertThat(viewModel.uiState.value.errorMessage).contains("Cookie")
    }

    @Test
    fun `onCleared cancels polling`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)
        viewModel.cancelPolling()

        advanceTimeBy(10000)
        coVerify(exactly = 1) { loginRepository.checkAppQrLoginState(any()) }
    }

    @Test
    fun `requestAppQrCode cancels previous polling`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        viewModel.requestAppQrCode()
        advanceTimeBy(1500)

        coVerify(atLeast = 2) { loginRepository.checkAppQrLoginState(any()) }
        viewModel.cancelPolling()
    }

    @Test
    fun `web QR polling success calls addUser`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestWebQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkWebQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.Success,
            cookies = fakeCookies,
            accessToken = null,
            refreshToken = null,
        )
        coEvery { accountRepository.addUser(any()) } just Runs

        viewModel.requestWebQrCode()
        advanceTimeBy(1500)

        coVerify { accountRepository.addUser(any()) }
        assertThat(viewModel.uiState.value.state).isEqualTo(QrLoginState.Success)
    }

    @Test
    fun `requestAppQrCode sets RequestingQRCode before result`() = runTest(testDispatcher) {
        coEvery { loginRepository.requestAppQrLogin() } returns fakeQrData
        coEvery { loginRepository.checkAppQrLoginState(any()) } returns QrLoginResult(
            state = QrLoginState.WaitingForScan,
        )

        viewModel.requestAppQrCode()

        assertThat(viewModel.uiState.value.state).isAtLeast(QrLoginState.RequestingQRCode)
        viewModel.cancelPolling()
    }
}
