package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState

/**
 * 倍速设置面板。
 *
 * 单列列表，5 个预设倍速选项。
 * 按方向右键返回导航列表。
 *
 * @param modifier 修饰符
 * @param currentSelectedPlaySpeedItem 当前选中的倍速项
 * @param onPlaySpeedChange 倍速变化回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
fun PlaySpeedMenuList(
    modifier: Modifier = Modifier,
    currentSelectedPlaySpeedItem: PlaySpeedItem,
    onPlaySpeedChange: (Float) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                return@onPreviewKeyEvent false
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.DirectionRight) {
                            onFocusStateChange(MenuFocusState.MenuNav)
                        }
                        false
                    }.focusRestorer(focusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(PlaySpeedItem.entries.toMutableList()) { index, item ->
                MenuListItem(
                    modifier =
                        Modifier
                            .ifElse(
                                index == currentSelectedPlaySpeedItem.ordinal,
                                Modifier.focusRequester(focusRequester),
                            ),
                    text = item.displayName,
                    selected = currentSelectedPlaySpeedItem == item,
                    onClick = { onPlaySpeedChange(item.speed) },
                )
            }
        }
    }
}

/**
 * 播放速度预设。
 *
 * @property code 速度标识（用于持久化）
 * @property displayName 显示名称
 * @property speed 实际倍速值
 */
enum class PlaySpeedItem(
    val code: Int,
    val displayName: String,
    val speed: Float,
) {
    X2(4, "2.0x", 2f),
    X1_5(3, "1.5x", 1.5f),
    X1_25(2, "1.25x", 1.25f),
    X1(1, "1.0x", 1f),
    X0_5(0, "0.5x", 0.5f),
    ;

    companion object {
        fun fromCode(code: Int): PlaySpeedItem = entries.find { it.code == code } ?: X1

        fun fromSpeed(speed: Float): PlaySpeedItem = entries.find { it.speed == speed } ?: X1
    }
}
