package dev.frost819.newbv.biliapi.grpc.utils

import com.google.common.truth.Truth.assertThat
import io.grpc.NameResolver
import io.grpc.Status
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.Executor

/**
 * [BiliNameResolver] 的单元测试。
 *
 * 验证 gRPC 按 [dev.frost819.newbv.biliapi.http.util.BiliDns] 返回的地址顺序解析
 * （issue #285：IPv4 优先 / 仅 IPv4 对 App 接口生效），以及异常与空结果的降级。
 */
class BiliNameResolverTest {
    private val ipv4 = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))
    private val ipv6 = InetAddress.getByAddress(ByteArray(16).also { it[15] = 1 })
    private val targetUri = URI("dns:///grpc.biliapi.net:443")
    private val directExecutor = Executor { it.run() }

    @Test
    fun `resolves addresses in lookup order with target port`() {
        val listener = RecordingListener()
        val resolver =
            BiliNameResolver(
                targetUri = targetUri,
                lookup = { listOf(ipv4, ipv6) },
                executor = directExecutor,
            )

        resolver.start(listener)

        val addresses =
            listener.requireResult().addressesOrError.value.orEmpty().map {
                it.addresses.single() as InetSocketAddress
            }
        assertThat(addresses.map { it.address }).containsExactly(ipv4, ipv6).inOrder()
        assertThat(addresses.map { it.port }).containsExactly(443, 443)
    }

    @Test
    fun `uses host as service authority`() {
        val resolver =
            BiliNameResolver(
                targetUri = targetUri,
                lookup = { listOf(ipv4) },
                executor = directExecutor,
            )

        assertThat(resolver.serviceAuthority).isEqualTo("grpc.biliapi.net")
    }

    @Test
    fun `reports unavailable when lookup throws`() {
        val listener = RecordingListener()
        val resolver =
            BiliNameResolver(
                targetUri = targetUri,
                lookup = { throw UnknownHostException("no such host") },
                executor = directExecutor,
            )

        resolver.start(listener)

        assertThat(listener.error?.code).isEqualTo(Status.Code.UNAVAILABLE)
    }

    @Test
    fun `reports unavailable when no address resolved`() {
        val listener = RecordingListener()
        val resolver =
            BiliNameResolver(
                targetUri = targetUri,
                lookup = { emptyList() },
                executor = directExecutor,
            )

        resolver.start(listener)

        assertThat(listener.error?.code).isEqualTo(Status.Code.UNAVAILABLE)
    }

    private class RecordingListener : NameResolver.Listener2() {
        var result: NameResolver.ResolutionResult? = null
        var error: Status? = null

        override fun onResult(resolutionResult: NameResolver.ResolutionResult) {
            result = resolutionResult
        }

        override fun onError(error: Status) {
            this.error = error
        }

        fun requireResult(): NameResolver.ResolutionResult = checkNotNull(result)
    }
}
