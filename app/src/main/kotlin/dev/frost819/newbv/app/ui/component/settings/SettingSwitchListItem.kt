package dev.frost819.newbv.app.ui.component.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 带开关的设置列表项。
 *
 * 点击整行切换开关状态。Switch 本身不可聚焦（避免与行抢焦点）。
 *
 * @param title 标题。
 * @param supportText 副文本。
 * @param checked 当前开关状态。
 * @param onCheckedChange 开关变化回调。
 */
@Composable
fun SettingSwitchListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    var switchChecked by remember(checked) { mutableStateOf(checked) }

    ListItem(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .touchClickable(onClick = {
                switchChecked = !switchChecked
                onCheckedChange(switchChecked)
            }),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = supportText) },
        trailingContent = {
            Box(
                modifier = Modifier
                    .border(2.dp, MaterialTheme.colorScheme.border, CircleShape),
            ) {
                Switch(
                    modifier = Modifier
                        .focusable(false)
                        .padding(2.dp),
                    checked = switchChecked,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        },
        onClick = {
            switchChecked = !switchChecked
            onCheckedChange(switchChecked)
        },
        selected = hasFocus,
    )
}
