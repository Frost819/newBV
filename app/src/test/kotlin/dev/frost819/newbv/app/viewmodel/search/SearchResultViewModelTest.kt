package dev.frost819.newbv.app.viewmodel.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.biliapi.repositories.SearchTypePage
import dev.frost819.newbv.biliapi.repositories.SearchTypeResult
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/**
 * [SearchResultViewModel] 的单元测试。
 *
 * 验证搜索结果加载、分页、筛选、Tab 切换逻辑。
 * 使用 MockK mock [SearchRepository]。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var searchRepo: SearchRepository
    private lateinit var viewModel: SearchResultViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_search_result_vm", ".preferences_pb")
            file.deleteOnExit()
            testDataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized
        }
    }

    private fun fakeVideoResult(aid: Long) = SearchTypeResult.Video(
        aid = aid,
        bvid = "BV$aid",
        title = "video $aid",
        cover = "http://example.com/cover.jpg",
        author = "up",
        mid = 100L,
        duration = 120,
        play = 10000,
        danmaku = 500,
    )

    private fun fakePgcResult(seasonId: Int) = SearchTypeResult.Pgc(
        title = "番剧 $seasonId",
        cover = "http://example.com/cover.jpg",
        star = 9.0f,
        seasonId = seasonId,
    )

    private fun fakeUserResult(mid: Long) = SearchTypeResult.User(
        mid = mid,
        name = "user $mid",
        avatar = "http://example.com/avatar.jpg",
        sign = "签名",
    )

    private fun fakeVideoList(count: Int): List<SearchTypeResult.Video> =
        (1..count).map { fakeVideoResult(it.toLong()) }

    private fun fakeVideoSearchResult(videos: List<SearchTypeResult.Video>) = SearchTypeResult(
        videos = videos,
        page = SearchTypePage(nextPageForWeb = 2),
    )

    private fun fakePgcSearchResult(pgcs: List<SearchTypeResult.Pgc>) = SearchTypeResult(
        pgcs = pgcs,
        page = SearchTypePage(nextPageForWeb = 2),
    )

    private fun fakeUserSearchResult(users: List<SearchTypeResult.User>) = SearchTypeResult(
        users = users,
        page = SearchTypePage(nextPageForWeb = 2),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { Prefs.clear() }

        searchRepo = mockk()

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeVideoSearchResult(fakeVideoList(20))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.MediaBangumi,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakePgcSearchResult(listOf(fakePgcResult(101)))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.MediaFt,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakePgcSearchResult(listOf(fakePgcResult(201)))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.BiliUser,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeUserSearchResult(listOf(fakeUserResult(301)))

        viewModel = SearchResultViewModel(searchRepo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search loads all 4 types in parallel`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.keyword).isEqualTo("测试")

        val videoResult = state.results[SearchType.Video]!!
        assertThat(videoResult.items).hasSize(20)
        assertThat(videoResult.isLoading).isFalse()
        assertThat(videoResult.error).isFalse()

        val bangumiResult = state.results[SearchType.MediaBangumi]!!
        assertThat(bangumiResult.items).hasSize(1)

        val ftResult = state.results[SearchType.MediaFt]!!
        assertThat(ftResult.items).hasSize(1)

        val userResult = state.results[SearchType.BiliUser]!!
        assertThat(userResult.items).hasSize(1)
    }

    @Test
    fun `search resets all previous results`() = runTest(testDispatcher) {
        viewModel.search("第一次")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.results[SearchType.Video]!!.items).hasSize(20)

        viewModel.search("第二次")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.keyword).isEqualTo("第二次")
        // Results should have been reset then reloaded
        val videoResult = state.results[SearchType.Video]!!
        assertThat(videoResult.items).hasSize(20)
        val first = videoResult.items[0] as dev.frost819.newbv.app.ui.state.search.SearchResultItem.VideoItem
        assertThat(first.video.aid).isEqualTo(1)
    }

    @Test
    fun `switchType changes active type`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        viewModel.switchType(SearchType.BiliUser)
        assertThat(viewModel.uiState.value.activeType).isEqualTo(SearchType.BiliUser)
    }

    @Test
    fun `loadMore appends items to existing results`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        val firstPageCount = viewModel.uiState.value.results[SearchType.Video]!!.items.size

        // Return more items for second page (aid 21-40, no duplicates with page 1)
        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeVideoSearchResult((21..40).map { fakeVideoResult(it.toLong()) })

        viewModel.loadMore(SearchType.Video)
        advanceUntilIdle()

        val result = viewModel.uiState.value.results[SearchType.Video]!!
        assertThat(result.items).hasSize(firstPageCount + 20)
    }

    @Test
    fun `loadMore skips when already loading`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        // Start loading
        viewModel.loadMore(SearchType.Video)
        // Immediately try again without advancing
        viewModel.loadMore(SearchType.Video)
        advanceUntilIdle()

        // Should only have called searchType twice (initial search + one loadMore)
        coVerify(exactly = 2) {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        }
    }

    @Test
    fun `loadMore skips when no more data`() = runTest(testDispatcher) {
        // Return empty result = no more
        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns SearchTypeResult(
            videos = emptyList(),
            page = SearchTypePage(),
        )

        viewModel.search("测试")
        advanceUntilIdle()

        val result = viewModel.uiState.value.results[SearchType.Video]!!
        assertThat(result.hasMore).isFalse()
        assertThat(result.items).isEmpty()

        // Try to load more
        viewModel.loadMore(SearchType.Video)
        advanceUntilIdle()

        // Should not have made additional calls
        coVerify(exactly = 1) {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        }
    }

    @Test
    fun `loadMore failure sets error flag`() = runTest(testDispatcher) {
        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } throws RuntimeException("network error")

        viewModel.search("测试")
        advanceUntilIdle()

        val result = viewModel.uiState.value.results[SearchType.Video]!!
        assertThat(result.error).isTrue()
        assertThat(result.isLoading).isFalse()
    }

    @Test
    fun `updateFilter updates order and duration then re-searches`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        viewModel.updateFilter(
            SearchFilterOrderType.LatestPublish,
            SearchFilterDuration.LessThan10Minutes,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedOrder).isEqualTo(SearchFilterOrderType.LatestPublish)
        assertThat(state.selectedDuration).isEqualTo(SearchFilterDuration.LessThan10Minutes)
        assertThat(state.showFilter).isFalse()

        // Verify searchType was called with new filter
        coVerify(atLeast = 1) {
            searchRepo.searchType(
                keyword = "测试",
                type = SearchType.Video,
                tid = null,
                order = SearchFilterOrderType.LatestPublish,
                duration = SearchFilterDuration.LessThan10Minutes,
                page = any(),
                preferApiType = any(),
            )
        }
    }

    @Test
    fun `toggleFilter shows and hides filter`() = runTest(testDispatcher) {
        assertThat(viewModel.uiState.value.showFilter).isFalse()

        viewModel.toggleFilter(true)
        assertThat(viewModel.uiState.value.showFilter).isTrue()

        viewModel.toggleFilter(false)
        assertThat(viewModel.uiState.value.showFilter).isFalse()
    }

    @Test
    fun `search with blank keyword does nothing`() = runTest(testDispatcher) {
        viewModel.search("")
        advanceUntilIdle()

        coVerify(exactly = 0) {
            searchRepo.searchType(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `video items are correctly mapped to VideoItem`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        val items = viewModel.uiState.value.results[SearchType.Video]!!.items
        assertThat(items).hasSize(20)
        val first = items[0] as dev.frost819.newbv.app.ui.state.search.SearchResultItem.VideoItem
        assertThat(first.video.aid).isEqualTo(1)
        assertThat(first.video.title).isEqualTo("video 1")
    }

    @Test
    fun `user items are correctly mapped to UserItem`() = runTest(testDispatcher) {
        viewModel.search("测试")
        advanceUntilIdle()

        val items = viewModel.uiState.value.results[SearchType.BiliUser]!!.items
        assertThat(items).hasSize(1)
        val first = items[0] as dev.frost819.newbv.app.ui.state.search.SearchResultItem.UserItem
        assertThat(first.user.mid).isEqualTo(301L)
    }
}
