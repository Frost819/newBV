package dev.frost819.newbv.app.viewmodel.personal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.FavoriteFolderData
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import dev.frost819.newbv.biliapi.entity.FavoriteItem
import dev.frost819.newbv.biliapi.entity.FavoriteItemType
import dev.frost819.newbv.biliapi.entity.Upper
import dev.frost819.newbv.biliapi.entity.season.FollowingSeason
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonData
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonStatus
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.biliapi.entity.user.HistoryData
import dev.frost819.newbv.biliapi.entity.user.HistoryItem
import dev.frost819.newbv.biliapi.entity.user.HistoryItemType
import dev.frost819.newbv.biliapi.entity.user.ToViewData
import dev.frost819.newbv.biliapi.entity.user.ToViewItem
import dev.frost819.newbv.biliapi.entity.user.ToViewItemType
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.HistoryRepository
import dev.frost819.newbv.biliapi.repositories.SeasonRepository
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
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
 * [PersonalViewModel] 的单元测试。
 *
 * 验证稍后再看、历史、收藏、追番四个 Tab 的数据加载、分页、刷新、筛选逻辑。
 * 使用 MockK mock 四个 Repository。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var toViewRepo: ToViewRepository
    private lateinit var historyRepo: HistoryRepository
    private lateinit var favoriteRepo: FavoriteRepository
    private lateinit var seasonRepo: SeasonRepository
    private lateinit var viewModel: PersonalViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_personal_vm", ".preferences_pb")
            file.deleteOnExit()
            testDataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            Prefs.init(testDataStore)
            Prefs.isLogin = true
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
        }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        Prefs.isLogin = true
        toViewRepo = mockk()
        historyRepo = mockk()
        favoriteRepo = mockk()
        seasonRepo = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PersonalViewModel {
        viewModel = PersonalViewModel(
            toViewRepository = toViewRepo,
            historyRepository = historyRepo,
            favoriteRepository = favoriteRepo,
            seasonRepository = seasonRepo,
        )
        return viewModel
    }

    private fun fakeToViewItem(aid: Long, progress: Int = 100, duration: Int = 300) =
        ToViewItem(
            oid = aid,
            bvid = "BV$aid",
            cid = aid * 10,
            kid = 0,
            epid = null,
            seasonId = null,
            title = "视频 $aid",
            cover = "http://example.com/cover.jpg",
            author = "UP主",
            mid = 100L,
            duration = duration,
            progress = progress,
            type = ToViewItemType.Archive,
        )

    private fun fakeToViewData(items: List<ToViewItem>) = ToViewData(
        cursor = 0,
        data = items,
    )

    private fun fakeHistoryItem(aid: Long, progress: Int = 100, duration: Int = 300) =
        HistoryItem(
            oid = aid,
            bvid = "BV$aid",
            cid = aid * 10,
            kid = 0,
            epid = null,
            seasonId = null,
            title = "历史 $aid",
            cover = "http://example.com/cover.jpg",
            author = "UP主",
            mid = 100L,
            duration = duration,
            progress = progress,
            type = HistoryItemType.Archive,
        )

    private fun fakeHistoryData(items: List<HistoryItem>, cursor: Long) = HistoryData(
        cursor = cursor,
        data = items,
    )

    private fun fakeFolder(id: Long, title: String = "收藏夹$id") = FavoriteFolderMetadata(
        id = id,
        fid = id,
        mid = 1L,
        title = title,
        cover = null,
        videoInThisFav = false,
        mediaCount = 10,
    )

    private fun fakeFavoriteItem(id: Long) = FavoriteItem(
        id = id,
        type = FavoriteItemType.Video,
        title = "收藏 $id",
        cover = "http://example.com/cover.jpg",
        intro = "",
        page = 1,
        duration = 300,
        upper = Upper(mid = 100L, name = "UP主", face = ""),
        link = "",
        pubtime = 0L,
        bvid = "BV$id",
    )

    private fun fakeFavoriteFolderData(items: List<FavoriteItem>, hasMore: Boolean) =
        FavoriteFolderData(
            info = fakeFolder(1),
            medias = items,
            hasMore = hasMore,
        )

    private fun fakeFollowingSeason(id: Int) = FollowingSeason(
        seasonId = id,
        title = "番剧 $id",
        cover = "http://example.com/cover.jpg",
    )

    private fun fakeFollowingSeasonData(items: List<FollowingSeason>, total: Int) =
        FollowingSeasonData(
            list = items,
            total = total,
        )

    // region ToView

    @Test
    fun `loadToView loads items successfully`() = runTest(testDispatcher) {
        val items = listOf(fakeToViewItem(1), fakeToViewItem(2))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(items)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems).hasSize(2)
        assertThat(vm.uiState.value.toViewLoading).isFalse()
        assertThat(vm.uiState.value.toViewError).isFalse()
    }

    @Test
    fun `loadToView sets error on failure`() = runTest(testDispatcher) {
        coEvery { toViewRepo.getToView(any(), any()) } throws RuntimeException("network error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems).isEmpty()
        assertThat(vm.uiState.value.toViewError).isTrue()
        assertThat(vm.uiState.value.toViewLoading).isFalse()
    }

    @Test
    fun `delToView removes item from list`() = runTest(testDispatcher) {
        val items = listOf(fakeToViewItem(1), fakeToViewItem(2))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(items)
        coEvery { toViewRepo.delToView(any(), any(), any()) } returns Unit

        val vm = createViewModel()
        advanceUntilIdle()

        vm.delToView(aid = 1)
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems).hasSize(1)
        assertThat(vm.uiState.value.toViewItems[0].oid).isEqualTo(2)
    }

    @Test
    fun `refreshToView clears and reloads`() = runTest(testDispatcher) {
        val items1 = listOf(fakeToViewItem(1))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(items1)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems).hasSize(1)

        val items2 = listOf(fakeToViewItem(3), fakeToViewItem(4))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(items2)

        vm.refreshToView()
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems).hasSize(2)
        assertThat(vm.uiState.value.toViewItems[0].oid).isEqualTo(3)
    }

    // endregion

    // region History

    @Test
    fun `loadHistory loads items with cursor pagination`() = runTest(testDispatcher) {
        val items1 = listOf(fakeHistoryItem(1), fakeHistoryItem(2))
        coEvery { historyRepo.getHistories(0, any()) } returns fakeHistoryData(items1, cursor = 100)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.historyItems).hasSize(2)
        assertThat(vm.uiState.value.historyHasMore).isTrue()
    }

    @Test
    fun `loadHistory stops when cursor is 0`() = runTest(testDispatcher) {
        val items = listOf(fakeHistoryItem(1))
        coEvery { historyRepo.getHistories(any(), any()) } returns fakeHistoryData(items, cursor = 0)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.historyItems).hasSize(1)
        assertThat(vm.uiState.value.historyHasMore).isFalse()
    }

    @Test
    fun `loadHistory sets error on failure`() = runTest(testDispatcher) {
        coEvery { historyRepo.getHistories(any(), any()) } throws RuntimeException("error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.historyItems).isEmpty()
        assertThat(vm.uiState.value.historyError).isTrue()
    }

    @Test
    fun `refreshHistory resets cursor and reloads`() = runTest(testDispatcher) {
        val items1 = listOf(fakeHistoryItem(1))
        coEvery { historyRepo.getHistories(0, any()) } returns fakeHistoryData(items1, cursor = 100)

        val vm = createViewModel()
        advanceUntilIdle()

        val items2 = listOf(fakeHistoryItem(2))
        coEvery { historyRepo.getHistories(0, any()) } returns fakeHistoryData(items2, cursor = 0)

        vm.refreshHistory()
        advanceUntilIdle()

        assertThat(vm.uiState.value.historyItems).hasSize(1)
        assertThat(vm.uiState.value.historyItems[0].oid).isEqualTo(2)
        assertThat(vm.uiState.value.historyHasMore).isFalse()
    }

    // endregion

    // region Favorite

    @Test
    fun `loadFavoriteFolders loads folders and first folder items`() = runTest(testDispatcher) {
        val folders = listOf(fakeFolder(1), fakeFolder(2))
        val items = listOf(fakeFavoriteItem(10), fakeFavoriteItem(11))
        coEvery { favoriteRepo.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns folders
        coEvery { favoriteRepo.getFavoriteFolderData(any(), any(), any(), any()) } returns
            fakeFavoriteFolderData(items, hasMore = false)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.favoriteFolders).hasSize(2)
        assertThat(vm.uiState.value.currentFolderId).isEqualTo(1)
        assertThat(vm.uiState.value.favoriteItems).hasSize(2)
    }

    @Test
    fun `loadFavoriteItems switches folder and loads new items`() = runTest(testDispatcher) {
        val folders = listOf(fakeFolder(1), fakeFolder(2))
        val items1 = listOf(fakeFavoriteItem(10))
        coEvery { favoriteRepo.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns folders
        coEvery { favoriteRepo.getFavoriteFolderData(any(), any(), any(), any()) } returns
            fakeFavoriteFolderData(items1, hasMore = false)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentFolderId).isEqualTo(1)
        assertThat(vm.uiState.value.favoriteItems).hasSize(1)

        val items2 = listOf(fakeFavoriteItem(20), fakeFavoriteItem(21))
        coEvery { favoriteRepo.getFavoriteFolderData(2, any(), any(), any()) } returns
            fakeFavoriteFolderData(items2, hasMore = false)

        vm.loadFavoriteItems(2, forceRefresh = true)
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentFolderId).isEqualTo(2)
        assertThat(vm.uiState.value.favoriteItems).hasSize(2)
        assertThat(vm.uiState.value.favoriteItems[0].id).isEqualTo(20)
    }

    @Test
    fun `loadFavoriteFolders sets error on failure`() = runTest(testDispatcher) {
        coEvery { favoriteRepo.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } throws
            RuntimeException("error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.favoriteFolders).isEmpty()
        assertThat(vm.uiState.value.favoriteError).isTrue()
    }

    // endregion

    // region FollowingSeason

    @Test
    fun `loadFollowingSeasons loads items with pagination`() = runTest(testDispatcher) {
        val seasons = listOf(fakeFollowingSeason(1), fakeFollowingSeason(2))
        coEvery { seasonRepo.getFollowingSeasons(any(), any(), any(), any(), any()) } returns
            fakeFollowingSeasonData(seasons, total = 5)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.followingSeasons).hasSize(2)
        assertThat(vm.uiState.value.followingHasMore).isTrue()
    }

    @Test
    fun `loadFollowingSeasons stops when all loaded`() = runTest(testDispatcher) {
        val seasons = listOf(fakeFollowingSeason(1), fakeFollowingSeason(2))
        coEvery { seasonRepo.getFollowingSeasons(any(), any(), any(), any(), any()) } returns
            fakeFollowingSeasonData(seasons, total = 2)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.followingSeasons).hasSize(2)
        assertThat(vm.uiState.value.followingHasMore).isFalse()
    }

    @Test
    fun `loadFollowingSeasons sets error on failure`() = runTest(testDispatcher) {
        coEvery { seasonRepo.getFollowingSeasons(any(), any(), any(), any(), any()) } throws
            RuntimeException("error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.followingSeasons).isEmpty()
        assertThat(vm.uiState.value.followingError).isTrue()
    }

    @Test
    fun `setFollowingFilter resets and reloads with new filter`() = runTest(testDispatcher) {
        val seasons1 = listOf(fakeFollowingSeason(1))
        coEvery { seasonRepo.getFollowingSeasons(any(), any(), any(), any(), any()) } returns
            fakeFollowingSeasonData(seasons1, total = 1)

        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.followingSeasons).hasSize(1)

        val seasons2 = listOf(fakeFollowingSeason(10), fakeFollowingSeason(11))
        coEvery { seasonRepo.getFollowingSeasons(any(), any(), any(), any(), any()) } returns
            fakeFollowingSeasonData(seasons2, total = 2)

        vm.setFollowingFilter(FollowingSeasonType.Cinema, FollowingSeasonStatus.Watching)
        advanceUntilIdle()

        assertThat(vm.uiState.value.followingType).isEqualTo(FollowingSeasonType.Cinema)
        assertThat(vm.uiState.value.followingStatus).isEqualTo(FollowingSeasonStatus.Watching)
        assertThat(vm.uiState.value.followingSeasons).hasSize(2)
    }

    // endregion

    // region refresh(tab)

    @Test
    fun `refresh dispatches to correct tab method`() = runTest(testDispatcher) {
        val items = listOf(fakeToViewItem(1))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(items)

        val vm = createViewModel()
        advanceUntilIdle()

        val newItems = listOf(fakeToViewItem(99))
        coEvery { toViewRepo.getToView(any(), any()) } returns fakeToViewData(newItems)

        vm.refresh(dev.frost819.newbv.data.datastore.PersonalTopNavItem.ToView)
        advanceUntilIdle()

        assertThat(vm.uiState.value.toViewItems[0].oid).isEqualTo(99)
    }

    // endregion
}
