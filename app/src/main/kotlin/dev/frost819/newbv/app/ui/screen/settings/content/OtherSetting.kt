package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.settings.OptionDialog
import dev.frost819.newbv.app.ui.component.settings.SettingListItem
import dev.frost819.newbv.app.ui.component.settings.SettingSwitchListItem
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 其他设置页。
 *
 * 接口选择/交互日志开关/崩溃上报端点/查看日志/CDN 测速。
 *
 * @param onNavigateToSpeedTest 跳转 CDN 测速页回调。
 * @param onNavigateToLogViewer 跳转日志查看页回调。
 */
@Composable
fun OtherSetting(
    modifier: Modifier = Modifier,
    onNavigateToSpeedTest: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    var showPreferedApiDialog by remember { mutableStateOf(false) }
    var selectedApi by remember { mutableStateOf(Prefs.apiType) }
    var interactionLog by remember { mutableStateOf(Prefs.interactionLog) }
    var crashReportEndpoint by remember { mutableStateOf(Prefs.crashReportEndpoint) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = SettingsMenuNavItem.Other.displayName,
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingListItem(
            title = "接口选择",
            supportText = "当前：${selectedApi.displayName}",
            onClick = { showPreferedApiDialog = true },
        )

        SettingSwitchListItem(
            title = "交互日志",
            supportText = "记录用户交互行为用于崩溃诊断",
            checked = interactionLog,
            onCheckedChange = {
                interactionLog = it
                Prefs.interactionLog = it
            },
        )

        SettingListItem(
            title = "崩溃上报端点",
            supportText = "当前：${crashReportEndpoint.ifEmpty { "未设置" }}",
            onClick = {
                crashReportEndpoint = if (crashReportEndpoint.isEmpty()) {
                    "https://your-endpoint.example.com/crash"
                } else {
                    ""
                }
                Prefs.crashReportEndpoint = crashReportEndpoint
            },
        )

        SettingListItem(
            title = "查看日志",
            supportText = "查看崩溃日志和交互日志，支持扫码下载",
            onClick = onNavigateToLogViewer,
        )

        SettingListItem(
            title = "CDN 测速",
            supportText = "B 站 CDN 测速工具",
            onClick = onNavigateToSpeedTest,
        )
    }

    if (showPreferedApiDialog) {
        OptionDialog(
            options = ApiType.entries.toTypedArray(),
            selectedOption = selectedApi,
            onDismiss = { showPreferedApiDialog = false },
            onSelect = {
                Prefs.apiType = it
                selectedApi = it
            },
            getDisplayName = { it.displayName },
        )
    }
}
