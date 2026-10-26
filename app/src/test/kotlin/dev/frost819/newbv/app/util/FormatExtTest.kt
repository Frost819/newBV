package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Calendar

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

    // ── formatCommentTime ───────────────────────────────────────

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month - 1, day, hour, minute, 0)
        return calendar.timeInMillis
    }

    @Test
    fun `formatCommentTime returns empty for invalid timestamp`() {
        assertThat(0L.formatCommentTime(now = timestamp(2026, 6, 15))).isEmpty()
        assertThat((-1L).formatCommentTime(now = timestamp(2026, 6, 15))).isEmpty()
    }

    @Test
    fun `formatCommentTime returns just now within one minute`() {
        val now = timestamp(2026, 6, 15)
        assertThat((now - 59_000L).formatCommentTime(now = now)).isEqualTo("刚刚")
        // 时间戳在未来时同样按「刚刚」处理
        assertThat((now + 60_000L).formatCommentTime(now = now)).isEqualTo("刚刚")
    }

    @Test
    fun `formatCommentTime returns minutes within one hour`() {
        val now = timestamp(2026, 6, 15)
        assertThat((now - 5 * 60_000L).formatCommentTime(now = now)).isEqualTo("5分钟前")
        assertThat((now - 59 * 60_000L).formatCommentTime(now = now)).isEqualTo("59分钟前")
    }

    @Test
    fun `formatCommentTime returns hours within one day`() {
        val now = timestamp(2026, 6, 15)
        assertThat((now - 3 * 3_600_000L).formatCommentTime(now = now)).isEqualTo("3小时前")
        assertThat((now - 23 * 3_600_000L).formatCommentTime(now = now)).isEqualTo("23小时前")
    }

    @Test
    fun `formatCommentTime returns yesterday within two days`() {
        val now = timestamp(2026, 6, 15)
        assertThat((now - 25 * 3_600_000L).formatCommentTime(now = now)).isEqualTo("昨天")
        assertThat((now - 47 * 3_600_000L).formatCommentTime(now = now)).isEqualTo("昨天")
    }

    @Test
    fun `formatCommentTime returns date for same year and older`() {
        val now = timestamp(2026, 6, 15)
        // 超过两天：同年省略年份
        assertThat(timestamp(2026, 3, 2).formatCommentTime(now = now)).isEqualTo("3月2日")
        // 往年带年份
        assertThat(timestamp(2025, 12, 31).formatCommentTime(now = now)).isEqualTo("2025年12月31日")
    }

    @Test
    fun `formatCommentTime accepts second level timestamp`() {
        val now = timestamp(2026, 6, 15)
        val fiveMinutesAgoSeconds = (now - 5 * 60_000L) / 1000L
        assertThat(fiveMinutesAgoSeconds.formatCommentTime(now = now)).isEqualTo("5分钟前")
    }
}
