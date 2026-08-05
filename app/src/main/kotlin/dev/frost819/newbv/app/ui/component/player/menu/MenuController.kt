package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.ui.state.player.PlayerUiState
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.MenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerMenuNavItem
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiStatus
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiType
import dev.frost819.newbv.biliapi.entity.video.SubtitleType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.DanmakuType
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 设置菜单控制器。
 *
 * 从右侧滑入的半透明面板，包含导航列表和菜单面板。
 * 三态焦点模型：MenuNav ↔ Menu ↔ Items。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param uiState 播放器 UI 状态
 * @param onResolutionChange 画质变化回调
 * @param onCodecChange 编码变化回调
 * @param onAspectRatioChange 宽高比变化回调
 * @param onPlaySpeedChange 倍速变化回调
 * @param onAudioChange 音轨变化回调
 * @param onDanmakuSwitchChange 弹幕类型变化回调
 * @param onDanmakuSizeChange 弹幕大小变化回调
 * @param onDanmakuOpacityChange 弹幕透明度变化回调
 * @param onDanmakuSpeedFactorChange 弹幕速度变化回调
 * @param onDanmakuAreaChange 弹幕区域变化回调
 * @param onDanmakuMaskChange 弹幕蒙版变化回调
 * @param onSubtitleChange 字幕轨道变化回调
 * @param onSubtitleSizeChange 字幕大小变化回调
 * @param onSubtitleBackgroundOpacityChange 字幕透明度变化回调
 * @param onSubtitleBottomPadding 字幕间距变化回调
 */
@Composable
@Suppress("LongParameterList")
fun MenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    uiState: PlayerUiState,
    onResolutionChange: (Int) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit = {},
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (Int) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Int) -> Unit,
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
            .focusRequester(focusRequester)
            .onFocusChanged {},
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
        ) {
            MenuControllerContent(
                uiState = uiState,
                onResolutionChange = onResolutionChange,
                onCodecChange = onCodecChange,
                onAspectRatioChange = onAspectRatioChange,
                onPlaySpeedChange = onPlaySpeedChange,
                onAudioChange = onAudioChange,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onDanmakuSizeChange = onDanmakuSizeChange,
                onDanmakuOpacityChange = onDanmakuOpacityChange,
                onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                onDanmakuAreaChange = onDanmakuAreaChange,
                onDanmakuMaskChange = onDanmakuMaskChange,
                onSubtitleChange = onSubtitleChange,
                onSubtitleSizeChange = onSubtitleSizeChange,
                onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                onSubtitleBottomPadding = onSubtitleBottomPadding,
            )
        }
    }
}

/**
 * 菜单控制器内容（不含动画包裹层）。
 */
@Composable
@Suppress("LongParameterList")
private fun MenuControllerContent(
    uiState: PlayerUiState,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (Int) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Int) -> Unit,
) {
    var selectedNavItem by remember { mutableStateOf(VideoPlayerMenuNavItem.PlaySpeed) }
    var focusState by remember { mutableStateOf(MenuFocusState.MenuNav) }

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
                MenuList(
                    uiState = uiState,
                    selectedNavMenu = selectedNavItem,
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onAudioChange = onAudioChange,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                    onFocusStateChange = { focusState = it },
                    onSubtitleChange = onSubtitleChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding,
                )
                MenuNavList(
                    modifier = Modifier.onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                return@onPreviewKeyEvent false
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.DirectionLeft) focusState = MenuFocusState.Menu
                        false
                    },
                    selectedMenu = selectedNavItem,
                    onSelectedChanged = { selectedNavItem = it },
                    isFocusing = focusState == MenuFocusState.MenuNav,
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
private fun MenuList(
    uiState: PlayerUiState,
    selectedNavMenu: VideoPlayerMenuNavItem,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (Int) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Int) -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        when (selectedNavMenu) {
            VideoPlayerMenuNavItem.Picture -> PictureMenuList(
                availableQualityIds = uiState.availableQuality.keys.toList(),
                availableAudio = uiState.availableAudio,
                availableVideoCodec = uiState.availableVideoCodec,
                currentResolution = uiState.mediaProfileState.qualityId,
                currentVideoCodec = uiState.mediaProfileState.videoCodec,
                currentVideoAspectRatio = uiState.aspectRatio,
                currentAudio = uiState.mediaProfileState.audio,
                onResolutionChange = onResolutionChange,
                onCodecChange = onCodecChange,
                onAspectRatioChange = onAspectRatioChange,
                onAudioChange = onAudioChange,
                onFocusStateChange = onFocusStateChange,
            )

            VideoPlayerMenuNavItem.PlaySpeed -> PlaySpeedMenuList(
                currentSelectedPlaySpeedItem = PlaySpeedItem.fromSpeed(uiState.playSpeed),
                onPlaySpeedChange = onPlaySpeedChange,
                onFocusStateChange = onFocusStateChange,
            )

            VideoPlayerMenuNavItem.Danmaku -> {
                // 将 danmaku 模块 DanmakuType 映射为 data 模块 DanmakuType
                val dataTypes = uiState.danmakuState.enabledTypes.mapNotNull { entity ->
                    runCatching { dev.frost819.newbv.data.datastore.DanmakuType.entries[entity.ordinal] }.getOrNull()
                }
                DanmakuMenuList(
                    currentEnabledTypes = dataTypes,
                    currentScale = uiState.danmakuState.scale,
                    currentOpacity = uiState.danmakuState.opacity,
                    currentSpeedFactor = uiState.danmakuState.speedFactor,
                    currentArea = uiState.danmakuState.area,
                    currentMaskEnabled = uiState.danmakuState.maskEnabled,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onFocusStateChange = onFocusStateChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                )
            }

            VideoPlayerMenuNavItem.ClosedCaption -> {
                val subtitleTracks = buildList {
                    add(
                        Subtitle(
                            id = -1,
                            lang = "",
                            langDoc = "关闭",
                            url = "",
                            type = SubtitleType.CC,
                            aiType = SubtitleAiType.Normal,
                            aiStatus = SubtitleAiStatus.None,
                        ),
                    )
                    addAll(uiState.subtitleList)
                    sortBy { it.id }
                }
                ClosedCaptionMenuList(
                    currentSubtitleId = uiState.subtitleId,
                    availableSubtitleTracks = subtitleTracks,
                    currentFontSize = uiState.subtitleState.fontSize,
                    currentOpacity = uiState.subtitleState.opacity,
                    currentPadding = uiState.subtitleState.bottomPadding,
                    onSubtitleChange = onSubtitleChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding,
                    onFocusStateChange = onFocusStateChange,
                )
            }
        }
    }
}
