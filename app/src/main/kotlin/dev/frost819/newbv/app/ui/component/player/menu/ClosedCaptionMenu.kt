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
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.ui.component.player.menu.component.StepLessMenuItem
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerClosedCaptionMenuItem
import dev.frost819.newbv.biliapi.entity.video.Subtitle

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
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var selectedCcMenuItem by remember { mutableStateOf(VideoPlayerClosedCaptionMenuItem.Switch) }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val menuItemsModifier =
            Modifier
                .width(216.dp)
                .padding(horizontal = 8.dp)

        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedCcMenuItem) {
                VideoPlayerClosedCaptionMenuItem.Switch ->
                    RadioMenuList(
                        modifier = menuItemsModifier,
                        items =
                            availableSubtitleTracks.map { subtitle ->
                                if (subtitle.id == -1L) "关闭" else subtitle.langDoc
                            },
                        selected = availableSubtitleTracks.indexOfFirst { it.id == currentSubtitleId },
                        onSelectedChanged = { onSubtitleChange(availableSubtitleTracks[it]) },
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        },
                    )

                VideoPlayerClosedCaptionMenuItem.Size ->
                    StepLessMenuItem(
                        modifier = menuItemsModifier,
                        value = currentFontSize,
                        step = 1,
                        range = 8..48,
                        text = "${currentFontSize}sp",
                        onValueChange = onSubtitleSizeChange,
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        },
                    )

                VideoPlayerClosedCaptionMenuItem.Opacity ->
                    StepLessMenuItem(
                        modifier = menuItemsModifier,
                        value = currentOpacity,
                        step = 0.01f,
                        range = 0f..1f,
                        text = "${(currentOpacity * 100).toInt()}%",
                        onValueChange = onSubtitleBackgroundOpacityChange,
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        },
                    )

                VideoPlayerClosedCaptionMenuItem.Padding ->
                    StepLessMenuItem(
                        modifier = menuItemsModifier,
                        value = currentPadding,
                        step = 1,
                        range = 0..48,
                        text = "${currentPadding}dp",
                        onValueChange = onSubtitleBottomPadding,
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        },
                    )
            }
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
            itemsIndexed(VideoPlayerClosedCaptionMenuItem.entries.toMutableList()) { index, item ->
                MenuListItem(
                    modifier =
                        Modifier
                            .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                    text = item.displayName,
                    selected = selectedCcMenuItem == item,
                    onClick = {
                        selectedCcMenuItem = item
                        onFocusStateChange(MenuFocusState.Items)
                    },
                    onFocus = { selectedCcMenuItem = item },
                )
            }
        }
    }
}
