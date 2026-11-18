package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * [BiliOkHttpClient] 的单元测试。
 *
 * 验证共享实例的基础配置，以及 `newBuilder()` 派生出的客户端确实复用同一个
 * ConnectionPool / Dispatcher —— 这是「收敛 OkHttpClient 实例数量」的核心约束。
 */
class BiliOkHttpClientTest {
    @Test
    fun `shared instance uses bili dns and five second connect timeout`() {
        assertThat(BiliOkHttpClient.instance.dns).isSameInstanceAs(BiliDns)
        assertThat(BiliOkHttpClient.instance.connectTimeoutMillis).isEqualTo(5_000)
    }

    @Test
    fun `derived client shares connection pool and dispatcher`() {
        // 模拟播放器 / WebSocket 的派生方式
        val derived =
            BiliOkHttpClient.instance
                .newBuilder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build()

        assertThat(derived.connectionPool).isSameInstanceAs(BiliOkHttpClient.instance.connectionPool)
        assertThat(derived.dispatcher).isSameInstanceAs(BiliOkHttpClient.instance.dispatcher)
    }
}
