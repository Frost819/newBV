package dev.frost819.newbv.core.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.core.interaction.InputMethod
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 焦点反馈动画统一走短 tween，避免 spring 长时间占用帧。 */
private val FOCUS_ANIM_SPEC: AnimationSpec<Float> = tween(durationMillis = 110)

/**
 * 获取焦点时显示边框。
 *
 * 仅在 [InputMethod.DPad] 模式下显示焦点边框，触屏模式下隐藏。
 *
 * 实现上把焦点状态和输入方式都推迟到 draw 阶段读取：Composable 本身不订阅，
 * 方向键移动时不会触发任何重组。旧实现无条件创建 rememberInfiniteTransition，
 * 即使 animate=false 也让每个节点每帧重组。
 *
 * @param shape 边框形状，默认 [ShapeDefaults.Large]。
 * @param animate 保留以兼容调用方；项目内无 animate=true，呼吸效果已移除。
 * @param color 边框颜色，默认使用当前主题的边框色。
 * @param width 边框宽度，默认 3dp。
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.focusedBorder(
    shape: Shape = ShapeDefaults.Large,
    animate: Boolean = false,
    color: Color? = null,
    width: Dp = 3.dp,
): Modifier =
    composed {
        val tracker = LocalInteractionTracker.current
        val resolvedColor = color ?: MaterialTheme.colorScheme.border
        val focused = remember { mutableStateOf(false) }
        // 拿到 State 对象即可，不在组合阶段读 .value
        val inputMethodState = tracker?.inputMethod?.collectAsState()

        this
            .onFocusChanged { focused.value = it.hasFocus }
            .drawWithContent {
                drawContent()
                val visible =
                    focused.value && inputMethodState?.value != InputMethod.Touch
                if (visible) {
                    val radius =
                        when (shape) {
                            is RoundedCornerShape -> shape.topStart.toPx(size, this)
                            else -> 12.dp.toPx()
                        }
                    drawRoundRect(
                        color = resolvedColor,
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = width.toPx()),
                    )
                }
            }
    }

/**
 * 未获焦点时缩小，获焦点时恢复原大小。
 * scale 由 Animatable 在 graphicsLayer 阶段读取，每帧只重绘 layer，不重组。
 */
fun Modifier.focusedScale(scale: Float = 0.9f): Modifier =
    composed {
        val scope = rememberCoroutineScope()
        val animatedScale = remember(scale) { Animatable(scale) }

        this
            .onFocusChanged { state ->
                scope.launch {
                    animatedScale.animateTo(
                        targetValue = if (state.hasFocus) 1f else scale,
                        animationSpec = FOCUS_ANIM_SPEC,
                    )
                }
            }
            .graphicsLayer {
                val value = animatedScale.value
                scaleX = value
                scaleY = value
            }
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

/**
 * 构建「聚焦反色」的可点击 Surface 配色。
 *
 * TV 遥控器场景焦点必须足够醒目：聚焦时容器切换为
 * [androidx.tv.material3.ColorScheme.inverseSurface]，
 * 内容切换为 [androidx.tv.material3.ColorScheme.inverseOnSurface]，
 * 实现整体明暗反色。未聚焦时使用调用方传入的容器/内容色，
 * 以便表达「已选中 / 已激活」等常态。
 *
 * 注意：Surface 的内容色通过 [androidx.tv.material3.LocalContentColor] 下传，
 * 子组件（[androidx.tv.material3.Text] / [androidx.tv.material3.Icon]）**不要**再硬编码颜色，
 * 否则聚焦时不会随容器一起反色。
 *
 * @param containerColor 未聚焦时的容器色。
 * @param contentColor 未聚焦时的内容色。
 */
@Composable
@ReadOnlyComposable
fun focusInvertedColors(
    containerColor: Color,
    contentColor: Color,
): ClickableSurfaceColors =
    ClickableSurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
