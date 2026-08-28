package dev.frost819.newbv.app.viewmodel.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 播放器菜单 ViewModel。
 *
 * 管理设置菜单的 UI 状态：当前选中的 Tab、选中的设置项。
 * 不涉及数据加载或播放控制，纯 UI 状态。
 */
@HiltViewModel
class PlayerMenuViewModel
    @Inject
    constructor() : ViewModel() {
        private val _menuState = MutableStateFlow(PlayerMenuState())
        val menuState = _menuState.asStateFlow()

        /** 选中导航 Tab。 */
        fun selectNavItem(item: VideoPlayerMenuNavItem) {
            _menuState.update {
                it.copy(
                    selectedNavItem = item,
                    selectedPictureItem = null,
                    selectedDanmakuItem = null,
                    selectedClosedCaptionItem = null,
                )
            }
        }

        /** 选中画质/编码/宽高比/音轨中的子项。 */
        fun selectPictureItem(item: VideoPlayerPictureMenuItem?) {
            _menuState.update { it.copy(selectedPictureItem = item) }
        }

        /** 选中弹幕设置中的子项。 */
        fun selectDanmakuItem(item: VideoPlayerDanmakuMenuItem?) {
            _menuState.update { it.copy(selectedDanmakuItem = item) }
        }

        /** 选中字幕设置中的子项。 */
        fun selectClosedCaptionItem(item: VideoPlayerClosedCaptionMenuItem?) {
            _menuState.update { it.copy(selectedClosedCaptionItem = item) }
        }

        /** 关闭所有子项选择。 */
        fun clearSelection() {
            _menuState.update {
                it.copy(
                    selectedPictureItem = null,
                    selectedDanmakuItem = null,
                    selectedClosedCaptionItem = null,
                )
            }
        }
    }

/**
 * 播放器菜单 UI 状态。
 */
data class PlayerMenuState(
    val selectedNavItem: VideoPlayerMenuNavItem = VideoPlayerMenuNavItem.PlaySpeed,
    val selectedPictureItem: VideoPlayerPictureMenuItem? = null,
    val selectedDanmakuItem: VideoPlayerDanmakuMenuItem? = null,
    val selectedClosedCaptionItem: VideoPlayerClosedCaptionMenuItem? = null,
)

/** 设置菜单导航 Tab。 */
enum class VideoPlayerMenuNavItem(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    PlaySpeed("倍速", Icons.Outlined.Speed),
    Picture("画质", Icons.Outlined.Image),
    Danmaku("弹幕", Icons.Outlined.ClearAll),
    ClosedCaption("字幕", Icons.Outlined.ClosedCaption),
}

/** 画质设置子项。 */
enum class VideoPlayerPictureMenuItem(
    val displayName: String,
) {
    Resolution("分辨率"),
    Codec("编码"),
    AspectRatio("宽高比"),
    Audio("音轨"),
}

/** 弹幕设置子项。 */
enum class VideoPlayerDanmakuMenuItem(
    val displayName: String,
) {
    Switch("开关"),
    Size("大小"),
    Opacity("透明度"),
    SpeedFactor("速度"),
    Area("区域"),
    Mask("蒙版"),
}

/** 字幕设置子项。 */
enum class VideoPlayerClosedCaptionMenuItem(
    val displayName: String,
) {
    Switch("开关"),
    Size("大小"),
    Opacity("透明度"),
    Padding("间距"),
}

/**
 * 菜单焦点状态。
 *
 * 三态焦点模型：
 * - [MenuNav] — 焦点在右侧导航列表
 * - [Menu] — 焦点在中间子项列表
 * - [Items] — 焦点在最左侧选项值面板
 */
enum class MenuFocusState {
    MenuNav,
    Menu,
    Items,
}

/**
 * 菜单焦点状态数据，通过 CompositionLocal 传递给子组件。
 */
data class MenuFocusStateData(
    val focusState: MenuFocusState = MenuFocusState.MenuNav,
)

/**
 * 菜单焦点状态 CompositionLocal。
 */
val LocalMenuFocusStateData = compositionLocalOf { MenuFocusStateData() }
