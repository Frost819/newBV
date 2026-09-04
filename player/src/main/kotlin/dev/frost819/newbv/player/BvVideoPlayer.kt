package dev.frost819.newbv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.frost819.newbv.player.impl.exo.ExoMediaPlayer

/**
 * B 站视频播放器 Composable。
 *
 * 封装 Media3 [PlayerView]，将 [ExoMediaPlayer] 的 ExoPlayer 实例绑定到 UI。
 *
 * @param modifier Compose 修饰符
 * @param videoPlayer 播放器实例，为 null 时不渲染
 */
@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer?,
) {
    if (videoPlayer is ExoMediaPlayer) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = videoPlayer.mPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = videoPlayer.mPlayer
            },
            onRelease = { playerView ->
                playerView.player = null
            },
        )
    }
}
