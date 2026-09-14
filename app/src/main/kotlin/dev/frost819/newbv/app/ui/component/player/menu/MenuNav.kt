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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.app.ui.component.player.ifElse
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem

/**
 * 菜单导航列表（右侧）。
 *
 * 渲染导航 Tab，焦点切换时自动切换菜单面板。展开时显示图标+文本，折叠时仅显示图标。
 * 视频播放器与直播播放器共用，通过 [label]/[icon] 访问器适配各自的导航枚举。
 *
 * @param items 导航项列表
 * @param selected 当前选中的导航项
 * @param isFocusing 当前是否聚焦在导航列表
 * @param label 导航项文本访问器
 * @param icon 导航项图标访问器
 * @param onSelectedChanged 选中变化回调
 * @param modifier 修饰符
 * @param focusRequester 导航列表焦点请求器，默认内部创建；直播侧需与值面板共享时由外部传入
 */
@Composable
fun <T> MenuNavList(
    items: List<T>,
    selected: T,
    isFocusing: Boolean,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelectedChanged: (T) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val restorerFocusRequester = remember { FocusRequester() }

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
        itemsIndexed(items) { index, item ->
            MenuListItem(
                modifier =
                    Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                text = label(item),
                icon = icon(item),
                expanded = isFocusing,
                selected = selected == item,
                onClick = { onSelectedChanged(item) },
                onFocus = { onSelectedChanged(item) },
            )
        }
    }
}
