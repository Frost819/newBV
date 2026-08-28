package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.app.ui.component.player.menu.LiveMenuController
import dev.frost819.newbv.danmaku.config.DanmakuState
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 直播播放器根控制器。
 *
 * 管理所有覆盖层的可见性和焦点路由，处理 D-pad 按键事件、触屏手势、
 * 控制器自动隐藏。
 *
 * 布局层次（从底到顶）：
 * 1. content() — 视频画面 + 弹幕层
 * 2. PlayStateTips — 播放状态提示（缓冲/暂停/错误）
 * 3. GestureTip — 手势提示（亮度/音量）
 * 4. LiveControllerInfo — 顶部信息栏 + 底部按钮行
 * 5. LiveMenuController — 设置菜单
 *
 * @param isPlaying 是否正在播放
 * @param isBuffering 是否缓冲中
 * @param isError 是否出错
 * @param errorMessage 错误信息
 * @param title 直播间标题
 * @param areaName 分区名
 * @param onlineCount 人气值
 * @param clock 时钟（hour, minute）
 * @param danmakuEnabled 弹幕是否开启
 * @param danmakuState 弹幕配置状态
 * @param availableQualities 可用画质列表（qn, desc）
 * @param currentQuality 当前画质 qn
 * @param onBack 返回回调
 * @param onPlayPause 播放/暂停回调
 * @param onRefresh 刷新回调
 * @param onToggleDanmaku 弹幕开关回调
 * @param onQualityChange 画质变化回调
 * @param onDanmakuSettingChange 弹幕设置变化回调
 * @param debugInfo 调试信息文本（仅 [Prefs.showPlayerDebugInfo] 开启时有值）
 * @param content 视频画面 + 弹幕层内容
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
fun LivePlayerController(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isError: Boolean,
    errorMessage: String?,
    title: String,
    areaName: String,
    onlineCount: String,
    clock: Pair<Int, Int>,
    danmakuEnabled: Boolean,
    danmakuState: DanmakuState,
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onQualityChange: (Int) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    debugInfo: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showInfoController by remember { mutableStateOf(true) }
    var showMenuController by remember { mutableStateOf(false) }
    val showClickableControllers by remember {
        derivedStateOf { showInfoController || showMenuController }
    }

    var hideInfoCountdown: Job? by remember { mutableStateOf(null) }

    val gestureTipState = rememberGestureTipState()
    var currentBrightness by remember { mutableFloatStateOf(-1f) }

    fun startControllerAutoHide() {
        if (!showInfoController) return
        hideInfoCountdown?.cancel()
        hideInfoCountdown =
            scope.launch {
                delay(5000)
                showInfoController = false
            }
    }

    LaunchedEffect(Unit) {
        startControllerAutoHide()
    }

    fun closeAllControllers() {
        showMenuController = false
        showInfoController = false
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val confirmKeys = listOf(Key.DirectionCenter, Key.Enter, Key.Spacebar)

        if (event.type == KeyEventType.KeyUp && event.key !in confirmKeys) {
            return true
        }

        when (event.key) {
            Key.Back -> {
                if (event.type == KeyEventType.KeyUp) return true
                if (showClickableControllers) {
                    closeAllControllers()
                    return true
                }
                onBack()
                return true
            }

            Key.Menu -> {
                if (event.type == KeyEventType.KeyUp) return true
                showMenuController = !showMenuController
                showInfoController = false
                return true
            }

            Key.MediaPlayPause -> {
                if (event.type == KeyEventType.KeyUp) return true
                onPlayPause()
                return true
            }

            Key.MediaPlay -> {
                if (event.type == KeyEventType.KeyUp) return true
                if (!isPlaying) onPlayPause()
                return true
            }

            Key.MediaPause -> {
                if (event.type == KeyEventType.KeyUp) return true
                if (isPlaying) onPlayPause()
                return true
            }
        }

        if (!showClickableControllers) {
            when (event.key) {
                in confirmKeys -> {
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.isLongPress) {
                            showMenuController = true
                        }
                        return true
                    } else {
                        onPlayPause()
                        startControllerAutoHide()
                        return true
                    }
                }

                Key.DirectionDown -> {
                    if (event.type == KeyEventType.KeyUp) return true
                    showInfoController = true
                    startControllerAutoHide()
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
                    totalDuration = { 0L },
                    controllerVisible = { showInfoController },
                    callbacks =
                        PlayerGestureCallbacks(
                            onSingleTap = {
                                if (!showClickableControllers) {
                                    showInfoController = !showInfoController
                                    if (showInfoController) startControllerAutoHide()
                                } else {
                                    closeAllControllers()
                                }
                            },
                            onDoubleTap = { onPlayPause() },
                            onSeekDelta = { },
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
                            onCycleAspectRatio = { },
                        ),
                    gestureTipState = gestureTipState,
                ),
    ) {
        content()

        // 调试信息
        if (Prefs.showPlayerDebugInfo && debugInfo.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = debugInfo,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        PlayStateTips(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isError = isError,
            errorMessage = errorMessage,
        )

        GestureTip(
            state = gestureTipState.value,
            modifier = Modifier.align(Alignment.Center),
        )

        LiveControllerInfo(
            show = showInfoController && !showMenuController,
            title = title,
            areaName = areaName,
            onlineCount = onlineCount,
            clock = clock,
            isPlaying = isPlaying,
            danmakuEnabled = danmakuEnabled,
            onPlayPause = {
                onPlayPause()
                startControllerAutoHide()
            },
            onRefresh = {
                onRefresh()
                startControllerAutoHide()
            },
            onDanmakuSwitchChange = {
                onToggleDanmaku()
                startControllerAutoHide()
            },
            onShowSettings = { showMenuController = true },
        )

        LiveMenuController(
            show = showMenuController,
            availableQualities = availableQualities,
            currentQuality = currentQuality,
            onQualityChange = onQualityChange,
            danmakuState = danmakuState,
            onDanmakuSettingChange = onDanmakuSettingChange,
        )
    }
}
