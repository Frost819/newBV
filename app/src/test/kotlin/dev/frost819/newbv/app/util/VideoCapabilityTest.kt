package dev.frost819.newbv.app.util

import androidx.media3.common.C
import androidx.media3.exoplayer.RendererCapabilities
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [isOptimisticallyDecodable] 的纯函数单测。
 *
 * 覆盖 issue #335：老设备的 MediaCodec 能力上报不可靠，`FORMAT_EXCEEDS_CAPABILITIES`
 * 不能当作硬性不可解码，否则会稳定误降档；只有「无对应 MIME 解码器」才应排除。
 */
class VideoCapabilityTest {
    /** 按 [RendererCapabilities] 的位布局拼装能力掩码（低 3 位为格式支持等级）。 */
    private fun capabilities(
        formatSupport: Int,
        adaptive: Int = 0,
        tunneling: Int = 0,
    ): Int = formatSupport or adaptive or tunneling

    @Test
    fun `handled format is decodable`() {
        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_HANDLED))).isTrue()
    }

    @Test
    fun `exceeds capabilities is treated as decodable`() {
        // 尺寸 / 帧率 / profile 超限：厂商上报不可靠，乐观放行交给运行时回退
        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_EXCEEDS_CAPABILITIES))).isTrue()
    }

    @Test
    fun `unsupported subtype is not decodable`() {
        // 完全查不到对应 MIME 的解码器
        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_UNSUPPORTED_SUBTYPE))).isFalse()
    }

    @Test
    fun `unsupported type is not decodable`() {
        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_UNSUPPORTED_TYPE))).isFalse()
    }

    @Test
    fun `adaptive and tunneling bits do not affect the format support result`() {
        val highBits = RendererCapabilities.ADAPTIVE_SEAMLESS or RendererCapabilities.TUNNELING_SUPPORTED

        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_HANDLED, highBits))).isTrue()
        assertThat(isOptimisticallyDecodable(capabilities(C.FORMAT_UNSUPPORTED_SUBTYPE, highBits))).isFalse()
    }
}
