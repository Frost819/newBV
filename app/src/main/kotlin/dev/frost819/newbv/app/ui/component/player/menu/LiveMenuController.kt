package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Image
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.app.ui.component.player.ifElse
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.MenuFocusStateData
import dev.frost819.newbv.danmaku.config.DanmakuState
import dev.frost819.newbv.data.datastore.DanmakuType

/**
 * 直播播放器设置菜单导航 Tab。
 */
enum class LivePlayerMenuNavItem(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Picture("画质", Icons.Outlined.Image),
    Danmaku("弹幕", Icons.Outlined.ClearAll),
}

/**
 * 直播播放器设置菜单控制器。
 *
 * 从右侧滑入的半透明面板，包含 2 个 Tab（画质/弹幕）。
 * 三态焦点模型与视频播放器一致：MenuNav ↔ Menu ↔ Items。
 * 画质 Tab 简化为二级：MenuNav ↔ Items（跳过 Menu 层级）。
 *
 * @param show 是否显示
 * @param availableQualities 可用画质列表（qn, desc）
 * @param currentQuality 当前画质 qn
 * @param onQualityChange 画质变化回调
 * @param danmakuState 弹幕配置状态
 * @param onDanmakuSettingChange 弹幕设置变化回调
 */
@Composable
@Suppress("LongParameterList")
fun LiveMenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onQualityChange: (Int) -> Unit,
    danmakuState: DanmakuState,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
        ) {
            LiveMenuControllerContent(
                availableQualities = availableQualities,
                currentQuality = currentQuality,
                onQualityChange = onQualityChange,
                danmakuState = danmakuState,
                onDanmakuSettingChange = onDanmakuSettingChange,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun LiveMenuControllerContent(
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onQualityChange: (Int) -> Unit,
    danmakuState: DanmakuState,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
) {
    var selectedNavItem by remember { mutableStateOf(LivePlayerMenuNavItem.Picture) }
    var focusState by remember { mutableStateOf(MenuFocusState.MenuNav) }
    val navFocusRequester = remember { FocusRequester() }

    Surface(
        modifier = Modifier.fillMaxHeight(),
        colors = SurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.5f),
        ),
    ) {
        CompositionLocalProvider(
            LocalMenuFocusStateData provides MenuFocusStateData(focusState = focusState),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                LiveMenuList(
                    selectedNavMenu = selectedNavItem,
                    availableQualities = availableQualities,
                    currentQuality = currentQuality,
                    onQualityChange = onQualityChange,
                    danmakuState = danmakuState,
                    onDanmakuSettingChange = onDanmakuSettingChange,
                    navFocusRequester = navFocusRequester,
                    onFocusStateChange = { focusState = it },
                )
                LiveMenuNavList(
                    modifier = Modifier.onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                return@onPreviewKeyEvent false
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.DirectionLeft) {
                            focusState = if (selectedNavItem == LivePlayerMenuNavItem.Picture) {
                                MenuFocusState.Items
                            } else {
                                MenuFocusState.Menu
                            }
                        }
                        false
                    },
                    selectedMenu = selectedNavItem,
                    onSelectedChanged = { selectedNavItem = it },
                    isFocusing = focusState == MenuFocusState.MenuNav,
                    navFocusRequester = navFocusRequester,
                )
            }
        }
    }
}

/**
 * 根据选中的导航项渲染对应菜单面板。
 */
@Composable
@Suppress("LongParameterList")
private fun LiveMenuList(
    selectedNavMenu: LivePlayerMenuNavItem,
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onQualityChange: (Int) -> Unit,
    danmakuState: DanmakuState,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    navFocusRequester: FocusRequester,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        when (selectedNavMenu) {
            LivePlayerMenuNavItem.Picture -> LivePictureMenuList(
                availableQualities = availableQualities,
                currentQuality = currentQuality,
                onQualityChange = onQualityChange,
                navFocusRequester = navFocusRequester,
                onFocusStateChange = onFocusStateChange,
            )

            LivePlayerMenuNavItem.Danmaku -> {
                val dataTypes = danmakuState.enabledTypes.mapNotNull { entity ->
                    runCatching { DanmakuType.entries[entity.ordinal] }.getOrNull()
                }
                DanmakuMenuList(
                    currentEnabledTypes = dataTypes,
                    currentScale = danmakuState.scale,
                    currentOpacity = danmakuState.opacity,
                    currentSpeedFactor = danmakuState.speedFactor,
                    currentArea = danmakuState.area,
                    currentMaskEnabled = danmakuState.maskEnabled,
                    onDanmakuSwitchChange = { types ->
                        val entityTypes = types.mapNotNull {
                            runCatching { dev.frost819.newbv.danmaku.entity.DanmakuType.entries[it.ordinal] }.getOrNull()
                        }
                        onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(entityTypes))
                    },
                    onDanmakuSizeChange = { onDanmakuSettingChange(DanmakuSettingAction.SetScale(it)) },
                    onDanmakuOpacityChange = { onDanmakuSettingChange(DanmakuSettingAction.SetOpacity(it)) },
                    onDanmakuSpeedFactorChange = { onDanmakuSettingChange(DanmakuSettingAction.SetSpeedFactor(it)) },
                    onDanmakuAreaChange = { onDanmakuSettingChange(DanmakuSettingAction.SetArea(it)) },
                    onFocusStateChange = onFocusStateChange,
                    onDanmakuMaskChange = { onDanmakuSettingChange(DanmakuSettingAction.SetMaskEnabled(it)) },
                )
            }
        }
    }
}

/**
 * 直播画质设置面板（二级：MenuNav ↔ Items）。
 *
 * 直接展示画质选项列表（[RadioMenuList]），按 RIGHT 返回导航栏。
 */
@Composable
private fun LivePictureMenuList(
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onQualityChange: (Int) -> Unit,
    navFocusRequester: FocusRequester,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val focusState = LocalMenuFocusStateData.current
    val itemsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusState.focusState) {
        if (focusState.focusState == MenuFocusState.Items) {
            runCatching { itemsFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(visible = true) {
        RadioMenuList(
            modifier = Modifier
                .width(216.dp)
                .padding(horizontal = 8.dp)
                .focusRequester(itemsFocusRequester),
            items = availableQualities.map { it.second },
            selected = availableQualities.indexOfFirst { it.first == currentQuality }.coerceAtLeast(0),
            onSelectedChanged = { onQualityChange(availableQualities[it].first) },
            onFocusBackToParent = {
                onFocusStateChange(MenuFocusState.MenuNav)
                navFocusRequester.requestFocus()
            },
        )
    }
}

/**
 * 直播菜单导航列表（右侧）。
 *
 * 与视频播放器的 [MenuNavList] 结构相同，仅 Tab 项不同。
 */
@Composable
private fun LiveMenuNavList(
    modifier: Modifier = Modifier,
    selectedMenu: LivePlayerMenuNavItem,
    onSelectedChanged: (LivePlayerMenuNavItem) -> Unit,
    isFocusing: Boolean,
    navFocusRequester: FocusRequester,
) {
    val restorerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { navFocusRequester.requestFocus() }
    }

    LazyColumn(
        modifier = modifier
            .focusRestorer(restorerFocusRequester)
            .focusRequester(navFocusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        itemsIndexed(LivePlayerMenuNavItem.entries) { index, item ->
            MenuListItem(
                modifier = Modifier
                    .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                text = item.displayName,
                icon = item.icon,
                expanded = isFocusing,
                selected = selectedMenu == item,
                onClick = { onSelectedChanged(item) },
                onFocus = { onSelectedChanged(item) },
            )
        }
    }
}
