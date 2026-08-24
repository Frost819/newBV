package dev.frost819.newbv.biliapi

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.login.QrLoginState
import dev.frost819.newbv.biliapi.repositories.LoginRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * [LoginRepository] 二维码登录的集成测试。
 *
 * 非交互式测试验证 QR URL 获取逻辑，
 * 交互式测试（需手动扫码）标记 [Disabled] 不在 CI 中运行。
 */
class BvLoginRepositoryTest {
    private val loginRepository = LoginRepository()

    @Test
    fun `request web qr login url`() {
        runBlocking {
            val qrData = loginRepository.requestWebQrLogin()
            println("web qr url: ${qrData.url}")
            println("web qr key: ${qrData.key}")
            assertThat(qrData.url).isNotEmpty()
            assertThat(qrData.key).isNotEmpty()
        }
    }

    @Test
    fun `check web qr login state returns waiting or expired`() {
        runBlocking {
            val qrData = loginRepository.requestWebQrLogin()
            val result = loginRepository.checkWebQrLoginState(qrData.key)
            println("web qr login state: ${result.state}")
            assertThat(result.state).isAnyOf(
                QrLoginState.WaitingForScan,
                QrLoginState.Expired,
                QrLoginState.Unknown,
            )
        }
    }

    @Test
    fun `request app qr login url`() {
        runBlocking {
            val qrData = loginRepository.requestAppQrLogin()
            println("app qr url: ${qrData.url}")
            println("app qr key: ${qrData.key}")
            assertThat(qrData.url).isNotEmpty()
            assertThat(qrData.key).isNotEmpty()
        }
    }

    @Test
    fun `check app qr login state returns waiting or expired`() {
        runBlocking {
            val qrData = loginRepository.requestAppQrLogin()
            val result = loginRepository.checkAppQrLoginState(qrData.key)
            println("app qr login state: ${result.state}")
            assertThat(result.state).isAnyOf(
                QrLoginState.WaitingForScan,
                QrLoginState.Expired,
                QrLoginState.Unknown,
            )
        }
    }

    @Disabled("交互式 QR 登录测试，需手动扫码，不在 CI 中运行")
    @Test
    fun `web qr login full flow`() {
        runBlocking {
            val qrData = loginRepository.requestWebQrLogin()
            println("请扫描此二维码登录: ${qrData.url}")

            var state = QrLoginState.WaitingForScan
            while (state == QrLoginState.WaitingForScan || state == QrLoginState.WaitingForConfirm) {
                val result = loginRepository.checkWebQrLoginState(qrData.key)
                state = result.state
                println("login state: $state")
                if (state == QrLoginState.Success) {
                    println("登录成功！")
                    println("cookies: ${result.cookies}")
                    assertThat(result.cookies).isNotNull()
                    assertThat(result.cookies!!.sessData).isNotEmpty()
                }
                delay(1000)
            }
        }
    }

    @Disabled("交互式 QR 登录测试，需手动扫码，不在 CI 中运行")
    @Test
    fun `app qr login full flow`() {
        runBlocking {
            val qrData = loginRepository.requestAppQrLogin()
            println("请扫描此二维码登录: ${qrData.url}")

            var state = QrLoginState.WaitingForScan
            while (state == QrLoginState.WaitingForScan || state == QrLoginState.WaitingForConfirm) {
                val result = loginRepository.checkAppQrLoginState(qrData.key)
                state = result.state
                println("login state: $state")
                if (state == QrLoginState.Success) {
                    println("登录成功！")
                    println("accessToken: ${result.accessToken}")
                    println("refreshToken: ${result.refreshToken}")
                    println("cookies: ${result.cookies}")
                    assertThat(result.accessToken).isNotEmpty()
                }
                delay(1000)
            }
        }
    }
}
