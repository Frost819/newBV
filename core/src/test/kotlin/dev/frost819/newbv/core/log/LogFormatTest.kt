package dev.frost819.newbv.core.log

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [LogFormat] 的单元测试。
 *
 * 验证交互日志条目格式、日志头部格式、崩溃头部格式。
 */
class LogFormatTest {

    @Test
    fun `interactionEntry formats correctly with detail`() {
        val entry = LogFormat.interactionEntry(
            timestamp = 1700000000000L,
            level = LogLevel.INFO,
            category = "NAV",
            action = "navigate",
            detail = mapOf("from" to "home", "to" to "detail")
        )
        assertThat(entry).contains("[INFO]")
        assertThat(entry).contains("[NAV]")
        assertThat(entry).contains("navigate")
        assertThat(entry).contains("from=home")
        assertThat(entry).contains("to=detail")
    }

    @Test
    fun `interactionEntry formats correctly without detail`() {
        val entry = LogFormat.interactionEntry(
            timestamp = 1700000000000L,
            level = LogLevel.WARN,
            category = "PLAY",
            action = "pause"
        )
        assertThat(entry).contains("[WARN]")
        assertThat(entry).contains("[PLAY]")
        assertThat(entry).contains("pause")
        assertThat(entry).endsWith("pause")
    }

    @Test
    fun `interactionEntry includes timestamp`() {
        val entry = LogFormat.interactionEntry(
            timestamp = 1700000000000L,
            level = LogLevel.DEBUG,
            category = "CARD",
            action = "click"
        )
        assertThat(entry).contains("2023")  // 1700000000000ms = 2023-11-14
    }

    @Test
    fun `header contains all device info fields`() {
        val header = LogFormat.header(
            appVersion = "1.0.0",
            appVersionCode = 100,
            androidVersion = "14",
            androidSdk = 34,
            device = "Pixel",
            model = "Pixel 8",
            manufacturer = "Google"
        )
        assertThat(header).contains("new BV Log")
        assertThat(header).contains("App Version: 1.0.0 (100)")
        assertThat(header).contains("Android Version: 14 (34)")
        assertThat(header).contains("Device: Pixel")
        assertThat(header).contains("Model: Pixel 8")
        assertThat(header).contains("Manufacturer: Google")
    }

    @Test
    fun `crashHeader contains recent interaction count`() {
        val header = LogFormat.crashHeader(
            appVersion = "1.0.0",
            appVersionCode = 100,
            androidVersion = "14",
            androidSdk = 34,
            device = "Pixel",
            model = "Pixel 8",
            manufacturer = "Google",
            recentInteractionCount = 42
        )
        assertThat(header).contains("new BV Crash")
        assertThat(header).contains("Recent interaction logs (42 entries):")
    }

    @Test
    fun `LogLevel fromString returns correct level`() {
        assertThat(LogLevel.fromString("DEBUG")).isEqualTo(LogLevel.DEBUG)
        assertThat(LogLevel.fromString("INFO")).isEqualTo(LogLevel.INFO)
        assertThat(LogLevel.fromString("WARN")).isEqualTo(LogLevel.WARN)
        assertThat(LogLevel.fromString("ERROR")).isEqualTo(LogLevel.ERROR)
    }

    @Test
    fun `LogLevel fromString returns INFO for invalid string`() {
        assertThat(LogLevel.fromString("INVALID")).isEqualTo(LogLevel.INFO)
        assertThat(LogLevel.fromString("")).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `LogCategory fromString returns correct category`() {
        assertThat(LogCategory.fromString("NAV")).isEqualTo(LogCategory.NAV)
        assertThat(LogCategory.fromString("PLAY")).isEqualTo(LogCategory.PLAY)
    }

    @Test
    fun `LogCategory fromString returns NAV for invalid string`() {
        assertThat(LogCategory.fromString("INVALID")).isEqualTo(LogCategory.NAV)
    }
}
