package dev.frost819.newbv.app.ui.component.settings

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.RadioButtonDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 选项弹窗中的单选项。
 *
 * 显示文本 + RadioButton，点击选中。RadioButton 不可聚焦。
 *
 * @param text 选项文本。
 * @param selected 是否选中。
 * @param onClick 点击回调。
 */
@Composable
fun SettingsMenuSelectItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .touchClickable(onClick = onClick),
        headlineContent = { Text(text = text) },
        trailingContent = {
            RadioButton(
                modifier = Modifier.focusable(false),
                selected = selected,
                onClick = { },
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (hasFocus) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    unselectedColor = if (hasFocus) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.border
                    },
                ),
            )
        },
        onClick = onClick,
        selected = selected,
    )
}
