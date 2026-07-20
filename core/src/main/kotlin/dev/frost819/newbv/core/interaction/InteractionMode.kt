package dev.frost819.newbv.core.interaction

import android.content.res.Configuration

/**
 * 交互模式。
 *
 * 区分 D-Pad（遥控器/键盘）与 Touch（触屏）两种交互方式。
 * 由 [InteractionModeDetector] 根据设备 touchscreen 配置判断。
 *
 * @see InteractionModeDetector
 */
enum class InteractionMode {
    /** D-Pad / 遥控器 / 键盘方向键导航。 */
    DPad,

    /** 触屏点击/手势。 */
    Touch;

    val isTouch: Boolean get() = this == Touch
    val isDPad: Boolean get() = this == DPad
}

/**
 * 交互模式检测器。
 *
 * 根据 Android [Configuration.touchscreen] 判断设备交互模式：
 * - [Configuration.TOUCHSCREEN_UNDEFINED]：无触屏（TV/机顶盒）→ [InteractionMode.DPad]
 * - [Configuration.TOUCHSCREEN_FINGER] / [Configuration.TOUCHSCREEN_STYLUS]：有触屏 → [InteractionMode.Touch]
 *
 * 将检测逻辑抽离为纯函数，便于单元测试。
 */
object InteractionModeDetector {

    /**
     * 根据 touchscreen 配置值判断交互模式。
     *
     * @param touchscreen [Configuration.touchscreen] 的值。
     * @return 对应的 [InteractionMode]。
     */
    fun detect(touchscreen: Int): InteractionMode =
        if (touchscreen == Configuration.TOUCHSCREEN_UNDEFINED) InteractionMode.DPad
        else InteractionMode.Touch
}
