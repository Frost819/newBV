package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.grpc.utils.generateChannel
import dev.frost819.newbv.biliapi.http.util.BiliNetworkConfig
import io.grpc.ManagedChannel

/**
 * 管理当前账号使用的 App gRPC channel。
 *
 * Web 请求不依赖此类。账号或 access_token 变化时必须重建 channel，避免继续使用旧账号
 * 的 authorization metadata。
 */
class ChannelRepository {
    /** 当前账号的 App gRPC channel。 */
    var defaultChannel: ManagedChannel? = null
        private set

    /** 地址族偏好变化时让当前 channel 重连，下次 RPC 会用新的地址顺序重新解析。 */
    private var addressFamilyListener: (() -> Unit)? = null

    /**
     * 使用新的 App 凭证初始化 channel。
     *
     * @param accessKey 当前账号的 access_token。
     * @param buvid 当前设备 buvid。
     * @throws IllegalArgumentException access token 或 buvid 为空。
     */
    fun initDefaultChannel(
        accessKey: String,
        buvid: String,
    ) {
        require(accessKey.isNotBlank()) { "access_token is empty" }
        require(buvid.isNotBlank()) { "buvid is empty" }
        close()
        val channel = generateChannel(accessKey, buvid)
        defaultChannel = channel
        // 用户切换网络地址族后，丢弃现有连接，让下一次 RPC 重新解析并应用新顺序
        val listener = { channel.enterIdle() }
        BiliNetworkConfig.addAddressFamilyListener(listener)
        addressFamilyListener = listener
    }

    /**
     * 关闭当前 channel 并清除引用。
     *
     * 账号退出登录或切换账号时调用，避免 gRPC 工作线程泄漏。
     */
    fun close() {
        addressFamilyListener?.let { BiliNetworkConfig.removeAddressFamilyListener(it) }
        addressFamilyListener = null
        defaultChannel?.shutdownNow()
        defaultChannel = null
    }

    /**
     * 获取已初始化的 channel。
     *
     * @return 当前 channel。
     * @throws IllegalStateException 未初始化 App gRPC channel。
     */
    fun requireDefaultChannel(): ManagedChannel =
        defaultChannel ?: throw IllegalStateException("App gRPC channel is not initialized")
}
