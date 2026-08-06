package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.settings.OptionDialog
import dev.frost819.newbv.app.ui.component.settings.SettingListItem
import dev.frost819.newbv.app.ui.component.settings.SettingSwitchListItem
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.app.ui.screen.main.displayName
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.LeftNaviItem
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.ThemeMode
import kotlin.math.roundToInt

/**
 * 界面设置页。
 *
 * 启动页/首页 Tab/个人页 Tab/显示视频详情/常显进度条/Density/主题模式。
 */
@Composable
fun UISetting(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var showDensityDialog by remember { mutableStateOf(false) }
    var showStartupPageDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showPersonalPageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    var showVideoInfo by remember { mutableStateOf(Prefs.showVideoInfo) }
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }
    var selectedLeftNavItem by remember { mutableStateOf(Prefs.homeLeftNavItem) }
    var selectedFirstHomeTopNavItem by remember { mutableStateOf(Prefs.firstHomeTopNavItem) }
    var selectedFirstPersonalTopNavItem by remember { mutableStateOf(Prefs.firstPersonalTopNavItem) }
    var selectedThemeMode by remember { mutableStateOf(Prefs.themeMode) }
    var density by remember { mutableFloatStateOf(Prefs.density) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = SettingsMenuNavItem.UI.displayName,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SettingListItem(
                        title = "启动页",
                        supportText = "当前：${selectedLeftNavItem.displayName}",
                        onClick = { showStartupPageDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = "首页置顶 Tab",
                        supportText = "当前：${selectedFirstHomeTopNavItem.displayName}",
                        onClick = { showHomepageDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = "个人页置顶 Tab",
                        supportText = "当前：${selectedFirstPersonalTopNavItem.displayName}",
                        onClick = { showPersonalPageDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = "主题模式",
                        supportText = "当前：${selectedThemeMode.displayName}",
                        onClick = { showThemeDialog = true },
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "显示视频详情",
                        supportText = "点击卡片时先进入详情页而非直接播放",
                        checked = showVideoInfo,
                        onCheckedChange = {
                            showVideoInfo = it
                            Prefs.showVideoInfo = it
                        },
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "常显进度条",
                        supportText = "播放器底部常驻迷你进度条",
                        checked = showPersistentSeek,
                        onCheckedChange = {
                            showPersistentSeek = it
                            Prefs.showPersistentSeek = it
                        },
                    )
                }
                item {
                    SettingListItem(
                        title = "界面缩放",
                        supportText = "当前：$density",
                        onClick = { showDensityDialog = true },
                    )
                }
            }
        }
    }

    UIDensityDialog(
        show = showDensityDialog,
        onHideDialog = { showDensityDialog = false },
        density = density,
        onDensityChange = {
            density = it
            Prefs.density = it
        },
    )

    if (showStartupPageDialog) {
        OptionDialog(
            options = LeftNaviItem.entries.toTypedArray(),
            selectedOption = selectedLeftNavItem,
            onDismiss = { showStartupPageDialog = false },
            onSelect = {
                Prefs.homeLeftNavItem = it
                selectedLeftNavItem = it
            },
            getDisplayName = { it.displayName },
        )
    }

    if (showHomepageDialog) {
        OptionDialog(
            options = HomeTopNavItem.entries.toTypedArray(),
            selectedOption = selectedFirstHomeTopNavItem,
            onDismiss = { showHomepageDialog = false },
            onSelect = {
                Prefs.firstHomeTopNavItem = it
                selectedFirstHomeTopNavItem = it
            },
            getDisplayName = { it.displayName },
        )
    }

    if (showPersonalPageDialog) {
        OptionDialog(
            options = PersonalTopNavItem.entries.toTypedArray(),
            selectedOption = selectedFirstPersonalTopNavItem,
            onDismiss = { showPersonalPageDialog = false },
            onSelect = {
                Prefs.firstPersonalTopNavItem = it
                selectedFirstPersonalTopNavItem = it
            },
            getDisplayName = { it.displayName },
        )
    }

    if (showThemeDialog) {
        OptionDialog(
            options = ThemeMode.entries.toTypedArray(),
            selectedOption = selectedThemeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                Prefs.themeMode = it
                selectedThemeMode = it
            },
            getDisplayName = { it.displayName },
        )
    }
}

/**
 * Density 调节弹窗。
 *
 * D-Pad Up/Down 调整 density 值，范围 0.5 ~ 5.0，步进 0.1。
 * 使用固定 Density 避免弹窗自身随 density 变化而重组。
 */
@Composable
private fun UIDensityDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    density: Float,
    onDensityChange: (Float) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val defaultDensity = LocalDensity.current.density

    LaunchedEffect(show) {
        if (show) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = defaultDensity,
            fontScale = LocalDensity.current.fontScale,
        ),
    ) {
        if (show) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = { onHideDialog() },
                title = { Text(text = "界面缩放") },
                text = {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                if ((it.key == Key.DirectionUp || it.key == Key.DirectionDown) &&
                                    it.type == KeyEventType.KeyDown
                                ) {
                                    var newDensity = if (it.key == Key.DirectionUp) {
                                        density + 0.1f
                                    } else {
                                        density - 0.1f
                                    }
                                    newDensity = (newDensity * 10).roundToInt() / 10f
                                    newDensity = newDensity.coerceIn(0.5f, 5f)
                                    onDensityChange(newDensity)
                                    true
                                } else {
                                    false
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
                        Text(text = "$density")
                        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                },
                confirmButton = {},
            )
        }
    }
}
