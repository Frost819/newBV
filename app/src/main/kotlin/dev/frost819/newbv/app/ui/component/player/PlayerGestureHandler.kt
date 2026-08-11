package dev.frost819.newbv.app.ui.component.player

import android.app.Activity
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * 手势状态，用于驱动 [GestureTip] 覆盖层显示。
 *
 * @property isActive 手势是否正在执行中（用于显示提示）。
 * @property type 当前手势类型。
 * @property value 当前手势值（亮度百分比 0~1、音量百分比 0~100、倍速值）。
 */
data class GestureTipState(
    val isActive: Boolean = false,
    val type: GestureTipType = GestureTipType.None,
    val value: Float = 0f,
)

/**
 * 手势提示类型。
 */
enum class GestureTipType {
    None,
    Brightness,
    Volume,
    Speed,
    Seek,
}

/**
 * 播放器手势回调。
 *
 * @param onSingleTap 单击：显示/隐藏控制器。
 * @param onDoubleTap 双击：播放/暂停。
 * @param onSeekDelta 水平拖拽 seek：正值快进、负值快退（毫秒增量）。
 * @param onSeekCommit seek 提交（手指松开时调用）。
 * @param onBrightnessChange 亮度变化：deltaY > 0 增加亮度，< 0 降低亮度。
 * @param onVolumeChange 音量变化：deltaY > 0 增加音量，< 0 降低音量。
 * @param onCycleAspectRatio 捏合缩放：循环切换宽高比。
 */
data class PlayerGestureCallbacks(
    val onSingleTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onSeekDelta: (deltaMs: Long) -> Unit,
    val onSeekCommit: () -> Unit,
    val onBrightnessChange: (deltaY: Float) -> Unit,
    val onVolumeChange: (deltaY: Float) -> Unit,
    val onCycleAspectRatio: () -> Unit,
)

/**
 * 播放器手势处理器。
 *
 * 使用 `awaitEachGesture` 手动分发 6 种手势（PRD 4.3.3.2）：
 * - 单击：显示/隐藏控制器
 * - 双击：播放/暂停
 * - 水平滑动：快进/快退
 * - 左半屏垂直滑动：亮度调节
 * - 右半屏垂直滑动：音量调节
 * - 双指捏合：宽高比循环
 *
 * D-pad 模式不受影响——此 modifier 仅处理触摸事件，按键事件由 `onPreviewKeyEvent` 处理。
 *
 * @param totalDuration 视频总时长（毫秒），用于将像素位移转换为时间增量。
 * @param viewWidth 容器宽度（像素），用于判断左/右半屏。
 * @param callbacks 手势回调。
 * @param gestureTipState 手势提示状态（外部持有，用于驱动覆盖层 UI）。
 */
@Composable
fun rememberGestureTipState(): androidx.compose.runtime.MutableState<GestureTipState> =
    remember { mutableStateOf(GestureTipState()) }

/**
 * 播放器手势 Modifier 扩展。
 *
 * 使用 `awaitEachGesture` 手动分发触摸事件，避免多个 `detectXxxGestures` 互相消费事件。
 *
 * @param totalDuration 视频总时长（毫秒）。
 * @param controllerVisible 控制器（信息栏+进度条）是否可见。可见时水平拖拽交给进度条处理。
 * @param callbacks 手势回调集合。
 * @param gestureTipState 手势提示状态（外部持有）。
 */
fun Modifier.playerGestures(
    totalDuration: () -> Long,
    controllerVisible: () -> Boolean,
    callbacks: PlayerGestureCallbacks,
    gestureTipState: androidx.compose.runtime.MutableState<GestureTipState>,
): Modifier = this.pointerInput(Unit) {
    val doubleTapTimeout = 300L
    val tapSlop = 40f
    val dragThreshold = 10f

    var lastTapTime = 0L

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val startTime = System.currentTimeMillis()
        val startX = firstDown.position.x
        val startY = firstDown.position.y
        val width = this.size.width.toFloat()

        var isDragging = false
        var totalDeltaX = 0f
        var totalDeltaY = 0f
        var isHorizontalDrag: Boolean? = null

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val changes = event.changes

            // 多指检测（捏合缩放）
            if (changes.size >= 2) {
                gestureTipState.value = GestureTipState(isActive = false)
                changes.forEach { it.consume() }
                if (changes.all { !it.pressed }) {
                    callbacks.onCycleAspectRatio()
                    break
                }
                continue
            }

            val change = changes.firstOrNull() ?: continue

            if (!change.pressed) {
                // 手指抬起
                val duration = System.currentTimeMillis() - startTime

                // 如果事件已被子组件消费（如按钮点击），跳过手势处理
                if (change.isConsumed) break

                if (isDragging) {
                    if (isHorizontalDrag == true) {
                        callbacks.onSeekCommit()
                    }
                    gestureTipState.value = GestureTipState(isActive = false)
                } else {
                    // 判断是否为 tap
                    val moved = abs(change.position.x - startX) > tapSlop ||
                        abs(change.position.y - startY) > tapSlop
                    if (!moved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < doubleTapTimeout) {
                            callbacks.onDoubleTap()
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                            callbacks.onSingleTap()
                        }
                    }
                }
                change.consume()
                break
            }

            // 如果事件已被子组件消费（如进度条拖拽），跳过移动处理
            if (change.isConsumed) continue

            // 手指移动中
            if (change.positionChanged()) {
                val deltaX = change.positionChange().x
                val deltaY = change.positionChange().y
                totalDeltaX += deltaX
                totalDeltaY += deltaY

                val absX = abs(totalDeltaX)
                val absY = abs(totalDeltaY)

                // 判断拖拽方向（仅首次超过阈值时）
                if (isHorizontalDrag == null && (absX > dragThreshold || absY > dragThreshold)) {
                    isHorizontalDrag = absX > absY
                    isDragging = true
                }

                if (isHorizontalDrag == true) {
                    // 水平拖拽 → seek
                    val durationMs = totalDuration()
                    if (durationMs > 0) {
                        val deltaMs = (deltaX / width * durationMs * 0.5f).toLong()
                        if (deltaMs != 0L) {
                            callbacks.onSeekDelta(deltaMs)
                            gestureTipState.value = GestureTipState(
                                isActive = true,
                                type = GestureTipType.Seek,
                            )
                        }
                    }
                } else if (isHorizontalDrag == false) {
                    // 垂直拖拽 → 亮度/音量
                    // Compose 中 y 向下为正，上滑（deltaY < 0）应增加亮度/音量，故取反
                    val isLeftHalf = startX < width / 2
                    if (isLeftHalf) {
                        callbacks.onBrightnessChange(-deltaY)
                    } else {
                        callbacks.onVolumeChange(-deltaY)
                    }
                }
                change.consume()
            }
        }
    }
}

/**
 * 亮度调节辅助函数。
 *
 * 通过修改 Activity 窗口的 `screenBrightness` 控制亮度。
 *
 * @param activity 当前 Activity。
 * @param deltaY 垂直位移增量（正值增加亮度，负值降低亮度）。
 * @param currentBrightness 当前亮度值（0~1），-1 表示系统默认。
 * @return 调整后的亮度值（0~1）。
 */
fun adjustBrightness(
    activity: Activity,
    deltaY: Float,
    currentBrightness: Float,
): Float {
    val newBrightness = if (currentBrightness < 0) {
        0.5f + deltaY / 1000f
    } else {
        currentBrightness + deltaY / 1000f
    }
    val clamped = newBrightness.coerceIn(0.01f, 1f)
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = clamped
    activity.window.attributes = layoutParams
    return clamped
}

/**
 * 音量调节辅助函数。
 *
 * @param audioManager 系统音频管理器。
 * @param deltaY 垂直位移增量（正值增加音量，负值降低音量）。
 * @return 调整后的音量百分比（0~100）。
 */
fun adjustVolume(
    audioManager: AudioManager,
    deltaY: Float,
): Int {
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val deltaSteps = (deltaY / 80f).toInt()
    val newVolume = (currentVolume + deltaSteps).coerceIn(0, maxVolume)
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        newVolume,
        0,
    )
    return (newVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
}
