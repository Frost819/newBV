package dev.frost819.newbv.app.ui.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 设置列表项。
 *
 * 显示标题 + 副文本，点击触发回调。
 * 获焦时高亮（由 [ListItem] 的 selected 状态自动处理）。
 *
 * @param title 标题。
 * @param supportText 副文本（当前值描述）。
 * @param onClick 点击回调。
 * @param trailingContent 尾部内容（如开关），可选。
 */
@Composable
fun SettingListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    var hasFocus by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .touchClickable(onClick = onClick),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = supportText) },
        trailingContent = trailingContent,
        onClick = onClick,
        selected = hasFocus,
    )
}
