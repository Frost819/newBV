package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** 胶囊圆角＝高度的一半，直接在绘制阶段按 px 算，不再为每个 tab 构造 RoundedCornerShape。 */
private const val PillRadiusFactor = 0.5f

/**
 * 顶部导航 Tab 栏。
 *
 * 刻意不使用 tv-material3 的 TabRow / Tab：那套组件对指示器位置、容器色、描边、缩放
 * 都做补间动画，快速左右切换时上一帧的胶囊还没退场、下一帧已经在画，视觉上就是重影；
 * 一次按键还要等动画跑完才停稳，手感就是「不跟手」。
 *
 * 这里用一行 [Row] 手工实现：没有指示器、没有补间，选中与聚焦在绘制阶段直接读值，
 * 按一下立刻到位；焦点变化只触发重绘，不触发重组。
 *
 * @param items Tab 项列表（已按首选项排序）。
 * @param selectedIndex 当前选中的 Tab 索引（由外部控制，用于导航返回后恢复）。
 * @param isLargePadding 内容区未获焦点时使用较大内边距。
 * @param onSelectedChanged Tab 焦点切换回调。
 * @param onClick Tab 点击回调。
 */
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    selectedIndex: Int = 0,
    isLargePadding: Boolean,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    var selectedTabIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        selectedTabIndex = selectedIndex
    }
    // 直接取值。原先用 animateDpAsState 让顶栏高度在焦点进出内容区时持续动画，
    // 每帧都会让整个 Scaffold 重新测量。
    val verticalPadding = if (isLargePadding) 12.dp else 6.dp

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp, verticalPadding)
                // 焦点回到顶栏时落在上次停留的 tab，而不是永远回第一个。
                .focusRestorer(focusRequester),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, tab ->
            NavItemTab(
                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                topNavItem = tab,
                selected = index == selectedTabIndex,
                onFocus = {
                    selectedTabIndex = index
                    onSelectedChanged(tab)
                },
                onClick = { onClick(tab) },
            )
        }
    }
}

@Composable
private fun NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    // 统一使用品牌青绿作为 TopNav 强调色，避免各页面颜色不一致
    val accentColor = MaterialTheme.colorScheme.secondary
    // 焦点写进 State 后只在 drawBehind 里读：焦点变化走「重绘」而不是「重组」。
    val focusState = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .onFocusChanged { state ->
                    focusState.value = state.isFocused
                    if (state.isFocused) onFocus()
                }
                // clickable 自带 focusable 语义与 D-Pad 中键/Enter 处理；
                // 关掉水波纹，视觉反馈交给下面的 drawBehind。
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).drawBehind {
                    val focused = focusState.value
                    if (!selected && !focused) return@drawBehind

                    val radius = CornerRadius(size.height * PillRadiusFactor)
                    val fill =
                        if (selected) {
                            accentColor.copy(alpha = if (focused) 0.24f else 0.14f)
                        } else {
                            Color.Transparent
                        }
                    if (fill.alpha > 0f) {
                        drawRoundRect(color = fill, cornerRadius = radius)
                    }
                    if (focused) {
                        drawRoundRect(
                            color = accentColor,
                            cornerRadius = radius,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }.height(32.dp)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 用固定高度的 Box 居中文字：直接对 Text 设 height 会把字形顶对齐，
        // 字体行高与剩余空间不等时文字就偏上，不同字体/字号下表现不一致。
        Text(
            text = topNavItem.displayName,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * 顶部导航项接口。
 */
interface TopNavItem {
    val displayName: String
}

private data class DummyTopNavItem(
    override val displayName: String,
) : TopNavItem

@Preview(showBackground = true)
@Composable
private fun TopNavPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        TopNav(
            items =
                listOf(
                    DummyTopNavItem("推荐"),
                    DummyTopNavItem("热门"),
                    DummyTopNavItem("动态"),
                ),
            isLargePadding = true,
        )
    }
}
