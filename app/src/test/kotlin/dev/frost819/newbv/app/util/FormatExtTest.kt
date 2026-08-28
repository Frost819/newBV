package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [FormatExt] 扩展函数的单元测试。
 *
 * 验证播放数格式化、时间格式化、HTML 标签去除。
 */
class FormatExtTest {
    @Test
    fun `toWanString returns empty for null`() {
        assertThat((null as Int?).toWanString()).isEmpty()
    }

    @Test
    fun `toWanString returns empty for -1`() {
        assertThat((-1).toWanString()).isEmpty()
    }

    @Test
    fun `toWanString returns raw number when less than 10000`() {
        assertThat(9999.toWanString()).isEqualTo("9999")
        assertThat(0.toWanString()).isEqualTo("0")
    }

    @Test
    fun `toWanString returns wan format for 10000 and above`() {
        assertThat(10000.toWanString()).isEqualTo("1.0万")
        assertThat(15000.toWanString()).isEqualTo("1.5万")
        assertThat(100000.toWanString()).isEqualTo("10.0万")
    }

    @Test
    fun `formatHourMinSec with zero milliseconds`() {
        assertThat(0L.formatHourMinSec()).isEqualTo("00:00")
    }

    @Test
    fun `formatHourMinSec with seconds only`() {
        assertThat(5000L.formatHourMinSec()).isEqualTo("00:05")
    }

    @Test
    fun `formatHourMinSec with minutes and seconds`() {
        assertThat((65_000L).formatHourMinSec()).isEqualTo("01:05")
    }

    @Test
    fun `formatHourMinSec with hours minutes and seconds`() {
        assertThat((3_661_000L).formatHourMinSec()).isEqualTo("01:01:01")
    }

    @Test
    fun `formatHourMinSec with negative returns dots`() {
        assertThat((-1L).formatHourMinSec()).isEqualTo("...")
    }

    @Test
    fun `Int formatHourMinSec delegates to Long version`() {
        assertThat(65.formatHourMinSec()).isEqualTo("01:05")
    }
}
