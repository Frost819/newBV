package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 视频进度条。
 *
 * 使用 Canvas 绘制三层进度线：背景轨道、缓冲进度、播放进度。
 * 三段轨道用「明度阶梯 + 色相」双重区分，取值见 [BVColors.PlayerSeekActive] /
 * [BVColors.PlayerSeekBuffered] / [BVColors.PlayerSeekInactive]：
 * 已播放用提亮的品牌紫，已缓冲用中性中灰，未缓冲用中性暗灰（相对亮度约 0.41 / 0.25 / 0.08）。
 *
 * 这里刻意使用固定色而不是半透明白：进度条直接压在视频画面上，
 * 半透明白的观感亮度随画面而变，亮画面上「未缓冲」会反超「已播放」段。
 * 支持两种显示模式：
 * - **常显模式**（[isPersistentSeek] = true）：2dp 细线，不显示缓冲进度，
 *   用于播放器底部始终可见的进度条。
 * - **交互模式**（[isPersistentSeek] = false）：8dp 粗线，显示缓冲进度，
 *   用于控制器信息栏中的可交互 seek bar。
 *
 * @param modifier 修饰符
 * @param duration 视频总时长（毫秒）
 * @param position 当前播放位置（毫秒）
 * @param bufferedPercentage 缓冲百分比（0-100）
 * @param isPersistentSeek 是否为常显模式
 */
@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean,
) {
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp
    val activeTrackColor = BVColors.PlayerSeekActive
    val bufferedTrackColor = BVColors.PlayerSeekBuffered
    val inactiveTrackColor = BVColors.PlayerSeekInactive

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(trackWidthDp)
                .clip(RoundedCornerShape(50)),
    ) {
        val trackWidthPx = trackWidthDp.toPx()

        // 背景轨道
        drawLine(
            color = inactiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = trackWidthPx,
            cap = StrokeCap.Round,
        )

        // 缓冲进度（仅交互模式显示）
        if (!isPersistentSeek && bufferedPercentage > 0) {
            drawLine(
                color = bufferedTrackColor,
                start = Offset(trackWidthPx / 2, center.y),
                end = Offset(size.width * bufferedPercentage / 100, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round,
            )
        }

        // 播放进度
        if (duration > 0) {
            val progressRatio = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            drawLine(
                color = activeTrackColor,
                start = Offset(trackWidthPx / 2, center.y),
                end = Offset(size.width * progressRatio, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun VideoProgressSeekPersistentPreview() {
    BVTheme {
        VideoProgressSeek(
            duration = 100_000L,
            position = 35_000L,
            bufferedPercentage = 60,
            isPersistentSeek = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoProgressSeekNormalPreview() {
    BVTheme {
        VideoProgressSeek(
            duration = 100_000L,
            position = 35_000L,
            bufferedPercentage = 60,
            isPersistentSeek = false,
        )
    }
}

/**
 * 亮画面场景：验证进度条压在浅色视频画面上时三档仍可区分，
 * 且不会出现「未缓冲轨道比已播放段更亮」的倒挂。
 */
@Preview(showBackground = true, backgroundColor = 0xFF808080)
@Composable
private fun VideoProgressSeekOnBrightFramePreview() {
    BVTheme {
        Box(
            modifier =
                Modifier
                    .background(Color(0xFF808080))
                    .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            VideoProgressSeek(
                duration = 100_000L,
                position = 35_000L,
                bufferedPercentage = 60,
                isPersistentSeek = false,
            )
        }
    }
}

// endregion
