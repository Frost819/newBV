package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.Dimension
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

    private val sampleVideoList = listOf(
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
    fun `findNextVideo returns next video when not last`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1001)
        assertThat(next).isNotNull()
        assertThat(next!!.aid).isEqualTo(200)
        assertThat(next.cid).isEqualTo(2001)
    }

    @Test
    fun `findNextVideo returns null when already last`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val next = viewModel.findNextVideo(currentAid = 300, currentCid = 3001)
        assertThat(next).isNull()
    }

    @Test
    fun `findNextVideo returns null when current not in list`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val next = viewModel.findNextVideo(currentAid = 999, currentCid = 9999)
        assertThat(next).isNull()
    }

    @Test
    fun `findNextVideo returns null when list is empty`() = runTest(testDispatcher) {
        val next = viewModel.findNextVideo(currentAid = 100, currentCid = 1001)
        assertThat(next).isNull()
    }

    @Test
    fun `findPreviousVideo returns previous video when not first`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val prev = viewModel.findPreviousVideo(currentAid = 200, currentCid = 2001)
        assertThat(prev).isNotNull()
        assertThat(prev!!.aid).isEqualTo(100)
        assertThat(prev.cid).isEqualTo(1001)
    }

    @Test
    fun `findPreviousVideo returns null when already first`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val prev = viewModel.findPreviousVideo(currentAid = 100, currentCid = 1001)
        assertThat(prev).isNull()
    }

    @Test
    fun `findPreviousVideo returns null when list is empty`() = runTest(testDispatcher) {
        val prev = viewModel.findPreviousVideo(currentAid = 100, currentCid = 1001)
        assertThat(prev).isNull()
    }

    @Test
    fun `videoListState syncs when repository updates`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val state = viewModel.videoListState.value
        assertThat(state.videoList).hasSize(3)
        assertThat(state.videoList[0].title).isEqualTo("第一集")
    }

    @Test
    fun `findNextVideo returns next UGC page within same video`() = runTest(testDispatcher) {
        val videoList = listOf(
            VideoListItem(
                aid = 100, cid = 1001, title = "视频1",
                ugcPages = listOf(
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
    fun `findNextVideo returns next video when at last UGC page`() = runTest(testDispatcher) {
        val videoList = listOf(
            VideoListItem(
                aid = 100, cid = 1001, title = "视频1",
                ugcPages = listOf(
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
    fun `findPreviousVideo returns previous UGC page within same video`() = runTest(testDispatcher) {
        val videoList = listOf(
            VideoListItem(
                aid = 100, cid = 1001, title = "视频1",
                ugcPages = listOf(
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
    fun `findPreviousVideo returns last page of previous video when at first page`() = runTest(testDispatcher) {
        val videoList = listOf(
            VideoListItem(
                aid = 100, cid = 1001, title = "视频1",
                ugcPages = listOf(
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
    fun `findPreviousVideo returns previous video when no UGC pages`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val prev = viewModel.findPreviousVideo(currentAid = 200, currentCid = 2001)
        assertThat(prev).isNotNull()
        assertThat(prev!!.aid).isEqualTo(100)
        assertThat(prev.cid).isEqualTo(1001)
    }

    @Test
    fun `findPreviousVideo returns null when current not in list`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(sampleVideoList)
        advanceUntilIdle()

        val prev = viewModel.findPreviousVideo(currentAid = 999, currentCid = 9999)
        assertThat(prev).isNull()
    }

    @Test
    fun `setCurrentAid sets current aid`() = runTest(testDispatcher) {
        viewModel.setCurrentAid(42L)
        assertThat(viewModel.videoListState.value.videoList).isEmpty()
    }

    @Test
    fun `empty video list in repository does not update state`() = runTest(testDispatcher) {
        videoInfoRepository.updateVideoList(emptyList())
        advanceUntilIdle()

        assertThat(viewModel.videoListState.value.videoList).isEmpty()
    }
}
