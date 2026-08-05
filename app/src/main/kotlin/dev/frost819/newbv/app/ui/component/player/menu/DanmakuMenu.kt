package dev.frost819.newbv.app.ui.component.player.menu

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
import dev.frost819.newbv.app.ui.component.player.menu.component.CheckBoxMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.StepLessMenuItem
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerDanmakuMenuItem
import dev.frost819.newbv.data.datastore.DanmakuType
import java.text.NumberFormat

/**
 * 弹幕设置面板。
 *
 * 双列布局：左侧为选项值面板（CheckBoxMenuList / StepLessMenuItem），右侧为子项列表。
 * 子项包括：开关、大小、透明度、速度、区域、蒙版。
 *
 * @param modifier 修饰符
 * @param currentEnabledTypes 当前启用的弹幕类型
 * @param currentScale 当前字体缩放
 * @param currentOpacity 当前透明度
 * @param currentSpeedFactor 当前速度因子
 * @param currentArea 当前显示区域
 * @param currentMaskEnabled 当前蒙版状态
 * @param onDanmakuSwitchChange 弹幕类型变化回调
 * @param onDanmakuSizeChange 大小变化回调
 * @param onDanmakuOpacityChange 透明度变化回调
 * @param onDanmakuSpeedFactorChange 速度因子变化回调
 * @param onDanmakuAreaChange 区域变化回调
 * @param onDanmakuMaskChange 蒙版变化回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
fun DanmakuMenuList(
    modifier: Modifier = Modifier,
    currentEnabledTypes: List<DanmakuType>,
    currentScale: Float,
    currentOpacity: Float,
    currentSpeedFactor: Float,
    currentArea: Float,
    currentMaskEnabled: Boolean,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var selectedDanmakuMenuItem by remember { mutableStateOf(VideoPlayerDanmakuMenuItem.Switch) }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)

        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedDanmakuMenuItem) {
                VideoPlayerDanmakuMenuItem.Switch -> CheckBoxMenuList(
                    modifier = menuItemsModifier,
                    items = DanmakuType.entries.map { it.displayName },
                    selected = currentEnabledTypes.map { it.ordinal },
                    onSelectedChanged = { indices ->
                        handleDanmakuTypeChange(
                            currentTypes = currentEnabledTypes,
                            newIndices = indices,
                            onChange = onDanmakuSwitchChange,
                        )
                    },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerDanmakuMenuItem.Size -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentScale,
                    step = 0.01f,
                    range = 0.5f..4f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentScale),
                    onValueChange = onDanmakuSizeChange,
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerDanmakuMenuItem.Opacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    onValueChange = onDanmakuOpacityChange,
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerDanmakuMenuItem.SpeedFactor -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentSpeedFactor,
                    step = 0.1f,
                    range = 0.2f..5f,
                    text = "${String.format("%.1f", currentSpeedFactor)}x",
                    onValueChange = onDanmakuSpeedFactorChange,
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerDanmakuMenuItem.Area -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentArea,
                    step = 0.01f,
                    range = 0.1f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentArea),
                    onValueChange = onDanmakuAreaChange,
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerDanmakuMenuItem.Mask -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentMaskEnabled) 1 else 0,
                    onSelectedChanged = { onDanmakuMaskChange(it == 1) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier
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
                }
                .focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(VideoPlayerDanmakuMenuItem.entries.toMutableList()) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                    text = item.displayName,
                    selected = selectedDanmakuMenuItem == item,
                    onClick = {},
                    onFocus = { selectedDanmakuMenuItem = item },
                )
            }
        }
    }
}

/**
 * 处理弹幕类型多选联动逻辑。
 *
 * - 点击"全部"：选中时全选，取消时清空
 * - 子项全选时自动添加"全部"
 * - 子项未全选时自动移除"全部"
 */
private fun handleDanmakuTypeChange(
    currentTypes: List<DanmakuType>,
    newIndices: List<Int>,
    onChange: (List<DanmakuType>) -> Unit,
) {
    val allType = DanmakuType.All
    val allEntries = DanmakuType.entries
    val realItemsCount = allEntries.size - 1

    val newSelection = newIndices.map { allEntries[it] }.toMutableList()

    val isAllInOld = currentTypes.contains(allType)
    val isAllInNew = newSelection.contains(allType)

    when {
        // 从无到有：点击全选 -> 选中所有
        !isAllInOld && isAllInNew -> onChange(allEntries)

        // 从有到无：取消全选 -> 清空所有
        isAllInOld && !isAllInNew -> onChange(emptyList())

        // 点击子选项的联动逻辑
        else -> {
            val currentRealItemsCount = newSelection.count { it != allType }
            if (currentRealItemsCount == realItemsCount) {
                // 子项全选，自动添加 All
                if (!newSelection.contains(allType)) {
                    newSelection.add(allType)
                }
                onChange(newSelection)
            } else {
                // 子项未全选，自动移除 All
                if (newSelection.contains(allType)) {
                    newSelection.remove(allType)
                }
                onChange(newSelection)
            }
        }
    }
}

/** 弹幕类型显示名称。 */
private val DanmakuType.displayName: String
    get() = when (this) {
        DanmakuType.All -> "全部"
        DanmakuType.Top -> "顶部"
        DanmakuType.Rolling -> "滚动"
        DanmakuType.Bottom -> "底部"
    }
