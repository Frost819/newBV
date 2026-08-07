package dev.frost819.newbv.app.viewmodel.detail

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.biliapi.entity.user.Author
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.UserActions
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.entity.video.season.Episode
import dev.frost819.newbv.biliapi.entity.video.season.Section
import dev.frost819.newbv.biliapi.entity.video.season.UgcSeason
import dev.frost819.newbv.biliapi.http.entity.video.OneClickTripleAction
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import dev.frost819.newbv.biliapi.repositories.CoinRepository
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.LikeRepository
import dev.frost819.newbv.biliapi.repositories.OneClickTripleActionRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import app.cash.turbine.test
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
import java.util.Date

/**
 * [VideoDetailViewModel] 的单元测试。
 *
 * 验证详情数据加载、点赞/投币/收藏操作、一键三连、超时/错误处理。
 * 使用 MockK mock 所有 Repository。
 */
class VideoDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var videoDetailRepository: VideoDetailRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var coinRepository: CoinRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var oneClickTripleActionRepository: OneClickTripleActionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var videoInfoRepository: VideoInfoRepository
    private lateinit var viewModel: VideoDetailViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        videoDetailRepository = mockk()
        likeRepository = mockk()
        coinRepository = mockk()
        favoriteRepository = mockk()
        oneClickTripleActionRepository = mockk()
        userRepository = mockk()
        videoInfoRepository = mockk(relaxed = true)

        mockkObject(Prefs)
        coEvery { Prefs.isLogin } returns false
    }

    @AfterEach
    fun tearDown() {
        io.mockk.unmockkObject(Prefs)
        Dispatchers.resetMain()
    }

    private fun createViewModel(aid: Long = 1L): VideoDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("aid" to aid))
        return VideoDetailViewModel(
            videoDetailRepository = videoDetailRepository,
            likeRepository = likeRepository,
            coinRepository = coinRepository,
            favoriteRepository = favoriteRepository,
            oneClickTripleActionRepository = oneClickTripleActionRepository,
            userRepository = userRepository,
            videoInfoRepository = videoInfoRepository,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun fakeVideoDetail(aid: Long = 1L) = VideoDetail(
        bvid = "BV$aid",
        aid = aid,
        cid = 100L,
        cover = "https://example.com/cover.jpg",
        title = "测试视频 $aid",
        publishDate = Date(1700000000000L),
        description = "这是测试视频描述",
        stat = VideoDetail.Stat(
            view = 10000,
            danmaku = 500,
            reply = 200,
            favorite = 300,
            coin = 100,
            share = 50,
            like = 800,
            historyRank = 0,
        ),
        author = Author(mid = 999L, name = "测试UP", face = ""),
        pages = listOf(
            VideoPage(cid = 100L, index = 1, title = "P1", duration = 600, dimension = mockk()),
        ),
        ugcSeason = null,
        relatedVideos = emptyList(),
        redirectToEp = false,
        epid = null,
        argueTip = null,
        tags = emptyList(),
        userActions = UserActions(),
        history = VideoDetail.History(0, 0),
    )

    @Test
    fun `init loads video detail successfully`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.detail).isNotNull()
        assertThat(state.detail?.title).isEqualTo("测试视频 1")
        assertThat(state.loading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `init sets error on network failure`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws IOException("network error")

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.detail).isNull()
        assertThat(state.error).isTrue()
        assertThat(state.loading).isFalse()
        assertThat(state.errorTip).contains("network error")
    }

    @Test
    fun `toggleLike updates liked state on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail().copy(
            userActions = UserActions(like = false, coin = false, favorite = false),
        )
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { likeRepository.updateVideoLiked(any(), any(), any()) } returns Unit

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLiked).isFalse()

        viewModel.toggleLike(true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLiked).isTrue()
    }

    @Test
    fun `toggleLike emits toast on failure`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { likeRepository.updateVideoLiked(any(), any(), any()) } throws Exception("already liked")

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.toggleLike(true)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("点赞失败")
        }
    }

    @Test
    fun `sendCoin updates coined state on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail().copy(
            userActions = UserActions(coin = false),
        )
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { coinRepository.sendVideoCoin(any(), any(), any()) } returns Unit

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isCoined).isFalse()

        viewModel.sendCoin()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isCoined).isTrue()
    }

    @Test
    fun `sendCoin emits toast on failure`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { coinRepository.sendVideoCoin(any(), any(), any()) } throws Exception("no coins")

        viewModel = createViewModel(aid = 1L)
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.sendCoin()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("投币失败")
        }
    }

    @Test
    fun `aid property matches route parameter`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns fakeVideoDetail()

        viewModel = createViewModel(aid = 999L)
        advanceUntilIdle()

        assertThat(viewModel.aid).isEqualTo(999L)
        coVerify { videoDetailRepository.getVideoDetail(aid = 999L, any()) }
    }

    @Test
    fun `toggleFollow follows UP on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { userRepository.followUser(any()) } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFollow()
        advanceUntilIdle()

        coVerify { userRepository.followUser(mid = 999L) }
        assertThat(viewModel.uiState.value.isFollowing).isTrue()
    }

    @Test
    fun `toggleFollow unfollows UP on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { Prefs.isLogin } returns true
        coEvery { userRepository.checkIsFollowing(any()) } returns true
        coEvery { userRepository.unfollowUser(any()) } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFollowing).isTrue()

        viewModel.toggleFollow()
        advanceUntilIdle()

        coVerify { userRepository.unfollowUser(mid = 999L) }
        assertThat(viewModel.uiState.value.isFollowing).isFalse()
    }

    @Test
    fun `toggleFollow emits toast on failure`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { userRepository.followUser(any()) } throws RuntimeException("already followed")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("关注失败")
        }
    }

    @Test
    fun `toggleFollow is no-op when detail is null`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFollow()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.followUser(any()) }
        coVerify(exactly = 0) { userRepository.unfollowUser(any()) }
    }

    @Test
    fun `updateFavorite updates favorite state on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { favoriteRepository.updateVideoToFavoriteFolder(any(), any(), any(), any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateFavorite(listOf(1L, 2L))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFavorite).isTrue()
        assertThat(viewModel.uiState.value.videoFavoriteFolderIds).containsExactly(1L, 2L)
    }

    @Test
    fun `updateFavorite emits toast on failure`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { favoriteRepository.updateVideoToFavoriteFolder(any(), any(), any(), any()) } throws RuntimeException("fav error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.updateFavorite(listOf(1L))
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("收藏失败")
        }
    }

    @Test
    fun `toggleFavorite with default folder adds to favorite`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { Prefs.isLogin } returns true
        coEvery { favoriteRepository.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns listOf(
            FavoriteFolderMetadata(id = 10, fid = 10, mid = 1L, title = "默认收藏夹", cover = null, videoInThisFav = false, mediaCount = 5),
        )
        coEvery { favoriteRepository.updateVideoToFavoriteFolder(any(), any(), any(), any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFavorite).isTrue()
    }

    @Test
    fun `toggleFavorite without default folder emits toast`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { Prefs.isLogin } returns true
        coEvery { favoriteRepository.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns listOf(
            FavoriteFolderMetadata(id = 10, fid = 10, mid = 1L, title = "其他收藏夹", cover = null, videoInThisFav = false, mediaCount = 5),
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("未找到默认收藏夹")
        }
    }

    @Test
    fun `toggleFavorite unfavorites when already favorited`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail().copy(
            userActions = UserActions(favorite = true),
        )
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { Prefs.isLogin } returns true
        coEvery { favoriteRepository.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns listOf(
            FavoriteFolderMetadata(id = 10, fid = 10, mid = 1L, title = "默认收藏夹", cover = null, videoInThisFav = true, mediaCount = 5),
        )
        coEvery { favoriteRepository.updateVideoToFavoriteFolder(any(), any(), any(), any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFavorite).isTrue()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFavorite).isFalse()
    }

    @Test
    fun `oneClickTripleAction updates all states on success`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { oneClickTripleActionRepository.sendVideoOneClickTripleAction(any(), any()) } returns
            OneClickTripleAction(like = true, coin = true, fav = true)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.oneClickTripleAction()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).isEqualTo("一键三连")
        }

        assertThat(viewModel.uiState.value.isLiked).isTrue()
        assertThat(viewModel.uiState.value.isCoined).isTrue()
        assertThat(viewModel.uiState.value.isFavorite).isTrue()
    }

    @Test
    fun `oneClickTripleAction emits toast on failure`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { oneClickTripleActionRepository.sendVideoOneClickTripleAction(any(), any()) } throws RuntimeException("triple error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.oneClickTripleAction()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(VideoDetailUiEffect.ShowToast::class.java)
            assertThat((effect as VideoDetailUiEffect.ShowToast).message).contains("一键三连失败")
        }
    }

    @Test
    fun `oneClickTripleAction is no-op when detail is null`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.oneClickTripleAction()
        advanceUntilIdle()

        coVerify(exactly = 0) { oneClickTripleActionRepository.sendVideoOneClickTripleAction(any(), any()) }
    }

    @Test
    fun `updateVideoList single video calls repository`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns fakeVideoDetail()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateVideoList(aid = 1L, cid = 100L, title = "测试视频 1")
        advanceUntilIdle()

        coVerify { videoInfoRepository.updateVideoList(any()) }
    }

    @Test
    fun `updateVideoList sectionIndex with ugcSeason episodes`() = runTest(testDispatcher) {
        val ugcSeason = UgcSeason(
            id = 1,
            title = "合集",
            cover = "",
            sections = listOf(
                Section(
                    id = 1,
                    title = "第一集",
                    episodes = listOf(
                        Episode(
                            id = 1, aid = 100L, bvid = "BV100", cid = 1000L, epid = 1,
                            title = "P1", longTitle = "第一话", cover = "", duration = 600,
                            dimension = Dimension(1920, 1080),
                        ),
                        Episode(
                            id = 2, aid = 200L, bvid = "BV200", cid = 2000L, epid = 2,
                            title = "P2", longTitle = "第二话", cover = "", duration = 600,
                            dimension = Dimension(1920, 1080),
                        ),
                    ),
                ),
            ),
        )
        val detail = fakeVideoDetail().copy(ugcSeason = ugcSeason)
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateVideoList(sectionIndex = 0)
        advanceUntilIdle()

        coVerify { videoInfoRepository.updateVideoList(any()) }
    }

    @Test
    fun `updateVideoList sectionIndex with null ugcSeason does nothing`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns fakeVideoDetail()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateVideoList(sectionIndex = 0)
        advanceUntilIdle()

        coVerify(exactly = 0) { videoInfoRepository.updateVideoList(any()) }
    }

    @Test
    fun `loadVideoDetail when logged in fetches favorite folders and following`() = runTest(testDispatcher) {
        val detail = fakeVideoDetail()
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns detail
        coEvery { Prefs.isLogin } returns true
        coEvery { Prefs.uid } returns 123L
        coEvery { favoriteRepository.getAllFavoriteFolderMetadataList(any(), any(), any(), any()) } returns listOf(
            FavoriteFolderMetadata(id = 1, fid = 1, mid = 123L, title = "收藏夹1", cover = null, videoInThisFav = true, mediaCount = 10),
        )
        coEvery { userRepository.checkIsFollowing(any()) } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.favoriteFolders).hasSize(1)
        assertThat(viewModel.uiState.value.videoFavoriteFolderIds).contains(1L)
        assertThat(viewModel.uiState.value.isFollowing).isTrue()
    }

    @Test
    fun `loadVideoDetail retry after error succeeds`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("first error")

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isTrue()

        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } returns fakeVideoDetail()

        viewModel.loadVideoDetail()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isFalse()
        assertThat(viewModel.uiState.value.detail).isNotNull()
    }

    @Test
    fun `toggleLike is no-op when detail is null`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleLike(true)
        advanceUntilIdle()

        coVerify(exactly = 0) { likeRepository.updateVideoLiked(any(), any(), any()) }
    }

    @Test
    fun `sendCoin is no-op when detail is null`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendCoin()
        advanceUntilIdle()

        coVerify(exactly = 0) { coinRepository.sendVideoCoin(any(), any(), any()) }
    }

    @Test
    fun `updateFavorite is no-op when detail is null`() = runTest(testDispatcher) {
        coEvery { videoDetailRepository.getVideoDetail(any(), any()) } throws RuntimeException("error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateFavorite(listOf(1L))
        advanceUntilIdle()

        coVerify(exactly = 0) { favoriteRepository.updateVideoToFavoriteFolder(any(), any(), any(), any()) }
    }
}
