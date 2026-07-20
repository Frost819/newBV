package dev.frost819.newbv.core.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 获取焦点时显示边框。
 *
 * TV D-Pad 导航时为获得焦点的元素添加视觉反馈。
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
    width: androidx.compose.ui.unit.Dp = 3.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "focused-border-transition")
    var hasFocus by remember { mutableStateOf(false) }

    val animateColor by infiniteTransition.animateColor(
        initialValue = color.copy(alpha = 1f),
        targetValue = color.copy(alpha = 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focused-border-color"
    )
    val borderColor = if (hasFocus) {
        if (animate) animateColor else color
    } else Color.Transparent

    onFocusChanged { hasFocus = it.hasFocus }
        .border(width = width, color = borderColor, shape = shape)
}

/**
 * 未获焦点时缩小，获焦点时恢复原大小，制造"放大"效果。
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
