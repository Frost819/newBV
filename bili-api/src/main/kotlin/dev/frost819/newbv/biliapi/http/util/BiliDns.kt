package dev.frost819.newbv.biliapi.http.util

import dev.frost819.newbv.biliapi.entity.IpFamilyPreference
import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 全局网络地址族偏好。
 *
 * 由 app 层在启动与设置变更时下发；[BiliDns] 每次解析时读取，因此修改后无需重建
 * OkHttpClient / gRPC Channel 即可生效。
 */
object BiliNetworkConfig {
    private val addressFamilyListeners = CopyOnWriteArrayList<() -> Unit>()

    /** 当前地址族偏好，默认跟随系统。修改时通知监听者（如让 gRPC 重连）。 */
    @Volatile
    var addressFamily: IpFamilyPreference = IpFamilyPreference.Auto
        set(value) {
            if (field == value) return
            field = value
            addressFamilyListeners.forEach { it() }
        }

    /**
     * 注册地址族偏好变化监听。
     *
     * @param listener 偏好变化时回调（保证在设置变更的线程上同步执行）
     */
    fun addAddressFamilyListener(listener: () -> Unit) {
        addressFamilyListeners.add(listener)
    }

    /**
     * 移除地址族偏好变化监听。
     *
     * @param listener 之前注册的监听器
     */
    fun removeAddressFamilyListener(listener: () -> Unit) {
        addressFamilyListeners.remove(listener)
    }
}

/**
 * 按 [BiliNetworkConfig.addressFamily] 调整 DNS 解析结果的 OkHttp [Dns]。
 *
 * 背景：Android 在设备拿到 IPv6 地址时会按 RFC 6724 把 AAAA 排在 A 之前；当路由器
 * 分配了 IPv6 但出口异常时，OkHttp 会先对 IPv6 地址串行等待连接超时，叠加 App 层
 * 10 秒超时后表现为「加载失败」（issue #285）。
 *
 * 所有 OkHttp 客户端（HTTP API / 播放器媒体 / WebSocket / 图片）都应挂载本实例。
 */
object BiliDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        orderAddresses(
            addresses = Dns.SYSTEM.lookup(hostname),
            preference = BiliNetworkConfig.addressFamily,
        )
}

/**
 * 按偏好重排 / 过滤 DNS 结果。
 *
 * - [IpFamilyPreference.Auto]：保持系统顺序；
 * - [IpFamilyPreference.PreferIpv4]：IPv4 在前、IPv6 在后，保留两者作为回退；
 * - [IpFamilyPreference.PreferIpv6]：IPv6 在前、IPv4 在后，保留两者作为回退；
 * - [IpFamilyPreference.Ipv4Only]：只保留 IPv4；若域名只有 AAAA 记录则回退全部结果，
 *   避免 IPv6-only 网络下直接不可用。
 *
 * @param addresses 系统解析结果
 * @param preference 地址族偏好
 * @return 重排 / 过滤后的地址列表
 */
internal fun orderAddresses(
    addresses: List<InetAddress>,
    preference: IpFamilyPreference,
): List<InetAddress> =
    when (preference) {
        IpFamilyPreference.Auto -> addresses
        IpFamilyPreference.PreferIpv4 ->
            addresses.filterIsInstance<Inet4Address>() + addresses.filterIsInstance<Inet6Address>()
        IpFamilyPreference.PreferIpv6 ->
            addresses.filterIsInstance<Inet6Address>() + addresses.filterIsInstance<Inet4Address>()
        IpFamilyPreference.Ipv4Only -> addresses.filterIsInstance<Inet4Address>().ifEmpty { addresses }
    }
