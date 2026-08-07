package dev.frost819.newbv.bilisubtitle.entity

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [BiliSubtitle] 和 [BiliSubtitleItem] 的单元测试。
 *
 * 验证 JSON 反序列化、@JsonNames 别名兼容、默认值及序列化往返。
 */
class BiliSubtitleTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    // ---- BiliSubtitleItem deserialization ----

    @Test
    fun `deserialize BiliSubtitleItem with required fields only`() {
        val jsonString = """{"from":0.0,"to":2.5,"content":"hello"}"""
        val item = json.decodeFromString<BiliSubtitleItem>(jsonString)

        assertThat(item.from).isEqualTo(0.0f)
        assertThat(item.to).isEqualTo(2.5f)
        assertThat(item.content).isEqualTo("hello")
        assertThat(item.sid).isNull()
        assertThat(item.location).isNull()
        assertThat(item.music).isNull()
        assertThat(item.version).isNull()
    }

    @Test
    fun `deserialize BiliSubtitleItem with all fields`() {
        val jsonString = """{"from":1.0,"to":3.0,"sid":42,"location":2,"content":"test","music":0.5,"version":"ai"}"""
        val item = json.decodeFromString<BiliSubtitleItem>(jsonString)

        assertThat(item.from).isEqualTo(1.0f)
        assertThat(item.to).isEqualTo(3.0f)
        assertThat(item.sid).isEqualTo(42)
        assertThat(item.location).isEqualTo(2)
        assertThat(item.content).isEqualTo("test")
        assertThat(item.music).isEqualTo(0.5f)
        assertThat(item.version).isEqualTo("ai")
    }

    @Test
    fun `serialize BiliSubtitleItem preserves required fields`() {
        val item = BiliSubtitleItem(from = 1.0f, to = 2.0f, content = "hello")
        val jsonString = json.encodeToString(item)

        assertThat(jsonString).contains("\"from\":1.0")
        assertThat(jsonString).contains("\"to\":2.0")
        assertThat(jsonString).contains("\"hello\"")
    }

    // ---- BiliSubtitle deserialization with defaults ----

    @Test
    fun `deserialize BiliSubtitle with empty body uses default empty list`() {
        val jsonString = """{"font_size":0.4}"""
        val subtitle = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(subtitle.fontSize).isEqualTo(0.4f)
        assertThat(subtitle.body).isEmpty()
    }

    @Test
    fun `deserialize BiliSubtitle with full content`() {
        val jsonString =
            """
            {
                "font_size": 0.4,
                "font_color": "#FFFFFF",
                "background_alpha": 0.5,
                "background_color": "#9C27B0",
                "stroke": "none",
                "type": "web",
                "lang": "zh-CN",
                "version": "1.0",
                "body": [
                    {"from": 0.0, "to": 2.0, "content": "first"},
                    {"from": 2.0, "to": 4.0, "content": "second"}
                ]
            }
            """.trimIndent()
        val subtitle = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(subtitle.fontSize).isEqualTo(0.4f)
        assertThat(subtitle.fontColor).isEqualTo("#FFFFFF")
        assertThat(subtitle.backgroundAlpha).isEqualTo(0.5f)
        assertThat(subtitle.backgroundColor).isEqualTo("#9C27B0")
        assertThat(subtitle.stroke).isEqualTo("none")
        assertThat(subtitle.type).isEqualTo("web")
        assertThat(subtitle.lang).isEqualTo("zh-CN")
        assertThat(subtitle.version).isEqualTo("1.0")
        assertThat(subtitle.body).hasSize(2)
        assertThat(subtitle.body[0].content).isEqualTo("first")
        assertThat(subtitle.body[1].content).isEqualTo("second")
    }

    // ---- @JsonNames("Stroke") alternate name ----

    @Test
    fun `deserialize BiliSubtitle with lowercase stroke field`() {
        val jsonString = """{"stroke":"some_value","body":[]}"""
        val subtitle = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(subtitle.stroke).isEqualTo("some_value")
    }

    @Test
    fun `deserialize BiliSubtitle with uppercase Stroke alias from AI subtitles`() {
        val jsonString = """{"Stroke":"ai_value","body":[]}"""
        val subtitle = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(subtitle.stroke).isEqualTo("ai_value")
    }

    // ---- Serialization roundtrip ----

    @Test
    fun `serialize then deserialize BiliSubtitle preserves data`() {
        val original =
            BiliSubtitle(
                fontSize = 0.4f,
                fontColor = "#FFFFFF",
                backgroundAlpha = 0.5f,
                backgroundColor = "#9C27B0",
                stroke = "none",
                body =
                    listOf(
                        BiliSubtitleItem(from = 0.0f, to = 2.0f, content = "hello"),
                    ),
            )
        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(deserialized.fontSize).isEqualTo(0.4f)
        assertThat(deserialized.fontColor).isEqualTo("#FFFFFF")
        assertThat(deserialized.stroke).isEqualTo("none")
        assertThat(deserialized.body).hasSize(1)
        assertThat(deserialized.body[0].content).isEqualTo("hello")
        assertThat(deserialized.body[0].from).isEqualTo(0.0f)
        assertThat(deserialized.body[0].to).isEqualTo(2.0f)
    }

    @Test
    fun `serialized output uses lowercase stroke not uppercase Stroke`() {
        val subtitle = BiliSubtitle(stroke = "test")
        val jsonString = json.encodeToString(subtitle)

        assertThat(jsonString).contains("\"stroke\"")
        assertThat(jsonString).doesNotContain("\"Stroke\"")
    }

    // ---- Null defaults ----

    @Test
    fun `BiliSubtitle defaults to null for optional fields`() {
        val subtitle = BiliSubtitle()

        assertThat(subtitle.fontSize).isNull()
        assertThat(subtitle.fontColor).isNull()
        assertThat(subtitle.backgroundAlpha).isNull()
        assertThat(subtitle.backgroundColor).isNull()
        assertThat(subtitle.stroke).isNull()
        assertThat(subtitle.type).isNull()
        assertThat(subtitle.lang).isNull()
        assertThat(subtitle.version).isNull()
        assertThat(subtitle.body).isEmpty()
    }

    @Test
    fun `BiliSubtitleItem defaults to null for optional fields`() {
        val item = BiliSubtitleItem(from = 0.0f, to = 1.0f, content = "x")

        assertThat(item.sid).isNull()
        assertThat(item.location).isNull()
        assertThat(item.music).isNull()
        assertThat(item.version).isNull()
    }

    // ---- Unknown keys are ignored ----

    @Test
    fun `deserialize BiliSubtitle ignores unknown keys`() {
        val jsonString = """{"unknown_field":"value","body":[]}"""
        val subtitle = json.decodeFromString<BiliSubtitle>(jsonString)

        assertThat(subtitle.body).isEmpty()
    }
}
