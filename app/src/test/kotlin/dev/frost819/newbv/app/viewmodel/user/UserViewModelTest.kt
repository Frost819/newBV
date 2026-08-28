package dev.frost819.newbv.app.viewmodel.user

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.data.AccountUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [UserViewModel] 的单元测试。
 *
 * 验证用户信息状态映射、刷新用户信息、切换无痕模式。
 */
class UserViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var viewModel: UserViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = mockk(relaxed = true)
        every { accountRepository.uiState } returns
            MutableStateFlow(
                AccountUiState(isLogin = true, uid = 100L, username = "tester"),
            )
        coEvery { accountRepository.isLogin() } returns true
        coEvery { accountRepository.reloadAvatar() } returns Unit
        coEvery { accountRepository.refreshUserInfo() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init refreshes user info when logged in`() =
        runTest(testDispatcher) {
            viewModel = UserViewModel(accountRepository)
            advanceUntilIdle()

            coVerify { accountRepository.refreshUserInfo() }
        }

    @Test
    fun `init does not refresh when not logged in`() =
        runTest(testDispatcher) {
            coEvery { accountRepository.isLogin() } returns false
            viewModel = UserViewModel(accountRepository)
            advanceUntilIdle()

            coVerify(exactly = 0) { accountRepository.refreshUserInfo() }
            coVerify(exactly = 0) { accountRepository.reloadAvatar() }
        }

    @Test
    fun `uiState reflects account repository state`() =
        runTest(testDispatcher) {
            viewModel = UserViewModel(accountRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.isLogin).isTrue()
                assertThat(state.uid).isEqualTo(100L)
                assertThat(state.username).isEqualTo("tester")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshUserInfo calls accountRepository`() =
        runTest(testDispatcher) {
            viewModel = UserViewModel(accountRepository)
            advanceUntilIdle()

            viewModel.refreshUserInfo()
            advanceUntilIdle()

            coVerify(atLeast = 1) { accountRepository.refreshUserInfo() }
        }

    @Test
    fun `toggleIncognitoMode calls accountRepository`() =
        runTest(testDispatcher) {
            viewModel = UserViewModel(accountRepository)
            advanceUntilIdle()

            viewModel.toggleIncognitoMode()

            verify { accountRepository.toggleIncognitoMode() }
        }
}
