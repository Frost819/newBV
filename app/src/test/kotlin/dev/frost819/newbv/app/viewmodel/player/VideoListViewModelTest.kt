package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.user.Author
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import dev.frost819.newbv.biliapi.entity.video.UserActions
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
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
import java.util.Date

/**
 * [VideoListViewModel] 的单元测试。
 *
 * 验证分集列表查找逻辑：下一集、上一集查找，
 * 以及与 [VideoInfoRepository] 的状态同步。
 */
class VideoListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var videoInfoRepository: VideoInfoRepository
    private lateinit var videoDetailRepository: VideoDetailRepository
    private lateinit var viewModel: VideoListViewModel

    private val sampleVideoList =
        listOf(
            VideoListItem(aid = 100, cid = 1001, title = "第一集"),
            VideoListItem(aid = 200, cid = 2001, title = "第二集"),
            VideoListItem(aid = 300, cid = 3001, title = "第三集"),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        videoDetailRepository = mockk()
        videoInfoRepository = VideoInfoRepository(videoDetailRepository)
        viewModel = VideoListViewModel(videoInfoRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.videoListState.value
        assertThat(state.videoList).isEmpty()
        assertThat(state.relatedVideos).isEmpty()
    }

    @Test
    fun `findNextVideo returns next video when not last`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1001)
            assertThat(next).isNotNull()
            assertThat(next!!.aid).isEqualTo(200)
            assertThat(next.cid).isEqualTo(2001)
        }

    @Test
    fun `findNextVideo returns null when already last`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val next = viewModel.findNextVideo(currentAid = 300, currentCid = 3001)
            assertThat(next).isNull()
        }

    @Test
    fun `findNextVideo returns null when current not in list`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val next = viewModel.findNextVideo(currentAid = 999, currentCid = 9999)
            assertThat(next).isNull()
        }

    @Test
    fun `findNextVideo returns null when list is empty`() =
        runTest(testDispatcher) {
            val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1001)
            assertThat(next).isNull()
        }

    @Test
    fun `findPreviousVideo returns previous video when not first`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 200, currentCid = 2001)
            assertThat(prev).isNotNull()
            assertThat(prev!!.aid).isEqualTo(100)
            assertThat(prev.cid).isEqualTo(1001)
        }

    @Test
    fun `findPreviousVideo returns null when already first`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 100, currentCid = 1001)
            assertThat(prev).isNull()
        }

    @Test
    fun `findPreviousVideo returns null when list is empty`() =
        runTest(testDispatcher) {
            val prev = viewModel.findPreviousVideo(currentAid = 100, currentCid = 1001)
            assertThat(prev).isNull()
        }

    @Test
    fun `videoListState syncs when repository updates`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val state = viewModel.videoListState.value
            assertThat(state.videoList).hasSize(3)
            assertThat(state.videoList[0].title).isEqualTo("第一集")
        }

    @Test
    fun `findNextVideo returns next UGC page within same video`() =
        runTest(testDispatcher) {
            val videoList =
                listOf(
                    VideoListItem(
                        aid = 100,
                        cid = 1001,
                        title = "视频1",
                        ugcPages =
                            listOf(
                                VideoPage(cid = 1001, index = 1, title = "P1", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1002, index = 2, title = "P2", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1003, index = 3, title = "P3", duration = 600, dimension = mockk()),
                            ),
                    ),
                )
            videoInfoRepository.updateVideoList(videoList)
            advanceUntilIdle()

            val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1001)
            assertThat(next).isNotNull()
            assertThat(next!!.aid).isEqualTo(100)
            assertThat(next.cid).isEqualTo(1002)
            assertThat(next.title).isEqualTo("P2")
        }

    @Test
    fun `findNextVideo returns next video when at last UGC page`() =
        runTest(testDispatcher) {
            val videoList =
                listOf(
                    VideoListItem(
                        aid = 100,
                        cid = 1001,
                        title = "视频1",
                        ugcPages =
                            listOf(
                                VideoPage(cid = 1001, index = 1, title = "P1", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1002, index = 2, title = "P2", duration = 600, dimension = mockk()),
                            ),
                    ),
                    VideoListItem(aid = 200, cid = 2001, title = "视频2"),
                )
            videoInfoRepository.updateVideoList(videoList)
            advanceUntilIdle()

            val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1002)
            assertThat(next).isNotNull()
            assertThat(next!!.aid).isEqualTo(200)
        }

    @Test
    fun `findPreviousVideo returns previous UGC page within same video`() =
        runTest(testDispatcher) {
            val videoList =
                listOf(
                    VideoListItem(
                        aid = 100,
                        cid = 1001,
                        title = "视频1",
                        ugcPages =
                            listOf(
                                VideoPage(cid = 1001, index = 1, title = "P1", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1002, index = 2, title = "P2", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1003, index = 3, title = "P3", duration = 600, dimension = mockk()),
                            ),
                    ),
                )
            videoInfoRepository.updateVideoList(videoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 100, currentCid = 1003)
            assertThat(prev).isNotNull()
            assertThat(prev!!.aid).isEqualTo(100)
            assertThat(prev.cid).isEqualTo(1002)
            assertThat(prev.title).isEqualTo("P2")
        }

    @Test
    fun `findPreviousVideo returns last page of previous video when at first page`() =
        runTest(testDispatcher) {
            val videoList =
                listOf(
                    VideoListItem(
                        aid = 100,
                        cid = 1001,
                        title = "视频1",
                        ugcPages =
                            listOf(
                                VideoPage(cid = 1001, index = 1, title = "P1", duration = 600, dimension = mockk()),
                                VideoPage(cid = 1002, index = 2, title = "P2", duration = 600, dimension = mockk()),
                            ),
                    ),
                    VideoListItem(aid = 200, cid = 2001, title = "视频2"),
                )
            videoInfoRepository.updateVideoList(videoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 200, currentCid = 2001)
            assertThat(prev).isNotNull()
            assertThat(prev!!.aid).isEqualTo(100)
            assertThat(prev.cid).isEqualTo(1002)
            assertThat(prev.title).isEqualTo("P2")
        }

    @Test
    fun `findPreviousVideo returns previous video when no UGC pages`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 200, currentCid = 2001)
            assertThat(prev).isNotNull()
            assertThat(prev!!.aid).isEqualTo(100)
            assertThat(prev.cid).isEqualTo(1001)
        }

    @Test
    fun `findPreviousVideo returns null when current not in list`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(sampleVideoList)
            advanceUntilIdle()

            val prev = viewModel.findPreviousVideo(currentAid = 999, currentCid = 9999)
            assertThat(prev).isNull()
        }

    @Test
    fun `setCurrentAid sets current aid`() =
        runTest(testDispatcher) {
            viewModel.setCurrentAid(42L)
            assertThat(viewModel.videoListState.value.videoList).isEmpty()
        }

    private fun fakeRelatedVideo(aid: Long): RelatedVideo =
        RelatedVideo(
            aid = aid,
            cid = aid * 10,
            cover = "",
            title = "相关视频$aid",
            duration = 60,
            author = Author(mid = 1L, name = "UP", face = ""),
            jumpToSeason = false,
            epid = null,
            view = 0,
            danmaku = 0,
        )

    private fun fakeVideoDetail(
        aid: Long,
        relatedVideos: List<RelatedVideo> = emptyList(),
    ): VideoDetail =
        VideoDetail(
            bvid = "BV$aid",
            aid = aid,
            cid = aid * 10,
            cover = "",
            title = "视频$aid",
            publishDate = Date(0),
            description = "",
            stat = VideoDetail.Stat(0, 0, 0, 0, 0, 0, 0, 0),
            author = Author(mid = aid, name = "UP$aid", face = ""),
            pages = emptyList(),
            ugcSeason = null,
            relatedVideos = relatedVideos,
            redirectToEp = false,
            epid = null,
            argueTip = null,
            tags = emptyList(),
            userActions = UserActions(),
            history = VideoDetail.History(0, 0),
        )

    @Test
    fun `related videos follow current aid after switching video`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoDetail(
                fakeVideoDetail(aid = 1, relatedVideos = listOf(fakeRelatedVideo(11))),
            )
            viewModel.setCurrentAid(1L)
            advanceUntilIdle()
            val firstRelatedAids =
                viewModel.videoListState.value.relatedVideos
                    .map { it.aid }
            assertThat(firstRelatedAids).containsExactly(11L)

            // 播放器内切到 B：详情与相关视频都应换成 B 的
            videoInfoRepository.updateVideoDetail(
                fakeVideoDetail(aid = 2, relatedVideos = listOf(fakeRelatedVideo(21), fakeRelatedVideo(22))),
            )
            viewModel.setCurrentAid(2L)
            advanceUntilIdle()

            val secondRelatedAids =
                viewModel.videoListState.value.relatedVideos
                    .map { it.aid }
            assertThat(secondRelatedAids).containsExactly(21L, 22L).inOrder()
        }

    @Test
    fun `related videos stay empty when loaded detail belongs to another video`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoDetail(
                fakeVideoDetail(aid = 1, relatedVideos = listOf(fakeRelatedVideo(11))),
            )
            viewModel.setCurrentAid(2L)
            advanceUntilIdle()

            // 不得把视频 1 的相关视频串到视频 2
            assertThat(viewModel.videoListState.value.relatedVideos).isEmpty()
        }

    @Test
    fun `switching aid clears old related videos until new detail arrives`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoDetail(
                fakeVideoDetail(aid = 1, relatedVideos = listOf(fakeRelatedVideo(11))),
            )
            viewModel.setCurrentAid(1L)
            advanceUntilIdle()
            assertThat(viewModel.videoListState.value.relatedVideos).isNotEmpty()

            // 切到 B 但 B 的详情还没返回：先清空，避免显示 A 的列表
            viewModel.setCurrentAid(2L)
            advanceUntilIdle()
            assertThat(viewModel.videoListState.value.relatedVideos).isEmpty()
        }

    @Test
    fun `empty video list in repository does not update state`() =
        runTest(testDispatcher) {
            videoInfoRepository.updateVideoList(emptyList())
            advanceUntilIdle()

            assertThat(viewModel.videoListState.value.videoList).isEmpty()
        }
}
