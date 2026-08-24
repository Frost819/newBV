package dev.frost819.newbv.biliapi.grpc.utils

import bilibili.rpc.Status
import com.google.protobuf.Message
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.Status as GrpcStatus
import io.grpc.Status.Code as GrpcStatusCode

// 如果没有设置 java_multiple_files = true; 那么就需要使用下面的代码
// 这时候生成的代码会将很多 class 放在同一个 class 里面
// 例如 bilibili.rpc.Status 会被放在 bilibili.rpc.StatusOuterClass$Status 中
// 这时候直接从 typeUrl 中获取 class 名称是不对的

/*
@Suppress("UNCHECKED_CAST")
fun Status.getTypeClass(): Class<Message> {
    val protoClassName = this.detailsList.first().typeUrl.split("/").last()
    val splitProtoClassNames = protoClassName.split(".")
    val nameClass = splitProtoClassNames
        .subList(0, splitProtoClassNames.size - 1)
        .joinToString(".") +
            ".${splitProtoClassNames.last()}OuterClass$${splitProtoClassNames.last()}"
    return Class.forName(nameClass) as Class<Message>
}
*/

@Suppress("UNCHECKED_CAST")
fun Status.getTypeClass(): Class<Message> {
    val nameClass = this.detailsList.first().typeUrl.split("/").last()
    return Class.forName(nameClass) as Class<Message>
}

fun Status.getDetail(): Any {
    val clazz = this.getTypeClass()
    return this.detailsList.first().unpack(clazz)
}

/**
 * gRPC 调用失败的类别，供上层展示错误，不用于自动切换接口。
 */
enum class GrpcErrorKind {
    Authentication,
    RiskControl,
    Network,
    Server,
    Unknown,
}

/**
 * App gRPC 调用异常。
 *
 * @property kind 可用于 UI 分类展示的错误类别。
 * @property grpcStatus gRPC 原始状态。
 */
class BiliGrpcException(
    val kind: GrpcErrorKind,
    override val message: String,
    val grpcStatus: GrpcStatus? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/**
 * B 站业务错误码 → 风控映射。
 *
 * Web 端 HTTP 风控返回 `-352`（见 `BiliResponse` 的识别逻辑）。App gRPC 的
 * `Status.code` 使用同一套业务码，因此复用 `-352` 识别风控。
 */
object BiliGrpcCode {
    /** 风控业务码。 */
    const val RISK_CONTROL = -352
}

/**
 * 解析 gRPC trailer 中的 `bilibili.rpc.Status` 业务信息。
 *
 * @param error 原始 gRPC 异常
 * @return 业务信息（业务码 + 消息），解析失败返回 null
 */
private fun grpcStatusDetail(error: Throwable): BiliDetail? {
    val trailers =
        when (error) {
            is StatusException -> error.trailers
            is StatusRuntimeException -> error.trailers
            else -> null
        } ?: return null
    val statusDetailsKey =
        Metadata.Key.of("grpc-status-details-bin", Metadata.BINARY_BYTE_MARSHALLER)
    val bytes = trailers[statusDetailsKey] ?: return null
    return runCatching {
        val status = bilibili.rpc.Status.parseFrom(bytes)
        // 优先用 bilibili.rpc.Status 顶层业务码/消息（details 为空时也可用）
        if (status.code != 0 || status.message.isNotBlank()) {
            BiliDetail(status.code, status.message)
        } else {
            // 兜底：尝试解包 common.ErrorProto
            when (val detail = status.getDetail()) {
                is bilibili.rpc.Status -> BiliDetail(detail.code, detail.message)
                is common.ErrorProto -> BiliDetail(detail.code, detail.message)
                else -> null
            }
        }
    }.getOrNull()
}

/** gRPC 业务详情：业务码 + 消息。 */
private data class BiliDetail(val code: Int, val message: String)

/**
 * 将 gRPC 异常转换为稳定的业务异常。
 *
 * 该函数只负责分类和保留错误，不执行重试或 Web/App fallback。
 *
 * @param error 原始 gRPC 异常。
 * @throws BiliGrpcException 分类后的异常。
 */
fun handleGrpcException(error: Throwable): Nothing {
    if (error is BiliGrpcException) throw error
    val status = GrpcStatus.fromThrowable(error)
    val biliDetail = grpcStatusDetail(error)
    // 优先按 B 站业务码分类（风控优先，业务码比 gRPC 状态码更精确）
    val kind =
        if (biliDetail?.code == BiliGrpcCode.RISK_CONTROL) {
            GrpcErrorKind.RiskControl
        } else {
            when (status.code) {
                GrpcStatusCode.UNAUTHENTICATED, GrpcStatusCode.PERMISSION_DENIED -> GrpcErrorKind.Authentication
                GrpcStatusCode.UNAVAILABLE, GrpcStatusCode.DEADLINE_EXCEEDED,
                GrpcStatusCode.CANCELLED,
                -> GrpcErrorKind.Network
                GrpcStatusCode.INTERNAL, GrpcStatusCode.RESOURCE_EXHAUSTED -> GrpcErrorKind.Server
                else -> GrpcErrorKind.Unknown
            }
        }
    val message =
        biliDetail?.message?.takeIf { it.isNotBlank() }
            ?: status.description
            ?: "App gRPC request failed"
    throw BiliGrpcException(
        kind = kind,
        message = message,
        grpcStatus = status,
        cause = error,
    )
}
