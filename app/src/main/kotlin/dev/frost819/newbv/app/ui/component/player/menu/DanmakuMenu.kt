package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.frost819.newbv.app.ui.component.player.menu.component.CheckBoxMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.PlayerThreeLevelMenu
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.StepLessMenuItem
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerDanmakuFilterMenuItem
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerDanmakuMenuItem
import dev.frost819.newbv.danmaku.config.DanmakuState
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
@Suppress("CyclomaticComplexMethod")
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
    PlayerThreeLevelMenu(
        modifier = modifier,
        categories = VideoPlayerDanmakuMenuItem.entries,
        categoryLabel = { it.displayName },
        onFocusStateChange = onFocusStateChange,
    ) { selectedItem, itemModifier, backToMenu ->
        when (selectedItem) {
            VideoPlayerDanmakuMenuItem.Switch ->
                CheckBoxMenuList(
                    modifier = itemModifier,
                    items = DanmakuType.entries.map { it.displayName },
                    selected = currentEnabledTypes.map { it.ordinal },
                    onSelectedChanged = { indices ->
                        handleDanmakuTypeChange(
                            currentTypes = currentEnabledTypes,
                            newIndices = indices,
                            onChange = onDanmakuSwitchChange,
                        )
                    },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuMenuItem.Size ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentScale,
                    step = 0.01f,
                    range = 0.5f..4f,
                    text =
                        NumberFormat
                            .getPercentInstance()
                            .apply { maximumFractionDigits = 0 }
                            .format(currentScale),
                    onValueChange = onDanmakuSizeChange,
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuMenuItem.Opacity ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text =
                        NumberFormat
                            .getPercentInstance()
                            .apply { maximumFractionDigits = 0 }
                            .format(currentOpacity),
                    onValueChange = onDanmakuOpacityChange,
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuMenuItem.SpeedFactor ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentSpeedFactor,
                    step = 0.1f,
                    range = 0.2f..5f,
                    text = "${String.format("%.1f", currentSpeedFactor)}x",
                    onValueChange = onDanmakuSpeedFactorChange,
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuMenuItem.Area ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentArea,
                    step = 0.01f,
                    range = 0.1f..1f,
                    text =
                        NumberFormat
                            .getPercentInstance()
                            .apply { maximumFractionDigits = 0 }
                            .format(currentArea),
                    onValueChange = onDanmakuAreaChange,
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuMenuItem.Mask ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentMaskEnabled) 1 else 0,
                    onSelectedChanged = { onDanmakuMaskChange(it == 1) },
                    onFocusBackToParent = backToMenu,
                )
        }
    }
}

/**
 * 弹幕屏蔽设置面板：二级为屏蔽项列表，三级为各项的开关/等级选择。
 *
 * @param visibleItems 可见的屏蔽项；直播侧无 weight/ai_flag、无 mode 7-9、不经合并，
 *   只传实际生效的项（屏蔽彩色弹幕、账号屏蔽规则）
 * @param userFilterRuleCount 已加载的账号云端屏蔽规则数，>0 时在项名称后展示
 */
@Composable
fun DanmakuFilterMenuList(
    currentBlockColorful: Boolean = false,
    currentBlockRepeat: Boolean = false,
    currentBlockSpecial: Boolean = false,
    currentBlockByUserFilter: Boolean = true,
    currentAiCloudLevel: Int = 0,
    onDanmakuBlockColorfulChange: (Boolean) -> Unit = {},
    onDanmakuBlockRepeatChange: (Boolean) -> Unit = {},
    onDanmakuBlockSpecialChange: (Boolean) -> Unit = {},
    onDanmakuBlockByUserFilterChange: (Boolean) -> Unit = {},
    onDanmakuAiCloudLevelChange: (Int) -> Unit = {},
    visibleItems: List<VideoPlayerDanmakuFilterMenuItem> = VideoPlayerDanmakuFilterMenuItem.entries,
    userFilterRuleCount: Int = 0,
    onFocusStateChange: (MenuFocusState) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerThreeLevelMenu(
        modifier = modifier,
        categories = visibleItems,
        categoryLabel = { item ->
            if (item == VideoPlayerDanmakuFilterMenuItem.BlockByUserFilter && userFilterRuleCount > 0) {
                "${item.displayName}（$userFilterRuleCount 条）"
            } else {
                item.displayName
            }
        },
        onFocusStateChange = onFocusStateChange,
    ) { selectedItem, itemModifier, backToMenu ->
        when (selectedItem) {
            VideoPlayerDanmakuFilterMenuItem.BlockColorful ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentBlockColorful) 1 else 0,
                    onSelectedChanged = { onDanmakuBlockColorfulChange(it == 1) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuFilterMenuItem.BlockRepeat ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentBlockRepeat) 1 else 0,
                    onSelectedChanged = { onDanmakuBlockRepeatChange(it == 1) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuFilterMenuItem.BlockSpecial ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentBlockSpecial) 1 else 0,
                    onSelectedChanged = { onDanmakuBlockSpecialChange(it == 1) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuFilterMenuItem.BlockByUserFilter ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentBlockByUserFilter) 1 else 0,
                    onSelectedChanged = { onDanmakuBlockByUserFilterChange(it == 1) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerDanmakuFilterMenuItem.AiCloudLevel ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = listOf("关闭", "弱", "中", "强", "极强"),
                    selected = aiLevelToPresetIndex(currentAiCloudLevel),
                    onSelectedChanged = { onDanmakuAiCloudLevelChange(DanmakuState.AI_LEVEL_PRESETS[it]) },
                    onFocusBackToParent = backToMenu,
                )
        }
    }
}

/**
 * 将服务端 1-10 的 AI 云屏蔽等级映射回最近的五档序号（0-4），用于 UI 展示。
 */
private fun aiLevelToPresetIndex(level: Int): Int {
    if (level <= 0) return 0
    val presets = DanmakuState.AI_LEVEL_PRESETS
    return presets.indices.drop(1).minBy { kotlin.math.abs(presets[it] - level) }
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
    get() =
        when (this) {
            DanmakuType.All -> "全部"
            DanmakuType.Top -> "顶部"
            DanmakuType.Rolling -> "滚动"
            DanmakuType.Bottom -> "底部"
        }
