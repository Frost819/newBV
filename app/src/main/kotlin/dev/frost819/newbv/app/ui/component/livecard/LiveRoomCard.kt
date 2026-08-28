package dev.frost819.newbv.app.ui.component.livecard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 直播卡片。
 *
 * 展示直播封面、分区名、人气数、标题、主播名。
 * 点击卡片进入直播间。
 *
 * @param data 卡片数据。
 * @param onClick 点击回调。
 */
@Composable
fun LiveRoomCard(
    modifier: Modifier = Modifier,
    data: LiveRoomCardData,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.78f)
                    .touchClickable(onClick = onClick),
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border =
                CardDefaults.border(
                    focusedBorder =
                        Border(
                            border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.border),
                            shape = MaterialTheme.shapes.large,
                        ),
                ),
        ) {
            LiveCardCover(
                cover = data.cover,
                areaName = data.areaV2Name,
                onlineString = data.onlineString,
            )
        }

        LiveCardInfo(
            modifier = Modifier.fillMaxWidth(),
            title = data.title,
            uname = data.uname,
        )
    }
}

@Composable
private fun LiveCardCover(
    modifier: Modifier = Modifier,
    cover: String,
    areaName: String,
    onlineString: String,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
    ) {
        AsyncImage(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large),
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                ),
                        ),
                    ),
        )

        if (areaName.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.error),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = onlineString,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LiveCardInfo(
    modifier: Modifier = Modifier,
    title: String,
    uname: String,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uname,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveRoomCardPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        Box(modifier = Modifier.width(380.dp)) {
            LiveRoomCard(
                data =
                    LiveRoomCardData(
                        roomId = 2537621,
                        title = "你控灯，我来拍。互动拍拍灯(砸地鼠玩法)",
                        uname = "诺艾尔",
                        uid = 33306582,
                        cover = "",
                        face = "",
                        areaV2Name = "搞笑整蛊",
                        areaV2ParentName = "互动玩法",
                        onlineString = "1.2万",
                        watchedString = "3.5万",
                    ),
                onClick = {},
            )
        }
    }
}
