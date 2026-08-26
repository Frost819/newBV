package dev.frost819.newbv.biliapi.http.entity.video

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [OnlineTotal] 的单元测试。
 *
 * 验证 JSON 解析与展示文案选择逻辑（show_switch 三分支）。
 */
class OnlineTotalTest {
    private val json =
        Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

    @Test
    fun `parses online total response from docs`() {
        val raw =
            """{"total":"9.4万+","count":"50953","show_switch":{"total":true,"count":true}}"""

        val data = json.decodeFromString<OnlineTotal>(raw)

        assertThat(data.total).isEqualTo("9.4万+")
        assertThat(data.count).isEqualTo("50953")
        assertThat(data.showSwitch.total).isTrue()
        assertThat(data.showSwitch.count).isTrue()
    }

    @Test
    fun `displayText prefers total when enabled`() {
        val data =
            OnlineTotal(
                total = "9.4万+",
                count = "50953",
                showSwitch = OnlineTotal.ShowSwitch(total = true, count = true),
            )

        assertThat(data.displayText()).isEqualTo("9.4万+")
    }

    @Test
    fun `displayText falls back to count when total disabled`() {
        val data =
            OnlineTotal(
                total = "9.4万+",
                count = "50953",
                showSwitch = OnlineTotal.ShowSwitch(total = false, count = true),
            )

        assertThat(data.displayText()).isEqualTo("50953")
    }

    @Test
    fun `displayText falls back to count when total blank`() {
        val data =
            OnlineTotal(
                total = "",
                count = "50953",
                showSwitch = OnlineTotal.ShowSwitch(total = true, count = true),
            )

        assertThat(data.displayText()).isEqualTo("50953")
    }

    @Test
    fun `displayText returns null when both switches disabled`() {
        val data =
            OnlineTotal(
                total = "9.4万+",
                count = "50953",
                showSwitch = OnlineTotal.ShowSwitch(total = false, count = false),
            )

        assertThat(data.displayText()).isNull()
    }

    @Test
    fun `displayText returns null when texts blank regardless of switches`() {
        val data =
            OnlineTotal(
                total = "",
                count = "",
                showSwitch = OnlineTotal.ShowSwitch(total = true, count = true),
            )

        assertThat(data.displayText()).isNull()
    }

    @Test
    fun `parses response with unknown fields`() {
        val raw =
            """{"total":"3","count":"2","show_switch":{"total":false,"count":false},"extra_field":"ignored"}"""

        val data = json.decodeFromString<OnlineTotal>(raw)

        assertThat(data.total).isEqualTo("3")
        assertThat(data.displayText()).isNull()
    }

    @Test
    fun `missing fields default to empty and switches default to true`() {
        val data = json.decodeFromString<OnlineTotal>("{}")

        assertThat(data.total).isEmpty()
        assertThat(data.count).isEmpty()
        assertThat(data.showSwitch.total).isTrue()
        assertThat(data.showSwitch.count).isTrue()
    }

    @Test
    fun `parses app online total response from docs`() {
        val raw = """{"online":{"total_text":"8.8万+人在看"}}"""

        val data = json.decodeFromString<OnlineTotalApp>(raw)

        assertThat(data.online.totalText).isEqualTo("8.8万+人在看")
    }

    @Test
    fun `app online total defaults when online missing`() {
        val data = json.decodeFromString<OnlineTotalApp>("{}")

        assertThat(data.online.totalText).isEmpty()
    }
}
