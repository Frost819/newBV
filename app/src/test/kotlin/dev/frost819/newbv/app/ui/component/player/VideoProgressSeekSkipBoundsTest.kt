package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.entity.player.SponsorCategories
import dev.frost819.newbv.app.entity.player.SponsorSegment
import org.junit.jupiter.api.Test

/**
 * [computeSkipSegmentBounds] 的单元测试。
 *
 * 验证空降片段在进度条上的像素区间映射：时间比例换算、相邻连续片段缩进
 * （抵消 StrokeCap.Round 半圆帽并留出可见间隙）、非连续片段保留自然空隙与过窄占位。
 */
class VideoProgressSeekSkipBoundsTest {
    private fun segment(
        startMs: Long,
        endMs: Long,
        category: String = SponsorCategories.SPONSOR,
    ) = SponsorSegment(
        uuid = "$startMs-$endMs",
        category = category,
        actionType = SponsorActionType.SKIP,
        startMs = startMs,
        endMs = endMs,
    )

    @Test
    fun `returns empty for empty segments or non-positive duration or width`() {
        val segments = listOf(segment(0L, 500L))

        assertThat(computeSkipSegmentBounds(emptyList(), 1_000L, 1_000f, 8f, 2f)).isEmpty()
        assertThat(computeSkipSegmentBounds(segments, 0L, 1_000f, 8f, 2f)).isEmpty()
        assertThat(computeSkipSegmentBounds(segments, -1L, 1_000f, 8f, 2f)).isEmpty()
        assertThat(computeSkipSegmentBounds(segments, 1_000L, 0f, 8f, 2f)).isEmpty()
    }

    @Test
    fun `insets contiguous segments to expose visible gap`() {
        val segments =
            listOf(
                segment(0L, 500L, SponsorCategories.SPONSOR),
                segment(500L, 1_000L, SponsorCategories.OUTRO),
            )
        // inset = strokeWidth/2 + gap/2 = 5px；可见缺口 = 2*inset - strokeWidth = 2px
        val bounds = computeSkipSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx = 8f, gapPx = 2f)

        assertThat(bounds).containsExactly(0f to 495f, 505f to 1_000f).inOrder()
    }

    @Test
    fun `keeps non-contiguous segments un-inset`() {
        val segments =
            listOf(
                segment(100L, 300L),
                segment(600L, 900L),
            )

        val bounds = computeSkipSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx = 8f, gapPx = 2f)

        assertThat(bounds).containsExactly(100f to 300f, 600f to 900f).inOrder()
    }

    @Test
    fun `collapses too narrow adjacent segment to empty placeholder`() {
        val segments =
            listOf(
                segment(0L, 100L),
                segment(100L, 105L),
                segment(105L, 1_000L),
            )

        val bounds = computeSkipSegmentBounds(segments, 1_000L, 1_000f, strokeWidthPx = 8f, gapPx = 2f)

        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].first).isEqualTo(bounds[1].second)
    }
}
