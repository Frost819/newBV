package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.settings.SettingListItem
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.InteractionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 存储设置页。
 *
 * 图片缓存/其他缓存/崩溃日志 大小显示与清理。
 */
@Composable
fun StorageSetting(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var imageCacheSize by remember { mutableLongStateOf(0L) }
    var updateCacheSize by remember { mutableLongStateOf(0L) }
    var crashLogsSize by remember { mutableLongStateOf(0L) }
    var interactionLogsSize by remember { mutableLongStateOf(0L) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var clearFun: (() -> Unit)? by remember { mutableStateOf(null) }
    var dialogContent by remember { mutableStateOf("") }
    var dialogSize by remember { mutableLongStateOf(0L) }

    val calSize = {
        val imageCacheDir = File(context.cacheDir, "image_cache")
        val updateCacheDir = File(context.cacheDir, "update_downloader")
        val crashLogsDir = File(context.filesDir, CrashHandler.LOG_DIR)
        val interactionLogsDir = File(context.filesDir, "interaction_logs")

        imageCacheSize = getFolderSize(imageCacheDir)
        updateCacheSize = getFolderSize(updateCacheDir)
        crashLogsSize = getFolderSize(crashLogsDir)
        interactionLogsSize = getFolderSize(interactionLogsDir)
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            loading = true
            calSize()
            loading = false
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = SettingsMenuNavItem.Storage.displayName,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SettingListItem(
                        title = "图片缓存",
                        supportText = if (loading) "计算中..." else "${imageCacheSize / 1024 / 1024} MB",
                        onClick = {
                            clearFun = {
                                File(context.cacheDir, "image_cache").deleteRecursively()
                            }
                            dialogContent = "图片缓存"
                            dialogSize = imageCacheSize
                            showConfirmDialog = true
                        },
                    )
                }
                item {
                    SettingListItem(
                        title = "其他缓存",
                        supportText = if (loading) "计算中..." else "${updateCacheSize / 1024 / 1024} MB",
                        onClick = {
                            clearFun = {
                                File(context.cacheDir, "update_downloader").deleteRecursively()
                            }
                            dialogContent = "其他缓存"
                            dialogSize = updateCacheSize
                            showConfirmDialog = true
                        },
                    )
                }
                item {
                    SettingListItem(
                        title = "崩溃日志",
                        supportText = if (loading) "计算中..." else "${crashLogsSize / 1024 / 1024} MB",
                        onClick = {
                            clearFun = {
                                File(context.filesDir, CrashHandler.LOG_DIR).deleteRecursively()
                            }
                            dialogContent = "崩溃日志"
                            dialogSize = crashLogsSize
                            showConfirmDialog = true
                        },
                    )
                }
                item {
                    SettingListItem(
                        title = "交互日志",
                        supportText = if (loading) "计算中..." else "${interactionLogsSize / 1024 / 1024} MB",
                        onClick = {
                            clearFun = {
                                File(context.filesDir, "interaction_logs").deleteRecursively()
                            }
                            dialogContent = "交互日志"
                            dialogSize = interactionLogsSize
                            showConfirmDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmDeleteDialog(
            content = dialogContent,
            size = dialogSize,
            onConfirm = {
                clearFun?.invoke()
                calSize()
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false },
        )
    }
}

private fun getFolderSize(f: File): Long {
    if (!f.exists()) return 0
    if (f.isDirectory) {
        return f.listFiles()?.sumOf { getFolderSize(it) } ?: 0
    }
    return f.length()
}

@Composable
private fun ConfirmDeleteDialog(
    content: String,
    size: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "清除$content") },
        text = { Text(text = "${size / 1024 / 1024} MB") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}
