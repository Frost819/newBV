package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcut
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutAction
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutCatalog
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutKeys
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutsStore
import dev.frost819.newbv.app.ui.component.settings.SettingsMenuSelectItem
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 自定义快捷键配置弹窗。
 *
 * 多阶段流程：
 * 1. [Stage.Main] — 显示已绑定快捷键列表，可添加/编辑/清空
 * 2. [Stage.CaptureKey] — 捕获按键
 * 3. [Stage.PickAction] — 选择动作（简单动作或参数化动作组）
 * 4. [Stage.PickActionValue] — 选择参数化动作的具体值
 * 5. [Stage.ConfirmClear] — 确认清空
 *
 * @param onDismiss 关闭弹窗回调。
 * @param onShortcutsChanged 快捷键列表变化回调。
 */
@Composable
fun PlayerCustomShortcutsDialog(
    onDismiss: () -> Unit,
    onShortcutsChanged: (List<PlayerCustomShortcut>) -> Unit,
) {
    var shortcuts by remember { mutableStateOf(PlayerCustomShortcutsStore.get()) }
    var stage by remember { mutableStateOf<Stage>(Stage.Main) }

    fun updateShortcuts(next: List<PlayerCustomShortcut>) {
        shortcuts = next
        onShortcutsChanged(next)
    }

    when (val currentStage = stage) {
        Stage.Main ->
            MainStage(
                shortcuts = shortcuts,
                onDismiss = onDismiss,
                onAdd = { stage = Stage.CaptureKey },
                onClear = { stage = Stage.ConfirmClear },
                onEdit = { shortcut -> stage = Stage.PickAction(shortcut.keyCode) },
            )

        Stage.CaptureKey ->
            CaptureKeyStage(
                onDismiss = { stage = Stage.Main },
                onCaptured = { keyCode -> stage = Stage.PickAction(keyCode) },
            )

        is Stage.PickAction ->
            PickActionStage(
                keyCode = currentStage.keyCode,
                currentShortcut = shortcuts.firstOrNull { it.keyCode == currentStage.keyCode },
                onDismiss = { stage = Stage.Main },
                onSelectAction = { action ->
                    PlayerCustomShortcutsStore.upsert(currentStage.keyCode, action)
                    updateShortcuts(PlayerCustomShortcutsStore.get())
                    stage = Stage.Main
                },
                onPickValues = { group ->
                    stage =
                        Stage.PickActionValue(
                            keyCode = currentStage.keyCode,
                            groupId = group.id,
                        )
                },
                onRemove = {
                    PlayerCustomShortcutsStore.remove(currentStage.keyCode)
                    updateShortcuts(PlayerCustomShortcutsStore.get())
                    stage = Stage.Main
                },
            )

        is Stage.PickActionValue -> {
            val group =
                PlayerCustomShortcutCatalog
                    .groups()
                    .firstOrNull { it.id == currentStage.groupId }
            if (group == null) {
                stage = Stage.Main
            } else {
                PickActionValueStage(
                    keyCode = currentStage.keyCode,
                    groupDisplayName = group.displayName,
                    values = group.values,
                    currentShortcut = shortcuts.firstOrNull { it.keyCode == currentStage.keyCode },
                    onDismiss = { stage = Stage.PickAction(currentStage.keyCode) },
                    onSelect = { action ->
                        PlayerCustomShortcutsStore.upsert(currentStage.keyCode, action)
                        updateShortcuts(PlayerCustomShortcutsStore.get())
                        stage = Stage.Main
                    },
                )
            }
        }

        Stage.ConfirmClear ->
            ConfirmClearStage(
                onDismiss = { stage = Stage.Main },
                onConfirm = {
                    PlayerCustomShortcutsStore.clear()
                    updateShortcuts(PlayerCustomShortcutsStore.get())
                    stage = Stage.Main
                },
            )
    }
}

/** 弹窗阶段。 */
private sealed interface Stage {
    data object Main : Stage

    data object CaptureKey : Stage

    data class PickAction(
        val keyCode: Int,
    ) : Stage

    data class PickActionValue(
        val keyCode: Int,
        val groupId: String,
    ) : Stage

    data object ConfirmClear : Stage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainStage(
    shortcuts: List<PlayerCustomShortcut>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onEdit: (PlayerCustomShortcut) -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) { (windowInfo.containerSize.height * 0.7f).toDp() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "自定义快捷键",
                    style = MaterialTheme.typography.headlineSmall,
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .wrapContentHeight()
                            .heightIn(max = maxHeightDp)
                            .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (shortcuts.isEmpty()) {
                        item {
                            Text(
                                text = "暂无自定义快捷键，点击下方按钮添加",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        items(shortcuts) { shortcut ->
                            ListItem(
                                headlineContent = {
                                    Text(PlayerCustomShortcutKeys.getDisplayName(shortcut.keyCode))
                                },
                                supportingContent = {
                                    Text(
                                        PlayerCustomShortcutCatalog
                                            .getActionDisplayName(shortcut.action),
                                    )
                                },
                                modifier = Modifier.touchClickable(onClick = { onEdit(shortcut) }),
                                onClick = { onEdit(shortcut) },
                                selected = false,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.touchClickable(onClick = onAdd),
                    ) { Text("添加") }
                    OutlinedButton(
                        onClick = onClear,
                        enabled = shortcuts.isNotEmpty(),
                        modifier = Modifier.touchClickable(onClick = { if (shortcuts.isNotEmpty()) onClear() }),
                    ) { Text("清空") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.touchClickable(onClick = onDismiss),
                    ) { Text("关闭") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureKeyStage(
    onDismiss: () -> Unit,
    onCaptured: (keyCode: Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(32.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            if (keyEvent.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent true

                            val keyCode = keyEvent.key.nativeKeyCode
                            when {
                                PlayerCustomShortcutKeys.isCancelKeyCode(keyCode) -> {
                                    onDismiss()
                                }

                                PlayerCustomShortcutKeys.isAllowedKeyCode(keyCode) -> {
                                    onCaptured(keyCode)
                                }
                            }
                            true
                        },
            ) {
                Text(
                    text = "按下要绑定的按键...\n（按返回键取消）",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickActionStage(
    keyCode: Int,
    currentShortcut: PlayerCustomShortcut?,
    onDismiss: () -> Unit,
    onSelectAction: (PlayerCustomShortcutAction) -> Unit,
    onPickValues: (PlayerCustomShortcutCatalog.ActionGroup) -> Unit,
    onRemove: () -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) { (windowInfo.containerSize.height * 0.7f).toDp() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "选择动作 — ${PlayerCustomShortcutKeys.getDisplayName(keyCode)}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .wrapContentHeight()
                            .heightIn(max = maxHeightDp)
                            .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val groups = PlayerCustomShortcutCatalog.groups()
                    items(groups) { group ->
                        SettingsMenuSelectItem(
                            text = group.displayName,
                            selected = false,
                            onClick = {
                                if (group.action != null) {
                                    onSelectAction(group.action)
                                } else {
                                    onPickValues(group)
                                }
                            },
                        )
                    }
                }
                if (currentShortcut != null) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onRemove,
                            modifier = Modifier.touchClickable(onClick = onRemove),
                        ) { Text("移除绑定") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickActionValueStage(
    keyCode: Int,
    groupDisplayName: String,
    values: List<PlayerCustomShortcutCatalog.ActionEntry>,
    currentShortcut: PlayerCustomShortcut?,
    onDismiss: () -> Unit,
    onSelect: (PlayerCustomShortcutAction) -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) { (windowInfo.containerSize.height * 0.7f).toDp() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = groupDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .wrapContentHeight()
                            .heightIn(max = maxHeightDp)
                            .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(values) { entry ->
                        SettingsMenuSelectItem(
                            text = entry.valueDisplayName,
                            selected = currentShortcut?.action == entry.action,
                            onClick = { onSelect(entry.action) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmClearStage(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "清空所有快捷键？",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.touchClickable(onClick = onConfirm),
                    ) { Text("确定") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.touchClickable(onClick = onDismiss),
                    ) { Text("取消") }
                }
            }
        }
    }
}
