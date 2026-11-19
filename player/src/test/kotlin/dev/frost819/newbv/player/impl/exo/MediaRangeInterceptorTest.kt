package dev.frost819.newbv.player.impl.exo

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [MediaRangeInterceptor] 的单元测试。
 *
 * 核心约束：
 * - 首包（没有 Range）必须补出恰好一个 `Range: bytes=0-`，否则 B 站海外 CDN 会以
 *   HTTP 567（HTML 反盗链页）拒绝整段 GET；
 * - 已有 Range（media3 在 seek 时算出的 `bytes=<pos>-`）必须原样保留且**不能多出第二个**，
 *   否则从哪个字节开始返回取决于 CDN 实现；
 * - HLS 播放列表（`.m3u8`）不是字节流，不补 Range，避免影响直播。
 */
class MediaRangeInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().addInterceptor(MediaRangeInterceptor).build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `request without range gets open range`() {
        // Given
        server.enqueue(MockResponse().setBody("ok"))

        // When
        execute("/video.m4s")

        // Then: 补齐首包的开区间 Range
        assertThat(rangeValues("/video.m4s")).containsExactly(MediaRangeInterceptor.OPEN_RANGE_VALUE)
    }

    @Test
    fun `request with range keeps exactly one range`() {
        // Given: 模拟 media3 seek 时自己算出的 Range
        server.enqueue(MockResponse().setBody("ok"))

        // When
        execute("/video.m4s", range = "bytes=5000-")

        // Then: 原值保留，且没有追加第二个 Range
        assertThat(rangeValues("/video.m4s")).containsExactly("bytes=5000-")
    }

    @Test
    fun `hls playlist is not given a range`() {
        // Given: 直播播放列表
        server.enqueue(MockResponse().setBody("#EXTM3U\n"))

        // When
        execute("/live/stream.m3u8")

        // Then: 播放列表不带 Range
        assertThat(rangeValues("/live/stream.m3u8")).isEmpty()
    }

    /** 发起一次请求并关闭响应。 */
    private fun execute(
        path: String,
        range: String? = null,
    ) {
        val builder = Request.Builder().url(server.url(path))
        range?.let { builder.header(MediaRangeInterceptor.RANGE_HEADER, it) }
        client.newCall(builder.build()).execute().use { }
    }

    /** 取服务端收到的该路径请求的 Range 值（可能为空或多值）。 */
    private fun rangeValues(path: String): List<String> {
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo(path)
        return request.headers.values(MediaRangeInterceptor.RANGE_HEADER)
    }
}
