package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutAction
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutCatalog
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutKeys
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutsStore
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.app.ui.action.player.MediaProfileSettingAction
import dev.frost819.newbv.app.ui.action.player.SubtitleSettingAction
import dev.frost819.newbv.app.ui.component.player.menu.MenuController
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.ui.state.player.PlayerUiState
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放器根控制器。
 *
 * 管理所有覆盖层的可见性和焦点路由，处理 D-pad 按键事件、seek 加速、
 * 自定义快捷键、以及视频列表/菜单/相关信息控制器之间的协调。
 *
 * 布局层次（从底到顶）：
 * 1. content() — 视频画面 + 弹幕层
 * 2. BottomSubtitle — 字幕文本
 * 3. SkipTips — 跳转提示
 * 4. PlayStateTips — 播放状态提示
 * 5. RelatedVideosController — 相关视频
 * 6. ControllerVideoInfo — 信息栏 + 进度条 + 按钮
 * 7. VideoListController — 分集列表
 * 8. MenuController — 设置菜单
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    fromSeason: Boolean,
    isLooping: Boolean,
    videoShotCache: VideoShotImageCache,
    uiState: PlayerUiState,
    seekerState: State<SeekerState>,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToStart: () -> Unit,
    onCancelSkipToNextEp: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleSubtitle: () -> Unit,
    onGoToUpPage: () -> Unit,
    onGoToVideoDetail: () -> Unit,
    onShowInteraction: () -> Unit,
    onShowComments: () -> Unit,
    onMediaProfileSettingChange: (MediaProfileSettingAction) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSettingChange: (SubtitleSettingAction) -> Unit,
    onRelatedVideoClicked: (dev.frost819.newbv.app.ui.component.videocard.VideoCardData) -> Unit,
    onToggleDanmaku: () -> Unit,
    onShowShortcutTip: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 覆盖层可见性
    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showInfoSeekController by remember { mutableStateOf(false) }
    var showRelatedVideosController by remember { mutableStateOf(false) }
    val showClickableControllers by remember {
        derivedStateOf {
            showListController ||
                showMenuController ||
                showInfoSeekController ||
                showRelatedVideosController
        }
    }

    // Seek 加速状态
    var goTime by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekChangeCount by remember { mutableLongStateOf(0L) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }
    var seekCountdown: Job? by remember { mutableStateOf(null) }
    var hideInfoSeekCountdown: Job? by remember { mutableStateOf(null) }

    // 常显进度条
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }

    // 手势状态
    val gestureTipState = rememberGestureTipState()
    var currentBrightness by remember { mutableFloatStateOf(-1f) }

    fun calCoefficient(): Long =
        if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }

    fun onTimeForward() {
        isSeeking = true
        val coefficient = calCoefficient()
        val step = 10_000L + coefficient * 5_000L
        goTime = (goTime + step).coerceAtMost(seekerState.value.totalDuration)
        lastSeekChangeTime = System.currentTimeMillis()
    }

    fun onTimeBack() {
        isSeeking = true
        val coefficient = calCoefficient()
        val step = 10_000L + coefficient * 5_000L
        goTime = (goTime - step).coerceAtLeast(0L)
        lastSeekChangeTime = System.currentTimeMillis()
    }

    fun startSeekCountdown() {
        seekCountdown?.cancel()
        seekCountdown =
            scope.launch {
                delay(1000)
                onGoTime(goTime)
                if (uiState.playerState != PlayerState.Playing) onPlay()
                isSeeking = false
                showInfoSeekController = false
            }
    }

    fun onDirectionLeft() {
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeBack()
        startSeekCountdown()
    }

    fun onDirectionRight() {
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeForward()
        startSeekCountdown()
    }

    fun onSeekGoTime() {
        seekCountdown?.cancel()
        onGoTime(goTime)
        if (uiState.playerState != PlayerState.Playing) onPlay()
        isSeeking = false
        showInfoSeekController = false
    }

    /**
     * 控制器自动隐藏计时器。触屏交互后 5 秒无操作自动收起控制器。
     */
    fun startControllerAutoHide() {
        if (!showInfoSeekController) return
        hideInfoSeekCountdown?.cancel()
        hideInfoSeekCountdown =
            scope.launch {
                delay(5000)
                showInfoSeekController = false
            }
    }

    fun onSeekToPosition(positionMs: Long) {
        isSeeking = true
        goTime = positionMs.coerceIn(0L, seekerState.value.totalDuration)
        lastSeekChangeTime = System.currentTimeMillis()
        startSeekCountdown()
        startControllerAutoHide()
    }

    fun closeAllControllers() {
        showListController = false
        showMenuController = false
        showInfoSeekController = false
        showRelatedVideosController = false
    }

    /**
     * 显示自定义快捷键触发提示浮层。
     *
     * 复用 [PlayerTip] 组件显示动作名称，定时结束后自动消失。
     * 快捷键提示始终显示，新的提示会覆盖旧提示。
     *
     * 对于开关类/参数类动作，[status] 会追加在动作名称后面（如"字幕：开"）。
     */
    fun showShortcutTip(
        action: PlayerCustomShortcutAction,
        status: String? = null,
    ) {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(action)
        val tip = if (status != null) "$name：$status" else name
        onShowShortcutTip(tip)
    }

    fun executeCustomShortcut(action: PlayerCustomShortcutAction) {
        var status: String? = null
        when (action) {
            PlayerCustomShortcutAction.OpenSettings -> showMenuController = true
            PlayerCustomShortcutAction.OpenRelatedVideos -> showRelatedVideosController = true
            PlayerCustomShortcutAction.PlayPrevious -> onPlayPrevious()
            PlayerCustomShortcutAction.PlayNext -> onPlayNext()
            PlayerCustomShortcutAction.OpenVideoDetail -> onGoToVideoDetail()
            PlayerCustomShortcutAction.OpenUpPage -> onGoToUpPage()
            PlayerCustomShortcutAction.OpenComments -> onShowComments()
            PlayerCustomShortcutAction.OpenInteraction -> onShowInteraction()
            PlayerCustomShortcutAction.ToggleLoop -> {
                status = if (!isLooping) "开" else "关"
                onToggleLoop()
            }
            PlayerCustomShortcutAction.ToggleDanmaku -> {
                status = if (uiState.danmakuState.enabledTypes.isEmpty()) "开" else "关"
                onToggleDanmaku()
            }
            PlayerCustomShortcutAction.ToggleSubtitle -> {
                status = if (uiState.subtitleId == -1L) "开" else "关"
                onToggleSubtitle()
            }
            PlayerCustomShortcutAction.TogglePersistentBottomProgress -> {
                showPersistentSeek = !showPersistentSeek
                Prefs.showPersistentSeek = showPersistentSeek
                status = if (showPersistentSeek) "开" else "关"
            }

            is PlayerCustomShortcutAction.TogglePlaybackSpeed -> {
                val targetSpeed = if (uiState.playSpeed == action.speed) 1f else action.speed
                status = "${targetSpeed}x"
                onPlaySpeedChange(targetSpeed)
            }

            is PlayerCustomShortcutAction.ToggleDanmakuMask -> {
                val newMaskEnabled = !uiState.danmakuState.maskEnabled
                status = if (newMaskEnabled) "开" else "关"
                onDanmakuSettingChange(
                    DanmakuSettingAction.SetMaskEnabled(newMaskEnabled),
                )
            }
        }
        showShortcutTip(action, status)
    }

    fun handleCustomShortcut(event: KeyEvent): Boolean {
        if (showClickableControllers) return false
        val keyCode = event.nativeKeyEvent.keyCode
        if (!PlayerCustomShortcutKeys.isAllowedKeyCode(keyCode)) return false
        val shortcut = PlayerCustomShortcutsStore.getByKey()[keyCode] ?: return false
        if (event.type == KeyEventType.KeyUp) return true
        if (event.type != KeyEventType.KeyDown) return false
        if (event.nativeKeyEvent.repeatCount != 0) return true
        executeCustomShortcut(shortcut.action)
        return true
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val confirmKeys = listOf(Key.DirectionCenter, Key.Enter, Key.Spacebar)

        // 非 confirm 键的 KeyUp 事件消费掉
        if (event.type == KeyEventType.KeyUp && event.key !in confirmKeys) {
            return true
        }

        // 自定义快捷键
        if (handleCustomShortcut(event)) return true

        // 始终生效的按键（KeyUp 已被顶层过滤，此处均为 KeyDown）
        when (event.key) {
            Key.Back -> {
                if (showClickableControllers) {
                    closeAllControllers()
                    return true
                }
                onExit()
                return true
            }

            Key.Menu, Key(763) -> {
                showInfoSeekController = false
                showMenuController = !showMenuController
                return true
            }

            Key.MediaPlayPause -> {
                onPlay()
                return true
            }

            Key.MediaPlay -> {
                if (uiState.playerState != PlayerState.Playing) onPlay()
                return true
            }

            Key.MediaPause -> {
                if (uiState.playerState == PlayerState.Playing) onPause()
                return true
            }
        }

        // 覆盖层未打开时的按键（KeyUp 已被顶层过滤，此处均为 KeyDown）
        if (!showClickableControllers) {
            when (event.key) {
                in confirmKeys -> {
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.isLongPress) {
                            showMenuController = true
                        }
                        return true
                    } else {
                        if (uiState.showBackToStart) {
                            onBackToStart()
                        } else {
                            onPlay()
                        }
                        return true
                    }
                }

                Key.DirectionUp -> {
                    showListController = true
                    return true
                }

                Key.DirectionDown -> {
                    showInfoSeekController = true
                    return true
                }

                Key.DirectionLeft, Key.MediaRewind -> {
                    if (uiState.showSkipToNextEp) {
                        onCancelSkipToNextEp()
                        return true
                    }
                    showInfoSeekController = true
                    onDirectionLeft()
                    return true
                }

                Key.DirectionRight, Key.MediaFastForward -> {
                    showInfoSeekController = true
                    onDirectionRight()
                    return true
                }
            }
        }

        return false
    }

    Box(
        modifier =
            modifier
                .background(Color.Black)
                .focusable()
                .onPreviewKeyEvent { event ->
                    startControllerAutoHide()
                    handleKeyEvent(event)
                }.playerGestures(
                    totalDuration = { seekerState.value.totalDuration },
                    controllerVisible = { showInfoSeekController },
                    callbacks =
                        PlayerGestureCallbacks(
                            onSingleTap = {
                                if (!showClickableControllers) {
                                    showInfoSeekController = !showInfoSeekController
                                    if (showInfoSeekController) startControllerAutoHide()
                                } else {
                                    closeAllControllers()
                                }
                            },
                            onDoubleTap = { onPlay() },
                            onSeekDelta = { deltaMs ->
                                if (!isSeeking) goTime = seekerState.value.currentTime
                                goTime = (goTime + deltaMs).coerceIn(0L, seekerState.value.totalDuration)
                                isSeeking = true
                                showInfoSeekController = true
                                startSeekCountdown()
                            },
                            onSeekCommit = { },
                            onBrightnessChange = { deltaY ->
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    currentBrightness = adjustBrightness(activity, deltaY, currentBrightness)
                                    gestureTipState.value =
                                        GestureTipState(
                                            isActive = true,
                                            type = GestureTipType.Brightness,
                                            value = currentBrightness,
                                        )
                                }
                            },
                            onVolumeChange = { deltaY ->
                                val audioManager =
                                    context.getSystemService(android.content.Context.AUDIO_SERVICE)
                                        as? android.media.AudioManager
                                if (audioManager != null) {
                                    val volumePercent = adjustVolume(audioManager, deltaY)
                                    gestureTipState.value =
                                        GestureTipState(
                                            isActive = true,
                                            type = GestureTipType.Volume,
                                            value = volumePercent.toFloat(),
                                        )
                                }
                            },
                            onCycleAspectRatio = {
                                val current = uiState.aspectRatio
                                val next =
                                    VideoAspectRatio.entries[
                                        (current.ordinal + 1) % VideoAspectRatio.entries.size,
                                    ]
                                onAspectRatioChange(next)
                            },
                        ),
                    gestureTipState = gestureTipState,
                ),
    ) {
        // 视频画面 + 弹幕层
        content()

        // 调试信息
        if (Prefs.showPlayerDebugInfo) {
            Box(
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = seekerState.value.debugInfo,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 常显进度条
        if (showPersistentSeek && !showInfoSeekController) {
            VideoProgressSeek(
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                duration = seekerState.value.totalDuration,
                position = seekerState.value.currentTime,
                bufferedPercentage = seekerState.value.bufferedPercentage,
                isPersistentSeek = true,
            )
        }

        // 字幕
        if (uiState.subtitleId != -1L) {
            BottomSubtitle(
                subtitleData = uiState.subtitleData,
                currentTime = seekerState.value.currentTime,
                fontSize =
                    androidx.compose.ui.unit.TextUnit(
                        uiState.subtitleState.fontSize.toFloat(),
                        androidx.compose.ui.unit.TextUnitType.Sp,
                    ),
                opacity = uiState.subtitleState.opacity,
                padding =
                    androidx.compose.ui.unit
                        .Dp(uiState.subtitleState.bottomPadding.toFloat()),
            )
        }

        // 跳转提示
        SkipTips(
            showBackToStart = uiState.showBackToStart,
            showSkipToNextEp = uiState.showSkipToNextEp,
            showPreviewTip = uiState.showPreviewTip,
            shortcutTipText = uiState.shortcutTipText,
        )

        // 播放状态提示
        PlayStateTips(
            isPlaying = uiState.playerState == PlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState is PlayerState.Error,
            errorMessage = (uiState.playerState as? PlayerState.Error)?.message,
        )

        // 手势提示（亮度/音量/倍速反馈）
        GestureTip(
            state = gestureTipState.value,
            modifier = Modifier.align(Alignment.Center),
        )

        // 相关视频
        RelatedVideosController(
            show = showRelatedVideosController,
            relatedVideos = uiState.relatedVideos,
            onVideoClicked = onRelatedVideoClicked,
        )

        // 信息栏 + 进度条 + 按钮
        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showInfoSeekController,
            isSeeking = isSeeking,
            goTime = goTime,
            seekerState = seekerState.value,
            title = uiState.title,
            clock = uiState.clock,
            onlineWatching = uiState.onlineWatching,
            videoShot = uiState.videoShot,
            videoShotCache = videoShotCache,
            fromSeason = fromSeason,
            danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty(),
            isLooping = isLooping,
            onDirectionLeft = ::onDirectionLeft,
            onDirectionRight = ::onDirectionRight,
            onSeekGoTime = ::onSeekGoTime,
            onSeekToPosition = ::onSeekToPosition,
            onPlayPause = {
                onPlay()
                startControllerAutoHide()
            },
            onDanmakuSwitchChange = {
                onToggleDanmaku()
                startControllerAutoHide()
            },
            onShowSettings = { showMenuController = true },
            onShowRelatedVideos = { showRelatedVideosController = true },
            onGoToVideoInfo = onGoToVideoDetail,
            onToggleLoop = {
                onToggleLoop()
                startControllerAutoHide()
            },
            onGoToUpPage = onGoToUpPage,
            onShowInteraction = onShowInteraction,
            onShowComments = onShowComments,
        )

        // 分集列表
        VideoListController(
            show = showListController,
            currentCid = uiState.cid,
            videoList = uiState.videoList,
            onPlayNewVideo = { item ->
                onPlayNewVideo(item)
                showListController = false
            },
        )

        // 设置菜单
        MenuController(
            show = showMenuController,
            uiState = uiState,
            onResolutionChange = { onMediaProfileSettingChange(MediaProfileSettingAction.SetQuality(it)) },
            onCodecChange = { onMediaProfileSettingChange(MediaProfileSettingAction.SetVideoCodec(it)) },
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onAudioChange = { onMediaProfileSettingChange(MediaProfileSettingAction.SetAudio(it)) },
            onDanmakuSwitchChange = { types ->
                // data DanmakuType → danmaku entity DanmakuType
                val entityTypes =
                    types.mapNotNull {
                        runCatching { dev.frost819.newbv.danmaku.entity.DanmakuType.entries[it.ordinal] }.getOrNull()
                    }
                onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(entityTypes))
            },
            onDanmakuSizeChange = { onDanmakuSettingChange(DanmakuSettingAction.SetScale(it)) },
            onDanmakuOpacityChange = { onDanmakuSettingChange(DanmakuSettingAction.SetOpacity(it)) },
            onDanmakuSpeedFactorChange = { onDanmakuSettingChange(DanmakuSettingAction.SetSpeedFactor(it)) },
            onDanmakuAreaChange = { onDanmakuSettingChange(DanmakuSettingAction.SetArea(it)) },
            onDanmakuMaskChange = { onDanmakuSettingChange(DanmakuSettingAction.SetMaskEnabled(it)) },
            onSubtitleChange = { subtitle -> onSubtitleChange(subtitle) },
            onSubtitleSizeChange = { onSubtitleSettingChange(SubtitleSettingAction.SetFontSize(it)) },
            onSubtitleBackgroundOpacityChange = { onSubtitleSettingChange(SubtitleSettingAction.SetOpacity(it)) },
            onSubtitleBottomPadding = { onSubtitleSettingChange(SubtitleSettingAction.SetBottomPadding(it)) },
        )
    }
}
