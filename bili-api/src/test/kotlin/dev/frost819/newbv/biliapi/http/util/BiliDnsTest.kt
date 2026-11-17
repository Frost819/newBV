package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.IpFamilyPreference
import org.junit.jupiter.api.Test
import java.net.InetAddress

/**
 * [orderAddresses] 的单元测试。
 *
 * 验证各地址族偏好下的重排 / 过滤规则（issue #285）：
 * 自动保持系统顺序、优先模式重排、仅 IPv4 过滤、无 A 记录时回退系统结果。
 */
class BiliDnsTest {
    private val ipv4a = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))
    private val ipv4b = InetAddress.getByAddress(byteArrayOf(2, 2, 2, 2))
    private val ipv6a = InetAddress.getByAddress(ByteArray(16).also { it[15] = 1 })
    private val ipv6b = InetAddress.getByAddress(ByteArray(16).also { it[15] = 2 })

    @Test
    fun `auto keeps system order`() {
        val addresses = listOf(ipv6a, ipv4a, ipv6b, ipv4b)

        val result = orderAddresses(addresses, IpFamilyPreference.Auto)

        assertThat(result).containsExactly(ipv6a, ipv4a, ipv6b, ipv4b).inOrder()
    }

    @Test
    fun `prefer ipv4 puts v4 first and keeps v6 as fallback`() {
        val addresses = listOf(ipv6a, ipv4a, ipv6b, ipv4b)

        val result = orderAddresses(addresses, IpFamilyPreference.PreferIpv4)

        assertThat(result).containsExactly(ipv4a, ipv4b, ipv6a, ipv6b).inOrder()
    }

    @Test
    fun `prefer ipv6 puts v6 first and keeps v4 as fallback`() {
        val addresses = listOf(ipv4a, ipv6a, ipv4b, ipv6b)

        val result = orderAddresses(addresses, IpFamilyPreference.PreferIpv6)

        assertThat(result).containsExactly(ipv6a, ipv6b, ipv4a, ipv4b).inOrder()
    }

    @Test
    fun `ipv4 only filters out v6 addresses`() {
        val addresses = listOf(ipv6a, ipv4a, ipv6b, ipv4b)

        val result = orderAddresses(addresses, IpFamilyPreference.Ipv4Only)

        assertThat(result).containsExactly(ipv4a, ipv4b).inOrder()
    }

    @Test
    fun `ipv4 only falls back to system result when no v4 address exists`() {
        val addresses = listOf(ipv6a, ipv6b)

        val result = orderAddresses(addresses, IpFamilyPreference.Ipv4Only)

        assertThat(result).containsExactly(ipv6a, ipv6b).inOrder()
    }

    @Test
    fun `fromOrdinal falls back to auto for unknown value`() {
        assertThat(IpFamilyPreference.fromOrdinal(0)).isEqualTo(IpFamilyPreference.Auto)
        assertThat(IpFamilyPreference.fromOrdinal(1)).isEqualTo(IpFamilyPreference.PreferIpv4)
        assertThat(IpFamilyPreference.fromOrdinal(2)).isEqualTo(IpFamilyPreference.PreferIpv6)
        assertThat(IpFamilyPreference.fromOrdinal(3)).isEqualTo(IpFamilyPreference.Ipv4Only)
        assertThat(IpFamilyPreference.fromOrdinal(99)).isEqualTo(IpFamilyPreference.Auto)
        assertThat(IpFamilyPreference.fromOrdinal(-1)).isEqualTo(IpFamilyPreference.Auto)
    }
}
