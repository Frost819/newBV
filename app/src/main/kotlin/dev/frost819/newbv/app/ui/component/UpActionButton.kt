package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.touchClickable

/**
 * UP 主相关操作按钮（关注、点赞、投币、收藏等）。
 *
 * 详情页与 UP 主主页共用，保证两处关注按钮的图标、配色与聚焦样式一致。
 * 支持 D-Pad 聚焦以及触屏点击/长按。
 *
 * @param text 按钮文案（如"关注"/"已关注"）。
 * @param icon 按钮图标。
 * @param highlighted 是否为激活态（如已关注、已点赞），激活态使用主题色容器。
 * @param onClick 点击回调。
 * @param onLongClick 长按回调，默认无。
 * @param modifier 外部修饰符，用于焦点绑定与布局。
 */
@Composable
fun UpActionButton(
    text: String,
    icon: ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        // 不要在外层用 .clip()：它会裁剪掉 Surface 内部的聚焦放大，导致漂浮被裁切
        modifier = modifier.touchClickable(onClick = onClick, onLongClick = onLongClick),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors =
            focusInvertedColors(
                containerColor =
                    if (highlighted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (highlighted) {
                        Border(
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.border),
                            shape = MaterialTheme.shapes.small,
                        )
                    } else {
                        Border.None
                    },
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
