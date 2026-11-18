package dev.frost819.newbv.app.ui.component.player

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** 手势状态，用于驱动 [GestureTip] 覆盖层显示。 */
data class GestureTipState(
    /** 手势是否正在执行中（用于显示提示）。 */
    val isActive: Boolean = false,
    /** 当前手势类型。 */
    val type: GestureTipType = GestureTipType.None,
    /** 当前手势值（亮度 0~1、音量百分比 0~100、倍速值）。 */
    val value: Float = 0f,
)

/** 手势提示类型。 */
enum class GestureTipType {
    None,
    Brightness,
    Volume,
    Speed,
}

/**
 * 播放器手势回调。
 *
 * 竖直方向的亮度/音量共用同一套换算：滑动一屏高度（乘以
 * [VERTICAL_DRAG_RANGE_RATIO]）对应满量程，回调收到的是**满量程比例**增量。
 *
 * @param onSingleTap 单击：显示/隐藏控制器（双击判定超时后才会触发）。
 * @param onDoubleTap 双击：播放/暂停。
 * @param onSeekDelta 水平拖拽 seek：正值快进、负值快退（毫秒增量）。
 * @param onSeekCommit 水平拖拽结束（手指松开时调用），用于一次性提交 seek。
 * @param onBrightnessChange 亮度变化：正值为增大，单位为满量程比例（1 = 100%）。
 * @param onVolumeChange 音量变化：正值为增大，单位为满量程比例（1 = 最大音量）。
 * @param onUserInteraction 任意触摸交互（含被子组件消费的按钮点击），用于重置控制器自动隐藏倒计时。
 * @param onSpeedPressStart 长按开始（长按倍速），为 null 时不启用长按手势。
 * @param onSpeedPressEnd 长按结束（抬起、开始拖动或被取消）。
 */
data class PlayerGestureCallbacks(
    val onSingleTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onSeekDelta: (deltaMs: Long) -> Unit,
    val onSeekCommit: () -> Unit,
    val onBrightnessChange: (deltaFraction: Float) -> Unit,
    val onVolumeChange: (deltaFraction: Float) -> Unit,
    val onUserInteraction: () -> Unit = {},
    val onSpeedPressStart: (() -> Unit)? = null,
    val onSpeedPressEnd: (() -> Unit)? = null,
)

/**
 * 创建并记住手势提示状态。
 *
 * 由播放器控制器持有，同时传给 [playerGestures]（清理/长按）与 [GestureTip]（显示）。
 */
@Composable
fun rememberGestureTipState(): MutableState<GestureTipState> = remember { mutableStateOf(GestureTipState()) }

/**
 * 播放器手势 Modifier 扩展。
 *
 * 使用 `awaitEachGesture` 手动分发手势（PRD 4.3.3.2），避免多个 `detectXxxGestures`
 * 互相消费事件：
 * - 单击：显示/隐藏控制器（延迟 `doubleTapTimeout` 派发，避免双击时先切一次控制器）
 * - 双击：播放/暂停
 * - 水平滑动：快进/快退（松开时经 [PlayerGestureCallbacks.onSeekCommit] 提交）
 * - 左半屏垂直滑动：亮度调节；右半屏垂直滑动：音量调节
 * - 长按：倍速播放（由 [PlayerGestureCallbacks.onSpeedPressStart] 决定是否启用）
 *
 * D-pad 模式不受影响——此 modifier 仅处理触摸事件，按键事件由 `onPreviewKeyEvent` 处理。
 *
 * **与子组件的事件边界**：按下事件若已被子组件（按钮、进度条等）消费，则本手势直接让行；
 * 越过 `touchSlop` 前也不消费移动事件。否则手指的轻微抖动会让子组件
 * `detectTapGestures` 判定手势被取消（遇到已消费的 change 即返回 null），
 * 表现为播放器内按钮"点不动"。
 *
 * @param totalDuration 视频总时长（毫秒），用于将像素位移转换为时间增量；直播传 0 即可。
 * @param callbacks 手势回调；内部通过 [rememberUpdatedState] 始终调用最新实例。
 * @param gestureTipState 手势提示状态，手势结束后延迟清理。
 */
fun Modifier.playerGestures(
    totalDuration: () -> Long,
    callbacks: PlayerGestureCallbacks,
    gestureTipState: MutableState<GestureTipState>,
): Modifier =
    composed {
        val currentCallbacks by rememberUpdatedState(callbacks)
        val currentTotalDuration by rememberUpdatedState(totalDuration)
        // 延迟任务（双击等待、提示残留）需要独立于单次手势的生命周期
        val scope = rememberCoroutineScope()

        this.pointerInput(Unit) {
            var pendingSingleTap: Job? = null
            var pendingTipHide: Job? = null
            var lastTapUpTime = 0L

            fun hideTipSoon() {
                pendingTipHide?.cancel()
                pendingTipHide =
                    scope.launch {
                        delay(GESTURE_TIP_LINGER_MS)
                        gestureTipState.value = GestureTipState()
                    }
            }

            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                // 触摸按下即续期自动隐藏倒计时：即使 DOWN 已被子组件（如按钮）消费也要执行
                currentCallbacks.onUserInteraction()

                // 子组件已接管本次按下：只续期控制器，不再参与手势识别，
                // 否则父节点会与子组件抢事件，导致按钮点击被取消
                if (firstDown.isConsumed) {
                    pendingSingleTap?.cancel()
                    pendingSingleTap = null
                    lastTapUpTime = 0L
                    return@awaitEachGesture
                }

                pendingSingleTap?.cancel()
                pendingSingleTap = null
                pendingTipHide?.cancel()

                val pointerId = firstDown.id
                val startX = firstDown.position.x
                val startY = firstDown.position.y
                // 兜底 1px，避免极端情况下除零
                val width = size.width.toFloat().coerceAtLeast(1f)
                val height = size.height.toFloat().coerceAtLeast(1f)
                // 亮度/音量共用：滑动这段距离等于调整满量程
                val verticalRangePx = height * VERTICAL_DRAG_RANGE_RATIO
                val touchSlop = viewConfiguration.touchSlop
                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                var isDragging = false
                var isHorizontalDrag: Boolean? = null
                var totalDeltaX = 0f
                var totalDeltaY = 0f
                var isSpeedPressActive = false

                // 长按倍速：按住不动超过系统长按时长后开始，移动或抬起即结束
                val speedPressJob =
                    if (currentCallbacks.onSpeedPressStart != null) {
                        scope.launch {
                            delay(longPressTimeout)
                            if (!isDragging && !isSpeedPressActive) {
                                isSpeedPressActive = true
                                currentCallbacks.onSpeedPressStart?.invoke()
                            }
                        }
                    } else {
                        null
                    }

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (!change.pressed) {
                            // 手指抬起
                            if (change.isConsumed) break

                            if (isDragging) {
                                if (isHorizontalDrag == true) currentCallbacks.onSeekCommit()
                            } else if (!isSpeedPressActive) {
                                val moved =
                                    abs(change.position.x - startX) > touchSlop ||
                                        abs(change.position.y - startY) > touchSlop
                                if (!moved) {
                                    val now = change.uptimeMillis
                                    if (lastTapUpTime != 0L && now - lastTapUpTime < doubleTapTimeout) {
                                        // 双击：取消上一次的待派发单击，避免控制器先显隐一次
                                        lastTapUpTime = 0L
                                        currentCallbacks.onDoubleTap()
                                    } else {
                                        lastTapUpTime = now
                                        pendingSingleTap =
                                            scope.launch {
                                                delay(doubleTapTimeout)
                                                currentCallbacks.onSingleTap()
                                            }
                                    }
                                }
                            }
                            break
                        }

                        // 被子组件中途消费（如进度条拖拽）：让行，避免与子组件抢事件
                        if (change.isConsumed) break
                        if (!change.positionChanged()) continue

                        val deltaX = change.positionChange().x
                        val deltaY = change.positionChange().y
                        totalDeltaX += deltaX
                        totalDeltaY += deltaY

                        val absX = abs(totalDeltaX)
                        val absY = abs(totalDeltaY)

                        // 越过触摸阈值才判定拖拽方向：阈值以下的抖动不消费，保留给子组件点击
                        if (isHorizontalDrag == null && (absX > touchSlop || absY > touchSlop)) {
                            isHorizontalDrag = absX > absY
                            isDragging = true
                            speedPressJob?.cancel()
                            if (isSpeedPressActive) {
                                isSpeedPressActive = false
                                currentCallbacks.onSpeedPressEnd?.invoke()
                            }
                        }

                        when (isHorizontalDrag) {
                            true -> {
                                val durationMs = currentTotalDuration()
                                if (durationMs > 0) {
                                    // 拖动一个屏幕宽度约等于视频时长的 SEEK_DURATION_RATIO
                                    val deltaMs = (deltaX / width * durationMs * SEEK_DURATION_RATIO).toLong()
                                    if (deltaMs != 0L) currentCallbacks.onSeekDelta(deltaMs)
                                }
                                change.consume()
                            }

                            false -> {
                                // Compose 中 y 向下为正，上滑（deltaY < 0）应增加亮度/音量，故取反。
                                // 换算成满量程比例，左右半屏走同一套计算
                                val deltaFraction = -deltaY / verticalRangePx
                                if (startX < width / 2) {
                                    currentCallbacks.onBrightnessChange(deltaFraction)
                                } else {
                                    currentCallbacks.onVolumeChange(deltaFraction)
                                }
                                change.consume()
                            }

                            // 未越过阈值：不消费，让子组件的点击识别保留
                            null -> Unit
                        }
                    }
                } finally {
                    speedPressJob?.cancel()
                    if (isSpeedPressActive) currentCallbacks.onSpeedPressEnd?.invoke()
                    // 拖拽或长按结束后让提示短暂停留再消失，避免一闪而过
                    if (isDragging || isSpeedPressActive) hideTipSoon()
                }
            }
        }
    }

/**
 * 拖动一个屏幕宽度对应的可 seek 时长比例。
 */
private const val SEEK_DURATION_RATIO = 0.5f

/** 手势松开后提示层的残留时长（毫秒）。 */
private const val GESTURE_TIP_LINGER_MS = 400L

/**
 * 竖直滑动调整满量程所需的距离（相对播放器高度）。
 *
 * 亮度与音量共用：`1f` 表示滑动一屏高度恰好把亮度从 0 调到 100%（或音量从静音调到最大）。
 */
const val VERTICAL_DRAG_RANGE_RATIO: Float = 1f

/**
 * 按系统值与当前覆盖值计算调整后的亮度。
 *
 * @param currentBrightness 当前亮度覆盖值（0~1），小于 0 表示尚未覆盖过
 * @param deltaFraction 满量程比例增量（正值增加亮度，1 = 100%）
 * @param systemBrightness 系统亮度（0~1），未覆盖时作为调整起点
 * @return 调整后的亮度（0.01~1）
 */
fun calculateBrightness(
    currentBrightness: Float,
    deltaFraction: Float,
    systemBrightness: Float,
): Float {
    val base = if (currentBrightness < 0) systemBrightness else currentBrightness
    return (base + deltaFraction).coerceIn(0.01f, 1f)
}

/**
 * 亮度调节辅助函数。
 *
 * 通过修改 Activity 窗口的 `screenBrightness` 控制亮度。首次调节以系统亮度为起点，
 * 避免一上来就跳到 50%。
 *
 * @param activity 当前 Activity。
 * @param deltaFraction 满量程比例增量（正值增加亮度）。
 * @param currentBrightness 当前亮度值（0~1），-1 表示系统默认。
 * @return 调整后的亮度值（0~1）。
 */
fun adjustBrightness(
    activity: Activity,
    deltaFraction: Float,
    currentBrightness: Float,
): Float {
    val systemBrightness =
        Settings.System.getInt(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            (SYSTEM_BRIGHTNESS_FALLBACK * 255).toInt(),
        ) / 255f
    val clamped = calculateBrightness(currentBrightness, deltaFraction, systemBrightness)
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = clamped
    activity.window.attributes = layoutParams
    return clamped
}

/** 读不到系统亮度时的兜底值（0~1）。 */
private const val SYSTEM_BRIGHTNESS_FALLBACK = 0.5f

/**
 * 按系统音量与当前覆盖值计算调整后的音量比例。
 *
 * 与 [calculateBrightness] 同构：都是「float 状态 + 满量程比例增量 + clamp」。
 *
 * @param currentVolume 当前音量比例（0~1），小于 0 表示尚未覆盖过
 * @param deltaFraction 满量程比例增量（正值增大音量，1 = 最大音量）
 * @param systemVolume 系统音量比例（0~1），未覆盖时作为调整起点
 * @return 调整后的音量比例（0~1）
 */
fun calculateVolume(
    currentVolume: Float,
    deltaFraction: Float,
    systemVolume: Float,
): Float {
    val base = if (currentVolume < 0) systemVolume else currentVolume
    return (base + deltaFraction).coerceIn(0f, 1f)
}

/**
 * 音量调节辅助函数。
 *
 * 与 [adjustBrightness] 同构：维护 0~1 的音量比例，按比例写入系统音量整数格。
 * 比例与系统格数的换算使用四舍五入，保证提示百分比与实际音量一致。
 *
 * @param audioManager 系统音频管理器。
 * @param deltaFraction 满量程比例增量（正值增大音量）。
 * @param currentVolume 当前音量比例（0~1），-1 表示尚未接管。
 * @return 调整后的音量比例（0~1）。
 */
fun adjustVolume(
    audioManager: AudioManager,
    deltaFraction: Float,
    currentVolume: Float,
): Float {
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    val newVolume = calculateVolume(currentVolume, deltaFraction, systemVolume)
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (newVolume * maxVolume).roundToInt(),
        0,
    )
    return newVolume
}
