package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Buvid] 生成函数的单元测试。
 *
 * 验证 buvid 格式与 md5 工具函数。
 */
class BuvidTest {
    @Test
    fun `generateBuvid starts with XY prefix`() {
        val buvid = generateBuvid()
        assertThat(buvid.startsWith("XY")).isTrue()
    }

    @Test
    fun `generateBuvid has correct length`() {
        // XY + 3 chars from md5 + 32 chars md5 = 37
        val buvid = generateBuvid()
        assertThat(buvid).hasLength(37)
    }

    @Test
    fun `generateBuvid generates unique values`() {
        val buvid1 = generateBuvid()
        val buvid2 = generateBuvid()
        assertThat(buvid1).isNotEqualTo(buvid2)
    }

    @Test
    fun `md5 returns 32-char hex string`() {
        val result = md5("test")
        assertThat(result).hasLength(32)
    }

    @Test
    fun `md5 returns known value for empty string`() {
        val result = md5("")
        assertThat(result).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
    }

    @Test
    fun `md5 returns known value for hello`() {
        val result = md5("hello")
        assertThat(result).isEqualTo("5d41402abc4b2a76b9719d911017c592")
    }

    @Test
    fun `md5 pads leading zeros`() {
        val result = md5("test")
        // MD5 of "test" = 098f6bcd4621d373cade4e832627b4f6 — no leading zeros
        assertThat(result).isEqualTo("098f6bcd4621d373cade4e832627b4f6")
    }
}
