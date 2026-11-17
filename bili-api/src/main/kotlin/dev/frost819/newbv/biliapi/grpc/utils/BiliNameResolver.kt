package dev.frost819.newbv.biliapi.grpc.utils

import dev.frost819.newbv.biliapi.http.util.BiliDns
import io.grpc.EquivalentAddressGroup
import io.grpc.NameResolver
import io.grpc.NameResolverProvider
import io.grpc.NameResolverRegistry
import io.grpc.Status
import io.grpc.StatusOr
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 让 gRPC 复用 [BiliDns] 地址族偏好的 [NameResolverProvider]。
 *
 * gRPC 默认的 dns resolver 使用 JDK 地址顺序（Android 上通常 IPv6 在前），无法感知
 * [dev.frost819.newbv.biliapi.http.util.BiliNetworkConfig]，导致「仅 IPv4 / IPv4 优先」
 * 设置对 App 接口不生效（issue #285）。
 *
 * 通过 [NameResolverRegistry] 以更高的优先级覆盖默认 `dns` scheme；本进程内只有
 * B 站一个 gRPC 目标，因此全局注册不会影响其它 channel。
 */
class BiliNameResolverProvider : NameResolverProvider() {
    override fun isAvailable(): Boolean = true

    override fun getDefaultScheme(): String = SCHEME

    override fun priority(): Int = PRIORITY

    override fun newNameResolver(
        targetUri: URI,
        args: NameResolver.Args,
    ): NameResolver = BiliNameResolver(targetUri)

    companion object {
        /** 与 gRPC 默认 dns scheme 保持一致，以覆盖内置 provider。 */
        const val SCHEME: String = "dns"

        /** 高于 gRPC 内置 dns provider（优先级 5）。 */
        private const val PRIORITY = 10

        private val registered = AtomicBoolean(false)

        /** 全局注册一次（幂等）。 */
        fun ensureRegistered() {
            if (registered.compareAndSet(false, true)) {
                NameResolverRegistry.getDefaultRegistry().register(BiliNameResolverProvider())
            }
        }
    }
}

/**
 * 基于 [BiliDns] 的 gRPC name resolver。
 *
 * @param targetUri gRPC 解析目标（形如 `dns:///grpc.biliapi.net:443`）
 * @param lookup 域名解析函数，默认走 [BiliDns]（测试可注入假实现）
 * @param executor 执行阻塞解析的线程池，默认使用守护线程池（测试可注入直执行器）
 */
class BiliNameResolver(
    private val targetUri: URI,
    private val lookup: (String) -> List<InetAddress> = BiliDns::lookup,
    private val executor: Executor = LOOKUP_EXECUTOR,
) : NameResolver() {
    private val host: String
    private val port: Int

    init {
        // gRPC 传入的通常是 opaque URI（dns:///host:port），此时 getHost() 为 null，
        // 需要从 schemeSpecificPart 还原 authority 再解析。
        val normalized =
            if (targetUri.host != null) {
                targetUri
            } else {
                URI(
                    "${BiliNameResolverProvider.SCHEME}://" +
                        targetUri.schemeSpecificPart.trimStart('/'),
                )
            }
        host =
            requireNotNull(normalized.host) { "target 缺少 host: $targetUri" }
                .removeSurrounding("[", "]")
        port = normalized.port.takeIf { it > 0 } ?: DEFAULT_PORT
    }

    override fun getServiceAuthority(): String = host

    override fun start(listener: NameResolver.Listener2) {
        executor.execute {
            runCatching { lookup(host) }
                .onSuccess { addresses ->
                    val groups =
                        addresses.map { address ->
                            EquivalentAddressGroup(InetSocketAddress(address, port))
                        }
                    if (groups.isEmpty()) {
                        listener.onError(Status.UNAVAILABLE.withDescription("$host 没有可用地址"))
                    } else {
                        listener.onResult(
                            NameResolver.ResolutionResult
                                .newBuilder()
                                .setAddressesOrError(StatusOr.fromValue(groups))
                                .build(),
                        )
                    }
                }.onFailure { error ->
                    listener.onError(
                        Status.UNAVAILABLE
                            .withDescription("解析 $host 失败")
                            .withCause(error),
                    )
                }
        }
    }

    override fun shutdown() {}

    companion object {
        private const val DEFAULT_PORT = 443

        /** 阻塞 DNS 解析不能占用 gRPC 的同步上下文，统一放到守护线程。 */
        private val LOOKUP_EXECUTOR: Executor =
            Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "bili-name-resolver").apply { isDaemon = true }
            }
    }
}
