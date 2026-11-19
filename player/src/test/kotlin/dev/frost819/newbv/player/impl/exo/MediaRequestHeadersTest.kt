package dev.frost819.newbv.player.impl.exo

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [MediaRequestHeaders] 的单元测试。
 *
 * 默认头只负责 Referer：Web 模式需要，App 模式带上反而 403。
 * `Range` 绝不能出现在默认头里——media3 在 seek 时用 `addHeader` 追加自己算的 Range，
 * 默认头再常驻一个就会让请求带两个 Range，首包兜底交给 [MediaRangeInterceptor]。
 */
class MediaRequestHeadersTest {
    @Test
    fun `sends referer only when provided`() {
        // When
        val withReferer = MediaRequestHeaders.of("https://www.bilibili.com")
        val withoutReferer = MediaRequestHeaders.of(null)
        val blankReferer = MediaRequestHeaders.of("   ")

        // Then
        assertThat(withReferer[MediaRequestHeaders.REFERER_HEADER]).isEqualTo("https://www.bilibili.com")
        assertThat(withoutReferer).doesNotContainKey(MediaRequestHeaders.REFERER_HEADER)
        assertThat(blankReferer).doesNotContainKey(MediaRequestHeaders.REFERER_HEADER)
    }

    @Test
    fun `default headers never pin range`() {
        // Then: 默认头一旦带 Range，seek 请求会出现两个 Range 头（详见 MediaRangeInterceptor）
        val webHeaders = MediaRequestHeaders.of("https://www.bilibili.com")
        val appHeaders = MediaRequestHeaders.of(null)

        assertThat(webHeaders).doesNotContainKey(MediaRangeInterceptor.RANGE_HEADER)
        assertThat(appHeaders).doesNotContainKey(MediaRangeInterceptor.RANGE_HEADER)
    }

    @Test
    fun `returns empty map when referer missing`() {
        // Then: 不发送 Referer 时不为空表凭空构造键
        assertThat(MediaRequestHeaders.of(null)).isEmpty()
        assertThat(MediaRequestHeaders.of("")).isEmpty()
    }
}
