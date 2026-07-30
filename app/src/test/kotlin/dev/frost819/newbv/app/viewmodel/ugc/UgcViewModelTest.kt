package dev.frost819.newbv.app.viewmodel.ugc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.ugc.UgcTypeV2
import dev.frost819.newbv.biliapi.entity.ugc.region.UgcFeedData
import dev.frost819.newbv.biliapi.entity.ugc.region.UgcFeedPage
import dev.frost819.newbv.biliapi.repositories.UgcRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
 * [UgcViewModel] 的单元测试。
 *
 * 验证分区数据加载、切换分区、刷新、超时/错误处理。
 * 使用 MockK mock [UgcRepository]，用 answers + callCount 区分多次调用。
 */
class UgcViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ugcRepository: UgcRepository
    private lateinit var viewModel: UgcViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ugcRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeUgcItem(aid: Long) = UgcItem(
        aid = aid,
        bvid = "BV$aid",
        title = "视频 $aid",
        cover = "https://example.com/cover$aid.jpg",
        author = "UP$aid",
        authorMid = aid,
        play = 1000,
        danmaku = 100,
        duration = 600,
    )

    private fun fakeFeedData(items: List<UgcItem>, hasNext: Boolean, nextPage: Int) = UgcFeedData(
        hasNext = hasNext,
        nextPage = UgcFeedPage(nextPage),
        items = items,
    )

    @Test
    fun `init loads first page`() = runTest(testDispatcher) {
        val items = listOf(fakeUgcItem(1), fakeUgcItem(2))
        coEvery { ugcRepository.getRegionFeedRcmd(any(), any()) } returns
            fakeFeedData(items, hasNext = true, nextPage = 2)
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(2)
        assertThat(viewModel.uiState.value.loading).isFalse()
        assertThat(viewModel.uiState.value.hasMore).isTrue()
        assertThat(viewModel.uiState.value.error).isFalse()
    }

    @Test
    fun `loadMore appends items and stops when hasNext is false`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { ugcRepository.getRegionFeedRcmd(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakeUgcItem(1), fakeUgcItem(2)), hasNext = true, nextPage = 2)
            } else {
                fakeFeedData(listOf(fakeUgcItem(3), fakeUgcItem(4)), hasNext = false, nextPage = 3)
            }
        }
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(4)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `switchType clears and loads new region`() = runTest(testDispatcher) {
        coEvery {
            ugcRepository.getRegionFeedRcmd(UgcTypeV2.Douga, any())
        } returns fakeFeedData(listOf(fakeUgcItem(1)), hasNext = true, nextPage = 2)
        coEvery {
            ugcRepository.getRegionFeedRcmd(UgcTypeV2.Music, any())
        } returns fakeFeedData(listOf(fakeUgcItem(100)), hasNext = false, nextPage = 2)
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()
        viewModel.switchType(UgcTypeV2.Music)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].aid).isEqualTo(100)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `error sets error flag and preserves existing items`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { ugcRepository.getRegionFeedRcmd(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakeUgcItem(1)), hasNext = true, nextPage = 2)
            } else {
                throw IOException("network error")
            }
        }
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items).hasSize(1)

        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.error).isTrue()
        assertThat(viewModel.uiState.value.loading).isFalse()
    }

    @Test
    fun `refresh clears items and reloads`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { ugcRepository.getRegionFeedRcmd(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakeUgcItem(1)), hasNext = true, nextPage = 2)
            } else {
                fakeFeedData(listOf(fakeUgcItem(99)), hasNext = false, nextPage = 2)
            }
        }
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].aid).isEqualTo(1)

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].aid).isEqualTo(99)
    }

    @Test
    fun `loadMore is no-op when hasMore is false`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { ugcRepository.getRegionFeedRcmd(any(), any()) } answers {
            callCount++
            fakeFeedData(listOf(fakeUgcItem(callCount.toLong())), hasNext = false, nextPage = 2)
        }
        viewModel = UgcViewModel(ugcRepository)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.hasMore).isFalse()

        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(callCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.items).hasSize(1)
    }
}
