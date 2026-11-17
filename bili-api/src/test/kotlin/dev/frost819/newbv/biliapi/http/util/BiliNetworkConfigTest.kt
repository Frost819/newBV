package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.IpFamilyPreference
import org.junit.jupiter.api.Test

/**
 * [BiliNetworkConfig] 的单元测试。
 *
 * 验证地址族偏好变化时通知监听者（gRPC 依赖该回调让 channel 重连）。
 */
class BiliNetworkConfigTest {
    @Test
    fun `changing preference notifies listener once and ignores same value`() {
        val original = BiliNetworkConfig.addressFamily
        var calls = 0
        val listener: () -> Unit = { calls++ }
        BiliNetworkConfig.addAddressFamilyListener(listener)
        try {
            val target =
                if (original == IpFamilyPreference.Ipv4Only) {
                    IpFamilyPreference.PreferIpv4
                } else {
                    IpFamilyPreference.Ipv4Only
                }

            BiliNetworkConfig.addressFamily = target
            BiliNetworkConfig.addressFamily = target

            assertThat(calls).isEqualTo(1)
        } finally {
            BiliNetworkConfig.removeAddressFamilyListener(listener)
            BiliNetworkConfig.addressFamily = original
        }
    }

    @Test
    fun `removed listener is no longer notified`() {
        val original = BiliNetworkConfig.addressFamily
        var calls = 0
        val listener: () -> Unit = { calls++ }
        BiliNetworkConfig.addAddressFamilyListener(listener)
        BiliNetworkConfig.removeAddressFamilyListener(listener)
        try {
            val target =
                if (original == IpFamilyPreference.Ipv4Only) {
                    IpFamilyPreference.PreferIpv4
                } else {
                    IpFamilyPreference.Ipv4Only
                }

            BiliNetworkConfig.addressFamily = target

            assertThat(calls).isEqualTo(0)
        } finally {
            BiliNetworkConfig.addressFamily = original
        }
    }
}
