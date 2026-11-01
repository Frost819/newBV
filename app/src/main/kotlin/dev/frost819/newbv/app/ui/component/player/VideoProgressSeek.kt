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
import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.entity.player.SponsorCategories
import dev.frost819.newbv.app.entity.player.SponsorSegment
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
 * 空降片段标记（[skipSegments]）绘制在播放进度之上，按分类着色，
 * 使已播与未播区域的片段覆盖范围都清晰可辨。
 *
 * @param modifier 修饰符
 * @param duration 视频总时长（毫秒）
 * @param position 当前播放位置（毫秒）
 * @param bufferedPercentage 缓冲百分比（0-100）
 * @param isPersistentSeek 是否为常显模式
 * @param skipSegments 空降片段列表（已归一化、按起点升序），空列表不绘制标记层
 */
@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean,
    skipSegments: List<SponsorSegment> = emptyList(),
) {
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp
    val activeTrackColor = BVColors.PlayerSeekActive
    val bufferedTrackColor = BVColors.PlayerSeekBuffered
    val inactiveTrackColor = BVColors.PlayerSeekInactive
    val skipGap = if (isPersistentSeek) 1.dp else 2.dp

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

        // 空降片段标记：绘制在播放进度之上，已播/未播区域都清晰可辨
        if (skipSegments.isNotEmpty() && duration > 0) {
            val halfWidth = trackWidthPx / 2f
            val trackSpan = size.width - trackWidthPx
            if (trackSpan > 0f) {
                val skipBounds =
                    computeSkipSegmentBounds(
                        segments = skipSegments,
                        durationMs = duration,
                        trackWidthPx = trackSpan,
                        strokeWidthPx = trackWidthPx,
                        gapPx = skipGap.toPx(),
                    )
                skipSegments.forEachIndexed { index, segment ->
                    val (startX, endX) = skipBounds[index]
                    if (endX <= startX) return@forEachIndexed
                    drawLine(
                        color = Color(SponsorCategories.colorOf(segment.category)).copy(alpha = 0.6f),
                        start = Offset(halfWidth + startX, center.y),
                        end = Offset(halfWidth + endX, center.y),
                        strokeWidth = trackWidthPx,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/**
 * 计算空降片段在进度条有效轨道上的绘制区间（像素）。
 *
 * 每段按时间比例映射到轨道宽度；时间上首尾相接或重叠的相邻片段在边界处各缩进
 * 抵消 `StrokeCap.Round` 半圆帽外延并留出 [gapPx] 可见缺口，避免相邻不同分类颜色互相覆盖。
 *
 * @param segments 归一化并按起点升序的空降片段列表
 * @param durationMs 视频总时长（毫秒）
 * @param trackWidthPx 轨道有效宽度（像素）
 * @param strokeWidthPx 描边宽度（像素）
 * @param gapPx 相邻片段间可见缺口宽度（像素）
 * @return 与 [segments] 一一对应的 (startX, endX) 像素区间；过窄片段收成空区间
 */
internal fun computeSkipSegmentBounds(
    segments: List<SponsorSegment>,
    durationMs: Long,
    trackWidthPx: Float,
    strokeWidthPx: Float,
    gapPx: Float,
): List<Pair<Float, Float>> {
    if (durationMs <= 0L || segments.isEmpty() || trackWidthPx <= 0f) return emptyList()
    val inset = strokeWidthPx / 2f + gapPx / 2f
    val bounds = ArrayList<Pair<Float, Float>>(segments.size)
    for ((index, segment) in segments.withIndex()) {
        val startRatio = (segment.startMs / durationMs.toFloat()).coerceIn(0f, 1f)
        val endRatio = (segment.endMs / durationMs.toFloat()).coerceIn(0f, 1f)
        val startX = startRatio * trackWidthPx
        val endX = endRatio * trackWidthPx
        val previous = segments.getOrNull(index - 1)
        val next = segments.getOrNull(index + 1)
        val adjustedStart = if (previous != null && previous.endMs >= segment.startMs) startX + inset else startX
        val adjustedEnd = if (next != null && next.startMs <= segment.endMs) endX - inset else endX
        bounds += if (adjustedEnd > adjustedStart) adjustedStart to adjustedEnd else startX to startX
    }
    return bounds
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

@Preview(showBackground = true)
@Composable
private fun VideoProgressSeekSkipSegmentsPreview() {
    BVTheme {
        VideoProgressSeek(
            duration = 100_000L,
            position = 35_000L,
            bufferedPercentage = 60,
            isPersistentSeek = false,
            skipSegments =
                listOf(
                    SponsorSegment(
                        uuid = "1",
                        category = SponsorCategories.SPONSOR,
                        actionType = SponsorActionType.SKIP,
                        startMs = 10_000L,
                        endMs = 20_000L,
                    ),
                    SponsorSegment(
                        uuid = "2",
                        category = SponsorCategories.OUTRO,
                        actionType = SponsorActionType.SKIP,
                        startMs = 90_000L,
                        endMs = 98_000L,
                    ),
                ),
        )
    }
}

// endregion
