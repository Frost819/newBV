package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 播放状态提示覆盖层。
 *
 * 根据播放状态显示不同的提示：
 * - **暂停**：右下角显示暂停图标（仅当非播放、非缓冲、非错误时）
 * - **缓冲中**：屏幕中央显示加载指示器 + "缓冲中..."
 * - **错误**：屏幕中央显示错误信息
 *
 * 三种状态互斥，优先级：error > buffering > paused。
 *
 * @param modifier 修饰符
 * @param isPlaying 是否正在播放
 * @param isBuffering 是否正在缓冲
 * @param isError 是否发生错误
 * @param errorMessage 错误信息（为 null 时显示"未知错误"）
 */
@Composable
fun PlayStateTips(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isError: Boolean,
    errorMessage: String? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!isPlaying && !isBuffering && !isError) {
            PauseIcon(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
            )
        }
        if (isBuffering && !isError) {
            BufferingTip(
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (isError) {
            PlayErrorTip(
                modifier = Modifier.align(Alignment.Center),
                errorMessage = errorMessage,
            )
        }
    }
}

/**
 * 暂停图标。
 *
 * 右下角显示的半透明暂停图标，提示用户当前处于暂停状态。
 */
@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        colors =
            SurfaceDefaults.colors(
                containerColor = Color.Black.copy(0.5f),
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Icon(
            modifier =
                Modifier
                    .padding(12.dp, 4.dp)
                    .size(50.dp),
            imageVector = Icons.Rounded.Pause,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

/**
 * 缓冲提示。
 *
 * 屏幕中央显示的半透明加载框，包含圆形进度指示器和"缓冲中..."文本。
 */
@Composable
fun BufferingTip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        colors =
            SurfaceDefaults.colors(
                containerColor = Color.Black.copy(0.5f),
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(36.dp)
                        .padding(8.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Text(
                text = "缓冲中...",
                fontSize = 24.sp,
            )
        }
    }
}

/**
 * 播放错误提示。
 *
 * 屏幕中央显示的半透明错误框，包含标题和错误信息。
 *
 * @param errorMessage 错误信息（为 null 时显示"未知错误"）
 */
@Composable
fun PlayErrorTip(
    modifier: Modifier = Modifier,
    errorMessage: String?,
) {
    Surface(
        modifier = modifier,
        colors =
            SurfaceDefaults.colors(
                containerColor = Color.Black.copy(0.5f),
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp, 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "播放器正在抽风",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(text = " _(:з」∠)_")
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "错误信息：${errorMessage ?: "未知错误"}")
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayStateTipsPausedPreview() {
    BVTheme {
        PlayStateTips(
            isPlaying = false,
            isBuffering = false,
            isError = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayStateTipsBufferingPreview() {
    BVTheme {
        PlayStateTips(
            isPlaying = false,
            isBuffering = true,
            isError = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayStateTipsErrorPreview() {
    BVTheme {
        PlayStateTips(
            isPlaying = false,
            isBuffering = false,
            isError = true,
            errorMessage = "网络连接失败 (404)",
        )
    }
}

// endregion
