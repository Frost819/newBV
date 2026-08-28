package dev.frost819.newbv.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Date

/**
 * [Converters] 的单元测试。
 *
 * 验证 Date ↔ Long（时间戳毫秒）的双向转换逻辑，
 * 包括 null 值处理与往返一致性。
 */
class ConvertersTest {
    @Test
    fun `timestampToDate returns null for null input`() {
        assertThat(Converters.timestampToDate(null)).isNull()
    }

    @Test
    fun `timestampToDate converts valid timestamp to Date`() {
        val timestamp = 1700000000000L
        val result = Converters.timestampToDate(timestamp)
        assertThat(result).isNotNull()
        assertThat(result!!.time).isEqualTo(timestamp)
    }

    @Test
    fun `timestampToDate converts zero timestamp`() {
        val result = Converters.timestampToDate(0L)
        assertThat(result).isNotNull()
        assertThat(result!!.time).isEqualTo(0L)
    }

    @Test
    fun `timestampToDate converts negative timestamp`() {
        val result = Converters.timestampToDate(-1L)
        assertThat(result).isNotNull()
        assertThat(result!!.time).isEqualTo(-1L)
    }

    @Test
    fun `dateToTimestamp returns null for null input`() {
        assertThat(Converters.dateToTimestamp(null)).isNull()
    }

    @Test
    fun `dateToTimestamp converts valid Date to timestamp`() {
        val date = Date(1700000000000L)
        assertThat(Converters.dateToTimestamp(date)).isEqualTo(1700000000000L)
    }

    @Test
    fun `dateToTimestamp converts epoch Date`() {
        assertThat(Converters.dateToTimestamp(Date(0L))).isEqualTo(0L)
    }

    @Test
    fun `round trip Date to timestamp and back preserves value`() {
        val original = Date(1234567890123L)
        val timestamp = Converters.dateToTimestamp(original)
        val restored = Converters.timestampToDate(timestamp!!)
        assertThat(restored!!.time).isEqualTo(original.time)
    }

    @Test
    fun `round trip timestamp to Date and back preserves value`() {
        val original = 9876543210L
        val date = Converters.timestampToDate(original)!!
        val restored = Converters.dateToTimestamp(date)
        assertThat(restored).isEqualTo(original)
    }
}
