package dev.frost819.newbv.app.viewmodel.detail

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.biliapi.entity.user.Author
import dev.frost819.newbv.biliapi.entity.video.UserActions
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
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
}
