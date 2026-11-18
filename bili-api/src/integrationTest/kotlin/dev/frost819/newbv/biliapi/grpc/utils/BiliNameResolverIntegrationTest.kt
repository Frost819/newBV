package dev.frost819.newbv.biliapi.grpc.utils

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.IpFamilyPreference
import dev.frost819.newbv.biliapi.http.util.BiliAppConf
import dev.frost819.newbv.biliapi.http.util.BiliNetworkConfig
import io.grpc.ConnectivityState
import io.grpc.ManagedChannelBuilder
import io.grpc.NameResolver
import io.grpc.Status
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * [BiliNameResolverProvider] 的真实网络集成测试。
 *
 * 需要外网，默认不随 `:bili-api:test` 执行，运行方式：
 * `./gradlew :bili-api:integrationTest --tests "*BiliNameResolverIntegrationTest*"`
 *
 * 覆盖两件事：
 * 1. resolver 对真实 `grpc.biliapi.net` 的解析结果遵循地址族偏好；
 * 2. 通过 [NameResolverRegistry] 注册的自定义 resolver 能让真实 channel 建连到 READY。
 */
internal class BiliNameResolverIntegrationTest {
    @Test
    fun `resolver resolves real host and puts ipv4 first`() {
        val original = BiliNetworkConfig.addressFamily
        try {
            BiliNetworkConfig.addressFamily = IpFamilyPreference.PreferIpv4

            val addresses = resolveReal()

            assertThat(addresses).isNotEmpty()
            val firstIpv6Index = addresses.indexOfFirst { it.address is Inet6Address }
            val lastIpv4Index = addresses.indexOfLast { it.address is Inet4Address }
            // 域名同时有 A/AAAA 时，IPv4 必须全部排在 IPv6 之前
            if (firstIpv6Index >= 0 && lastIpv4Index >= 0) {
                assertThat(lastIpv4Index).isLessThan(firstIpv6Index)
            }
        } finally {
            BiliNetworkConfig.addressFamily = original
        }
    }

    @Test
    fun `ipv4 only resolver returns only ipv4 for real host`() {
        val original = BiliNetworkConfig.addressFamily
        try {
            BiliNetworkConfig.addressFamily = IpFamilyPreference.Ipv4Only

            val addresses = resolveReal()

            assertThat(addresses).isNotEmpty()
            assertThat(addresses.all { it.address is Inet4Address }).isTrue()
        } finally {
            BiliNetworkConfig.addressFamily = original
        }
    }

    @Test
    fun `channel with registered custom resolver reaches ready`() {
        BiliNameResolverProvider.ensureRegistered()

        val channel =
            ManagedChannelBuilder
                .forAddress(BiliAppConf.GRPC_HOST, BiliAppConf.GRPC_PORT)
                .useTransportSecurity()
                .build()
        try {
            channel.getState(true)
            val deadline = System.currentTimeMillis() + READY_TIMEOUT_MS
            var state = channel.getState(false)
            while (System.currentTimeMillis() < deadline &&
                state != ConnectivityState.READY &&
                state != ConnectivityState.TRANSIENT_FAILURE &&
                state != ConnectivityState.SHUTDOWN
            ) {
                Thread.sleep(STATE_POLL_INTERVAL_MS)
                state = channel.getState(false)
            }

            assertThat(state).isEqualTo(ConnectivityState.READY)
        } finally {
            channel.shutdownNow()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /** 用真实 [BiliDns] 解析 gRPC 域名，返回 host:port 列表。 */
    private fun resolveReal(): List<InetSocketAddress> =
        runBlocking {
            val listener = AwaitingListener()
            val resolver =
                BiliNameResolver(
                    URI("dns:///${BiliAppConf.GRPC_HOST}:${BiliAppConf.GRPC_PORT}"),
                )
            resolver.start(listener)
            try {
                withTimeout(RESOLVE_TIMEOUT_MS) {
                    listener.result
                        .await()
                        .addressesOrError.value
                        .orEmpty()
                        .map { group -> group.addresses.single() as InetSocketAddress }
                }
            } finally {
                resolver.shutdown()
            }
        }

    private class AwaitingListener : NameResolver.Listener2() {
        val result = CompletableDeferred<NameResolver.ResolutionResult>()

        override fun onResult(resolutionResult: NameResolver.ResolutionResult) {
            result.complete(resolutionResult)
        }

        override fun onError(error: Status) {
            result.completeExceptionally(error.asRuntimeException())
        }
    }

    private companion object {
        private const val RESOLVE_TIMEOUT_MS = 15_000L
        private const val READY_TIMEOUT_MS = 20_000L
        private const val STATE_POLL_INTERVAL_MS = 200L
    }
}
