package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.app.ui.component.player.ifElse

/**
 * 多选菜单列表。
 *
 * 使用 [LazyColumn] 渲染可选项，支持多选。
 * 按方向右键返回父级，焦点恢复到第一项。
 *
 * @param modifier 修饰符
 * @param items 选项文本列表
 * @param selected 已选中项索引列表
 * @param onSelectedChanged 选中变化回调（返回新的选中索引列表）
 * @param onFocusBackToParent 返回父级回调
 */
@Composable
fun CheckBoxMenuList(
    modifier: Modifier = Modifier,
    items: List<String>,
    selected: List<Int> = listOf(),
    onSelectedChanged: (indexes: List<Int>) -> Unit,
    onFocusBackToParent: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier =
            modifier
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    val result = it.key == Key.DirectionRight
                    if (result) onFocusBackToParent()
                    result
                }.focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 8.dp),
    ) {
        itemsIndexed(items) { index, item ->
            MenuListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                text = item,
                selected = selected.contains(index),
                onClick = {
                    val newSelectedIndexes = selected.toMutableList()
                    if (newSelectedIndexes.contains(index)) {
                        newSelectedIndexes.remove(index)
                    } else {
                        newSelectedIndexes.add(index)
                    }
                    onSelectedChanged(newSelectedIndexes)
                },
            )
        }
    }
}
