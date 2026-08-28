package dev.frost819.newbv.app.viewmodel.user

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.data.db.entity.UserEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [UserSwitchViewModel] 的单元测试。
 *
 * 验证账户列表加载、切换账户、删除账户逻辑。
 */
class UserSwitchViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var viewModel: UserSwitchViewModel

    private val user1 = UserEntity(id = 1, uid = 100L, username = "user1", avatar = "url1", auth = "{}")
    private val user2 = UserEntity(id = 2, uid = 200L, username = "user2", avatar = "url2", auth = "{}")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads users and current uid`() =
        runTest(testDispatcher) {
            coEvery { accountRepository.getAllUsers() } returns listOf(user1, user2)
            coEvery { accountRepository.currentUid() } returns 100L

            viewModel = UserSwitchViewModel(accountRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.loading).isFalse()
                assertThat(state.users).hasSize(2)
                assertThat(state.currentUid).isEqualTo(100L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `switchUser calls setCurrentUser and refreshes`() =
        runTest(testDispatcher) {
            coEvery { accountRepository.getAllUsers() } returns listOf(user1, user2)
            coEvery { accountRepository.currentUid() } returns 100L
            viewModel = UserSwitchViewModel(accountRepository)

            viewModel.switchUser(user2)

            coVerify { accountRepository.setCurrentUser(user2) }
        }

    @Test
    fun `deleteUser switches to first remaining when deleting current`() =
        runTest(testDispatcher) {
            coEvery { accountRepository.getAllUsers() } returnsMany
                listOf(
                    listOf(user1, user2),
                    listOf(user2),
                )
            coEvery { accountRepository.currentUid() } returns 100L
            viewModel = UserSwitchViewModel(accountRepository)

            viewModel.deleteUser(user1)

            coVerify { accountRepository.deleteUser(user1) }
            coVerify { accountRepository.setCurrentUser(user2) }
        }

    @Test
    fun `deleteUser logs out when no remaining users`() =
        runTest(testDispatcher) {
            coEvery { accountRepository.getAllUsers() } returnsMany
                listOf(
                    listOf(user1),
                    emptyList(),
                )
            coEvery { accountRepository.currentUid() } returns 100L
            viewModel = UserSwitchViewModel(accountRepository)

            viewModel.deleteUser(user1)

            coVerify { accountRepository.deleteUser(user1) }
            coVerify { accountRepository.logout() }
        }
}
