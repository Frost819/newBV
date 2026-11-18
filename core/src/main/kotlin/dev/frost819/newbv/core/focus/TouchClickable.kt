package dev.frost819.newbv.core.focus

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.tv.material3.MaterialTheme

/**
 * 为 TV Material3 组件添加触屏点击支持。
 *
 * TV Material3 的 `tvClickable()` / `tvSelectable()` 仅处理 D-Pad Enter 键事件，
 * 不包含 `pointerInput`/`clickable`，导致触屏点击无效。
 *
 * **根因**：不能在同一 modifier 链中叠加多个 `pointerInput` 修饰符
 * （如 `clickable` + `pointerInput(detectTapGestures)`），
 * 内层的 `pointerInput` 会消费 DOWN 事件（`change.consume()`），
 * 导致外层的 `detectTapGestures` 因 `requireUnconsumed=true` 而跳过该事件。
 *
 * **解决方案**：使用单个 `pointerInput` + `detectTapGestures`，
 * 不依赖 `clickable`/`combinedClickable`，避免多 `pointerInput` 冲突。
 * TV Material3 组件的 focusable/semantics/pressed-state 由其内部
 * `tvClickable`/`tvSelectable` 已处理，此处仅补充触屏手势检测。
 *
 * 触屏点击时仍通过 [FocusRequester] 请求焦点：项目里有大量「焦点即选中」的
 * Tab / 菜单 / Chip，去掉这一步会让触屏点不中这些入口。由此带来的两个副作用
 * 在别处消解：TvLazyVerticalGrid 在 Touch 模式改用系统默认 bringIntoView，
 * 卡片边框与反色在 Touch 模式不显示。
 *
 * 按压反馈不走 ripple：TV 组件自己带背景，indication 会被背景盖住。
 * 这里在 draw 阶段叠一层半透明高亮，按下时只重绘、不重组。
 *
 * 使用 `composed` + `rememberUpdatedState` 确保回调始终为最新值，
 * 同时 `pointerInput` 的 key 仅在 `onLongClick` 有无状态变化时重启协程。
 *
 * @param onClick 单击回调。
 * @param onLongClick 长按回调，null 时仅处理单击且响应更快（无长按等待）。
 */
fun Modifier.touchClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier =
    composed {
        val currentOnClick by rememberUpdatedState(onClick)
        val currentOnLongClick by rememberUpdatedState(onLongClick)
        val hasLongClick = onLongClick != null
        val focusRequester = remember { FocusRequester() }
        // 按下态只在绘制阶段读，手指按下/抬起都只是 invalidate 绘制。
        var pressed by remember { mutableStateOf(false) }
        val pressColor = MaterialTheme.colorScheme.onSurface.copy(alpha = PressHighlightAlpha)

        this
            .focusRequester(focusRequester)
            // 鼠标/触控板悬停时给手型光标，机器上接了鼠标也能看出哪里能点。
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(hasLongClick) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            // 手势被滚动/长按抢走时 tryAwaitRelease 返回 false，这里同样清掉高亮。
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        runCatching { focusRequester.requestFocus() }
                        currentOnClick()
                    },
                    onLongPress =
                        if (hasLongClick) {
                            {
                                runCatching { focusRequester.requestFocus() }
                                currentOnLongClick?.invoke()
                            }
                        } else {
                            null
                        },
                )
            }.drawWithContent {
                drawContent()
                if (pressed) {
                    drawRect(color = pressColor)
                }
            }
    }

/** 按压高亮透明度：深浅主题都用 onSurface，浅色下是淡灰、深色下是淡白。 */
private const val PressHighlightAlpha = 0.10f
