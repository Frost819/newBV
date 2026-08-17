package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.app.data.VideoSharedState
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 播放器视频交互弹窗。
 *
 * @param actionState 当前视频操作状态
 * @param onLike 点赞/取消点赞
 * @param onCoin 投币
 * @param onFavorite 收藏/取消收藏
 * @param onOneClickTriple 长按点赞触发一键三连
 * @param onDismiss 关闭回调
 */
@Composable
fun VideoInteractionDialog(
    actionState: VideoSharedState?,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onFavorite: () -> Unit,
    onOneClickTriple: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "视频交互",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InteractionAction(
                            text = if (actionState?.liked == true) "已点赞" else "点赞",
                            icon = if (actionState?.liked == true) {
                                Icons.Rounded.ThumbUp
                            } else {
                                Icons.Outlined.ThumbUp
                            },
                            onClick = onLike,
                            onLongClick = onOneClickTriple,
                        )

                        InteractionAction(
                            text = if (actionState?.coined == true) "已投币" else "投币",
                            icon = if (actionState?.coined == true) {
                                Icons.Rounded.Paid
                            } else {
                                Icons.Outlined.Paid
                            },
                            onClick = onCoin,
                        )

                        InteractionAction(
                            text = if (actionState?.favorited == true) "已收藏" else "收藏",
                            icon = if (actionState?.favorited == true) {
                                Icons.Rounded.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            onClick = onFavorite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .size(100.dp)
            .touchClickable(onClick = onClick, onLongClick = onLongClick),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(28.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// region Previews

@Preview(device = "id:tv_1080p", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoInteractionDialogPreview() {
    BVTheme {
        VideoInteractionDialog(
            actionState = VideoSharedState(
                aid = 1L,
                liked = false,
                coined = false,
                favorited = false,
            ),
            onLike = {},
            onCoin = {},
            onFavorite = {},
            onOneClickTriple = {},
            onDismiss = {},
        )
    }
}

@Preview(device = "id:tv_1080p", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoInteractionDialogAllDonePreview() {
    BVTheme {
        VideoInteractionDialog(
            actionState = VideoSharedState(
                aid = 1L,
                liked = true,
                coined = true,
                favorited = true,
            ),
            onLike = {},
            onCoin = {},
            onFavorite = {},
            onOneClickTriple = {},
            onDismiss = {},
        )
    }
}

// endregion
