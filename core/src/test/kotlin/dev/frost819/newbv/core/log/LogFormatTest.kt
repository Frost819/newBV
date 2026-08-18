package dev.frost819.newbv.core.log

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [LogFormat] 的单元测试。
 *
 * 验证日志头部格式和崩溃头部格式。
 */
class LogFormatTest {

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
    fun `crashHeader describes included logcat output`() {
        val header = LogFormat.crashHeader(
            appVersion = "1.0.0",
            appVersionCode = 100,
            androidVersion = "14",
            androidSdk = 34,
            device = "Pixel",
            model = "Pixel 8",
            manufacturer = "Google"
        )
        assertThat(header).contains("new BV Crash")
        assertThat(header).contains("Recent application logs are included below from Logcat:")
    }
}
