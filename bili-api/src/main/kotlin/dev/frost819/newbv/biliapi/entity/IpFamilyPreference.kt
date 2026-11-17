package dev.frost819.newbv.biliapi.entity

/**
 * 网络地址族偏好。
 *
 * 通过 ordinal 持久化到 Prefs，越界回退到 [Auto]。
 * 用于规避「路由器分配了 IPv6 但 IPv6 出口异常」时，B 站域名优先解析到 IPv6
 * 导致的连接超时（issue #285）。
 */
enum class IpFamilyPreference {
    /** 使用系统返回的地址顺序（Android 通常按 RFC 6724 优先 IPv6）。 */
    Auto,

    /** IPv4 地址优先尝试，全部失败后再回退 IPv6。 */
    PreferIpv4,

    /** IPv6 地址优先尝试，全部失败后再回退 IPv4。 */
    PreferIpv6,

    /** 仅使用 IPv4；域名没有 A 记录时回退系统结果，避免 IPv6-only 网络完全不可用。 */
    Ipv4Only,
    ;

    companion object {
        /** 从序号安全解析，越界返回 [Auto]。 */
        fun fromOrdinal(ordinal: Int): IpFamilyPreference = entries.getOrElse(ordinal) { Auto }
    }
}
