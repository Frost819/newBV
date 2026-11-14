package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 番剧/影视卡片。
 *
 * 封面（0.75 宽高比）+ 底部渐变评分 + 标题/副标题。
 * 焦点选中时显示主题边框。
 *
 * @param data 卡片数据。
 * @param onClick 点击回调。
 * @param onGoToDetailPage 跳转详情页回调（长按或菜单键）。
 * @param modifier Modifier。
 */
@Composable
fun SeasonCard(
    data: SeasonCardData,
    onClick: () -> Unit,
    onGoToDetailPage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 形状与边框只随主题变化，列表滚动时不再每次重组都分配 Border/BorderStroke。
    val largeShape = MaterialTheme.shapes.large
    val borderColor = MaterialTheme.colorScheme.border
    val focusedBorder =
        remember(largeShape, borderColor) {
            Border(border = BorderStroke(3.dp, borderColor), shape = largeShape)
        }
    Surface(
        modifier = modifier.touchClickable(onClick = onClick),
        onClick = onClick,
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                pressedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = ClickableSurfaceDefaults.shape(shape = largeShape),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusedBorder),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // 圆角裁剪由外层 Box 负责，图片不再重复 clip（少一层离屏图层）。
                AsyncImage(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f),
                    model = rememberCoverThumbnail(cover = data.cover, height = 640, width = 480),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                )

                if (data.hasRating) {
                    Box(
                        modifier =
                            Modifier
                                .height(48.dp)
                                .fillMaxWidth()
                                .background(
BottomScrim,
                                ),
                    )
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .fillMaxWidth()
                                .padding(8.dp, 0.dp),
                        text = data.rating ?: "",
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.End,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!data.subTitle.isNullOrEmpty()) {
                    Text(
                        text = data.subTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
/** 卡片底部信息条的渐变遮罩：颜色固定，类加载时构造一次，避免每张卡片每次重组都新建 Brush 和 List。 */
private val BottomScrim =
    Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
    )
