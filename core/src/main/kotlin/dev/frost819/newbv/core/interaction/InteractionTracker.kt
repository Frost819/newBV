package dev.frost819.newbv.core.interaction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 输入方式。
 *
 * 表示用户**最近一次**使用的输入方式，而非设备能力。
 * 应用始终同时支持两种输入，此枚举仅用于决定焦点视觉反馈是否显示：
 * - [DPad]：显示焦点边框（遥控器/键盘导航需要视觉提示）
 * - [Touch]：隐藏焦点边框（触屏点击自带视觉反馈，边框多余）
 *
 * @see InteractionTracker
 */
enum class InputMethod {
    /** D-Pad / 遥控器 / 键盘方向键导航。 */
    DPad,

    /** 触屏点击/手势。 */
    Touch;

    val isTouch: Boolean get() = this == Touch
    val isDPad: Boolean get() = this == DPad
}

/**
 * 交互追踪器。
 *
 * 运行时追踪用户最近一次的输入方式（Touch 或 DPad），
 * 驱动焦点视觉反馈的显示/隐藏。
 *
 * **使用方式**：
 * 1. 在 Activity 中创建实例，通过 [inputMethod] 观察当前状态。
 * 2. 在 `onTouchEvent` 中调用 [onTouch]。
 * 3. 在 `dispatchKeyEvent` 中调用 [onDpadKey]（任意物理按键均表示遥控器/键盘使用）。
 * 4. 通过 [LocalInputMethod] 在 Composable 树中提供当前状态。
 *
 * **设计理由**：
 * 不根据设备能力互斥地选择交互模式，而是始终同时支持两种输入。
 * 仅根据最近输入动态切换焦点边框可见性，避免触屏用户看到多余边框，
 * 同时保证遥控器用户随时拿起遥控器就能看到焦点提示。
 */
class InteractionTracker(initial: InputMethod = InputMethod.DPad) {

    private val _inputMethod = MutableStateFlow(initial)
    val inputMethod: StateFlow<InputMethod> = _inputMethod.asStateFlow()

    /** 当前输入方式（非 Compose 环境下可直接读取）。 */
    val current: InputMethod get() = _inputMethod.value

    /**
     * 标记发生了触摸事件，切换到 [InputMethod.Touch]。
     */
    fun onTouch() {
        _inputMethod.value = InputMethod.Touch
    }

    /**
     * 标记发生了 D-Pad 按键事件，切换到 [InputMethod.DPad]。
     */
    fun onDpadKey() {
        _inputMethod.value = InputMethod.DPad
    }

    /**
     * 重置为初始状态。
     */
    fun reset() {
        _inputMethod.value = InputMethod.DPad
    }
}
