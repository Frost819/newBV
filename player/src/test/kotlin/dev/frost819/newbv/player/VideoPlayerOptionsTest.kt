package dev.frost819.newbv.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [VideoPlayerOptions] 的单元测试。
 *
 * 验证默认值、相等性、拷贝及自定义配置。
 */
class VideoPlayerOptionsTest {
    @Test
    fun `default values are null or false`() {
        val options = VideoPlayerOptions()

        assertThat(options.userAgent).isNull()
        assertThat(options.referer).isNull()
        assertThat(options.enableFfmpegAudioRenderer).isFalse()
        assertThat(options.enableSoftwareVideoDecoder).isFalse()
    }

    @Test
    fun `custom values are retained`() {
        val options =
            VideoPlayerOptions(
                userAgent = "Mozilla/5.0",
                referer = "https://www.bilibili.com",
                enableFfmpegAudioRenderer = true,
                enableSoftwareVideoDecoder = true,
            )

        assertThat(options.userAgent).isEqualTo("Mozilla/5.0")
        assertThat(options.referer).isEqualTo("https://www.bilibili.com")
        assertThat(options.enableFfmpegAudioRenderer).isTrue()
        assertThat(options.enableSoftwareVideoDecoder).isTrue()
    }

    @Test
    fun `two instances with same values are equal`() {
        val a = VideoPlayerOptions(userAgent = "UA", referer = "R")
        val b = VideoPlayerOptions(userAgent = "UA", referer = "R")

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `instances with different values are not equal`() {
        val a = VideoPlayerOptions(userAgent = "UA1")
        val b = VideoPlayerOptions(userAgent = "UA2")

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `copy creates independent instance with modified field`() {
        val original = VideoPlayerOptions(userAgent = "UA", enableFfmpegAudioRenderer = false)
        val copied = original.copy(enableFfmpegAudioRenderer = true)

        assertThat(copied.userAgent).isEqualTo("UA")
        assertThat(copied.enableFfmpegAudioRenderer).isTrue()
        assertThat(original.enableFfmpegAudioRenderer).isFalse()
    }

    @Test
    fun `data class toString contains class name`() {
        val options = VideoPlayerOptions()
        assertThat(options.toString()).contains("VideoPlayerOptions")
    }
}
