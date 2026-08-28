package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

private val keyboardKeys =
    listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0"),
    )

/**
 * TV 软键盘。
 *
 * 6×6 字母数字网格 + 清除/删除/搜索按钮。
 * D-Pad 焦点导航，首个按键可绑定 [firstButtonFocusRequester]。
 *
 * @param firstButtonFocusRequester 首个按键的焦点请求器
 * @param searchButtonModifier 搜索按钮的额外 modifier（用于焦点恢复）
 * @param onClick 字符键点击回调
 * @param onClear 清除全部
 * @param onDelete 删除最后一个字符
 * @param onSearch 执行搜索
 */
@Composable
fun SoftKeyboard(
    modifier: Modifier = Modifier,
    firstButtonFocusRequester: FocusRequester,
    searchButtonModifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        modifier = modifier.width(258.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keyboardKeys.forEachIndexed { rowIndex, rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowKeys.forEachIndexed { index, key ->
                    val keyModifier =
                        if (rowIndex == 0 && index == 0) {
                            Modifier.focusRequester(firstButtonFocusRequester)
                        } else {
                            Modifier
                        }
                    SoftKeyboardKey(
                        modifier = keyModifier,
                        key = key,
                        onClick = { onClick(key) },
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SoftKeyboardButton(
                modifier = Modifier.weight(1f),
                key = "清除",
                onClick = onClear,
            )
            SoftKeyboardButton(
                modifier = Modifier.weight(1f),
                key = "删除",
                onClick = onDelete,
            )
            SoftKeyboardButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(searchButtonModifier),
                key = "搜索",
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun SoftKeyboardKey(
    modifier: Modifier = Modifier,
    key: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.touchClickable(onClick = onClick),
        onClick = onClick,
        colors =
            ClickableSurfaceDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            ),
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SoftKeyboardButton(
    modifier: Modifier = Modifier,
    key: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(38.dp).touchClickable(onClick = onClick),
        onClick = onClick,
        colors =
            ClickableSurfaceDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
