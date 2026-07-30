package dev.frost819.newbv.app.viewmodel.pgc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.CarouselData
import dev.frost819.newbv.biliapi.entity.pgc.PgcFeedData
import dev.frost819.newbv.biliapi.entity.pgc.PgcItem
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.http.SeasonIndexType
import dev.frost819.newbv.biliapi.repositories.PgcRepository
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
 * [PgcViewModel] 的单元测试。
 *
 * 验证分区数据加载、轮播图加载、切换分区、刷新、超时/错误处理。
 * 使用 MockK mock [PgcRepository]，用 answers + callCount 区分多次调用。
 */
class PgcViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pgcRepository: PgcRepository
    private lateinit var viewModel: PgcViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        pgcRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakePgcItem(seasonId: Int) = PgcItem(
        cover = "https://example.com/cover$seasonId.jpg",
        title = "番剧 $seasonId",
        subTitle = "副标题 $seasonId",
        seasonId = seasonId,
        episodeId = seasonId * 100,
        seasonType = SeasonIndexType.Anime,
        rating = "9.0",
    )

    private fun fakeFeedData(items: List<PgcItem>, hasNext: Boolean, cursor: Int) = PgcFeedData(
        hasNext = hasNext,
        cursor = cursor,
        items = items,
    )

    private fun fakeCarouselData() = CarouselData(
        items = listOf(
            CarouselData.CarouselItem(
                cover = "https://example.com/banner.jpg",
                title = "轮播标题",
                seasonId = 1,
                episodeId = 100,
            ),
        ),
    )

    @Test
    fun `init loads first page with carousel`() = runTest(testDispatcher) {
        val items = listOf(fakePgcItem(1), fakePgcItem(2))
        coEvery { pgcRepository.getFeed(any(), any()) } returns
            fakeFeedData(items, hasNext = true, cursor = 1)
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(2)
        assertThat(viewModel.uiState.value.loading).isFalse()
        assertThat(viewModel.uiState.value.hasMore).isTrue()
        assertThat(viewModel.uiState.value.error).isFalse()
        assertThat(viewModel.uiState.value.carouselItems).hasSize(1)
        assertThat(viewModel.uiState.value.carouselLoading).isFalse()
    }

    @Test
    fun `loadMore appends items and stops when hasNext is false`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { pgcRepository.getFeed(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakePgcItem(1), fakePgcItem(2)), hasNext = true, cursor = 1)
            } else {
                fakeFeedData(listOf(fakePgcItem(3), fakePgcItem(4)), hasNext = false, cursor = 2)
            }
        }
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(4)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `switchType clears and loads new region`() = runTest(testDispatcher) {
        coEvery {
            pgcRepository.getFeed(PgcType.Anime, any())
        } returns fakeFeedData(listOf(fakePgcItem(1)), hasNext = true, cursor = 1)
        coEvery {
            pgcRepository.getFeed(PgcType.Movie, any())
        } returns fakeFeedData(listOf(fakePgcItem(100)), hasNext = false, cursor = 1)
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()
        viewModel.switchType(PgcType.Movie)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].seasonId).isEqualTo(100)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `error sets error flag and preserves existing items`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { pgcRepository.getFeed(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakePgcItem(1)), hasNext = true, cursor = 1)
            } else {
                throw IOException("network error")
            }
        }
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

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
        coEvery { pgcRepository.getFeed(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                fakeFeedData(listOf(fakePgcItem(1)), hasNext = true, cursor = 1)
            } else {
                fakeFeedData(listOf(fakePgcItem(99)), hasNext = false, cursor = 1)
            }
        }
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].seasonId).isEqualTo(1)

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.items[0].seasonId).isEqualTo(99)
    }

    @Test
    fun `loadMore is no-op when hasMore is false`() = runTest(testDispatcher) {
        var callCount = 0
        coEvery { pgcRepository.getFeed(any(), any()) } answers {
            callCount++
            fakeFeedData(listOf(fakePgcItem(callCount)), hasNext = false, cursor = 1)
        }
        coEvery { pgcRepository.getCarousel(any()) } returns fakeCarouselData()
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.hasMore).isFalse()

        viewModel.loadMore()
        advanceUntilIdle()

        assertThat(callCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.items).hasSize(1)
    }

    @Test
    fun `carousel load failure does not block feed`() = runTest(testDispatcher) {
        coEvery { pgcRepository.getFeed(any(), any()) } returns
            fakeFeedData(listOf(fakePgcItem(1)), hasNext = true, cursor = 1)
        coEvery { pgcRepository.getCarousel(any()) } throws IOException("carousel error")
        viewModel = PgcViewModel(pgcRepository)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(1)
        assertThat(viewModel.uiState.value.carouselItems).isEmpty()
        assertThat(viewModel.uiState.value.carouselLoading).isFalse()
    }
}
