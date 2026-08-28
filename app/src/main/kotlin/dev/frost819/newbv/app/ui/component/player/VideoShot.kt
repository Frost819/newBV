package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.app.util.SpriteFrame
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.app.util.getSpriteFrame
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Seek 缩略图预览。
 *
 * 根据当前 seek 位置从 [VideoShot] 的 sprite sheet 中提取对应帧并显示。
 * 缩略图水平位置根据播放进度计算，两端通过 [coercedOffset] 防止越界。
 *
 * @param modifier 修饰符
 * @param videoShot 视频缩略图数据
 * @param imageCache 图片解码缓存
 * @param position 当前 seek 位置（毫秒）
 * @param duration 视频总时长（毫秒）
 * @param coercedOffset 水平位置边界偏移（dp），防止缩略图在两端超出屏幕
 */
@Composable
fun VideoShot(
    modifier: Modifier = Modifier,
    videoShot: VideoShot,
    imageCache: VideoShotImageCache,
    position: Long,
    duration: Long,
    coercedOffset: Dp = 0.dp,
) {
    var spriteFrame by remember { mutableStateOf<SpriteFrame?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        LaunchedEffect(position) {
            delay(16)
            spriteFrame = videoShot.getSpriteFrame(position.toInt() / 1000, imageCache)
        }

        spriteFrame?.let { frame ->
            VideoShotImage(
                modifier =
                    Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)

                            val containerWidthPx = constraints.maxWidth
                            val imageWidthPx = placeable.width
                            val coercedOffsetPx = coercedOffset.roundToPx()

                            val xPosition =
                                if (duration <= 0L) {
                                    0
                                } else {
                                    val progress = position.toDouble() / duration.toDouble()
                                    val rawOffset = (-imageWidthPx / 2.0) + (containerWidthPx * progress)

                                    val minOffset = coercedOffsetPx.toDouble()
                                    val maxOffset =
                                        (containerWidthPx - imageWidthPx - coercedOffsetPx).toDouble()

                                    rawOffset.coerceIn(minOffset, maxOffset).toInt()
                                }

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(x = xPosition, y = 0)
                            }
                        },
                spriteFrame = frame,
            )
        }
    }
}

/**
 * 渲染 sprite sheet 中的单个帧。
 *
 * 使用 [drawWithCache] + [drawImage] 直接从大图中绘制子区域，
 * 零像素拷贝。帧高度固定 100dp，宽度按宽高比自适应。
 *
 * @param modifier 修饰符
 * @param spriteFrame 帧信息（包含 sprite sheet 和子矩形区域）
 */
@Composable
fun VideoShotImage(
    modifier: Modifier = Modifier,
    spriteFrame: SpriteFrame,
) {
    val aspectRatio = spriteFrame.srcRect.width.toFloat() / spriteFrame.srcRect.height

    Spacer(
        modifier =
            modifier
                .height(100.dp)
                .aspectRatio(aspectRatio)
                .shadow(4.dp, MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.large)
                .drawWithCache {
                    onDrawBehind {
                        drawImage(
                            image = spriteFrame.spriteSheet,
                            srcOffset = spriteFrame.srcRect.topLeft,
                            srcSize = spriteFrame.srcRect.size,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.Low,
                        )
                    }
                },
    )
}

// region Previews

@Preview(device = "id:tv_1080p")
@Composable
private fun VideoShotPreview() {
    MaterialTheme {
        VideoShot(
            videoShot =
                VideoShot(
                    times = emptyList(),
                    imageCountX = 0,
                    imageCountY = 0,
                    imageWidth = 0,
                    imageHeight = 0,
                    images = emptyList(),
                ),
            imageCache = VideoShotImageCache(),
            position = 234_000L,
            duration = 1234_000L,
        )
    }
}

// endregion
