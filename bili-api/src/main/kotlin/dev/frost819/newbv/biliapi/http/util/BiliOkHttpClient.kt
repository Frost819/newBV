package dev.frost819.newbv.biliapi.http.util

import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 进程内共享的基础 OkHttpClient。
 *
 * 全进程的网络客户端（B 站 HTTP / 直播 / 登录 / 字幕 / 播放器媒体 / 直播 WebSocket /
 * 图片）都从该实例派生：[OkHttpClient.newBuilder] 会复用同一个 ConnectionPool 与
 * Dispatcher，避免每个客户端各自维护连接池、路由黑名单和线程池。
 *
 * 注意：派生实例只能通过 [OkHttpClient.newBuilder] 创建，不要重新 `OkHttpClient.Builder()`，
 * 否则会得到新的连接池。
 */
object BiliOkHttpClient {
    /** 共享连接池：空闲上限 10 条（OkHttp 默认 5），跨 host 全局计数。 */
    private val connectionPool =
        ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            TimeUnit.MINUTES,
        )

    /**
     * 共享连接池 / Dispatcher 的基础实例；连接超时 5s，读写保持 OkHttp 默认。
     *
     * 空闲连接上限取 10（OkHttp 默认 5），因为池合并且按全局计数：主接口、直播接口、
     * 多个图片 host、媒体 CDN 等常用 host 同时处于空闲时，5 条会频繁驱逐「最久未用」
     * 的连接，导致下一次请求重新握手。
     */
    val instance: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .dns(BiliDns)
            .connectTimeout(5, TimeUnit.SECONDS)
            .connectionPool(connectionPool)
            .build()
    }
}

/**
 * 让 Ktor 的 OkHttp 引擎复用 [BiliOkHttpClient.instance]。
 *
 * Ktor 创建引擎时会强制换成自己的 `Dispatcher()`，因此在 [OkHttpConfig.config]
 * 中把 Dispatcher 复位回共享实例，保证多套 Ktor 客户端共用同一个连接池与线程池。
 */
fun OkHttpConfig.useSharedBiliClient() {
    preconfigured = BiliOkHttpClient.instance
    config {
        dispatcher(BiliOkHttpClient.instance.dispatcher)
    }
}
