package dev.frost819.newbv.app.ui.component.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 区分播放器确认键的短按、长按和取消。 */
internal class PlayerConfirmKeyHandler(
    private val scope: CoroutineScope,
    private val longPressTimeoutMs: Long,
    private val onShortPress: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onLongPressEnd: () -> Unit,
) {
    private var pressedKeyCode: Int? = null
    private var longPressJob: Job? = null
    private var longPressTriggered = false

    /**
     * 开始计时。忽略系统长按产生的重复按下事件。
     *
     * @param keyCode 确认键的 Android 键码
     * @param isRepeat 是否为重复按下事件
     */
    fun onKeyDown(keyCode: Int, isRepeat: Boolean) {
        if (isRepeat || pressedKeyCode == keyCode) return
        cancel()
        pressedKeyCode = keyCode
        // 由系统长按时限驱动，兼容只发送一次按下事件的遥控器。
        longPressJob =
            scope.launch {
                delay(longPressTimeoutMs)
                longPressTriggered = true
                onLongPress()
            }
    }

    /**
     * 处理与本次按下匹配的松手事件。
     *
     * @param keyCode 抬起的 Android 键码
     * @param isCanceled 是否是系统取消按键序列产生的抬起事件
     * @return 是否消费了本次松手事件
     */
    fun onKeyUp(keyCode: Int, isCanceled: Boolean): Boolean {
        if (pressedKeyCode != keyCode) return false
        val isShortPress = !longPressTriggered && !isCanceled
        cancel()
        if (isShortPress) onShortPress()
        return true
    }

    /** 取消待触发的长按，或结束已触发的长按。 */
    fun cancel() {
        longPressJob?.cancel()
        longPressJob = null
        pressedKeyCode = null
        if (longPressTriggered) {
            longPressTriggered = false
            onLongPressEnd()
        }
    }
}
