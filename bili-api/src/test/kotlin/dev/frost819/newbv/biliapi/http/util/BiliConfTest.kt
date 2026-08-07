package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [BiliWebConf] 与 [BiliAppConf] 常量与默认值的单元测试。
 */
class BiliConfTest {
    @Test
    fun `BiliWebConf default webViewVersion is 144`() {
        assertThat(BiliWebConf.webViewVersion).isEqualTo(144)
    }

    @Test
    fun `BiliWebConf webViewVersion can be changed`() {
        val original = BiliWebConf.webViewVersion
        BiliWebConf.webViewVersion = 150
        assertThat(BiliWebConf.webViewVersion).isEqualTo(150)
        BiliWebConf.webViewVersion = original
    }

    @Test
    fun `BiliAppConf GRPC_HOST is correct`() {
        assertThat(BiliAppConf.GRPC_HOST).isEqualTo("grpc.biliapi.net")
    }

    @Test
    fun `BiliAppConf GRPC_PORT is 443`() {
        assertThat(BiliAppConf.GRPC_PORT).isEqualTo(443)
    }

    @Test
    fun `BiliAppConf APP_ID is 5`() {
        assertThat(BiliAppConf.APP_ID).isEqualTo(5)
    }

    @Test
    fun `BiliAppConf APP_BUILD_CODE is 2020100`() {
        assertThat(BiliAppConf.APP_BUILD_CODE).isEqualTo(2020100)
    }

    @Test
    fun `BiliAppConf APP_VERSION_NAME is correct`() {
        assertThat(BiliAppConf.APP_VERSION_NAME).isEqualTo("2.2.0")
    }

    @Test
    fun `BiliAppConf CHANNEL is yingyongbao`() {
        assertThat(BiliAppConf.CHANNEL).isEqualTo("yingyongbao")
    }

    @Test
    fun `BiliAppConf MOBI_APP is android_hd`() {
        assertThat(BiliAppConf.MOBI_APP).isEqualTo("android_hd")
    }

    @Test
    fun `BiliAppConf PLATFORM is android`() {
        assertThat(BiliAppConf.PLATFORM).isEqualTo("android")
    }

    @Test
    fun `BiliAppConf TIMEZONE is Asia Shanghai`() {
        assertThat(BiliAppConf.TIMEZONE).isEqualTo("Asia/Shanghai")
    }

    @Test
    fun `BiliAppConf NETWORK is 2`() {
        assertThat(BiliAppConf.NETWORK).isEqualTo(2)
    }
}
