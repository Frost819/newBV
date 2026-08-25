package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
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
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.settings.SettingListItem
import dev.frost819.newbv.app.ui.component.settings.SettingsMenuSelectItem
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.app.util.CacheManager
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** 缓存阈值可选项（MB），0 表示不限制。 */
private val CACHE_THRESHOLD_OPTIONS = listOf(50, 100, 200, 500, CacheManager.THRESHOLD_UNLIMITED)

/**
 * 存储设置页。
 *
 * 缓存阈值设置（超限自动 LRU 清理）/ 手动清理缓存 / 自动清空开关；
 * 崩溃日志大小显示与清理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSetting(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheManager = remember { CacheManager(context) }

    var loading by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableLongStateOf(0L) }
    var crashLogsSize by remember { mutableLongStateOf(0L) }
    var cacheThreshold by remember { mutableStateOf(Prefs.cacheThreshold) }
    var autoCleanEnabled by remember { mutableStateOf(Prefs.cacheAutoClean) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showClearLogsDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    val calSize = {
        cacheSize = cacheManager.cacheSize()
        crashLogsSize = CacheManager.folderSize(
            File(context.filesDir, CrashHandler.LOG_DIR)
        )
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
                        title = "缓存上限",
                        supportText = "当前：" +
                            if (cacheThreshold == CacheManager.THRESHOLD_UNLIMITED) {
                                "无限制"
                            } else {
                                "$cacheThreshold MB"
                            },
                        onClick = { showThresholdDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = "自动清空缓存",
                        supportText = "开启后缓存超过阈值时自动清理",
                        trailingContent = {
                            Switch(
                                checked = autoCleanEnabled,
                                onCheckedChange = {
                                    autoCleanEnabled = it
                                    Prefs.cacheAutoClean = it
                                },
                            )
                        },
                        onClick = {
                            autoCleanEnabled = !autoCleanEnabled
                            Prefs.cacheAutoClean = autoCleanEnabled
                        },
                    )
                }
                item {
                    SettingListItem(
                        title = "清理缓存",
                        supportText = if (loading) "计算中..." else "当前：${cacheSize / CacheManager.BYTES_PER_MB} MB",
                        onClick = { showClearDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = "清理日志",
                        supportText = if (loading) "计算中..." else "当前：${crashLogsSize / CacheManager.BYTES_PER_MB} MB",
                        onClick = { showClearLogsDialog = true },
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmClearDialog(
            title = "清空缓存",
            size = cacheSize,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    cacheManager.clearAll()
                    calSize()
                }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showClearLogsDialog) {
        ConfirmClearDialog(
            title = "清理日志",
            size = crashLogsSize,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    File(context.filesDir, CrashHandler.LOG_DIR).deleteRecursively()
                    calSize()
                }
                showClearLogsDialog = false
            },
            onDismiss = { showClearLogsDialog = false },
        )
    }

    if (showThresholdDialog) {
        ThresholdOptionDialog(
            options = CACHE_THRESHOLD_OPTIONS,
            selected = cacheThreshold,
            onDismiss = { showThresholdDialog = false },
            onSelect = { threshold ->
                cacheThreshold = threshold
                Prefs.cacheThreshold = threshold
                // 阈值调低时立即生效（清理 + 刷新大小显示）
                scope.launch(Dispatchers.IO) {
                    cacheManager.checkCache()
                    calSize()
                }
            },
        )
    }
}

/**
 * 缓存阈值选择弹窗。
 *
 * 列出预设阈值（MB，0 显示为"不限制"），当前选中项高亮，选中后触发回调并关闭。
 *
 * @param options 预设阈值列表（MB，0 = 不限制）。
 * @param selected 当前阈值。
 * @param onDismiss 关闭回调。
 * @param onSelect 选中回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdOptionDialog(
    options: List<Int>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    BasicAlertDialog(
        modifier = Modifier.padding(vertical = 24.dp),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            LazyColumn(
                modifier = Modifier
                    .wrapContentHeight()
                    .heightIn(max = 360.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "缓存阈值",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(options.size) { index ->
                    val option = options[index]
                    SettingsMenuSelectItem(
                        text = if (option == CacheManager.THRESHOLD_UNLIMITED) "无限制" else "$option MB",
                        selected = option == selected,
                        onClick = {
                            onSelect(option)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/**
 * 清理确认弹窗（清空缓存/清理日志共用，样式一致）。
 *
 * @param title 弹窗标题。
 * @param size 将释放的空间大小（字节）。
 * @param onConfirm 确认清理回调。
 * @param onDismiss 取消回调。
 */
@Composable
private fun ConfirmClearDialog(
    title: String,
    size: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = "将释放 ${size / CacheManager.BYTES_PER_MB} MB 空间") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.touchClickable(onClick = onConfirm),
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.touchClickable(onClick = onDismiss),
            ) {
                Text(text = "取消")
            }
        },
    )
}
