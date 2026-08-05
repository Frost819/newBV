package dev.frost819.newbv.app.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
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
 * [VideoInfoRepository] 的单元测试。
 *
 * 验证 StateFlow 传播：更新视频列表后，观察者能收到最新数据。
 */
class VideoInfoRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var videoDetailRepository: VideoDetailRepository
    private lateinit var repository: VideoInfoRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        videoDetailRepository = mockk()
        repository = VideoInfoRepository(videoDetailRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty videoList`() {
        assertThat(repository.videoList.value).isEmpty()
    }

    @Test
    fun `updateVideoList propagates to StateFlow`() = runTest(testDispatcher) {
        val items = listOf(
            VideoListItem(aid = 1, cid = 10, title = "视频1"),
            VideoListItem(aid = 2, cid = 20, title = "视频2"),
        )

        repository.updateVideoList(items)
        advanceUntilIdle()

        assertThat(repository.videoList.value).hasSize(2)
        assertThat(repository.videoList.value[0].title).isEqualTo("视频1")
    }

    @Test
    fun `videoList StateFlow emits on update`() = runTest(testDispatcher) {
        repository.videoList.test {
            assertThat(awaitItem()).isEmpty()

            val items = listOf(VideoListItem(aid = 1, cid = 10, title = "视频1"))
            repository.updateVideoList(items)
            advanceUntilIdle()

            val emitted = awaitItem()
            assertThat(emitted).hasSize(1)
            assertThat(emitted[0].aid).isEqualTo(1)
        }
    }

    @Test
    fun `reset clears all state`() = runTest(testDispatcher) {
        val items = listOf(VideoListItem(aid = 1, cid = 10, title = "视频1"))
        repository.updateVideoList(items)
        advanceUntilIdle()
        assertThat(repository.videoList.value).isNotEmpty()

        repository.reset()
        advanceUntilIdle()

        assertThat(repository.videoList.value).isEmpty()
    }
}
