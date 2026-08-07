package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Zlib] 扩展函数的单元测试。
 *
 * 验证 zlib 压缩/解压缩的 round-trip。
 */
class ZlibTest {
    @Test
    fun `zlibCompress and zlibDecompress round-trip preserves data`() {
        val original = "Hello, World! This is a test string.".toByteArray()
        val compressed = original.zlibCompress()
        val decompressed = compressed.zlibDecompress()
        assertThat(decompressed).isEqualTo(original)
    }

    @Test
    fun `zlibCompress produces smaller output for repetitive data`() {
        val original = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toByteArray()
        val compressed = original.zlibCompress()
        assertThat(compressed.size).isLessThan(original.size)
    }

    @Test
    fun `zlibDecompress handles empty compressed data`() {
        val original = ByteArray(0)
        val compressed = original.zlibCompress()
        val decompressed = compressed.zlibDecompress()
        assertThat(decompressed).isEqualTo(original)
    }

    @Test
    fun `zlib round-trip with large data`() {
        val original = (1..1000).joinToString("").toByteArray()
        val compressed = original.zlibCompress()
        val decompressed = compressed.zlibDecompress()
        assertThat(decompressed).isEqualTo(original)
    }

    @Test
    fun `zlib round-trip with binary data`() {
        val original = ByteArray(256) { it.toByte() }
        val compressed = original.zlibCompress()
        val decompressed = compressed.zlibDecompress()
        assertThat(decompressed).isEqualTo(original)
    }
}
