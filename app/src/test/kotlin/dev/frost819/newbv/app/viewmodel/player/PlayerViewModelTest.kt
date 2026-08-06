package dev.frost819.newbv.app.viewmodel.player

import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.action.player.MediaProfileSettingAction
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.ui.state.player.PlayerUiEffect
import dev.frost819.newbv.app.ui.state.player.PlayerUiState
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * [PlayerViewModel] 的单元测试。
 *
 * 验证播放器状态管理、播放控制、切集逻辑、播放结束动作、
 * 监听器回调等核心功能。
 *
 * 使用 MockK mock 所有 Repository 和播放器实例，
 * Prefs 通过 mockkObject 模拟。videoPlayer 和 videoPlayerListener
 * 通过反射设置/获取（因为它们有 private setter / 是 private 字段）。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var videoPlayRepository: VideoPlayRepository
    private lateinit var videoInfoRepository: VideoInfoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var exoPlayerFactory: ExoPlayerFactory
    private lateinit var viewModel: PlayerViewModel
    private lateinit var mockPlayer: AbstractVideoPlayer

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        videoPlayRepository = mockk(relaxed = true)
        videoInfoRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        exoPlayerFactory = mockk()

        mockkObject(Prefs)
        every { Prefs.apiType } returns DataApiType.Web
        every { Prefs.defaultQuality } returns Resolution.R1080P
        every { Prefs.defaultVideoCodec } returns VideoCodec.AVC
        every { Prefs.defaultAudio } returns Audio.A192K
        every { Prefs.incognitoMode } returns true
        every { Prefs.actionAfterPlay } returns ActionAfterPlay.Pause
        every { Prefs.defaultPlaySpeed } returns PlaySpeed.X1
        every { Prefs.enableFfmpegAudioRenderer } returns false
        every { Prefs.enableSoftwareVideoDecoder } returns false

        mockPlayer = mockk(relaxed = true)

        every { videoInfoRepository.videoList } returns MutableStateFlow(emptyList())
        every { videoInfoRepository.relatedVideos } returns MutableStateFlow(emptyList())
        every { videoInfoRepository.lastPlayedCid } returns MutableStateFlow(0L)
        every { videoInfoRepository.lastPlayedTime } returns MutableStateFlow(0)

        viewModel = PlayerViewModel(
            videoPlayRepository = videoPlayRepository,
            videoInfoRepository = videoInfoRepository,
            authRepository = authRepository,
            exoPlayerFactory = exoPlayerFactory,
        )
    }

    @AfterEach
    fun tearDown() {
        runCatching { viewModel.viewModelScope.cancel() }
        unmockkObject(Prefs)
        Dispatchers.resetMain()
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * Set the videoPlayer field via reflection.
     *
     * videoPlayer is `var by mutableStateOf(null) private set`,
     * so we access the Compose delegate field and set its value directly.
     */
    private fun setVideoPlayer(player: AbstractVideoPlayer?) {
        val field = PlayerViewModel::class.java.getDeclaredField("videoPlayer${'$'}delegate")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val delegate = field.get(viewModel) as MutableState<AbstractVideoPlayer?>
        delegate.value = player
    }

    /**
     * Get the private videoPlayerListener field via reflection.
     *
     * The listener is a `private val` anonymous object implementing [VideoPlayerListener].
     */
    private fun getVideoPlayerListener(): VideoPlayerListener {
        val field = PlayerViewModel::class.java.getDeclaredField("videoPlayerListener")
        field.isAccessible = true
        return field.get(viewModel) as VideoPlayerListener
    }

    /**
     * Update _uiState directly via reflection, for test setup.
     */
    private fun updateUiState(transform: (PlayerUiState) -> PlayerUiState) {
        val field = PlayerViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<PlayerUiState>
        state.value = transform(state.value)
    }

    // ── init tests ───────────────────────────────────────────

    @Test
    fun `init sets uiState with correct values`() {
        // init's state update is synchronous; no runTest needed to avoid
        // the infinite clock updater coroutine hanging runTest's scheduler drain
        viewModel.init(
            aid = 100L,
            cid = 200L,
            epid = 300,
            title = "Test Video",
            lastPlayed = 60,
            fromSeason = false,
            subType = 0,
            seasonId = 0,
            authorMid = 999L,
            authorName = "TestUP",
        )

        val state = viewModel.uiState.value
        assertThat(state.aid).isEqualTo(100L)
        assertThat(state.cid).isEqualTo(200L)
        assertThat(state.epid).isEqualTo(300)
        assertThat(state.title).isEqualTo("Test Video")
        assertThat(state.lastPlayed).isEqualTo(60)
        assertThat(state.fromSeason).isFalse()
        assertThat(state.authorMid).isEqualTo(999L)
        assertThat(state.authorName).isEqualTo("TestUP")
    }

    @Test
    fun `init with zero epid sets epid to null`() {
        viewModel.init(
            aid = 1L,
            cid = 2L,
            epid = 0,
            title = "Test",
            lastPlayed = 0,
            fromSeason = false,
            subType = 0,
            seasonId = 0,
            authorName = "UP",
        )

        assertThat(viewModel.uiState.value.epid).isNull()
    }

    @Test
    fun `init with null epid keeps epid null`() {
        viewModel.init(
            aid = 1L,
            cid = 2L,
            epid = null,
            title = "Test",
            lastPlayed = 0,
            fromSeason = false,
            subType = 0,
            seasonId = 0,
            authorName = "UP",
        )

        assertThat(viewModel.uiState.value.epid).isNull()
    }

    // ── togglePlayPause tests ────────────────────────────────

    @Test
    fun `togglePlayPause pauses when playing`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        every { mockPlayer.isPlaying } returns true

        viewModel.togglePlayPause()

        verify { mockPlayer.pause() }
    }

    @Test
    fun `togglePlayPause starts when paused`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        every { mockPlayer.isPlaying } returns false

        viewModel.togglePlayPause()

        verify { mockPlayer.start() }
    }

    @Test
    fun `togglePlayPause does nothing when player is null`() = runTest(testDispatcher) {
        viewModel.togglePlayPause()
    }

    // ── seekToTime test ──────────────────────────────────────

    @Test
    fun `seekToTime updates seekerState`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)

        viewModel.seekToTime(5000L)

        verify { mockPlayer.seekTo(5000L) }
        assertThat(viewModel.seekerState.value.currentTime).isEqualTo(5000L)
    }

    // ── backToStart test ─────────────────────────────────────

    @Test
    fun `backToStart seeks to 0 and clears showBackToStart`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        updateUiState { it.copy(showBackToStart = true) }

        viewModel.backToStart()

        verify { mockPlayer.seekTo(0) }
        assertThat(viewModel.uiState.value.showBackToStart).isFalse()
    }

    // ── playNewVideo tests ──────────────────────────────────

    @Test
    fun `playNewVideo clears playData and resets UI state`() = runTest(testDispatcher) {
        coEvery { videoPlayRepository.getPlayData(any(), any(), any()) } coAnswers {
            delay(Long.MAX_VALUE)
            error("unreachable")
        }

        viewModel.playNewVideo(VideoListItem(aid = 10, cid = 20, title = "New Video"))

        val state = viewModel.uiState.value
        assertThat(state.aid).isEqualTo(10L)
        assertThat(state.cid).isEqualTo(20L)
        assertThat(state.title).isEqualTo("New Video")
        assertThat(state.isBuffering).isTrue()
        assertThat(state.availableQuality).isEmpty()
        assertThat(state.videoShot).isNull()
        assertThat(state.lastPlayed).isEqualTo(0)
    }

    @Test
    fun `playNewVideo emits videoSwitchEvent`() = runTest(testDispatcher) {
        coEvery { videoPlayRepository.getPlayData(any(), any(), any()) } coAnswers {
            delay(Long.MAX_VALUE)
            error("unreachable")
        }

        viewModel.videoSwitchEvent.test {
            viewModel.playNewVideo(VideoListItem(aid = 10, cid = 20, title = "New Video"))
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event.aid).isEqualTo(10L)
            assertThat(event.cid).isEqualTo(20L)
        }
    }

    @Test
    fun `playNewVideo pauses old player`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        coEvery { videoPlayRepository.getPlayData(any(), any(), any()) } coAnswers {
            delay(Long.MAX_VALUE)
            error("unreachable")
        }

        viewModel.playNewVideo(VideoListItem(aid = 10, cid = 20, title = "New"))

        verify { mockPlayer.pause() }
    }

    // ── updatePlaySpeed tests ────────────────────────────────

    @Test
    fun `updatePlaySpeed updates state and player`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)

        viewModel.updatePlaySpeed(1.5f)

        assertThat(viewModel.uiState.value.playSpeed).isEqualTo(1.5f)
        verify { mockPlayer.speed = 1.5f }
    }

    @Test
    fun `updatePlaySpeed with forceUpdate applies even when unchanged`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)

        viewModel.updatePlaySpeed(1f, forceUpdate = true)

        assertThat(viewModel.uiState.value.playSpeed).isEqualTo(1f)
        verify { mockPlayer.speed = 1f }
    }

    @Test
    fun `updatePlaySpeed does nothing when speed unchanged and not forced`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)

        viewModel.updatePlaySpeed(1f)

        assertThat(viewModel.uiState.value.playSpeed).isEqualTo(1f)
        verify(exactly = 0) { mockPlayer.speed = any() }
    }

    // ── toggleLoop test ──────────────────────────────────────

    @Test
    fun `toggleLoop toggles isLooping`() = runTest(testDispatcher) {
        assertThat(viewModel.uiState.value.isLooping).isFalse()

        viewModel.toggleLoop()
        assertThat(viewModel.uiState.value.isLooping).isTrue()

        viewModel.toggleLoop()
        assertThat(viewModel.uiState.value.isLooping).isFalse()
    }

    // ── updateVideoAspectRatio test ──────────────────────────

    @Test
    fun `updateVideoAspectRatio updates state`() = runTest(testDispatcher) {
        viewModel.updateVideoAspectRatio(VideoAspectRatio.FourToThree)

        assertThat(viewModel.uiState.value.aspectRatio).isEqualTo(VideoAspectRatio.FourToThree)
    }

    // ── Listener callback tests ──────────────────────────────

    @Test
    fun `onError sets PlayerState Error and clears isBuffering`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()
        updateUiState { it.copy(isBuffering = true) }

        listener.onError(RuntimeException("test error"))

        val state = viewModel.uiState.value
        assertThat(state.playerState).isInstanceOf(PlayerState.Error::class.java)
        assertThat((state.playerState as PlayerState.Error).message).isEqualTo("test error")
        assertThat(state.isBuffering).isFalse()
    }

    @Test
    fun `onError with null message uses default`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()

        listener.onError(RuntimeException())

        val state = viewModel.uiState.value
        assertThat(state.playerState).isInstanceOf(PlayerState.Error::class.java)
        assertThat((state.playerState as PlayerState.Error).message).isEqualTo("Unknown error")
    }

    @Test
    fun `onPlay sets PlayerState Playing and clears isBuffering`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()
        updateUiState { it.copy(isBuffering = true) }

        listener.onPlay()

        val state = viewModel.uiState.value
        assertThat(state.playerState).isEqualTo(PlayerState.Playing)
        assertThat(state.isBuffering).isFalse()
    }

    @Test
    fun `onPlay with lastPlayed greater than zero seeks and clears lastPlayed`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        val listener = getVideoPlayerListener()
        updateUiState { it.copy(lastPlayed = 30, isBuffering = true) }

        listener.onPlay()

        verify { mockPlayer.seekTo(30L) }
        assertThat(viewModel.uiState.value.lastPlayed).isEqualTo(0)
        assertThat(viewModel.uiState.value.showBackToStart).isTrue()
    }

    @Test
    fun `onPause sets PlayerState Paused`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()

        listener.onPause()

        assertThat(viewModel.uiState.value.playerState).isEqualTo(PlayerState.Paused)
    }

    @Test
    fun `onBuffering sets isBuffering true`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()

        listener.onBuffering()

        assertThat(viewModel.uiState.value.isBuffering).isTrue()
    }

    @Test
    fun `onEnd sets PlayerState Ended and emits PlayEnded effect`() = runTest(testDispatcher) {
        val listener = getVideoPlayerListener()

        viewModel.uiEffect.test {
            listener.onEnd()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isEqualTo(PlayerUiEffect.PlayEnded)
        }

        assertThat(viewModel.uiState.value.playerState).isEqualTo(PlayerState.Ended)
    }

    // ── checkAndPlayNext tests ────────────────────────────────

    @Test
    fun `checkAndPlayNext with Pause action does nothing`() = runTest(testDispatcher) {
        every { Prefs.actionAfterPlay } returns ActionAfterPlay.Pause

        viewModel.checkAndPlayNext()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showSkipToNextEp).isFalse()
    }

    @Test
    fun `checkAndPlayNext with Exit action emits FinishActivity`() = runTest(testDispatcher) {
        every { Prefs.actionAfterPlay } returns ActionAfterPlay.Exit

        viewModel.uiEffect.test {
            viewModel.checkAndPlayNext()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isEqualTo(PlayerUiEffect.FinishActivity)
        }
    }

    @Test
    fun `checkAndPlayNext with PlayNext and no next target emits FinishActivity`() = runTest(testDispatcher) {
        every { Prefs.actionAfterPlay } returns ActionAfterPlay.PlayNext

        viewModel.uiEffect.test {
            viewModel.checkAndPlayNext()
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isEqualTo(PlayerUiEffect.FinishActivity)
        }
    }

    // ── cancelPlayNext test ───────────────────────────────────

    @Test
    fun `cancelPlayNext clears showSkipToNextEp`() = runTest(testDispatcher) {
        updateUiState { it.copy(showSkipToNextEp = true) }
        assertThat(viewModel.uiState.value.showSkipToNextEp).isTrue()

        viewModel.cancelPlayNext()

        assertThat(viewModel.uiState.value.showSkipToNextEp).isFalse()
    }

    // ── updateMediaProfile tests ──────────────────────────────

    @Test
    fun `updateMediaProfile updates quality in state`() = runTest(testDispatcher) {
        viewModel.updateMediaProfile(MediaProfileSettingAction.SetQuality(116))

        assertThat(viewModel.uiState.value.mediaProfileState.qualityId).isEqualTo(116)
    }

    @Test
    fun `updateMediaProfile updates videoCodec in state`() = runTest(testDispatcher) {
        viewModel.updateMediaProfile(MediaProfileSettingAction.SetVideoCodec(VideoCodec.HEVC))

        assertThat(viewModel.uiState.value.mediaProfileState.videoCodec).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `updateMediaProfile updates audio in state`() = runTest(testDispatcher) {
        viewModel.updateMediaProfile(MediaProfileSettingAction.SetAudio(Audio.A132K))

        assertThat(viewModel.uiState.value.mediaProfileState.audio).isEqualTo(Audio.A132K)
    }

    @Test
    fun `updateMediaProfile does nothing when state unchanged`() = runTest(testDispatcher) {
        val before = viewModel.uiState.value.mediaProfileState

        viewModel.updateMediaProfile(MediaProfileSettingAction.SetQuality(80))

        assertThat(viewModel.uiState.value.mediaProfileState).isEqualTo(before)
    }

    @Test
    fun `updateMediaProfile with player pauses and re-resolves`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        every { mockPlayer.currentPosition } returns 5000L

        viewModel.updateMediaProfile(MediaProfileSettingAction.SetQuality(116))

        assertThat(viewModel.uiState.value.mediaProfileState.qualityId).isEqualTo(116)
        verify { mockPlayer.pause() }
    }

    // ── detachPlayer test ─────────────────────────────────────

    @Test
    fun `detachPlayer releases player and sets videoPlayer to null`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)

        viewModel.detachPlayer()

        verify { mockPlayer.release() }
        assertThat(viewModel.videoPlayer).isNull()
    }

    @Test
    fun `detachPlayer does nothing when player is null`() = runTest(testDispatcher) {
        viewModel.detachPlayer()

        assertThat(viewModel.videoPlayer).isNull()
    }

    // ── trySendHeartbeat test ─────────────────────────────────

    @Test
    fun `trySendHeartbeat does not update local history`() = runTest(testDispatcher) {
        setVideoPlayer(mockPlayer)
        every { mockPlayer.currentPosition } returns 5000L
        every { mockPlayer.duration } returns 60000L

        viewModel.trySendHeartbeat()

        verify(exactly = 0) { videoInfoRepository.updateHistory(any(), any()) }
    }
}
