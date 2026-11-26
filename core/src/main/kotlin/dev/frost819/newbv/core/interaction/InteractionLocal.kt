package dev.frost819.newbv.core.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * 当前输入方式。
 *
 * 追踪器本身由 Activity 持有并订阅，组件树只需要这个普通值。
 */
val LocalInputMethod = compositionLocalOf { InputMethod.DPad }

/**
 * 在 Composable 中获取当前 [InputMethod]。
 *
 * 优先读 [LocalInputMethod]；未注入（例如 Preview）时按 [InputMethod.DPad] 处理。
 */
@Composable
fun currentInputMethod(): InputMethod = LocalInputMethod.current

/**
 * 判断当前是否应显示焦点视觉反馈（边框/缩放）。
 *
 * 仅在 [InputMethod.DPad] 模式下返回 `true`，触屏模式下隐藏焦点边框。
 */
@Composable
fun shouldShowFocusVisual(): Boolean = currentInputMethod() == InputMethod.DPad
