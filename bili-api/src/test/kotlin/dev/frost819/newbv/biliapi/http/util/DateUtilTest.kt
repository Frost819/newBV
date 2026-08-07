package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.TimeZone

/**
 * [DateUtil] 扩展函数的单元测试。
 *
 * 验证智能日期格式化：秒/毫秒时间戳自动识别、当年/跨年格式。
 */
class DateUtilTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `toSmartDate returns null for zero timestamp`() {
        assertThat(0L.toSmartDate(utc)).isNull()
    }

    @Test
    fun `toSmartDate returns null for negative timestamp`() {
        assertThat((-1L).toSmartDate(utc)).isNull()
    }

    @Test
    fun `toSmartDate handles seconds-level timestamp`() {
        // 2023-06-15 00:00:00 UTC = 1686787200 (seconds)
        val ts = 1686787200L
        val result = ts.toSmartDate(utc)
        assertThat(result).isNotNull()
        // Should contain month and day
        assertThat(result).contains("6月")
        assertThat(result).contains("15日")
    }

    @Test
    fun `toSmartDate handles milliseconds-level timestamp`() {
        // Same date in milliseconds
        val ts = 1686787200000L
        val result = ts.toSmartDate(utc)
        assertThat(result).isNotNull()
        assertThat(result).contains("6月")
        assertThat(result).contains("15日")
    }

    @Test
    fun `toSmartDate includes year for non-current-year dates`() {
        // 2000-01-01 00:00:00 UTC = 946684800 (seconds)
        val ts = 946684800L
        val result = ts.toSmartDate(utc)
        assertThat(result).contains("2000年")
    }

    @Test
    fun `smartDate extension on Int delegates to toSmartDate`() {
        // Test that the Int extension works
        val result: String? = 1686787200.smartDate
        // This uses system default timezone, just verify it's not null for valid ts
        assertThat(result).isNotNull()
    }
}
