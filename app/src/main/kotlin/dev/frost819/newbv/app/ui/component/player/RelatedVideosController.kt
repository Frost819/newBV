package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 相关视频覆盖层。
 *
 * 全屏淡入/淡出动画，中央显示一行相关视频卡片。
 * 背景为垂直渐变（上下透明，中间半透明黑色）。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param relatedVideos 相关视频列表
 * @param onVideoClicked 点击视频卡片回调
 */
@Composable
fun RelatedVideosController(
    modifier: Modifier = Modifier,
    show: Boolean,
    relatedVideos: List<VideoCardData>,
    onVideoClicked: (VideoCardData) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.15f to Color.Black.copy(alpha = 0.5f),
            0.85f to Color.Black.copy(alpha = 0.5f),
            1.0f to Color.Transparent,
        )
    }

    LaunchedEffect(show) {
        if (show) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush)
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    text = "相关视频",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
                ) {
                    items(
                        items = relatedVideos,
                        key = { it.avid },
                    ) { video ->
                        SmallVideoCard(
                            modifier = Modifier.width(240.dp),
                            data = video,
                            onClick = { onVideoClicked(video) },
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RelatedVideosControllerPreview() {
    val sampleVideos = listOf(
        VideoCardData(
            avid = 1,
            cid = 101,
            title = "相关视频标题 1",
            cover = "",
            upName = "UP主A",
            playString = "1.2万播放",
            danmakuString = "300弹幕",
            timeString = "10:30",
        ),
        VideoCardData(
            avid = 2,
            cid = 102,
            title = "相关视频标题 2",
            cover = "",
            upName = "UP主B",
            playString = "5000播放",
            danmakuString = "100弹幕",
            timeString = "5:20",
        ),
    )

    BVTheme {
        RelatedVideosController(
            show = true,
            relatedVideos = sampleVideos,
            onVideoClicked = {},
        )
    }
}

// endregion
