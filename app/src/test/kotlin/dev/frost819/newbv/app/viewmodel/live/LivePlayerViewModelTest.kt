package dev.frost819.newbv.app.viewmodel.live

import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.frost819.newbv.app.data.danmaku.DanmakuFilterRepository
import dev.frost819.newbv.app.ui.state.live.LivePlayerState
import dev.frost819.newbv.app.ui.state.live.LivePlayerUiState
import dev.frost819.newbv.app.util.VideoCapabilityProvider
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.repositories.LivePlayInfo
import dev.frost819.newbv.biliapi.repositories.LivePlayLine
import dev.frost819.newbv.biliapi.repositories.LiveQuality
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [LivePlayerViewModel] 的单元测试。
 *
 * 验证手动切线路逻辑：重新拉取流信息、越界 clamp、切画质重置线路、失败进错误态。
 * 通过反射设置 `_uiState` 与 `videoPlayer`，避免触发 `loadLive` 的 WebSocket 连接。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LivePlayerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var liveRepository: LiveRepository
    private lateinit var exoPlayerFactory: ExoPlayerFactory
    private lateinit var videoCapabilityProvider: VideoCapabilityProvider
    private lateinit var danmakuFilterRepository: DanmakuFilterRepository
    private lateinit var viewModel: LivePlayerViewModel
    private lateinit var mockPlayer: AbstractVideoPlayer

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        liveRepository = mockk(relaxed = true)
        exoPlayerFactory = mockk(relaxed = true)
        videoCapabilityProvider = mockk(relaxed = true)
        danmakuFilterRepository = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        viewModel =
            LivePlayerViewModel(
                liveRepository,
                exoPlayerFactory,
                videoCapabilityProvider,
                danmakuFilterRepository,
            )
        setVideoPlayer(mockPlayer)
    }

    @AfterEach
    fun tearDown() {
        runCatching { viewModel.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `changeLine reloads play info and plays target line`() =
        runTest(testDispatcher) {
            // Given
            updateUiState { it.copy(realRoomId = 123, currentQuality = 10000) }
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } returns
                LivePlayInfo(
                    currentQn = 10000,
                    qualities = listOf(LiveQuality(10000, "原画")),
                    lines = listOf(LivePlayLine(1, "url-1"), LivePlayLine(2, "url-2")),
                )

            // When
            viewModel.changeLine(2)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.currentLine).isEqualTo(2)
            assertThat(viewModel.uiState.value.availableLines).hasSize(2)
            verify { mockPlayer.playUrl("url-2") }
        }

    @Test
    fun `changeLine clamps to first line when order is out of range`() =
        runTest(testDispatcher) {
            // Given
            updateUiState { it.copy(realRoomId = 123, currentQuality = 10000) }
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } returns
                LivePlayInfo(
                    currentQn = 10000,
                    qualities = emptyList(),
                    lines = listOf(LivePlayLine(1, "url-1")),
                )

            // When
            viewModel.changeLine(5)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.currentLine).isEqualTo(1)
            verify { mockPlayer.playUrl("url-1") }
        }

    @Test
    fun `changeQuality resets line to first and reloads`() =
        runTest(testDispatcher) {
            // Given（画质以档位标识传递：qn * 100，蓝光 400 → 40000）
            updateUiState { it.copy(realRoomId = 123, currentQuality = 1000000, currentLine = 3) }
            coEvery { liveRepository.getLivePlayInfo(123, 40000) } returns
                LivePlayInfo(
                    currentQn = 400,
                    qualities = listOf(LiveQuality(400, "蓝光")),
                    lines = listOf(LivePlayLine(1, "url-1"), LivePlayLine(2, "url-2")),
                )

            // When
            viewModel.changeQuality(40000)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.currentQuality).isEqualTo(40000)
            assertThat(viewModel.uiState.value.currentLine).isEqualTo(1)
        }

    @Test
    fun `changeLine sets error state when reload fails`() =
        runTest(testDispatcher) {
            // Given
            updateUiState { it.copy(realRoomId = 123, currentQuality = 10000) }
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } throws RuntimeException("boom")

            // When
            viewModel.changeLine(2)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value.playerState
            assertThat(state).isInstanceOf(LivePlayerState.Error::class.java)
            assertThat((state as LivePlayerState.Error).message).isEqualTo("视频源加载失败，请稍后重试")
        }

    @Test
    fun `changeLine does nothing when realRoomId is zero`() =
        runTest(testDispatcher) {
            // When
            viewModel.changeLine(2)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { liveRepository.getLivePlayInfo(any(), any()) }
        }

    @Test
    fun `reload filters out HDR quality that is not decodable`() =
        runTest(testDispatcher) {
            // Given
            updateUiState { it.copy(realRoomId = 123, currentQuality = 10000) }
            every { videoCapabilityProvider.isDecodable(any()) } returns false
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } returns
                LivePlayInfo(
                    currentQn = 10000,
                    qualities =
                        listOf(
                            LiveQuality(
                                qn = 30000,
                                desc = "4K",
                                hdrType = 1,
                                hdrDesc = "HDR",
                                codecName = "hevc",
                                codecs = "hvc1.1.6.L120",
                                width = 3840,
                                height = 2160,
                            ),
                            LiveQuality(qn = 10000, desc = "原画"),
                        ),
                    lines = listOf(LivePlayLine(1, "url-1")),
                )

            // When
            viewModel.changeLine(1)
            advanceUntilIdle()

            // Then：不可解的 HDR 档从菜单中隐藏，SDR 档保留
            val qualities = viewModel.uiState.value.availableQualities
            assertThat(qualities.map { it.qn }).containsExactly(10000)
        }

    @Test
    fun `reload keeps HDR quality when decodable`() =
        runTest(testDispatcher) {
            // Given
            updateUiState { it.copy(realRoomId = 123, currentQuality = 10000) }
            every { videoCapabilityProvider.isDecodable(any()) } returns true
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } returns
                LivePlayInfo(
                    currentQn = 10000,
                    qualities =
                        listOf(
                            LiveQuality(
                                qn = 30000,
                                desc = "4K",
                                hdrType = 1,
                                hdrDesc = "HDR",
                                codecName = "hevc",
                                codecs = "hvc1.1.6.L120",
                                width = 3840,
                                height = 2160,
                            ),
                            LiveQuality(qn = 10000, desc = "原画"),
                        ),
                    lines = listOf(LivePlayLine(1, "url-1")),
                )

            // When
            viewModel.changeLine(1)
            advanceUntilIdle()

            // Then
            val qualities = viewModel.uiState.value.availableQualities
            assertThat(qualities.map { it.qn }).containsExactly(30000, 10000)
        }

    @Test
    fun `changeLine leaves error state while reloading`() =
        runTest(testDispatcher) {
            // Given: 上一次播放失败后用户手动换线路
            updateUiState {
                it.copy(
                    realRoomId = 123,
                    currentQuality = 10000,
                    playerState = LivePlayerState.Error("播放失败，请稍后重试"),
                    isBuffering = false,
                )
            }
            coEvery { liveRepository.getLivePlayInfo(123, 10000) } returns
                LivePlayInfo(
                    currentQn = 10000,
                    qualities = listOf(LiveQuality(10000, "原画")),
                    lines = listOf(LivePlayLine(1, "url-1"), LivePlayLine(2, "url-2")),
                )

            // When
            viewModel.changeLine(2)
            advanceUntilIdle()

            // Then: 错误态（连同文案）随状态切换消失，进入加载态
            val state = viewModel.uiState.value
            assertThat(state.playerState).isNotInstanceOf(LivePlayerState.Error::class.java)
            assertThat(state.playerState).isEqualTo(LivePlayerState.Loading)
        }

    // ── 播放器回调 ───────────────────────────────────────────

    @Test
    fun `onError sets error state with user facing message`() =
        runTest(testDispatcher) {
            // When
            getVideoPlayerListener().onError(RuntimeException("boom"))

            // Then
            val state = viewModel.uiState.value
            assertThat(state.playerState)
                .isEqualTo(LivePlayerState.Error("播放失败，请稍后重试"))
            assertThat(state.isBuffering).isFalse()
        }

    @Test
    fun `onPause after error keeps error state`() =
        runTest(testDispatcher) {
            // Given: 出错后 ExoPlayer 会立刻回调 isPlaying=false
            val listener = getVideoPlayerListener()
            listener.onError(RuntimeException("boom"))

            // When
            listener.onPause()

            // Then: 错误提示不能被暂停态覆盖
            assertThat(viewModel.uiState.value.playerState).isInstanceOf(LivePlayerState.Error::class.java)
        }

    @Test
    fun `onVideoDecodeUnsupported sets actionable error`() =
        runTest(testDispatcher) {
            // When
            getVideoPlayerListener().onVideoDecodeUnsupported()

            // Then
            val state = viewModel.uiState.value
            assertThat(state.playerState)
                .isEqualTo(LivePlayerState.Error("解码失败，请尝试切换画质或线路"))
            assertThat(state.isBuffering).isFalse()
        }

    @Test
    fun `onAudioDecodeUnsupported sets actionable error`() =
        runTest(testDispatcher) {
            // When
            getVideoPlayerListener().onAudioDecodeUnsupported()

            // Then
            val state = viewModel.uiState.value
            assertThat(state.playerState)
                .isEqualTo(LivePlayerState.Error("音频解码失败，请尝试切换线路"))
        }

    // ── UP 主信息 ─────────────────────────────────────────────

    @Test
    fun `withRoomInfo merges anchor info into ui state`() {
        val roomInfo =
            RoomInfoData(
                roomId = 1,
                shortId = 0,
                uid = 123L,
                liveStatus = 1,
                title = "直播间标题",
                areaV2Name = "虚拟主播",
                uname = "主播",
                face = "https://example.com/face.jpg",
            )

        val merged = LivePlayerUiState().withRoomInfo(roomInfo)

        assertThat(merged.title).isEqualTo("直播间标题")
        assertThat(merged.areaName).isEqualTo("虚拟主播")
        assertThat(merged.upMid).isEqualTo(123L)
        assertThat(merged.upName).isEqualTo("主播")
        assertThat(merged.upFace).isEqualTo("https://example.com/face.jpg")
    }

    @Test
    fun `withRoomInfo keeps route title when room title is blank`() {
        val roomInfo =
            RoomInfoData(
                roomId = 1,
                shortId = 0,
                uid = 123L,
                liveStatus = 1,
                title = "",
            )

        val merged = LivePlayerUiState(title = "路由标题").withRoomInfo(roomInfo)

        assertThat(merged.title).isEqualTo("路由标题")
    }

    @Test
    fun `withRoomInfo prefers anchor card info over blank room info`() {
        // 线上 /room/v1/Room/get_info 不返回 uname/face，主播信息由用户卡片接口补全
        val roomInfo =
            RoomInfoData(
                roomId = 1,
                shortId = 0,
                uid = 123L,
                liveStatus = 1,
                title = "标题",
            )

        val merged =
            LivePlayerUiState().withRoomInfo(
                roomInfo = roomInfo,
                anchorName = "主播",
                anchorFace = "https://example.com/face.jpg",
            )

        assertThat(merged.upMid).isEqualTo(123L)
        assertThat(merged.upName).isEqualTo("主播")
        assertThat(merged.upFace).isEqualTo("https://example.com/face.jpg")
    }

    // ── 弹幕屏蔽与透传 ──────────────────────────────────────

    @Test
    fun `live danmaku is dropped when account rule matches`() =
        runTest(testDispatcher) {
            val player = mockk<DanmakuPlayer>(relaxed = true)
            setDanmakuPlayer(player)
            every { danmakuFilterRepository.shouldDropLive(any(), any()) } returns true

            viewModel.handleLiveEvent(DanmakuEvent(content = "广告", mid = 1L, username = "u"))
            advanceUntilIdle()

            verify { danmakuFilterRepository.shouldDropLive(content = "广告", mid = 1L) }
            // 命中规则后直接返回，不读取播放位置、也不送入引擎
            verify(exactly = 0) { player.getCurrentTimeMs() }
        }

    @Test
    fun `buildLiveDanmakuData maps mode and color`() {
        val top =
            viewModel.buildLiveDanmakuData(
                danmakuId = 1L,
                content = "hi",
                mid = 2L,
                mode = 5,
                textColor = 0xFF0000,
                position = 0L,
            )
        assertThat(top.mode).isEqualTo(DanmakuItemData.DANMAKU_MODE_CENTER_TOP)
        assertThat(top.textColor).isEqualTo(0xFF0000)

        val bottom =
            viewModel.buildLiveDanmakuData(2L, "hi", 2L, mode = 4, textColor = 0xFFFFFF, position = 0L)
        assertThat(bottom.mode).isEqualTo(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM)

        val rolling =
            viewModel.buildLiveDanmakuData(3L, "hi", 2L, mode = 1, textColor = 0xFFFFFF, position = 0L)
        assertThat(rolling.mode).isEqualTo(DanmakuItemData.DANMAKU_MODE_ROLLING)
    }

    @Test
    fun `loadLive prefetches account rules`() {
        viewModel.loadLive(roomId = 1L)

        verify { danmakuFilterRepository.prefetch() }
    }

    // ── Helpers ──────────────────────────────────────────────

    /** 读取 `videoPlayerListener` 字段（播放器事件匿名对象）。 */
    private fun getVideoPlayerListener(): VideoPlayerListener {
        val field = LivePlayerViewModel::class.java.getDeclaredField("videoPlayerListener")
        field.isAccessible = true
        return field.get(viewModel) as VideoPlayerListener
    }

    private fun setVideoPlayer(player: AbstractVideoPlayer?) {
        val field = LivePlayerViewModel::class.java.getDeclaredField("videoPlayer${'$'}delegate")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val delegate = field.get(viewModel) as MutableState<AbstractVideoPlayer?>
        delegate.value = player
    }

    private fun setDanmakuPlayer(player: DanmakuPlayer?) {
        val field = LivePlayerViewModel::class.java.getDeclaredField("danmakuPlayer")
        field.isAccessible = true
        field.set(viewModel, player)
    }

    private fun updateUiState(transform: (LivePlayerUiState) -> LivePlayerUiState) {
        val field = LivePlayerViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<LivePlayerUiState>
        state.value = transform(state.value)
    }
}
