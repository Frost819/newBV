package dev.frost819.newbv.bilisubtitle.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Timestamp] 的单元测试。
 *
 * 验证 SRT/BCC 格式解析、时间转换、totalMills 计算及往返一致性。
 */
class TimestampTest {
    // ---- fromSrtString ----

    @Test
    fun `fromSrtString parses standard SRT timestamp`() {
        val ts = Timestamp.fromSrtString("01:03:17,775")

        assertThat(ts.hours).isEqualTo(1)
        assertThat(ts.minutes).isEqualTo(3)
        assertThat(ts.seconds).isEqualTo(17)
        assertThat(ts.milliSeconds).isEqualTo(775)
    }

    @Test
    fun `fromSrtString parses zero timestamp`() {
        val ts = Timestamp.fromSrtString("00:00:00,000")

        assertThat(ts.hours).isEqualTo(0)
        assertThat(ts.minutes).isEqualTo(0)
        assertThat(ts.seconds).isEqualTo(0)
        assertThat(ts.milliSeconds).isEqualTo(0)
    }

    @Test
    fun `fromSrtString parses max timestamp`() {
        val ts = Timestamp.fromSrtString("99:59:59,999")

        assertThat(ts.hours).isEqualTo(99)
        assertThat(ts.minutes).isEqualTo(59)
        assertThat(ts.seconds).isEqualTo(59)
        assertThat(ts.milliSeconds).isEqualTo(999)
    }

    // ---- fromBccString ----

    @Test
    fun `fromBccString parses whole seconds`() {
        val ts = Timestamp.fromBccString(120.0f)

        assertThat(ts.hours).isEqualTo(0)
        assertThat(ts.minutes).isEqualTo(2)
        assertThat(ts.seconds).isEqualTo(0)
        assertThat(ts.milliSeconds).isEqualTo(0)
    }

    @Test
    fun `fromBccString parses fractional seconds`() {
        val ts = Timestamp.fromBccString(7530.2f)

        assertThat(ts.hours).isEqualTo(2)
        assertThat(ts.minutes).isEqualTo(5)
        assertThat(ts.seconds).isEqualTo(30)
        assertThat(ts.milliSeconds).isEqualTo(200)
    }

    @Test
    fun `fromBccString parses zero`() {
        val ts = Timestamp.fromBccString(0.0f)

        assertThat(ts.hours).isEqualTo(0)
        assertThat(ts.minutes).isEqualTo(0)
        assertThat(ts.seconds).isEqualTo(0)
        assertThat(ts.milliSeconds).isEqualTo(0)
    }

    @Test
    fun `fromBccString parses value exceeding one hour`() {
        val ts = Timestamp.fromBccString(3661.5f)

        assertThat(ts.hours).isEqualTo(1)
        assertThat(ts.minutes).isEqualTo(1)
        assertThat(ts.seconds).isEqualTo(1)
        assertThat(ts.milliSeconds).isEqualTo(500)
    }

    // ---- totalMills (init block) ----

    @Test
    fun `totalMills calculates correctly for zero`() {
        val ts = Timestamp(0, 0, 0, 0)
        assertThat(ts.totalMills).isEqualTo(0L)
    }

    @Test
    fun `totalMills calculates correctly for hours only`() {
        val ts = Timestamp(1, 0, 0, 0)
        assertThat(ts.totalMills).isEqualTo(3_600_000L)
    }

    @Test
    fun `totalMills calculates correctly for all components`() {
        val ts = Timestamp(1, 2, 3, 4)
        assertThat(ts.totalMills).isEqualTo(1 * 3_600_000L + 2 * 60_000L + 3 * 1_000L + 4)
    }

    @Test
    fun `totalMills calculates correctly from fromSrtString`() {
        val ts = Timestamp.fromSrtString("01:03:17,775")
        assertThat(ts.totalMills).isEqualTo(1 * 3_600_000L + 3 * 60_000L + 17 * 1_000L + 775)
    }

    @Test
    fun `totalMills calculates correctly from fromBccString`() {
        val ts = Timestamp.fromBccString(7530.2f)
        assertThat(ts.totalMills).isEqualTo(2 * 3_600_000L + 5 * 60_000L + 30 * 1_000L + 200)
    }

    // ---- getBccTime ----

    @Test
    fun `getBccTime returns correct float for whole seconds`() {
        val ts = Timestamp(0, 2, 0, 0)
        assertThat(ts.getBccTime()).isEqualTo(120.0f)
    }

    @Test
    fun `getBccTime returns correct float for fractional`() {
        val ts = Timestamp(0, 0, 1, 500)
        assertThat(ts.getBccTime()).isEqualTo(1.5f)
    }

    @Test
    fun `getBccTime returns correct float for hours`() {
        val ts = Timestamp(1, 0, 0, 0)
        assertThat(ts.getBccTime()).isEqualTo(3600.0f)
    }

    @Test
    fun `getBccTime returns zero for zero timestamp`() {
        val ts = Timestamp(0, 0, 0, 0)
        assertThat(ts.getBccTime()).isEqualTo(0.0f)
    }

    @Test
    fun `fromBccString to getBccTime roundtrip preserves value`() {
        val original = 7530.2f
        val ts = Timestamp.fromBccString(original)
        assertThat(ts.getBccTime()).isEqualTo(original)
    }

    // ---- getSrtTime ----

    @Test
    fun `getSrtTime formats standard timestamp`() {
        val ts = Timestamp(1, 3, 17, 775)
        assertThat(ts.getSrtTime()).isEqualTo("01:03:17,775")
    }

    @Test
    fun `getSrtTime formats zero timestamp`() {
        val ts = Timestamp(0, 0, 0, 0)
        assertThat(ts.getSrtTime()).isEqualTo("00:00:00,000")
    }

    @Test
    fun `getSrtTime pads single digit millis to three places`() {
        val ts = Timestamp(0, 0, 0, 5)
        assertThat(ts.getSrtTime()).isEqualTo("00:00:00,500")
    }

    @Test
    fun `getSrtTime pads two digit millis to three places`() {
        val ts = Timestamp(0, 0, 0, 50)
        assertThat(ts.getSrtTime()).isEqualTo("00:00:00,500")
    }

    @Test
    fun `getSrtTime does not truncate three digit millis`() {
        val ts = Timestamp(0, 0, 0, 999)
        assertThat(ts.getSrtTime()).isEqualTo("00:00:00,999")
    }

    @Test
    fun `fromSrtString to getSrtTime roundtrip preserves string`() {
        val original = "01:03:17,775"
        val ts = Timestamp.fromSrtString(original)
        assertThat(ts.getSrtTime()).isEqualTo(original)
    }

    @Test
    fun `fromSrtString to getSrtTime roundtrip with zero millis`() {
        val original = "00:00:01,000"
        val ts = Timestamp.fromSrtString(original)
        assertThat(ts.getSrtTime()).isEqualTo(original)
    }

    // ---- Cross-format roundtrip ----

    @Test
    fun `fromBccString then getSrtTime produces correct SRT format`() {
        val ts = Timestamp.fromBccString(3661.5f)
        assertThat(ts.getSrtTime()).isEqualTo("01:01:01,500")
    }

    @Test
    fun `fromSrtString then getBccTime produces correct BCC float`() {
        val ts = Timestamp.fromSrtString("00:01:30,000")
        assertThat(ts.getBccTime()).isEqualTo(90.0f)
    }

    // ---- Data class equality ----

    @Test
    fun `two timestamps with same values are equal`() {
        val a = Timestamp(1, 2, 3, 4)
        val b = Timestamp(1, 2, 3, 4)
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `two timestamps with different values are not equal`() {
        val a = Timestamp(1, 2, 3, 4)
        val b = Timestamp(1, 2, 3, 5)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `totalMills is updated when created via fromSrtString`() {
        val ts = Timestamp.fromSrtString("00:10:00,000")
        assertThat(ts.totalMills).isEqualTo(600_000L)
    }
}
