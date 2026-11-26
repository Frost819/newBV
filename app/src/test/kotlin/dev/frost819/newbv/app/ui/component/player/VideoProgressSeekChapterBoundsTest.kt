package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.video.VideoChapterSegment
import org.junit.jupiter.api.Test

/**
 * [computeChapterSegmentBounds]、[computeChapterGapCenters] 与 [chapterLabelLayout] 的单元测试。
 *
 * 验证章节分段在进度条上的像素区间映射：时间比例换算、相邻分段缺口缩进
 * （抵消 StrokeCap.Round 半圆帽）、时间空隙保留、过窄分段占位不错位，
 * 边界分隔缺口中心计算，以及章节名标签的比例布局与过窄/空白过滤。
 */
class VideoProgressSeekChapterBoundsTest {
    private val strokeWidthPx = 8f
    private val gapPx = 2f

    @Test
    fun `returns empty for empty segments or non-positive duration or width`() {
        val segments = listOf(VideoChapterSegment(0L, 500L, "章节"))

        assertThat(computeChapterSegmentBounds(emptyList(), 1_000L, 1_000f, strokeWidthPx, gapPx)).isEmpty()
        assertThat(computeChapterSegmentBounds(segments, 0L, 1_000f, strokeWidthPx, gapPx)).isEmpty()
        assertThat(computeChapterSegmentBounds(segments, -1L, 1_000f, strokeWidthPx, gapPx)).isEmpty()
        assertThat(computeChapterSegmentBounds(segments, 1_000L, 0f, strokeWidthPx, gapPx)).isEmpty()
    }

    @Test
    fun `maps segment bounds proportionally to time`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 200_000L, "A"),
                VideoChapterSegment(200_000L, 800_000L, "B"),
            )
        // 时长 1s=1px：无相邻缩进的部分应精确按时间比例落位

        val bounds =
            computeChapterSegmentBounds(segments, 1_000_000L, 1_000f, strokeWidthPx = 0f, gapPx = 0f)

        assertThat(bounds).containsExactly(0f to 200f, 200f to 800f)
    }

    @Test
    fun `insets contiguous segments to expose visible gap`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 500L, "A"),
                VideoChapterSegment(500L, 1_000L, "B"),
            )
        // inset = strokeWidth/2 + gap/2 = 5px；可见缺口 = 2*inset - strokeWidth = 2px

        val bounds = computeChapterSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx, gapPx)

        assertThat(bounds[0].first).isEqualTo(0f)
        assertThat(bounds[0].second).isEqualTo(495f)
        assertThat(bounds[1].first).isEqualTo(505f)
        assertThat(bounds[1].second).isEqualTo(1_000f)
    }

    @Test
    fun `keeps time gaps between segments without extra inset`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 300L, "A"),
                VideoChapterSegment(600L, 1_000L, "B"),
            )

        val bounds = computeChapterSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx, gapPx)

        // 300-600ms 的空隙天然存在，不再缩进
        assertThat(bounds).containsExactly(0f to 300f, 600f to 1_000f)
    }

    @Test
    fun `collapses too narrow segment to placeholder keeping alignment`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 100L, "A"),
                VideoChapterSegment(100L, 105L, "过窄"),
                VideoChapterSegment(105L, 1_000L, "B"),
            )

        val bounds = computeChapterSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx, gapPx)

        // 返回与输入一一对应：过窄段收成空区间，绘制时跳过
        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].first).isEqualTo(bounds[1].second)
        assertThat(bounds[0].first).isEqualTo(0f)
        assertThat(bounds[2].second).isEqualTo(1_000f)
    }

    // ── computeChapterGapCenters ─────────────────────────────

    @Test
    fun `gap centers returns empty for invalid inputs or full-span single chapter`() {
        val fullSpan = listOf(VideoChapterSegment(0L, 1_000L, "全片"))

        assertThat(computeChapterGapCenters(emptyList(), 1_000L, 1_000f, 6f)).isEmpty()
        assertThat(computeChapterGapCenters(fullSpan, 0L, 1_000f, 6f)).isEmpty()
        assertThat(computeChapterGapCenters(fullSpan, 1_000L, 0f, 6f)).isEmpty()
        assertThat(computeChapterGapCenters(fullSpan, 1_000L, 1_000f, 6f)).isEmpty()
    }

    @Test
    fun `gap centers deduplicates contiguous boundaries and includes gap edges`() {
        val contiguous =
            listOf(
                VideoChapterSegment(0L, 200L, "A"),
                VideoChapterSegment(200L, 500L, "B"),
                VideoChapterSegment(500L, 1_000L, "C"),
            )
        val gapped =
            listOf(
                VideoChapterSegment(100L, 300L, "A"),
                VideoChapterSegment(600L, 900L, "B"),
            )

        assertThat(computeChapterGapCenters(contiguous, 1_000L, 1_000f, 6f))
            .containsExactly(200f, 500f)
            .inOrder()
        assertThat(computeChapterGapCenters(gapped, 1_000L, 1_000f, 6f))
            .containsExactly(100f, 300f, 600f, 900f)
            .inOrder()
    }

    @Test
    fun `gap centers skips overly dense boundaries`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 200L, "A"),
                VideoChapterSegment(200L, 203L, "极短"),
                VideoChapterSegment(203L, 1_000L, "B"),
            )

        assertThat(computeChapterGapCenters(segments, 1_000L, 1_000f, minSpacingPx = 6f))
            .containsExactly(200f)
    }

    // ── chapterLabelLayout ───────────────────────────────────

    @Test
    fun `label layout returns all null for non-positive duration`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 500L, "A"),
                VideoChapterSegment(500L, 1_000L, "B"),
            )

        assertThat(chapterLabelLayout(segments, 0L)).containsExactly(null, null).inOrder()
        assertThat(chapterLabelLayout(segments, -1L)).containsExactly(null, null).inOrder()
    }

    @Test
    fun `label layout maps segments to proportional left and width`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 200_000L, "开场"),
                VideoChapterSegment(200_000L, 500_000L, "正片"),
                VideoChapterSegment(500_000L, 1_000_000L, "片尾"),
            )

        val layout = chapterLabelLayout(segments, 1_000_000L, minWidthFraction = 0.05f)

        assertThat(layout).hasSize(3)
        assertThat(layout[0]).isEqualTo(ChapterLabelLayout("开场", 0f, 0.2f))
        assertThat(layout[1]).isEqualTo(ChapterLabelLayout("正片", 0.2f, 0.3f))
        assertThat(layout[2]).isEqualTo(ChapterLabelLayout("片尾", 0.5f, 0.5f))
    }

    @Test
    fun `label layout keeps moderately narrow segments and omits ultra narrow ones`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 965_000L, "正常"),
                VideoChapterSegment(965_000L, 995_000L, "较窄章节"),
                VideoChapterSegment(995_000L, 1_000_000L, "极窄"),
            )

        val layout = chapterLabelLayout(segments, 1_000_000L)

        // 返回与输入一一对应：3% 较窄段保留以便显示开头，0.5% 极窄段为 null
        assertThat(layout).hasSize(3)
        assertThat(layout[0]).isNotNull()
        assertThat(layout[1]?.content).isEqualTo("较窄章节")
        assertThat(layout[1]?.leftFraction).isWithin(1e-5f).of(0.965f)
        assertThat(layout[1]?.widthFraction).isWithin(1e-5f).of(0.03f)
        assertThat(layout[2]).isNull()
    }

    @Test
    fun `label layout skips blank content`() {
        val segments =
            listOf(
                VideoChapterSegment(0L, 500_000L, "  "),
                VideoChapterSegment(500_000L, 1_000_000L, "有效"),
            )

        val layout = chapterLabelLayout(segments, 1_000_000L)

        assertThat(layout[0]).isNull()
        assertThat(layout[1]?.content).isEqualTo("有效")
    }
}
