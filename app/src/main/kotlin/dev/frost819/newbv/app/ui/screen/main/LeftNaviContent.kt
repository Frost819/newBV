package dev.frost819.newbv.app.ui.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import coil3.compose.AsyncImage
import dev.frost819.newbv.core.focus.isDpadRight
import dev.frost819.newbv.core.focus.isKeyDown
import dev.frost819.newbv.data.datastore.LeftNaviItem

/**
 * 左侧导航栏。
 *
 * 使用 Material3 [NavigationRail]，顶部为用户头像（或登录按钮），
 * 中间为 6 个导航项（含直播），底部为设置入口。
 *
 * D-Pad 右键触发 [onFocusToContent] 将焦点移至内容区。
 *
 * @param isLogin 是否已登录。
 * @param avatar 头像 URL。
 * @param selectedItem 当前选中的导航项。
 * @param onLeftNaviItemChanged 导航项切换回调。
 * @param onOpenSettings 打开设置回调。
 * @param onShowUserPanel 显示用户面板回调。
 * @param onFocusToContent 焦点移至内容区回调。
 * @param onLogin 登录回调。
 */
@Composable
fun LeftNaviContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    selectedItem: LeftNaviItem,
    onLeftNaviItemChanged: (LeftNaviItem) -> Unit,
    onOpenSettings: () -> Unit,
    onShowUserPanel: () -> Unit,
    onFocusToContent: () -> Unit,
    onLogin: () -> Unit,
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        onFocusToContent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        var userIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                userIsFocused = it.hasFocus
            },
            onClick = {
                if (isLogin) {
                    onShowUserPanel()
                } else {
                    onLogin()
                }
            },
            selected = userIsFocused,
            icon = {
                if (isLogin) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        colors = SurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                    )
                }
            },
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            LeftNaviItem.entries.forEach { item ->
                var isFocused by remember { mutableStateOf(false) }
                val indicatorColor by animateColorAsState(
                    targetValue = if (item == selectedItem) {
                        MaterialTheme.colorScheme.border
                    } else {
                        Color.Transparent
                    },
                    label = "selection-indicator",
                )
                NavigationRailItem(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.hasFocus }
                        .selectionIndicator(indicatorColor),
                    onClick = { onLeftNaviItemChanged(item) },
                    selected = isFocused,
                    icon = {
                        Icon(
                            imageVector = item.displayIcon,
                            contentDescription = null,
                        )
                    },
                )
            }
        }

        var settingsIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                settingsIsFocused = it.hasFocus
            },
            onClick = onOpenSettings,
            selected = settingsIsFocused,
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
        )
    }
}

/** 左侧导航项的图标与显示名称扩展。 */
val LeftNaviItem.displayIcon: ImageVector
    get() = when (this) {
        LeftNaviItem.Search -> Icons.Default.Search
        LeftNaviItem.Personal -> Icons.Default.Person
        LeftNaviItem.Home -> Icons.Default.Home
        LeftNaviItem.UGC -> Icons.Default.OndemandVideo
        LeftNaviItem.PGC -> Icons.Default.Movie
        LeftNaviItem.Live -> Icons.Default.LiveTv
    }

/** 左侧导航项的显示名称扩展。 */
val LeftNaviItem.displayName: String
    get() = when (this) {
        LeftNaviItem.Search -> "搜索"
        LeftNaviItem.Personal -> "个人"
        LeftNaviItem.Home -> "主页"
        LeftNaviItem.UGC -> "分区"
        LeftNaviItem.PGC -> "影视"
        LeftNaviItem.Live -> "直播"
    }

/** 绘制左侧选中指示条。 */
private fun Modifier.selectionIndicator(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 4.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = Size(width = strokeWidth, height = size.height),
    )
}

@Preview(showBackground = true, heightDp = 1080)
@Composable
private fun LeftNaviContentPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        LeftNaviContent(
            isLogin = true,
            avatar = "",
            selectedItem = LeftNaviItem.Home,
            onLeftNaviItemChanged = {},
            onOpenSettings = {},
            onShowUserPanel = {},
            onFocusToContent = {},
            onLogin = {},
        )
    }
}
