package dev.frost819.newbv.app.ui.screen.live

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResult
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveRoom
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
 * [LiveFollowViewModel] 的单元测试。
 *
 * 验证关注直播的滚动分页：首屏只拉一页、翻页追加并在末页停止、
 * 失败后继续滚动会用同一页重试（与其它信息流的翻页行为一致）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveFollowViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var liveRepository: LiveRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        liveRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads only the first page and exposes live total`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getFollowLive(page = 1) } returns
                FollowLiveResult(rooms = List(10) { fakeRoom(it + 1L) }, hasMore = true, total = 12)

            val viewModel = LiveFollowViewModel(liveRepository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.items).hasSize(10)
            assertThat(state.total).isEqualTo(12)
            assertThat(state.hasMore).isTrue()
            assertThat(state.isLoading).isFalse()
            coVerify(exactly = 1) { liveRepository.getFollowLive(page = 1) }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `loadMore appends the last page and stops paging`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getFollowLive(page = 1) } returns
                FollowLiveResult(rooms = List(10) { fakeRoom(it + 1L) }, hasMore = true, total = 12)
            coEvery { liveRepository.getFollowLive(page = 2) } returns
                FollowLiveResult(rooms = List(2) { fakeRoom(it + 11L) }, hasMore = false, total = 12)

            val viewModel = LiveFollowViewModel(liveRepository)
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.items.map { it.roomId }).isEqualTo((1L..12L).toList())
            assertThat(state.hasMore).isFalse()
            coVerify(exactly = 1) { liveRepository.getFollowLive(page = 2) }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `loadMore after failure retries the same page`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getFollowLive(page = 1) } throws IOException("boom")

            val viewModel = LiveFollowViewModel(liveRepository)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isError).isTrue()

            coEvery { liveRepository.getFollowLive(page = 1) } returns
                FollowLiveResult(rooms = List(1) { fakeRoom(1L) }, hasMore = false, total = 1)
            viewModel.loadMore()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isError).isFalse()
            assertThat(state.items).hasSize(1)
            coVerify(exactly = 2) { liveRepository.getFollowLive(page = 1) }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `loadMore is ignored while loading or without more pages`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getFollowLive(page = 1) } returns
                FollowLiveResult(rooms = List(10) { fakeRoom(it + 1L) }, hasMore = false, total = 10)

            val viewModel = LiveFollowViewModel(liveRepository)
            advanceUntilIdle()

            viewModel.loadMore()
            viewModel.loadMore()
            advanceUntilIdle()

            coVerify(exactly = 1) { liveRepository.getFollowLive(page = 1) }
            viewModel.viewModelScope.cancel()
        }

    private fun fakeRoom(roomId: Long) =
        FollowLiveRoom(
            roomId = roomId,
            uid = roomId,
            title = "room $roomId",
            uname = "主播 $roomId",
            liveStatus = 1,
        )
}
