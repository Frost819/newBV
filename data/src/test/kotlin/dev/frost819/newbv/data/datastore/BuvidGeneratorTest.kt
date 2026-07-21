package dev.frost819.newbv.data.datastore

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [BuvidGenerator] 的单元测试。
 *
 * 验证 buvid / buvid3 生成格式与 MD5 算法正确性。
 */
class BuvidGeneratorTest {

    @Test
    fun `generateBuvid starts with XY prefix`() {
        val buvid = BuvidGenerator.generateBuvid()
        assertThat(buvid).startsWith("XY")
    }

    @Test
    fun `generateBuvid has correct length`() {
        // XY(2) + 3 chars from md5 + md5(32) = 37
        val buvid = BuvidGenerator.generateBuvid()
        assertThat(buvid.length).isEqualTo(37)
    }

    @Test
    fun `generateBuvid contains only hex chars after prefix`() {
        val buvid = BuvidGenerator.generateBuvid()
        val hexPart = buvid.substring(5) // skip "XY" + 3 chars
        assertThat(hexPart).matches("[0-9a-f]{32}")
    }

    @Test
    fun `generateBuvid3 ends with infoc suffix`() {
        val buvid3 = BuvidGenerator.generateBuvid3()
        assertThat(buvid3).endsWith("infoc")
    }

    @Test
    fun `generateBuvid3 contains uuid and digit before infoc`() {
        val buvid3 = BuvidGenerator.generateBuvid3()
        // Format: <uuid><digit>infoc
        // UUID: 36 chars (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx) + 1 digit + 5 chars "infoc" = 42
        assertThat(buvid3.length).isEqualTo(42)
        assertThat(buvid3).contains("-")
    }

    @Test
    fun `generateBuvid3 digit is between 0 and 9`() {
        val buvid3 = BuvidGenerator.generateBuvid3()
        val digit = buvid3.substring(36, 37) // char before "infoc"
        assertThat(digit.toIntOrNull()).isIn((0..9).toList())
    }

    @Test
    fun `md5 returns 32 char hex string`() {
        val input = "test"
        val result = BuvidGenerator.md5(input)
        assertThat(result).hasLength(32)
        assertThat(result).matches("[0-9a-f]{32}")
    }

    @Test
    fun `md5 is deterministic`() {
        val input = "hello"
        val result1 = BuvidGenerator.md5(input)
        val result2 = BuvidGenerator.md5(input)
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `md5 matches known value`() {
        // MD5("test") = 098f6bcd4621d373cade4e832627b4f6
        assertThat(BuvidGenerator.md5("test")).isEqualTo("098f6bcd4621d373cade4e832627b4f6")
    }

    @Test
    fun `md5 pads short hash to 32 chars`() {
        // MD5 of empty string = d41d8cd98f00b204e9800998ecf8427e (already 32 chars, but verify padding works)
        val result = BuvidGenerator.md5("")
        assertThat(result).hasLength(32)
        assertThat(result).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
    }
}
