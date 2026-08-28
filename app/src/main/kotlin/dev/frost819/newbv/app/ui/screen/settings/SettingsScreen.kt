package dev.frost819.newbv.app.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.frost819.newbv.app.ui.component.ScreenFocusSaver
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberScreenFocusSaver
import dev.frost819.newbv.core.focus.touchClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.screen.settings.content.AboutSetting
import dev.frost819.newbv.app.ui.screen.settings.content.AudioVideoSetting
import dev.frost819.newbv.app.ui.screen.settings.content.InfoSetting
import dev.frost819.newbv.app.ui.screen.settings.content.OtherSetting
import dev.frost819.newbv.app.ui.screen.settings.content.StorageSetting
import dev.frost819.newbv.app.ui.screen.settings.content.UISetting

/**
 * 设置页主屏幕。
 *
 * 左右分栏布局：左侧为设置导航列表，右侧为当前选中分类的设置内容。
 * D-Pad Left 从内容区返回导航列表。
 *
 * @param onNavigateToMediaCodec 跳转编解码信息页。
 * @param onNavigateToLogViewer 跳转日志查看页。
 * @param onBack 返回上一页。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToMediaCodec: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    var currentMenu by rememberSaveable { mutableStateOf(SettingsMenuNavItem.AudioVideo) }
    var focusInNav by remember { mutableStateOf(false) }

    val screenFocusSaver = rememberScreenFocusSaver()

    screenFocusSaver.RestoreFocus()

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = 48.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                    end = 48.dp,
                ),
            ) {
                Text(
                    text = "设置",
                    fontSize = 24.sp,
                )
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding),
        ) {
            SettingsNav(
                modifier = Modifier
                    .onFocusChanged { focusInNav = it.hasFocus }
                    .weight(3f)
                    .fillMaxHeight(),
                currentMenu = currentMenu,
                onMenuChanged = { currentMenu = it },
                isFocusing = focusInNav,
                screenFocusSaver = screenFocusSaver,
            )
            SettingContent(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxSize(),
                onBackNav = { focusInNav = true },
                currentMenu = currentMenu,
                onNavigateToMediaCodec = onNavigateToMediaCodec,
                onNavigateToLogViewer = onNavigateToLogViewer,
                screenFocusSaver = screenFocusSaver,
            )
        }
    }
}

/**
 * 设置导航列表。
 */
@Composable
private fun SettingsNav(
    modifier: Modifier = Modifier,
    currentMenu: SettingsMenuNavItem,
    onMenuChanged: (SettingsMenuNavItem) -> Unit,
    isFocusing: Boolean,
    screenFocusSaver: ScreenFocusSaver,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocusing) {
        if (isFocusing) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(Unit) {
        if (screenFocusSaver.savedKeyValue().isEmpty()) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (item in SettingsMenuNavItem.entries) {
            val buttonModifier = if (currentMenu == item) {
                Modifier
                    .focusRequester(focusRequester)
                    .focusSaverItem(screenFocusSaver, "nav_${item.name}")
                    .fillMaxWidth()
            } else {
                Modifier
                    .focusSaverItem(screenFocusSaver, "nav_${item.name}")
                    .fillMaxWidth()
            }
            item {
                SettingsMenuButton(
                    modifier = buttonModifier,
                    text = item.displayName,
                    selected = currentMenu == item,
                    onFocus = { onMenuChanged(item) },
                )
            }
        }
    }
}

/**
 * 设置内容区。
 *
 * D-Pad Left 返回导航列表。
 */
@Composable
private fun SettingContent(
    modifier: Modifier = Modifier,
    onBackNav: () -> Unit,
    currentMenu: SettingsMenuNavItem,
    onNavigateToMediaCodec: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
    screenFocusSaver: ScreenFocusSaver,
) {
    Box(
        modifier = modifier.padding(24.dp),
    ) {
        SettingsDetail(
            modifier = Modifier.fillMaxSize(),
            onFocusBackMenuList = onBackNav,
        ) {
            when (currentMenu) {
                SettingsMenuNavItem.AudioVideo -> AudioVideoSetting()
                SettingsMenuNavItem.UI -> UISetting()
                SettingsMenuNavItem.Other -> OtherSetting(
                    onNavigateToLogViewer = onNavigateToLogViewer,
                    screenFocusSaver = screenFocusSaver,
                )
                SettingsMenuNavItem.Storage -> StorageSetting()
                SettingsMenuNavItem.Info -> InfoSetting(
                    onOpenMediaCodec = onNavigateToMediaCodec,
                    screenFocusSaver = screenFocusSaver,
                )
                SettingsMenuNavItem.About -> AboutSetting()
            }
        }
    }
}

/**
 * 设置内容区容器，拦截 D-Pad Left 返回导航。
 */
@Composable
private fun SettingsDetail(
    modifier: Modifier = Modifier,
    onFocusBackMenuList: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown) {
                    onFocusBackMenuList()
                    true
                } else {
                    false
                }
            },
    ) {
        content()
    }
}

/**
 * 导航项按钮。
 */
@Composable
private fun SettingsMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onFocus: () -> Unit,
    selected: Boolean,
) {
    ListItem(
        modifier = modifier
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged { if (it.hasFocus) onFocus() }
            .touchClickable(onClick = { onFocus() }),
        selected = selected,
        onClick = { onFocus() },
        headlineContent = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    )
}

/**
 * 设置导航分类。
 */
enum class SettingsMenuNavItem(val displayName: String) {
    AudioVideo("音视频"),
    UI("界面"),
    Other("其他"),
    Storage("存储"),
    Info("设备信息"),
    About("关于"),
}
