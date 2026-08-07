package dev.frost819.newbv.biliapi.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [AvBvConverter] 的单元测试。
 *
 * 验证 AV/BV 互转算法的 round-trip 与已知值。
 */
class AvBvConverterTest {
    @Test
    fun `av2bv converts known aid to expected bvid`() {
        assertThat(AvBvConverter.av2bv(170001L)).isEqualTo("BV17x411w7KC")
    }

    @Test
    fun `bv2av converts known bvid to expected aid`() {
        assertThat(AvBvConverter.bv2av("BV17x411w7KC")).isEqualTo(170001L)
    }

    @Test
    fun `av2bv and bv2av round-trip preserves value`() {
        val testAids = listOf(1L, 100L, 170001L, 99999999L, 12345678L)
        for (aid in testAids) {
            val bvid = AvBvConverter.av2bv(aid)
            val recovered = AvBvConverter.bv2av(bvid)
            assertThat(recovered).isEqualTo(aid)
        }
    }

    @Test
    fun `av2bv always starts with BV`() {
        assertThat(AvBvConverter.av2bv(1L).startsWith("BV1")).isTrue()
        assertThat(AvBvConverter.av2bv(999999L).startsWith("BV1")).isTrue()
    }

    @Test
    fun `av2bv produces 12-character bvid`() {
        assertThat(AvBvConverter.av2bv(1L)).hasLength(12)
        assertThat(AvBvConverter.av2bv(999999999L)).hasLength(12)
    }
}
