package dev.frost819.newbv.biliapi.grpc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcCode
import dev.frost819.newbv.biliapi.grpc.utils.GrpcErrorKind
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.repositories.ChannelRepository
import io.grpc.Metadata
import io.grpc.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** App gRPC channel 生命周期与错误分类测试。 */
class GrpcInfrastructureTest {
    @Test
    fun `channel requires initialization before use`() {
        val repository = ChannelRepository()

        val exception =
            assertThrows<IllegalStateException> {
                repository.requireDefaultChannel()
            }

        assertThat(exception.message).isEqualTo("App gRPC channel is not initialized")
    }

    @Test
    fun `channel rejects empty credentials`() {
        val repository = ChannelRepository()

        assertThrows<IllegalArgumentException> {
            repository.initDefaultChannel("", "buvid")
        }
        assertThrows<IllegalArgumentException> {
            repository.initDefaultChannel("access-token", "")
        }
    }

    @Test
    fun `grpc authentication error is classified without fallback`() {
        val exception =
            assertThrows<RuntimeException> {
                handleGrpcException(Status.UNAUTHENTICATED.withDescription("login required").asException())
            }

        assertThat(exception).isInstanceOf(dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException::class.java)
        assertThat((exception as dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException).kind)
            .isEqualTo(GrpcErrorKind.Authentication)
    }

    @Test
    fun `grpc network error is classified as Network`() {
        val exception =
            assertThrows<RuntimeException> {
                handleGrpcException(Status.UNAVAILABLE.withDescription("connection refused").asRuntimeException())
            }

        assertThat(exception).isInstanceOf(dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException::class.java)
        assertThat((exception as dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException).kind)
            .isEqualTo(GrpcErrorKind.Network)
    }

    @Test
    fun `grpc server error is classified as Server`() {
        val exception =
            assertThrows<RuntimeException> {
                handleGrpcException(Status.INTERNAL.withDescription("boom").asRuntimeException())
            }

        assertThat(exception).isInstanceOf(dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException::class.java)
        assertThat((exception as dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException).kind)
            .isEqualTo(GrpcErrorKind.Server)
    }

    @Test
    fun `grpc business risk control code is classified as RiskControl`() {
        // 构造带 grpc-status-details-bin trailer 的异常，携带 bilibili.rpc.Status{ code=-352, message=... }
        val trailers = Metadata()
        val statusBytes =
            bilibili.rpc.Status
                .newBuilder()
                .setCode(BiliGrpcCode.RISK_CONTROL)
                .setMessage("风控触发")
                .build()
                .toByteArray()
        trailers.put(
            Metadata.Key.of("grpc-status-details-bin", Metadata.BINARY_BYTE_MARSHALLER),
            statusBytes,
        )
        val error = Status.UNKNOWN.asRuntimeException(trailers)

        val exception =
            assertThrows<RuntimeException> {
                handleGrpcException(error)
            }

        assertThat(exception).isInstanceOf(dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException::class.java)
        val biliException = exception as dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException
        assertThat(biliException.kind).isEqualTo(GrpcErrorKind.RiskControl)
        assertThat(biliException.message).isEqualTo("风控触发")
    }

    @Test
    fun `grpc unknown status without business code falls back to Unknown`() {
        val exception =
            assertThrows<RuntimeException> {
                handleGrpcException(Status.DATA_LOSS.withDescription("loss").asRuntimeException())
            }

        assertThat(exception).isInstanceOf(dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException::class.java)
        assertThat((exception as dev.frost819.newbv.biliapi.grpc.utils.BiliGrpcException).kind)
            .isEqualTo(GrpcErrorKind.Unknown)
    }
}
