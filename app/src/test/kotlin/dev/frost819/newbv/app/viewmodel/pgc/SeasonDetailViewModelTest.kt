package dev.frost819.newbv.app.viewmodel.pgc

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.data.VideoSharedState
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.season.Episode
import dev.frost819.newbv.biliapi.entity.video.season.PgcSeason
import dev.frost819.newbv.biliapi.entity.video.season.SeasonDetail
import dev.frost819.newbv.biliapi.entity.video.season.Section
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
 * [SeasonDetailViewModel] 的单元测试。
 *
 * 验证番剧详情加载、追番操作、播放跳转逻辑。
 */
class SeasonDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var videoDetailRepository: VideoDetailRepository
    private lateinit var userRepository: UserRepository
    private lateinit var videoInfoRepository: VideoInfoRepository
    private lateinit var sharedState: MutableStateFlow<VideoSharedState?>
    private lateinit var viewModel: SeasonDetailViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        videoDetailRepository = mockk()
        userRepository = mockk()
        videoInfoRepository = mockk()
        sharedState = MutableStateFlow(null)
        every { videoInfoRepository.videoSharedState } returns sharedState
        every { videoInfoRepository.updateVideoList(any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(seasonId: Long = 100L): SeasonDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("seasonId" to seasonId))
        return SeasonDetailViewModel(
            videoDetailRepository = videoDetailRepository,
            userRepository = userRepository,
            videoInfoRepository = videoInfoRepository,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun fakeSeasonDetail(
        seasonId: Int = 100,
        follow: Boolean = false,
        progress: SeasonDetail.UserStatus.Progress? = null,
    ) = SeasonDetail(
        title = "测试番剧",
        originTitle = null,
        styles = listOf("热血", "奇幻"),
        cover = "https://example.com/cover.jpg",
        description = "这是测试番剧简介",
        subType = 1,
        seasonId = seasonId,
        userStatus =
            SeasonDetail.UserStatus(
                follow = follow,
                pay = false,
                progress = progress,
            ),
        publish = SeasonDetail.Publish(isPublished = true, publishDate = "2024-01"),
        newEpDesc = "第1话",
        seasons =
            listOf(
                PgcSeason(
                    seasonId = 100,
                    title = "第一季",
                    shortTitle = "第一季",
                    cover = "",
                    horizontalCover = null,
                ),
                PgcSeason(
                    seasonId = 200,
                    title = "第二季",
                    shortTitle = "第二季",
                    cover = "",
                    horizontalCover = null,
                ),
            ),
        episodes =
            listOf(
                Episode(
                    id = 1,
                    aid = 10001L,
                    bvid = "BV10001",
                    cid = 20001L,
                    epid = 1001,
                    title = "第1话",
                    longTitle = "第一话",
                    cover = "https://example.com/ep1.jpg",
                    duration = 1440,
                    dimension = Dimension(1920, 1080),
                ),
                Episode(
                    id = 2,
                    aid = 10002L,
                    bvid = "BV10002",
                    cid = 20002L,
                    epid = 1002,
                    title = "第2话",
                    longTitle = "第二话",
                    cover = "https://example.com/ep2.jpg",
                    duration = 1440,
                    dimension = Dimension(1920, 1080),
                ),
            ),
        sections = emptyList(),
    )

    @Test
    fun `init loads season detail successfully`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(follow = true)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.seasonDetail).isNotNull()
            assertThat(state.seasonDetail?.title).isEqualTo("测试番剧")
            assertThat(state.loading).isFalse()
            assertThat(state.error).isFalse()
            assertThat(state.isFollowing).isTrue()
        }

    @Test
    fun `init sets error on network failure`() =
        runTest(testDispatcher) {
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } throws IOException("network error")

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.seasonDetail).isNull()
            assertThat(state.loading).isFalse()
            assertThat(state.error).isTrue()
        }

    @Test
    fun `toggleFollow calls addSeasonFollow when not following`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(follow = false)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail
            coEvery { userRepository.addSeasonFollow(any(), any()) } returns "追番成功"

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.toggleFollow()
            advanceUntilIdle()

            coVerify { userRepository.addSeasonFollow(seasonId = 100, any()) }
            assertThat(viewModel.uiState.value.isFollowing).isTrue()
        }

    @Test
    fun `toggleFollow calls delSeasonFollow when following`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(follow = true)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail
            coEvery { userRepository.delSeasonFollow(any(), any()) } returns "取消追番成功"

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.toggleFollow()
            advanceUntilIdle()

            coVerify { userRepository.delSeasonFollow(seasonId = 100, any()) }
            assertThat(viewModel.uiState.value.isFollowing).isFalse()
        }

    @Test
    fun `onPlay emits NavigateToPlayer with first episode when no progress`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(progress = null)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlay()
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(SeasonDetailUiEffect.NavigateToPlayer::class.java)
                val navEffect = effect as SeasonDetailUiEffect.NavigateToPlayer
                assertThat(navEffect.aid).isEqualTo(10001L)
                assertThat(navEffect.cid).isEqualTo(20001L)
                assertThat(navEffect.epid).isEqualTo(1001)
            }
        }

    @Test
    fun `onPlay emits NavigateToPlayer with last watched episode`() =
        runTest(testDispatcher) {
            val detail =
                fakeSeasonDetail(
                    progress =
                        SeasonDetail.UserStatus.Progress(
                            lastEpId = 1002,
                            lastEpIndex = "第2话",
                            lastTime = 300,
                        ),
                )
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlay()
                advanceUntilIdle()

                val effect = awaitItem()
                val navEffect = effect as SeasonDetailUiEffect.NavigateToPlayer
                assertThat(navEffect.aid).isEqualTo(10002L)
                assertThat(navEffect.cid).isEqualTo(20002L)
                assertThat(navEffect.epid).isEqualTo(1002)
            }
        }

    @Test
    fun `onSwitchSeason loads new season detail`() =
        runTest(testDispatcher) {
            val detail1 = fakeSeasonDetail(seasonId = 100, follow = true)
            val detail2 = fakeSeasonDetail(seasonId = 200, follow = false)
            coEvery { videoDetailRepository.getPgcVideoDetail(null, 100, any()) } returns detail1
            coEvery { videoDetailRepository.getPgcVideoDetail(null, 200, any()) } returns detail2

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.seasonDetail
                    ?.seasonId,
            ).isEqualTo(100)
            assertThat(viewModel.uiState.value.isFollowing).isTrue()

            viewModel.onSwitchSeason(200)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.seasonDetail
                    ?.seasonId,
            ).isEqualTo(200)
            assertThat(viewModel.uiState.value.isFollowing).isFalse()
        }

    @Test
    fun `toggleFollow emits toast on failure`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(follow = false)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail
            coEvery { userRepository.addSeasonFollow(any(), any()) } throws RuntimeException("follow error")

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.toggleFollow()
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(SeasonDetailUiEffect.ShowToast::class.java)
                assertThat((effect as SeasonDetailUiEffect.ShowToast).message).contains("追番失败")
            }
        }

    @Test
    fun `toggleFollow is no-op when detail is null`() =
        runTest(testDispatcher) {
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } throws IOException("error")

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.toggleFollow()
            advanceUntilIdle()

            coVerify(exactly = 0) { userRepository.addSeasonFollow(any(), any()) }
            coVerify(exactly = 0) { userRepository.delSeasonFollow(any(), any()) }
        }

    @Test
    fun `toggleFollow with toast message emits ShowToast`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(follow = false)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail
            coEvery { userRepository.addSeasonFollow(any(), any()) } returns "追番成功！"

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.toggleFollow()
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(SeasonDetailUiEffect.ShowToast::class.java)
                assertThat((effect as SeasonDetailUiEffect.ShowToast).message).isEqualTo("追番成功！")
            }
        }

    @Test
    fun `onPlayEpisode emits NavigateToPlayer`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail()
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlayEpisode(detail.episodes[1])
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(SeasonDetailUiEffect.NavigateToPlayer::class.java)
                val navEffect = effect as SeasonDetailUiEffect.NavigateToPlayer
                assertThat(navEffect.aid).isEqualTo(10002L)
                assertThat(navEffect.cid).isEqualTo(20002L)
            }
        }

    @Test
    fun `onPlay with progress but episode not found falls back to first`() =
        runTest(testDispatcher) {
            val detail =
                fakeSeasonDetail(
                    progress =
                        SeasonDetail.UserStatus.Progress(
                            lastEpId = 9999,
                            lastEpIndex = "不存在",
                            lastTime = 300,
                        ),
                )
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlay()
                advanceUntilIdle()

                val effect = awaitItem()
                val navEffect = effect as SeasonDetailUiEffect.NavigateToPlayer
                assertThat(navEffect.aid).isEqualTo(10001L)
            }
        }

    @Test
    fun `onPlay with no episodes does nothing`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail().copy(episodes = emptyList())
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlay()
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    @Test
    fun `onPlay is no-op when detail is null`() =
        runTest(testDispatcher) {
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } throws IOException("error")

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlay()
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    @Test
    fun `onSwitchSeason error sets error state`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail(seasonId = 100, follow = true)
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } throws IOException("switch error")
            viewModel.onSwitchSeason(200)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.error).isTrue()
            assertThat(viewModel.uiState.value.loading).isFalse()
        }

    @Test
    fun `onPlay finds episode in sections`() =
        runTest(testDispatcher) {
            val sectionEpisode =
                Episode(
                    id = 3,
                    aid = 10003L,
                    bvid = "BV10003",
                    cid = 20003L,
                    epid = 1003,
                    title = "特别篇",
                    longTitle = "SP1",
                    cover = "",
                    duration = 600,
                    dimension = Dimension(1920, 1080),
                )
            val detail =
                fakeSeasonDetail().copy(
                    sections =
                        listOf(
                            Section(id = 1, title = "特别篇", episodes = listOf(sectionEpisode)),
                        ),
                )
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.onPlayEpisode(sectionEpisode)
                advanceUntilIdle()

                val effect = awaitItem()
                val navEffect = effect as SeasonDetailUiEffect.NavigateToPlayer
                assertThat(navEffect.aid).isEqualTo(10003L)
                assertThat(navEffect.epid).isEqualTo(1003)
            }
        }

    @Test
    fun `init seeds historyLastPlayed from server progress`() =
        runTest(testDispatcher) {
            val detail =
                fakeSeasonDetail(
                    progress =
                        SeasonDetail.UserStatus.Progress(
                            lastEpId = 1002,
                            lastEpIndex = "第2话",
                            lastTime = 300,
                        ),
                )
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.historyLastPlayedCid).isEqualTo(20002L)
            assertThat(viewModel.uiState.value.historyLastPlayedTime).isEqualTo(300)
        }

    @Test
    fun `shared playback history updates lastPlayed when cid belongs to season`() =
        runTest(testDispatcher) {
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns fakeSeasonDetail()

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            sharedState.value = VideoSharedState(aid = 10002L, lastPlayedCid = 20002L, lastPlayedTime = 120)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.historyLastPlayedCid).isEqualTo(20002L)
            assertThat(viewModel.uiState.value.historyLastPlayedTime).isEqualTo(120)
        }

    @Test
    fun `shared playback history ignores cid not in season`() =
        runTest(testDispatcher) {
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns fakeSeasonDetail()

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            sharedState.value = VideoSharedState(aid = 999L, lastPlayedCid = 99999L, lastPlayedTime = 60)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.historyLastPlayedCid).isEqualTo(0L)
        }

    @Test
    fun `onPlayEpisode populates player video list from season episodes`() =
        runTest(testDispatcher) {
            val detail = fakeSeasonDetail()
            coEvery { videoDetailRepository.getPgcVideoDetail(any(), any(), any()) } returns detail

            viewModel = createViewModel(seasonId = 100L)
            advanceUntilIdle()

            viewModel.onPlayEpisode(detail.episodes[1])
            advanceUntilIdle()

            val slot = slot<List<VideoListItem>>()
            verify { videoInfoRepository.updateVideoList(capture(slot)) }
            assertThat(slot.captured).hasSize(2)
            assertThat(slot.captured[1].cid).isEqualTo(20002L)
            assertThat(slot.captured[1].epid).isEqualTo(1002)
            assertThat(slot.captured[1].seasonId).isEqualTo(100)
        }
}
