package dev.frost819.newbv.player.impl.exo

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [MediaRequestHeaders] 的单元测试。
 *
 * 核心约束：无论走哪个接口模式，媒体请求**必须**带 `Range`，否则 B 站海外 CDN
 * 会以 HTTP 567（HTML 反盗链页）拒绝整段 GET；Referer 则只在 Web 模式提供，
 * App 模式带上反而会 403。
 */
class MediaRequestHeadersTest {
    @Test
    fun `always sends open range header`() {
        // When: 两种接口模式（Web 带 Referer / App 不带）
        val webHeaders = MediaRequestHeaders.of("https://www.bilibili.com")
        val appHeaders = MediaRequestHeaders.of(null)

        // Then: 都带开区间 Range
        assertThat(webHeaders[MediaRequestHeaders.RANGE_HEADER]).isEqualTo("bytes=0-")
        assertThat(appHeaders[MediaRequestHeaders.RANGE_HEADER]).isEqualTo("bytes=0-")
    }

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
    fun `range value starts from zero and keeps open end`() {
        // Then: 开区间（结尾留空）才会拿到 206 分片流；闭区间会限制本次传输长度
        assertThat(MediaRequestHeaders.OPEN_RANGE_VALUE).isEqualTo("bytes=0-")
        assertThat(MediaRequestHeaders.OPEN_RANGE_VALUE.endsWith("-")).isTrue()
    }
}
