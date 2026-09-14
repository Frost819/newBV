package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState

/**
 * 视频/直播播放器二级设置菜单（MenuNav ↔ Items）。
 *
 * 直接渲染选项列表（[RadioMenuList]），没有中间子项列表。
 * 焦点进入 [MenuFocusState.Items] 时请求列表焦点，按 RIGHT 返回导航栏。
 * 与 [PlayerThreeLevelMenu] 对应：三级菜单多一层中间子项列表。
 *
 * @param items 选项列表
 * @param selected 当前选中项；不在 [items] 中时回退到第一项
 * @param itemLabel 选项文本访问器
 * @param onItemSelected 选项变化回调
 * @param navFocusRequester 导航栏焦点请求器，返回时用于请求焦点
 * @param onFocusStateChange 焦点状态变化回调
 * @param modifier 修饰符
 */
@Composable
fun <T> PlayerTwoLevelMenu(
    items: List<T>,
    selected: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    navFocusRequester: FocusRequester,
    onFocusStateChange: (MenuFocusState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = LocalMenuFocusStateData.current
    val itemsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusState.focusState) {
        if (focusState.focusState == MenuFocusState.Items) {
            runCatching { itemsFocusRequester.requestFocus() }
        }
    }

    val selectedIndex = items.indexOfFirst { it == selected }.coerceAtLeast(0)

    AnimatedVisibility(visible = true) {
        RadioMenuList(
            modifier =
                modifier
                    .width(216.dp)
                    .padding(horizontal = 8.dp)
                    .focusRequester(itemsFocusRequester),
            items = items.map(itemLabel),
            selected = selectedIndex,
            onSelectedChanged = { onItemSelected(items[it]) },
            onFocusBackToParent = {
                onFocusStateChange(MenuFocusState.MenuNav)
                navFocusRequester.requestFocus()
            },
        )
    }
}
