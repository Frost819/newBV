package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.frost819.newbv.app.ui.component.player.menu.component.PlayerThreeLevelMenu
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.StepLessMenuItem
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerClosedCaptionMenuItem
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.SubtitleType

/**
 * 字幕设置面板。
 *
 * 双列布局：左侧为选项值面板（RadioMenuList / StepLessMenuItem），右侧为子项列表。
 * 子项包括：开关、大小、透明度、底部间距。
 *
 * @param modifier 修饰符
 * @param currentSubtitleId 当前字幕 ID（-1 表示关闭）
 * @param availableSubtitleTracks 可用字幕轨道列表
 * @param currentFontSize 当前字体大小（sp）
 * @param currentOpacity 当前背景透明度
 * @param currentPadding 当前底部间距（dp）
 * @param onSubtitleChange 字幕轨道变化回调
 * @param onSubtitleSizeChange 字体大小变化回调
 * @param onSubtitleBackgroundOpacityChange 透明度变化回调
 * @param onSubtitleBottomPadding 底部间距变化回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
@Suppress("LongParameterList")
fun ClosedCaptionMenuList(
    modifier: Modifier = Modifier,
    currentSubtitleId: Long,
    availableSubtitleTracks: List<Subtitle>,
    currentFontSize: Int,
    currentOpacity: Float,
    currentPadding: Int,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (Int) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Int) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    PlayerThreeLevelMenu(
        modifier = modifier,
        categories = VideoPlayerClosedCaptionMenuItem.entries,
        categoryLabel = { it.displayName },
        onFocusStateChange = onFocusStateChange,
    ) { selectedItem, itemModifier, backToMenu ->
        val subtitleLabels =
            remember(availableSubtitleTracks) { availableSubtitleTracks.map { it.toMenuLabel() } }
        val selectedSubtitleIndex =
            remember(availableSubtitleTracks, currentSubtitleId) {
                availableSubtitleTracks.indexOfFirst { it.id == currentSubtitleId }
            }
        when (selectedItem) {
            VideoPlayerClosedCaptionMenuItem.Switch ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = subtitleLabels,
                    selected = selectedSubtitleIndex,
                    onSelectedChanged = { onSubtitleChange(availableSubtitleTracks[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerClosedCaptionMenuItem.Size ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentFontSize.toFloat(),
                    step = 1f,
                    range = 8f..48f,
                    text = "${currentFontSize}sp",
                    onValueChange = { onSubtitleSizeChange(it.toInt()) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerClosedCaptionMenuItem.Opacity ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = "${(currentOpacity * 100).toInt()}%",
                    onValueChange = onSubtitleBackgroundOpacityChange,
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerClosedCaptionMenuItem.Padding ->
                StepLessMenuItem(
                    modifier = itemModifier,
                    value = currentPadding.toFloat(),
                    step = 1f,
                    range = 0f..48f,
                    text = "${currentPadding}dp",
                    onValueChange = { onSubtitleBottomPadding(it.toInt()) },
                    onFocusBackToParent = backToMenu,
                )
        }
    }
}

/**
 * 字幕轨道在设置菜单中的显示名称。
 *
 * B 站返回的 `langDoc` 对 AI 字幕会带「（自动生成）」「（自动翻译）」后缀，
 * 与人工字幕的语言名混在一起时难以区分。这里去掉这类后缀，再按字幕类型
 * 补上 `(AI)` 标记，与 BV 原版行为保持一致。
 *
 * @return 菜单显示文案；[Subtitle.id] 为 -1 的占位轨道显示「关闭」
 */
internal fun Subtitle.toMenuLabel(): String {
    if (id == -1L) return "关闭"

    val languageName =
        langDoc
            .replace("（自动生成）", "")
            .replace("（自动翻译）", "")
            .trim()

    return if (type == SubtitleType.AI) "$languageName(AI)" else languageName
}
