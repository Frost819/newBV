package dev.frost819.newbv.app.viewmodel.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.data.AccountUiState
import dev.frost819.newbv.biliapi.entity.home.RecommendData
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoData
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.entity.user.DynamicVideoData
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.io.IOException

/**
 * [HomeViewModel] 的单元测试。
 *
 * 验证推荐/热门/动态数据加载、分页、刷新逻辑。
 * 使用 MockK mock [RecommendVideoRepository] 和 [UserRepository]。
 * Prefs 初始化一次，每个测试前 clear 重置。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var recommendRepo: RecommendVideoRepository
    private lateinit var userRepo: UserRepository
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var viewModel: HomeViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_home_vm", ".preferences_pb")
            file.deleteOnExit()
            testDataStore =
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = { file },
                )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized to avoid async write exceptions
        }
    }

    private fun fakeUgcItem(aid: Long) =
        UgcItem(
            aid = aid,
            title = "video $aid",
            cover = "http://example.com/cover.jpg",
            author = "up",
            authorMid = 100L,
            play = 10000,
            danmaku = 500,
            duration = 120,
        )

    private fun fakeDynamicVideo(aid: Long) =
        DynamicVideo(
            aid = aid,
            cid = aid * 10,
            title = "dynamic $aid",
            cover = "http://example.com/cover.jpg",
            author = "up",
            authorMid = 100L,
            duration = 120,
            play = 10000,
            danmaku = 500,
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { Prefs.clear() }

        recommendRepo = mockk()
        userRepo = mockk()
        accountRepo = mockk()
        every { accountRepo.uiState } returns MutableStateFlow(AccountUiState())

        coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
            RecommendData(
                items = listOf(fakeUgcItem(1), fakeUgcItem(2)),
                nextPage = RecommendPage(),
            )
        coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
            PopularVideoData(
                list = listOf(fakeUgcItem(3), fakeUgcItem(4)),
                nextPage = PopularVideoPage(),
                noMore = false,
            )
        coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
            DynamicVideoData(
                videos = listOf(fakeDynamicVideo(5), fakeDynamicVideo(6)),
                hasMore = true,
                historyOffset = "offset1",
                updateBaseline = "baseline1",
            )
    }

    private fun createViewModel() = HomeViewModel(recommendRepo, userRepo, accountRepo)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads recommend and popular`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.popularItems).isNotEmpty()
        }

    @Test
    fun `init does not load dynamics when not logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicItems).isEmpty()
            assertThat(state.isLogin).isFalse()
        }

    @Test
    fun `refreshRecommend clears and reloads`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(100)),
                    nextPage = RecommendPage(),
                )

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // loadRecommend loops until 24 items or 3 loads, so 3 × 1 = 3 items
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.recommendItems[0].aid).isEqualTo(100)
        }

    @Test
    fun `refreshPopular clears and reloads`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(200)),
                    nextPage = PopularVideoPage(),
                    noMore = true,
                )

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularItems).hasSize(1)
            assertThat(state.popularItems[0].aid).isEqualTo(200)
            assertThat(state.popularHasMore).isFalse()
        }

    @Test
    fun `loadRecommend on error sets loading false and error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("Network error")

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendLoading).isFalse()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `loadPopular on error sets loading false and error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws RuntimeException("Network error")

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularLoading).isFalse()
            assertThat(state.popularError).isTrue()
        }

    @Test
    fun `loadRecommend on timeout sets error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws IOException("timeout")

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendLoading).isFalse()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `loadPopular on timeout sets error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws IOException("timeout")

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularLoading).isFalse()
            assertThat(state.popularError).isTrue()
        }

    @Test
    fun `refreshRecommend clears error`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("error")
            viewModel.refreshRecommend()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendError).isTrue()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                )
            viewModel.refreshRecommend()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendError).isFalse()
            assertThat(viewModel.uiState.value.recommendItems).isNotEmpty()
        }

    @Test
    fun `refreshDynamic does nothing when not logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refreshDynamic()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicItems).isEmpty()
        }

    @Test
    fun `updateLoginState to true triggers dynamic load`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isTrue()
            assertThat(state.dynamicItems).isNotEmpty()
        }

    @Test
    fun `updateLoginState to false clears dynamics`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.updateLoginState(true)
            advanceUntilIdle()
            viewModel.updateLoginState(false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isFalse()
            assertThat(state.dynamicItems).isEmpty()
        }

    @Test
    fun `refresh dispatches correct tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(999)),
                    nextPage = RecommendPage(),
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.recommendItems[0]
                    .aid,
            ).isEqualTo(999)
        }

    @Test
    fun `loadMore dispatches correct tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(888)),
                    nextPage = PopularVideoPage(),
                    noMore = false,
                )

            viewModel.loadMore(dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularItems.any { it.aid == 888L }).isTrue()
        }

    @Test
    fun `loadDynamic loads items when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10), fakeDynamicVideo(11)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isTrue()
            assertThat(state.dynamicItems).hasSize(2)
            assertThat(state.dynamicLoading).isFalse()
            assertThat(state.dynamicHasMore).isFalse()
        }

    @Test
    fun `loadDynamic sets error on failure`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } throws RuntimeException("network error")

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicError).isTrue()
            assertThat(state.dynamicLoading).isFalse()
        }

    @Test
    fun `loadDynamic is no-op when hasMore is false`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)
            assertThat(viewModel.uiState.value.dynamicHasMore).isFalse()

            viewModel.loadDynamic()
            advanceUntilIdle()

            coVerify(exactly = 1) { userRepo.getDynamicVideos(any(), any(), any(), any()) }
        }

    @Test
    fun `loadRecommend loops until 24 items`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = (1..10).map { fakeUgcItem(it.toLong()) },
                    nextPage = RecommendPage(),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendItems.size).isAtLeast(20)
        }

    @Test
    fun `loadRecommend with existing items appends exactly one more page`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            // 首次加载：列表为空，最多补齐 3 页（每页 2 条），共 6 条
            assertThat(viewModel.uiState.value.recommendItems).hasSize(6)
            coVerify(exactly = 3) { recommendRepo.getRecommendVideos(any(), any()) }

            viewModel.loadRecommend()
            advanceUntilIdle()

            // 已有数据时「加载更多」只追加一页，而非空转（issue #286）
            assertThat(viewModel.uiState.value.recommendItems).hasSize(8)
            coVerify(exactly = 4) { recommendRepo.getRecommendVideos(any(), any()) }
        }

    @Test
    fun `loadRecommend sets hasMore false when page is empty`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                )
            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendHasMore).isTrue()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = emptyList(),
                    nextPage = RecommendPage(),
                )
            viewModel.loadRecommend()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendHasMore).isFalse()
        }

    @Test
    fun `loadRecommend keeps partial data and no error when a later page fails`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                ) andThenThrows RuntimeException("boom")

            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 第 2 页失败：保留第 1 页数据，不整块报错
            assertThat(state.recommendItems).hasSize(1)
            assertThat(state.recommendError).isFalse()
            assertThat(state.recommendLoading).isFalse()
        }

    @Test
    fun `loadRecommend load-more failure sets error but keeps items`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendItems).isNotEmpty()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("boom")
            viewModel.loadRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `refresh dispatches Popular tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(777)),
                    nextPage = PopularVideoPage(),
                    noMore = true,
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.popularItems[0]
                    .aid,
            ).isEqualTo(777)
            assertThat(viewModel.uiState.value.popularHasMore).isFalse()
        }

    @Test
    fun `refresh dispatches Dynamics tab when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "o",
                    updateBaseline = "b",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(66)),
                    hasMore = false,
                    historyOffset = "o2",
                    updateBaseline = "b2",
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.dynamicItems[0]
                    .aid,
            ).isEqualTo(66)
        }

    @Test
    fun `loadMore dispatches Dynamics tab when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = true,
                    historyOffset = "offset1",
                    updateBaseline = "baseline1",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(20)),
                    hasMore = false,
                    historyOffset = "offset2",
                    updateBaseline = "baseline2",
                )

            viewModel.loadMore(dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(2)
            assertThat(
                viewModel.uiState.value.dynamicItems[1]
                    .aid,
            ).isEqualTo(20)
        }

    @Test
    fun `updateLoginState to true with existing dynamics does not duplicate load`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)
        }

    @Test
    fun `refreshPopular clears error`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws RuntimeException("error")
            viewModel.refreshPopular()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.popularError).isTrue()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(1)),
                    nextPage = PopularVideoPage(),
                    noMore = false,
                )
            viewModel.refreshPopular()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.popularError).isFalse()
            assertThat(viewModel.uiState.value.popularItems).isNotEmpty()
        }
}
