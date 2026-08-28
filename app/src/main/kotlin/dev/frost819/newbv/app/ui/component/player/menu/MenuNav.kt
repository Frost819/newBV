package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.app.ui.component.player.ifElse
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerMenuNavItem

/**
 * 菜单导航列表（右侧）。
 *
 * 渲染 4 个导航 Tab（倍速/画质/弹幕/字幕），焦点切换时自动切换菜单面板。
 * 展开时显示图标+文本，折叠时仅显示图标。
 *
 * @param modifier 修饰符
 * @param selectedMenu 当前选中的导航项
 * @param onSelectedChanged 选中变化回调
 * @param isFocusing 当前是否聚焦在导航列表
 */
@Composable
fun MenuNavList(
    modifier: Modifier = Modifier,
    selectedMenu: VideoPlayerMenuNavItem,
    onSelectedChanged: (VideoPlayerMenuNavItem) -> Unit,
    isFocusing: Boolean,
) {
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LazyColumn(
        modifier =
            modifier
                .focusRestorer(restorerFocusRequester)
                .focusRequester(focusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        itemsIndexed(VideoPlayerMenuNavItem.entries) { index, item ->
            MenuListItem(
                modifier =
                    Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                text = item.displayName,
                icon = item.icon,
                expanded = isFocusing,
                selected = selectedMenu == item,
                onClick = { onSelectedChanged(item) },
                onFocus = { onSelectedChanged(item) },
            )
        }
    }
}
