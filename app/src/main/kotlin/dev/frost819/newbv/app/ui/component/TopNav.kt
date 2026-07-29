package dev.frost819.newbv.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text

/**
 * 顶部导航 Tab 栏。
 *
 * TV Material3 [TabRow] 封装，支持 D-Pad 焦点导航。
 * Tab 切换时触发 [onSelectedChanged]，点击同一 Tab 触发 [onClick]（用于刷新）。
 *
 * @param items Tab 项列表（已按首选项排序）。
 * @param isLargePadding 内容区未获焦点时使用较大内边距。
 * @param onSelectedChanged Tab 焦点切换回调。
 * @param onClick Tab 点击回调。
 */
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    isLargePadding: Boolean,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
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
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    Tab(
        modifier = modifier,
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = topNavItem.displayName,
            color = LocalContentColor.current,
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
