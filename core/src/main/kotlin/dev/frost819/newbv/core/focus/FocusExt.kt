package dev.frost819.newbv.core.focus

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.interaction.currentInputMethod
import dev.frost819.newbv.core.interaction.InputMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 获取焦点时显示边框。
 *
 * 仅在 [InputMethod.DPad] 模式下显示焦点边框，
 * 触屏模式下隐藏（触屏点击自带视觉反馈，无需额外焦点提示）。
 *
 * 当 [LocalInteractionTracker] 未注入时，始终显示边框（兼容未接入 tracker 的场景）。
 *
 * @param shape 边框形状，默认 [ShapeDefaults.Large]。
 * @param animate 是否启用呼吸动画。
 * @param color 边框颜色，默认白色。
 * @param width 边框宽度，默认 3dp。
 */
fun Modifier.focusedBorder(
    shape: Shape = ShapeDefaults.Large,
    animate: Boolean = false,
    color: Color = Color.White,
    width: Dp = 3.dp
): Modifier = composed {
    val tracker = LocalInteractionTracker.current
    var hasFocus by remember { mutableStateOf(false) }

    val showBorder = if (tracker != null) {
        val method by tracker.inputMethod.collectAsState()
        hasFocus && method == InputMethod.DPad
    } else {
        hasFocus
    }

    val infiniteTransition = rememberInfiniteTransition(label = "focused-border-transition")
    val animateColor by infiniteTransition.animateColor(
        initialValue = color.copy(alpha = 1f),
        targetValue = color.copy(alpha = 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focused-border-color"
    )
    val borderColor = if (showBorder) {
        if (animate) animateColor else color
    } else Color.Transparent

    onFocusChanged { hasFocus = it.hasFocus }
        .border(width = width, color = borderColor, shape = shape)
}

/**
 * 未获焦点时缩小，获焦点时恢复原大小。
 *
 * 触屏模式下保持显示缩放效果（D-Pad 用户看到放大，触屏用户看到缩小）。
 * 注意：与 [focusedBorder] 不同，[focusedScale] 不会根据输入方式隐藏。
 *
 * @param scale 未获焦点时的缩放比例，默认 0.9。
 */
fun Modifier.focusedScale(scale: Float = 0.9f): Modifier = composed {
    var hasFocus by remember { mutableStateOf(false) }
    val scaleValue by animateFloatAsState(
        targetValue = if (hasFocus) 1f else scale,
        label = "focused-scale"
    )

    onFocusChanged { hasFocus = it.hasFocus }
        .scale(scaleValue)
}

/**
 * 改进的请求焦点方法。
 *
 * 首次请求失败后等待 100ms 重试一次，处理 Compose 焦点系统时序问题。
 *
 * @param scope 协程作用域，在 Main 调度器执行。
 */
fun FocusRequester.requestFocus(scope: CoroutineScope) {
    scope.launch(Dispatchers.Main) {
        runCatching {
            requestFocus()
        }.onFailure {
            delay(100)
            runCatching { requestFocus() }
        }
    }
}
