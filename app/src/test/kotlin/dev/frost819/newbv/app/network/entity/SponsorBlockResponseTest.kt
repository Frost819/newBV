package dev.frost819.newbv.app.network.entity

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * SponsorBlock 响应模型的 JSON 解析测试。
 *
 * 验证真实响应结构（大写 UUID 字段、秒为单位的浮点 segment、未知字段容忍）
 * 与毫秒换算。
 */
class SponsorBlockResponseTest {
    private val json =
        Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

    @Test
    fun `parses skipSegments response with uppercase UUID field`() {
        val payload =
            """
            [
              {
                "videoID": "BV1xx411c7mD",
                "segments": [
                  {
                    "segment": [15.5, 30.2],
                    "UUID": "abc-123",
                    "category": "sponsor",
                    "actionType": "skip",
                    "cid": "1051761130",
                    "locked": 0,
                    "votes": 3,
                    "videoDuration": 300.0,
                    "unknownField": "ignored"
                  }
                ]
              }
            ]
            """.trimIndent()

        val videos = json.decodeFromString<List<SponsorHashVideo>>(payload)

        assertThat(videos).hasSize(1)
        assertThat(videos[0].videoID).isEqualTo("BV1xx411c7mD")
        val segment = videos[0].segments[0]
        assertThat(segment.uuid).isEqualTo("abc-123")
        assertThat(segment.category).isEqualTo("sponsor")
        assertThat(segment.actionType).isEqualTo("skip")
        assertThat(segment.cid).isEqualTo("1051761130")
        assertThat(segment.votes).isEqualTo(3)
        assertThat(segment.locked).isEqualTo(0)
        assertThat(segment.startMs).isEqualTo(15_500L)
        assertThat(segment.endMs).isEqualTo(30_200L)
    }

    @Test
    fun `defaults missing fields tolerantly`() {
        val payload =
            """
            [
              {"videoID": "BV1xx", "segments": [{"segment": [1, 2]}]}
            ]
            """.trimIndent()

        val videos = json.decodeFromString<List<SponsorHashVideo>>(payload)

        val segment = videos[0].segments[0]
        assertThat(segment.uuid).isEmpty()
        assertThat(segment.actionType).isEqualTo("skip")
        assertThat(segment.cid).isEmpty()
        assertThat(segment.startMs).isEqualTo(1_000L)
        assertThat(segment.endMs).isEqualTo(2_000L)
    }

    @Test
    fun `handles integer seconds in segment array`() {
        val payload =
            """
            [
              {"videoID": "BV1xx", "segments": [{"segment": [10, 20], "UUID": "u1"}]}
            ]
            """.trimIndent()

        val videos = json.decodeFromString<List<SponsorHashVideo>>(payload)

        assertThat(videos[0].segments[0].startMs).isEqualTo(10_000L)
        assertThat(videos[0].segments[0].endMs).isEqualTo(20_000L)
    }
}
