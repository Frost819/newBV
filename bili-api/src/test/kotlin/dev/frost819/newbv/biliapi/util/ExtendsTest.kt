package dev.frost819.newbv.biliapi.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Extends] 扩展函数的单元测试。
 *
 * 验证时间字符串转换与 AV/BV 便捷转换。
 */
class ExtendsTest {
    @Test
    fun `convertStringTimeToSeconds parses HH colon MM colon SS`() {
        assertThat("01:30:45".convertStringTimeToSeconds()).isEqualTo(5445)
    }

    @Test
    fun `convertStringTimeToSeconds parses MM colon SS`() {
        assertThat("30:45".convertStringTimeToSeconds()).isEqualTo(1845)
    }

    @Test
    fun `convertStringTimeToSeconds parses SS only`() {
        // "45" has no colon, parts.size = 1
        // minutes = parts[-1] throws IndexOutOfBoundsException
        org.junit.jupiter.api.assertThrows<IndexOutOfBoundsException> {
            "45".convertStringTimeToSeconds()
        }
    }

    @Test
    fun `convertStringTimeToSeconds handles zero values`() {
        assertThat("00:00:00".convertStringTimeToSeconds()).isEqualTo(0)
    }

    @Test
    fun `toBv delegates to AvBvConverter`() {
        val bvid = 170001L.toBv()
        assertThat(bvid).isEqualTo("BV17x411w7KC")
    }

    @Test
    fun `toAv delegates to AvBvConverter`() {
        val aid = "BV17x411w7KC".toAv()
        assertThat(aid).isEqualTo(170001L)
    }

    @Test
    fun `toBv and toAv round-trip`() {
        val original = 42L
        val recovered = original.toBv().toAv()
        assertThat(recovered).isEqualTo(original)
    }
}
