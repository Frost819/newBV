package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState

/**
 * 视频播放器三级设置菜单布局。
 *
 * 双列布局：右侧为子项列表（`Menu` 层），左侧为当前子项对应的值面板（`Items` 层）。
 * 三态焦点中的 `Menu`/`Items` 两层在此统一管理：
 * - 子项列表 RIGHT → 导航栏（`MenuNav`）
 * - 子项列表 LEFT → 值面板（`Items`）
 * - 值面板 RIGHT → 返回子项列表（`Menu`）
 *
 * 用于画质、弹幕、字幕等「多个子项 + 每个子项对应一组选项」的设置面板。
 *
 * @param categories 子项列表（首次组合默认选中第一项；为空时不渲染）
 * @param categoryLabel 子项显示名称
 * @param onFocusStateChange 焦点状态变化回调
 * @param modifier 修饰符
 * @param valuePanel 值面板插槽，参数依次为当前子项、值面板修饰符、返回子项列表回调
 */
@Composable
fun <T> PlayerThreeLevelMenu(
    categories: List<T>,
    categoryLabel: (T) -> String,
    onFocusStateChange: (MenuFocusState) -> Unit,
    modifier: Modifier = Modifier,
    valuePanel: @Composable (selected: T, itemModifier: Modifier, backToMenu: () -> Unit) -> Unit,
) {
    // 防空：无子项时不渲染任何内容
    if (categories.isEmpty()) return

    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    val itemModifier =
        Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)

    val backToMenu: () -> Unit = {
        onFocusStateChange(MenuFocusState.Menu)
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            valuePanel(selectedCategory, itemModifier, backToMenu)
        }

        LazyColumn(
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 8.dp)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                return@onPreviewKeyEvent false
                            }
                            return@onPreviewKeyEvent true
                        }
                        when (it.key) {
                            Key.DirectionRight -> onFocusStateChange(MenuFocusState.MenuNav)
                            Key.DirectionLeft -> onFocusStateChange(MenuFocusState.Items)
                            else -> {}
                        }
                        false
                    }.focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(categories) { index, item ->
                MenuListItem(
                    modifier =
                        Modifier
                            .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                    text = categoryLabel(item),
                    selected = selectedCategory == item,
                    onClick = {
                        selectedCategory = item
                        onFocusStateChange(MenuFocusState.Items)
                    },
                    onFocus = { selectedCategory = item },
                )
            }
        }
    }
}
