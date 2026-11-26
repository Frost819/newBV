package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.biliapi.entity.video.VideoChapterSegment
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 视频进度条。
 *
 * 使用 Canvas 绘制多层进度线：背景轨道、章节分段高亮、缓冲进度、播放进度。
 * 三段主轨道用「明度阶梯 + 色相」双重区分，取值见 [BVColors.PlayerSeekActive] /
 * [BVColors.PlayerSeekBuffered] / [BVColors.PlayerSeekInactive]：
 * 已播放用提亮的品牌紫，已缓冲用中性中灰，未缓冲用中性暗灰（相对亮度约 0.41 / 0.25 / 0.08）。
 *
 * 这里刻意使用固定色而不是半透明白：进度条直接压在视频画面上，
 * 半透明白的观感亮度随画面而变，亮画面上「未缓冲」会反超「已播放」段。
 *
 * 有章节数据（[chapterSegments] 非空）时，在背景轨道上按时间比例绘制章节分段，
 * 并在章节边界裁出透明缺口（使已缓冲/已播放区间同样保留可见的章节边界）；
 * 章节名标签由 [VideoChapterLabels] 独立渲染在聚焦轮廓线外上方，避免撑大进度条焦点框。
 * 无章节数据时回退为单线绘制，渲染与原行为完全一致。
 *
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
 * @param chapterSegments 归一化后的章节分段（[VideoChapters.normalize] 的产物），
 *   需已按 fromMs 升序、互不重叠、裁剪进片长
 */
@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean,
    chapterSegments: List<VideoChapterSegment> = emptyList(),
) {
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp
    val activeTrackColor = BVColors.PlayerSeekActive
    val bufferedTrackColor = BVColors.PlayerSeekBuffered
    val inactiveTrackColor = BVColors.PlayerSeekInactive
    val chapterTrackColor = BVColors.PlayerSeekChapter
    // 分段缺口宽度（可见部分）；常显模式线更细，缺口等比缩小
    val chapterGap = if (isPersistentSeek) 1.dp else 2.dp

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(trackWidthDp)
                // 使用离屏缓冲，使章节分隔的 BlendMode.Clear 仅挖空进度条自身图层露出底部背景
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clip(RoundedCornerShape(50)),
    ) {
        val trackWidthPx = trackWidthDp.toPx()
        // StrokeCap.Round 的半圆帽会向端点外侧各延伸 trackWidthPx/2，
        // 若端点落在 0 或 size.width，圆帽会被 Canvas 边界裁掉一半。
        // 所有轨道统一映射到内缩后的有效区间 [startX, endX]。
        val halfWidth = trackWidthPx / 2f
        val trackStartX = halfWidth
        val trackEndX = size.width - halfWidth
        val trackSpan = trackEndX - trackStartX
        if (trackSpan <= 0f) return@Canvas
        val gapPx = chapterGap.toPx()

        // 背景轨道
        drawLine(
            color = inactiveTrackColor,
            start = Offset(trackStartX, center.y),
            end = Offset(trackEndX, center.y),
            strokeWidth = trackWidthPx,
            cap = StrokeCap.Round,
        )

        // 章节分段高亮（有章节数据时）——按有效区间映射，并补偿分段圆帽外延宽度
        val segmentBounds =
            computeChapterSegmentBounds(
                segments = chapterSegments,
                durationMs = duration,
                trackWidthPx = trackSpan,
                strokeWidthPx = trackWidthPx,
                gapPx = gapPx,
            ).map { (start, end) -> trackStartX + start to trackStartX + end }
        for ((startX, endX) in segmentBounds) {
            if (endX <= startX) continue
            drawLine(
                color = chapterTrackColor,
                start = Offset(startX, center.y),
                end = Offset(endX, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round,
            )
        }

        // 缓冲进度（仅交互模式显示）
        if (!isPersistentSeek && bufferedPercentage > 0) {
            val bufferedRatio = (bufferedPercentage / 100f).coerceIn(0f, 1f)
            drawLine(
                color = bufferedTrackColor,
                start = Offset(trackStartX, center.y),
                end = Offset(trackStartX + trackSpan * bufferedRatio, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round,
            )
        }

        // 播放进度：一条连续主线，按播放比例增长。
        // 不依赖章节分段填充，保证章节间隙、尾章之后等无章节覆盖区也正常增长，
        // 避免播放到这些区间时进度条"卡死"的错觉。
        if (duration > 0) {
            val progressRatio = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            drawLine(
                color = activeTrackColor,
                start = Offset(trackStartX, center.y),
                end = Offset(trackStartX + trackSpan * progressRatio, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round,
            )
        }

        // 在章节边界裁出透明缺口，确保已缓冲/已播放线段覆盖后仍能清晰分辨章节边界
        if (chapterSegments.isNotEmpty() && duration > 0L) {
            val gapCenters =
                computeChapterGapCenters(
                    segments = chapterSegments,
                    durationMs = duration,
                    trackWidthPx = trackSpan,
                    minSpacingPx = gapPx * 3f,
                )
            for (offsetX in gapCenters) {
                val x = trackStartX + offsetX
                drawLine(
                    color = Color.Transparent,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = gapPx,
                    blendMode = BlendMode.Clear,
                )
            }
        }
    }
}

/**
 * 进度条上方的章节名标签行。
 *
 * 独立于 [VideoProgressSeek] 之外渲染，使外层的聚焦轮廓线只包裹进度条本身而不被文字撑开。
 * 按 [chapterLabelLayout] 的比例位置对齐各章节分段：宽分段居中显示并在末尾省略，
 * 较窄分段（能容下首字但容不下省略号）左对齐裁剪以至少展示开头字符，极窄分段自动隐藏。
 *
 * @param modifier 修饰符（应与进度条保持相同的水平宽度约束以保证对齐）
 * @param chapterSegments 归一化后的章节分段
 * @param duration 视频总时长（毫秒）
 */
@Composable
fun VideoChapterLabels(
    modifier: Modifier = Modifier,
    chapterSegments: List<VideoChapterSegment>,
    duration: Long,
) {
    if (duration <= 0L || chapterSegments.none { it.content.isNotBlank() }) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val minWidthFraction =
            if (containerWidth > 0.dp) {
                (MinChapterLabelWidth / containerWidth).coerceIn(0f, 1f)
            } else {
                DEFAULT_MIN_LABEL_WIDTH_FRACTION
            }
        // 播放过程中进度条 position 频繁变化，缓存标签布局避免重复分配
        val labelLayout =
            remember(chapterSegments, duration, minWidthFraction) {
                chapterLabelLayout(chapterSegments, duration, minWidthFraction)
            }
        if (labelLayout.none { it != null }) return@BoxWithConstraints

        val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        val labelStyle =
            MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(color = Color.Black, blurRadius = 1f),
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .height(ChapterLabelHeight),
        ) {
            labelLayout.forEach { label ->
                if (label == null) return@forEach
                val segmentWidth = containerWidth * label.widthFraction
                // 宽度不足以容纳「首字 + 省略号」时改用左对齐裁剪，避免只剩孤立省略号而看不到开头
                val clipOpening = segmentWidth < EllipsisLabelMinWidth
                Text(
                    text = label.content,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = containerWidth * label.leftFraction)
                            .width(segmentWidth),
                    color = labelColor,
                    textAlign = if (clipOpening) TextAlign.Start else TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    overflow = if (clipOpening) TextOverflow.Clip else TextOverflow.Ellipsis,
                    style = labelStyle,
                )
            }
        }
    }
}

/**
 * 计算章节分段在进度条上的绘制区间（像素）。
 *
 * 每段按时间比例映射到轨道宽度；时间上首尾相接的相邻分段之间在边界处各缩进
 * [gapPx] 形成可见缺口，时间上不连续的分段之间天然有间隙，不额外缩进。
 * 返回列表与 [segments] 一一对应（过窄的分段收成空区间，绘制时跳过）。
 *
 * @param segments 归一化后的章节分段（升序、不重叠、已裁剪进片长）
 * @param durationMs 视频总时长（毫秒）；<= 0 或分段为空时返回空列表
 * @param trackWidthPx 轨道总宽度（像素）
 * @param strokeWidthPx 分段描边宽度（像素），用于抵消 StrokeCap.Round 半圆帽的延伸
 * @param gapPx 相邻分段间可见缺口的宽度（像素）
 * @return 每段的 (startX, endX) 像素区间，与输入顺序一一对应
 */
internal fun computeChapterSegmentBounds(
    segments: List<VideoChapterSegment>,
    durationMs: Long,
    trackWidthPx: Float,
    strokeWidthPx: Float,
    gapPx: Float,
): List<Pair<Float, Float>> {
    if (durationMs <= 0L || segments.isEmpty() || trackWidthPx <= 0f) return emptyList()
    // StrokeCap.Round 的半圆帽会向端点外侧各延伸 strokeWidth/2，
    // 缺口需加上这部分宽度才能在相邻分段之间露出可见间隙
    val inset = strokeWidthPx / 2f + gapPx / 2f
    val bounds = ArrayList<Pair<Float, Float>>(segments.size)
    for ((index, segment) in segments.withIndex()) {
        val startX = segment.fromMs / durationMs.toFloat() * trackWidthPx
        val endX = segment.toMs / durationMs.toFloat() * trackWidthPx
        val previous = segments.getOrNull(index - 1)
        val next = segments.getOrNull(index + 1)
        val adjustedStart = if (previous != null && previous.toMs >= segment.fromMs) startX + inset else startX
        val adjustedEnd = if (next != null && next.fromMs <= segment.toMs) endX - inset else endX
        bounds +=
            if (adjustedEnd > adjustedStart) {
                adjustedStart to adjustedEnd
            } else {
                // 过窄的分段无法容纳缺口：收成空区间占位，保持与输入一一对应
                startX to startX
            }
    }
    return bounds
}

/**
 * 计算章节边界分隔缺口在轨道上的中心位置（相对轨道有效起点，像素）。
 *
 * 仅收集严格落在 `(0, durationMs)` 内部的章节起止点（连续分段共用同一边界时去重），
 * 避免在片头或片尾裁出缺口；相邻边界间距小于 [minSpacingPx] 时跳过过密边界，
 * 防止极短章节把整段抹空。
 *
 * @param segments 归一化后的章节分段（升序、不重叠、已裁剪进片长）
 * @param durationMs 视频总时长（毫秒）；<= 0 或分段为空时返回空列表
 * @param trackWidthPx 轨道有效宽度（像素）
 * @param minSpacingPx 相邻分隔缺口的最小中心间距（像素）
 * @return 升序的缺口中心 X 坐标列表
 */
internal fun computeChapterGapCenters(
    segments: List<VideoChapterSegment>,
    durationMs: Long,
    trackWidthPx: Float,
    minSpacingPx: Float,
): List<Float> {
    if (durationMs <= 0L || segments.isEmpty() || trackWidthPx <= 0f) return emptyList()
    val boundariesMs =
        buildList {
            for (segment in segments) {
                if (segment.fromMs > 0L) add(segment.fromMs)
                if (segment.toMs < durationMs) add(segment.toMs)
            }
        }.distinct()
    val centers = ArrayList<Float>(boundariesMs.size)
    for (ms in boundariesMs) {
        val x = ms / durationMs.toFloat() * trackWidthPx
        val prev = centers.lastOrNull()
        if (prev == null || x - prev >= minSpacingPx) {
            centers += x
        }
    }
    return centers
}

/** 章节名标签行高度；单行文本，留出足够空间避免被裁切。 */
private val ChapterLabelHeight = 18.dp

/** 显示章节标签的最小宽度（至少容下单个汉字开头，过窄时隐藏）。 */
private val MinChapterLabelWidth = 14.dp

/** 使用末尾省略号展示的最小宽度（不足时退化为左对齐裁剪以保留开头字符）。 */
private val EllipsisLabelMinWidth = 26.dp

/** [chapterLabelLayout] 默认最小宽度比例（约 2% 片长）。 */
private const val DEFAULT_MIN_LABEL_WIDTH_FRACTION = 0.02f

/** 章节名标签在进度条上的布局：水平起点与宽度均为轨道宽度比例（0-1）。 */
internal data class ChapterLabelLayout(
    val content: String,
    val leftFraction: Float,
    val widthFraction: Float,
)

/**
 * 计算章节名标签的布局比例，供交互模式在进度条上方按分段显示标签。
 *
 * 每个分段按时间比例映射到轨道水平区间 [leftFraction, leftFraction + widthFraction]；
 * 过窄（宽度比例 <= [minWidthFraction]）的分段返回 null 表示不显示标签，避免文字重叠。
 * 返回列表与 [segments] 一一对应，便于与分段高亮对齐。
 *
 * @param segments 归一化后的章节分段（升序、不重叠、已裁剪进片长）
 * @param durationMs 视频总时长（毫秒）；<= 0 时全部返回 null
 * @param minWidthFraction 标签显示的最小宽度比例，默认 [DEFAULT_MIN_LABEL_WIDTH_FRACTION]
 * @return 每段的标签布局；过窄或无效分段为 null，与输入顺序一一对应
 */
internal fun chapterLabelLayout(
    segments: List<VideoChapterSegment>,
    durationMs: Long,
    minWidthFraction: Float = DEFAULT_MIN_LABEL_WIDTH_FRACTION,
): List<ChapterLabelLayout?> {
    if (durationMs <= 0L) return List(segments.size) { null }
    return segments.map { segment ->
        val left = (segment.fromMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val right = (segment.toMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val width = right - left
        if (width <= minWidthFraction || segment.content.isBlank()) {
            null
        } else {
            ChapterLabelLayout(
                content = segment.content,
                leftFraction = left,
                widthFraction = width,
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

@Preview(showBackground = true)
@Composable
private fun VideoProgressSeekChaptersPreview() {
    val segments =
        listOf(
            VideoChapterSegment(0L, 20_000L, "开场"),
            VideoChapterSegment(20_000L, 45_000L, "正片"),
            VideoChapterSegment(45_000L, 100_000L, "片尾"),
        )
    BVTheme {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            VideoChapterLabels(
                chapterSegments = segments,
                duration = 100_000L,
            )
            VideoProgressSeek(
                duration = 100_000L,
                position = 35_000L,
                bufferedPercentage = 60,
                isPersistentSeek = false,
                chapterSegments = segments,
            )
        }
    }
}

// endregion
