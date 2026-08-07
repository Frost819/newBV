package dev.frost819.newbv.bilisubtitle.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SrtSubtitleItem] 和 [SrtSubtitle] 的单元测试。
 *
 * 验证 [toRaw] 输出格式及序列化默认值。
 */
class SrtSubtitleTest {
    // ---- SrtSubtitleItem.toRaw ----

    @Test
    fun `toRaw produces correct SRT block format`() {
        val item =
            SrtSubtitleItem(
                index = 1,
                from = "00:00:01,000",
                to = "00:00:03,000",
                content = "Hello world",
            )

        val raw = item.toRaw()

        assertThat(raw).contains("1")
        assertThat(raw).contains("00:00:01,000 --> 00:00:03,000")
        assertThat(raw).contains("Hello world")
    }

    @Test
    fun `toRaw contains index on first line`() {
        val item =
            SrtSubtitleItem(
                index = 42,
                from = "00:00:00,000",
                to = "00:00:01,000",
                content = "test",
            )

        val lines = item.toRaw().lines()
        assertThat(lines[0]).isEqualTo("42")
    }

    @Test
    fun `toRaw contains time range on second line`() {
        val item =
            SrtSubtitleItem(
                index = 1,
                from = "00:01:00,000",
                to = "00:02:00,000",
                content = "test",
            )

        val lines = item.toRaw().lines()
        assertThat(lines[1]).isEqualTo("00:01:00,000 --> 00:02:00,000")
    }

    @Test
    fun `toRaw contains content on third line`() {
        val item =
            SrtSubtitleItem(
                index = 1,
                from = "00:00:00,000",
                to = "00:00:01,000",
                content = "subtitle text here",
            )

        val lines = item.toRaw().lines()
        assertThat(lines[2]).isEqualTo("subtitle text here")
    }

    @Test
    fun `toRaw with multi-line content preserves content as-is`() {
        val item =
            SrtSubtitleItem(
                index = 1,
                from = "00:00:00,000",
                to = "00:00:01,000",
                content = "line1\\nline2",
            )

        val raw = item.toRaw()
        assertThat(raw).contains("line1\\nline2")
    }

    @Test
    fun `toRaw with large index`() {
        val item =
            SrtSubtitleItem(
                index = 9999,
                from = "00:00:00,000",
                to = "00:00:01,000",
                content = "x",
            )

        assertThat(item.toRaw()).contains("9999")
    }

    // ---- SrtSubtitle defaults ----

    @Test
    fun `SrtSubtitle defaults to empty content list`() {
        val subtitle = SrtSubtitle()
        assertThat(subtitle.content).isEmpty()
    }

    @Test
    fun `SrtSubtitle retains content items`() {
        val items =
            listOf(
                SrtSubtitleItem(index = 1, from = "00:00:00,000", to = "00:00:01,000", content = "a"),
                SrtSubtitleItem(index = 2, from = "00:00:01,000", to = "00:00:02,000", content = "b"),
            )
        val subtitle = SrtSubtitle(content = items)

        assertThat(subtitle.content).hasSize(2)
        assertThat(subtitle.content[0].content).isEqualTo("a")
        assertThat(subtitle.content[1].content).isEqualTo("b")
    }

    // ---- Data class equality ----

    @Test
    fun `SrtSubtitleItem equality`() {
        val a = SrtSubtitleItem(index = 1, from = "00:00:00,000", to = "00:00:01,000", content = "a")
        val b = SrtSubtitleItem(index = 1, from = "00:00:00,000", to = "00:00:01,000", content = "a")

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `SrtSubtitleItem inequality with different content`() {
        val a = SrtSubtitleItem(index = 1, from = "00:00:00,000", to = "00:00:01,000", content = "a")
        val b = SrtSubtitleItem(index = 1, from = "00:00:00,000", to = "00:00:01,000", content = "b")

        assertThat(a).isNotEqualTo(b)
    }
}
