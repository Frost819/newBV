package dev.frost819.newbv.biliapi.http.entity.danmaku

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [DanmakuFilterRule] 的 JSON 反序列化测试。
 *
 * 对应端点：GET /x/dm/filter/user（真机实测为普通 JSON，非 WBI 签名且无需参数）。
 * type 语义：0=关键词 1=正则 2=用户。
 */
class DanmakuFilterRuleTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes keyword and user rules`() {
        val payload =
            """
            {"rule":[
            {"id":1,"mid":12345,"type":0,"filter":"广告","comment":"关键词","opened":true},
            {"id":2,"mid":12345,"type":2,"filter":"48291043","comment":"用户","opened":false}
            ]}
            """.trimIndent()

        val data = json.decodeFromString<DanmakuFilterRuleData>(payload)

        assertThat(data.rule).hasSize(2)
        assertThat(data.rule[0].type).isEqualTo(0)
        assertThat(data.rule[0].filter).isEqualTo("广告")
        assertThat(data.rule[0].opened).isTrue()
        assertThat(data.rule[1].type).isEqualTo(2)
        assertThat(data.rule[1].filter).isEqualTo("48291043")
        assertThat(data.rule[1].mid).isEqualTo(12345L)
        assertThat(data.rule[1].comment).isEqualTo("用户")
        assertThat(data.rule[1].opened).isFalse()
    }

    @Test
    fun `missing fields fall back to defaults`() {
        val data = json.decodeFromString<DanmakuFilterRuleData>("""{"rule":[{"filter":"x"}]}""")

        val rule = data.rule.single()
        assertThat(rule.id).isEqualTo(0L)
        assertThat(rule.mid).isEqualTo(0L)
        assertThat(rule.type).isEqualTo(0)
        assertThat(rule.comment).isEmpty()
        assertThat(rule.opened).isTrue()
    }

    @Test
    fun `empty payload yields empty rule list`() {
        val data = json.decodeFromString<DanmakuFilterRuleData>("{}")

        assertThat(data.rule).isEmpty()
    }
}
