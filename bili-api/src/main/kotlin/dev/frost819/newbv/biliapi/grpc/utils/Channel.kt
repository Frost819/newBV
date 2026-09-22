package dev.frost819.newbv.biliapi.grpc.utils

import bilibili.metadata.device.device
import bilibili.metadata.locale.locale
import bilibili.metadata.metadata
import bilibili.metadata.network.NetworkType
import bilibili.metadata.network.network
import dev.frost819.newbv.biliapi.http.util.BiliAppConf
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.TimeUnit
import io.grpc.Metadata as GrpcMetadata

/** 单个 gRPC 调用的默认截止时间（秒），防止连接半开或网络异常时无限等待。 */
private const val GRPC_DEADLINE_SECONDS = 20L

fun generateChannel(
    accessKey: String,
    buvid: String,
    endPoint: String = BiliAppConf.GRPC_HOST,
    port: Int = BiliAppConf.GRPC_PORT,
    enableTransportSecurity: Boolean = true,
): ManagedChannel {
    val builder = ManagedChannelBuilder.forAddress(endPoint, port)
    if (enableTransportSecurity) {
        builder.useTransportSecurity()
    } else {
        builder.usePlaintext()
    }
    // 定期保活以及时发现被 NAT/防火墙静默断开的连接，避免请求挂起
    builder.keepAliveTime(30, TimeUnit.SECONDS)
    builder.keepAliveTimeout(10, TimeUnit.SECONDS)
    builder.keepAliveWithoutCalls(false)
    return builder
        .executor(Dispatchers.IO.asExecutor())
        .intercept(MetadataInterceptor(accessKey, buvid))
        .build()
}

private class MetadataInterceptor(
    private val accessKey: String,
    private val buvid: String,
) : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        // 调用方未显式设置截止时间时，附加默认截止时间，避免请求无限挂起
        val options =
            if (callOptions.deadline == null) {
                callOptions.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
            } else {
                callOptions
            }
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, options)) {
            override fun start(
                responseListener: Listener<RespT>,
                headers: GrpcMetadata,
            ) {
                headers.apply {
                    putAuthorization(accessKey)
                    putMetadataBin(accessKey, buvid)
                    putDeviceBin(buvid)
                    putLocalBin()
                    putNetworkBin()
                }
                super.start(responseListener, headers)
            }
        }
    }
}

fun GrpcMetadata.putAuthorization(accessKey: String) {
    put(
        GrpcMetadata.Key.of("authorization", GrpcMetadata.ASCII_STRING_MARSHALLER),
        "identify_v1 $accessKey",
    )
}

fun GrpcMetadata.putMetadataBin(
    accessKey: String,
    buvid: String,
) {
    put(
        GrpcMetadata.Key.of("x-bili-metadata-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        metadata {
            this.accessKey = accessKey
            mobiApp = BiliAppConf.MOBI_APP
            device = BiliAppConf.DEVICE
            build = BiliAppConf.APP_BUILD_CODE
            channel = BiliAppConf.CHANNEL
            this.buvid = buvid
            platform = BiliAppConf.PLATFORM
        }.toByteArray(),
    )
}

fun GrpcMetadata.putDeviceBin(buvid: String) {
    put(
        io.grpc.Metadata.Key
            .of("x-bili-device-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        device {
            appId = BiliAppConf.APP_ID
            mobiApp = BiliAppConf.MOBI_APP
            device = BiliAppConf.DEVICE
            build = BiliAppConf.APP_BUILD_CODE
            channel = BiliAppConf.CHANNEL
            this.buvid = buvid
            platform = BiliAppConf.PLATFORM
        }.toByteArray(),
    )
}

fun GrpcMetadata.putLocalBin() {
    put(
        io.grpc.Metadata.Key
            .of("x-bili-local-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        locale {
            timezone = BiliAppConf.TIMEZONE
        }.toByteArray(),
    )
}

fun GrpcMetadata.putNetworkBin() {
    put(
        io.grpc.Metadata.Key
            .of("x-bili-network-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        network {
            type = NetworkType.WIFI
        }.toByteArray(),
    )
}
