package dev.frost819.newbv.bilisubtitle

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import dev.frost819.newbv.bilisubtitle.entity.Timestamp
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [SubtitleEncoder] 的单元测试。
 *
 * 验证 BCC 和 SRT 格式字幕的编码逻辑，包括字段映射、
 * 格式正确性、空列表处理和往返一致性。
 */
class SubtitleEncoderTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleItem(
        fromMs: Long,
        toMs: Long,
        content: String,
    ) = SubtitleItem(
        from = Timestamp.fromSrtString(formatSrtTimestamp(fromMs)),
        to = Timestamp.fromSrtString(formatSrtTimestamp(toMs)),
        content = content,
    )

    private fun formatSrtTimestamp(ms: Long): String {
        val hours = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val millis = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    @Test
    fun `encode to bcc produces valid json`() {
        val items = listOf(sampleItem(0, 1000, "Hello"))

        val result = SubtitleEncoder.encodeToBcc(items)

        assertThat(result).contains("\"body\"")
        assertThat(result).contains("\"content\":\"Hello\"")
    }

    @Test
    fun `encode to bcc maps content correctly`() {
        val items = listOf(sampleItem(0, 1000, "Test content"))

        val result = SubtitleEncoder.encodeToBcc(items)
        val decoded = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(result)
        val body = decoded["body"]
        assertThat(body).isNotNull()
    }

    @Test
    fun `encode to bcc with multiple items`() {
        val items =
            listOf(
                sampleItem(0, 1000, "First"),
                sampleItem(1000, 2000, "Second"),
                sampleItem(2000, 3000, "Third"),
            )

        val result = SubtitleEncoder.encodeToBcc(items)

        assertThat(result).contains("\"First\"")
        assertThat(result).contains("\"Second\"")
        assertThat(result).contains("\"Third\"")
    }

    @Test
    fun `encode to bcc with empty list produces valid json`() {
        val result = SubtitleEncoder.encodeToBcc(emptyList())

        // body field may be omitted when empty (kotlinx.serialization default)
        assertThat(result).contains("\"font_size\"")
        assertThat(result).contains("\"stroke\":\"none\"")
    }

    @Test
    fun `encode to bcc sets default font properties`() {
        val items = listOf(sampleItem(0, 1000, "X"))

        val result = SubtitleEncoder.encodeToBcc(items)

        assertThat(result).contains("\"font_size\":0.4")
        assertThat(result).contains("\"font_color\":\"#FFFFFF\"")
        assertThat(result).contains("\"background_alpha\":0.5")
        assertThat(result).contains("\"background_color\":\"#9C27B0\"")
        assertThat(result).contains("\"stroke\":\"none\"")
    }

    @Test
    fun `encode to bcc maps timestamps to seconds`() {
        val items = listOf(sampleItem(1500, 3500, "X"))

        val result = SubtitleEncoder.encodeToBcc(items)

        assertThat(result).contains("\"from\":1.5")
        assertThat(result).contains("\"to\":3.5")
    }

    @Test
    fun `encode to srt produces correct format`() {
        val items = listOf(sampleItem(0, 1000, "Hello"))

        val result = SubtitleEncoder.encodeToSrt(items)

        assertThat(result).contains("00:00:00,000")
        assertThat(result).contains("00:00:01,000")
        assertThat(result).contains("Hello")
    }

    @Test
    fun `encode to srt with index numbering`() {
        val items =
            listOf(
                sampleItem(0, 1000, "First"),
                sampleItem(1000, 2000, "Second"),
            )

        val result = SubtitleEncoder.encodeToSrt(items)

        val lines = result.lines()
        assertThat(lines[0]).isEqualTo("1")
        assertThat(result).contains("2")
    }

    @Test
    fun `encode to srt with empty list returns empty string`() {
        val result = SubtitleEncoder.encodeToSrt(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun `encode to srt replaces newlines with backslash n`() {
        val item =
            SubtitleItem(
                from = Timestamp.fromSrtString("00:00:00,000"),
                to = Timestamp.fromSrtString("00:00:01,000"),
                content = "Line1\nLine2",
            )

        val result = SubtitleEncoder.encodeToSrt(listOf(item))

        assertThat(result).contains("Line1\\nLine2")
    }

    @Test
    fun `round-trip srt to bcc to srt preserves content`() {
        val srt =
            """
            1
            00:00:00,500 --> 00:00:02,000
            Hello

            2
            00:00:02,500 --> 00:00:04,000
            World
            """.trimIndent()

        val items = SubtitleParser.fromSrtString(srt)
        val bccOutput = SubtitleEncoder.encodeToBcc(items)
        val reparsed = SubtitleParser.fromBccString(bccOutput)

        assertThat(reparsed).hasSize(2)
        assertThat(reparsed[0].content).isEqualTo("Hello")
        assertThat(reparsed[1].content).isEqualTo("World")
    }

    @Test
    fun `encode to bcc sets location to 2`() {
        val items = listOf(sampleItem(0, 1000, "X"))

        val result = SubtitleEncoder.encodeToBcc(items)

        assertThat(result).contains("\"location\":2")
    }

    @Test
    fun `encode to srt with multiple items separates by newline`() {
        val items =
            listOf(
                sampleItem(0, 1000, "First"),
                sampleItem(1000, 2000, "Second"),
            )

        val result = SubtitleEncoder.encodeToSrt(items)

        assertThat(result).contains("First")
        assertThat(result).contains("Second")
        val parts = result.split("\n\n")
        assertThat(parts).hasSize(2)
    }
}
