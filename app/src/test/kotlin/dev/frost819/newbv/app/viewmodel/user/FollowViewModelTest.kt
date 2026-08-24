package dev.frost819.newbv.app.viewmodel.user

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.user.FollowedUser
import dev.frost819.newbv.biliapi.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [FollowViewModel] 的单元测试。
 *
 * 验证关注列表加载、刷新、幂等性、错误与超时处理。
 * 使用 MockK mock [UserRepository]。
 */
class FollowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: FollowViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FollowViewModel =
        FollowViewModel(userRepository = userRepository)

    private fun fakeFollowedUser(mid: Long) = FollowedUser(
        mid = mid,
        name = "用户$mid",
        avatar = "https://example.com/avatar$mid.jpg",
        sign = "签名$mid",
    )

    private fun fakeUsers(count: Int): List<FollowedUser> =
        (1..count).map { fakeFollowedUser(it.toLong()) }

    // ------------------------------------------------------------------
    // init
    // ------------------------------------------------------------------

    @Test
    fun `init loads followed users successfully`() = runTest(testDispatcher) {
        val users = fakeUsers(3)
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns users

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.mid).isEqualTo(1L)
        assertThat(state.users).hasSize(3)
        assertThat(state.users[0].name).isEqualTo("用户1")
        assertThat(state.loading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `init loads empty list successfully`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns emptyList()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.users).isEmpty()
        assertThat(state.loading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `init is idempotent when same mid and users already loaded`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns fakeUsers(2)

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.init(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.getFollowedUsers(1L, any()) }
    }

    @Test
    fun `init reloads when mid changes`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns fakeUsers(2)
        coEvery { userRepository.getFollowedUsers(2L, any()) } returns fakeUsers(3)

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.init(2L)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.getFollowedUsers(1L, any()) }
        coVerify(exactly = 1) { userRepository.getFollowedUsers(2L, any()) }
        assertThat(viewModel.uiState.value.users).hasSize(3)
    }

    @Test
    fun `init is not idempotent when users list is empty`() = runTest(testDispatcher) {
        // When users is empty, init should reload even for the same mid
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns emptyList()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.init(1L)
        advanceUntilIdle()

        coVerify(exactly = 2) { userRepository.getFollowedUsers(1L, any()) }
    }

    // ------------------------------------------------------------------
    // refresh
    // ------------------------------------------------------------------

    @Test
    fun `refresh clears and reloads`() = runTest(testDispatcher) {
        val firstLoad = fakeUsers(2)
        val secondLoad = fakeUsers(4)
        coEvery { userRepository.getFollowedUsers(1L, any()) } returnsMany listOf(firstLoad, secondLoad)

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.users).hasSize(2)

        viewModel.refresh(1L)
        advanceUntilIdle()

        coVerify(exactly = 2) { userRepository.getFollowedUsers(1L, any()) }
        assertThat(viewModel.uiState.value.users).hasSize(4)
        assertThat(viewModel.uiState.value.loading).isFalse()
        assertThat(viewModel.uiState.value.error).isFalse()
    }

    @Test
    fun `refresh sets mid even when different from current`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns fakeUsers(2)
        coEvery { userRepository.getFollowedUsers(2L, any()) } returns fakeUsers(3)

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.refresh(2L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.mid).isEqualTo(2L)
        assertThat(viewModel.uiState.value.users).hasSize(3)
    }

    // ------------------------------------------------------------------
    // error handling
    // ------------------------------------------------------------------

    @Test
    fun `init sets error on network failure`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } throws IOException("network error")

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isTrue()
        assertThat(state.loading).isFalse()
        assertThat(state.users).isEmpty()
    }

    @Test
    fun `refresh sets error on network failure`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(1L, any()) } returns fakeUsers(2) andThenThrows IOException("network error")

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isFalse()

        viewModel.refresh(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isTrue()
        assertThat(viewModel.uiState.value.loading).isFalse()
    }

    // ------------------------------------------------------------------
    // timeout handling
    // ------------------------------------------------------------------

    @Test
    fun `init sets error on timeout`() = runTest(testDispatcher) {
        coEvery { userRepository.getFollowedUsers(any(), any()) } coAnswers {
            delay(31_000)
            fakeUsers(2)
        }

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isTrue()
        assertThat(state.loading).isFalse()
    }
}
