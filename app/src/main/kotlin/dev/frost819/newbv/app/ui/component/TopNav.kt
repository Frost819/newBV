package dev.frost819.newbv.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 顶部导航 Tab 栏。
 *
 * TV Material3 [TabRow] 封装，支持 D-Pad 焦点导航。
 * Tab 切换时触发 [onSelectedChanged]，点击同一 Tab 触发 [onClick]（用于刷新）。
 *
 * @param items Tab 项列表（已按首选项排序）。
 * @param selectedIndex 当前选中的 Tab 索引（由外部控制，用于导航返回后恢复）。
 * @param isLargePadding 内容区未获焦点时使用较大内边距。
 * @param accentColor 当前页面 Tab 的强调色。
 * @param onSelectedChanged Tab 焦点切换回调。
 * @param onClick Tab 点击回调。
 */
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    selectedIndex: Int = 0,
    isLargePadding: Boolean,
    accentColor: Color? = null,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    var selectedTabIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        selectedTabIndex = selectedIndex
    }
    val verticalPadding by animateDpAsState(
        targetValue = if (isLargePadding) 12.dp else 6.dp,
        label = "top-nav-padding",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, verticalPadding),
        horizontalArrangement = Arrangement.Center,
    ) {
        TabRow(
            modifier = Modifier
                .focusRestorer(focusRequester),
            selectedTabIndex = selectedTabIndex,
            separator = { Spacer(modifier = Modifier.width(12.dp)) },
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
                    accentColor = accentColor ?: MaterialTheme.colorScheme.primary,
                    onClick = { onClick(tab) },
                )
            }
        }
    }
}

@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val containerColor = if (selected) {
        accentColor.copy(alpha = if (hasFocus) 0.24f else 0.14f)
    } else {
        Color.Transparent
    }
    val borderColor = if (hasFocus) accentColor else Color.Transparent
    Tab(
        modifier = modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(2.dp, borderColor, RoundedCornerShape(50))
            .touchClickable(onClick = onClick),
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
            items = listOf(
                DummyTopNavItem("推荐"),
                DummyTopNavItem("热门"),
                DummyTopNavItem("动态"),
            ),
            isLargePadding = true,
        )
    }
}
