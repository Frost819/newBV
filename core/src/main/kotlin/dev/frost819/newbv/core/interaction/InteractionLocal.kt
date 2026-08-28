package dev.frost819.newbv.core.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal，提供当前 [InteractionTracker]。
 *
 * 由 Activity 层通过 `CompositionLocalProvider(LocalInteractionTracker provides tracker)` 注入。
 * 子树中的 [focusedBorder][dev.frost819.newbv.core.focus.focusedBorder] 等组件读取此 tracker
 * 决定是否显示焦点边框。
 *
 * 默认值为 `null`，未注入时 [currentInputMethod] 回退到 [InputMethod.DPad]。
 */
val LocalInteractionTracker =
    staticCompositionLocalOf<InteractionTracker?> {
        null
    }

/**
 * 在 Composable 中获取当前 [InputMethod]。
 *
 * 读取 [LocalInteractionTracker] 的状态，若未注入则默认 [InputMethod.DPad]。
 *
 * @return 当前输入方式。
 */
@Composable
fun currentInputMethod(): InputMethod {
    val tracker = LocalInteractionTracker.current ?: return InputMethod.DPad
    val state by tracker.inputMethod.collectAsState()
    return state
}

/**
 * 判断当前是否应显示焦点视觉反馈（边框/缩放）。
 *
 * 仅在 [InputMethod.DPad] 模式下返回 `true`，
 * 触屏模式下隐藏焦点边框。
 */
@Composable
fun shouldShowFocusVisual(): Boolean = currentInputMethod() == InputMethod.DPad
