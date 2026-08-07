package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.login.QrLoginState
import dev.frost819.newbv.biliapi.http.BiliPassportHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.login.CaptchaData
import dev.frost819.newbv.biliapi.http.entity.login.qr.AppQRDataRequest
import dev.frost819.newbv.biliapi.http.entity.login.qr.AppQRLoginData
import dev.frost819.newbv.biliapi.http.entity.login.qr.RequestWebQRData
import dev.frost819.newbv.biliapi.http.entity.login.qr.WebQRLoginData
import dev.frost819.newbv.biliapi.http.entity.login.sms.SendSmsResponse
import dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse
import io.ktor.http.Cookie
import io.ktor.util.date.GMTDate
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [LoginRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliPassportHttpApi] 单例，验证二维码登录流程、验证码申请、
 * 短信发送等逻辑。不依赖真实网络。
 */
class LoginRepositoryUnitTest {
    private lateinit var repository: LoginRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliPassportHttpApi)
        repository = LoginRepository()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliPassportHttpApi)
    }

    // ------------------------------------------------------------------
    // requestWebQrLogin
    // ------------------------------------------------------------------

    @Test
    fun `requestWebQrLogin maps url and qrcodeKey`() =
        runTest {
            coEvery { BiliPassportHttpApi.getWebQRUrl() } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RequestWebQRData(
                            url = "https://example.com/qr",
                            qrcodeKey = "abc123",
                        ),
                )

            val result = repository.requestWebQrLogin()

            assertThat(result.url).isEqualTo("https://example.com/qr")
            assertThat(result.key).isEqualTo("abc123")
        }

    // ------------------------------------------------------------------
    // checkWebQrLoginState
    // ------------------------------------------------------------------

    @Test
    fun `checkWebQrLoginState returns WaitingForScan when data code is 86101`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "",
                        data =
                            WebQRLoginData(
                                url = "",
                                refreshToken = "",
                                timestamp = 0L,
                                code = 86101,
                                message = "请使用B站手机APP扫码登录",
                            ),
                    ),
                    emptyList(),
                )

            val result = repository.checkWebQrLoginState("test-key")

            assertThat(result.state).isEqualTo(QrLoginState.WaitingForScan)
            assertThat(result.cookies).isNull()
        }

    @Test
    fun `checkWebQrLoginState returns WaitingForConfirm when data code is 86090`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "",
                        data =
                            WebQRLoginData(
                                url = "",
                                refreshToken = "",
                                timestamp = 0L,
                                code = 86090,
                                message = "请确认登录",
                            ),
                    ),
                    emptyList(),
                )

            val result = repository.checkWebQrLoginState("test-key")

            assertThat(result.state).isEqualTo(QrLoginState.WaitingForConfirm)
        }

    @Test
    fun `checkWebQrLoginState returns Expired when data code is 86038`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "",
                        data =
                            WebQRLoginData(
                                url = "",
                                refreshToken = "",
                                timestamp = 0L,
                                code = 86038,
                                message = "二维码已失效",
                            ),
                    ),
                    emptyList(),
                )

            val result = repository.checkWebQrLoginState("test-key")

            assertThat(result.state).isEqualTo(QrLoginState.Expired)
        }

    @Test
    fun `checkWebQrLoginState returns Unknown for unrecognized data code`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "",
                        data =
                            WebQRLoginData(
                                url = "",
                                refreshToken = "",
                                timestamp = 0L,
                                code = 99999,
                                message = "未知状态",
                            ),
                    ),
                    emptyList(),
                )

            val result = repository.checkWebQrLoginState("test-key")

            assertThat(result.state).isEqualTo(QrLoginState.Unknown)
        }

    @Test
    fun `checkWebQrLoginState returns Success with cookies when data code is 0`() =
        runTest {
            val cookies =
                listOf(
                    Cookie(name = "DedeUserID", value = "12345", expires = GMTDate(0)),
                    Cookie(name = "DedeUserID__ckMd5", value = "abc", expires = null),
                    Cookie(name = "sid", value = "sid123", expires = null),
                    Cookie(name = "bili_jct", value = "jct456", expires = null),
                    Cookie(name = "SESSDATA", value = "sess789", expires = null),
                )
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "成功",
                        data =
                            WebQRLoginData(
                                url = "https://example.com",
                                refreshToken = "rt",
                                timestamp = 0L,
                                code = 0,
                                message = "成功",
                            ),
                    ),
                    cookies,
                )

            val result = repository.checkWebQrLoginState("test-key")

            assertThat(result.state).isEqualTo(QrLoginState.Success)
            assertThat(result.cookies).isNotNull()
            assertThat(result.cookies!!.dedeUserId).isEqualTo(12345L)
            assertThat(result.cookies!!.dedeUserIdCkMd5).isEqualTo("abc")
            assertThat(result.cookies!!.sid).isEqualTo("sid123")
            assertThat(result.cookies!!.biliJct).isEqualTo("jct456")
            assertThat(result.cookies!!.sessData).isEqualTo("sess789")
        }

    // ------------------------------------------------------------------
    // requestAppQrLogin
    // ------------------------------------------------------------------

    @Test
    fun `requestAppQrLogin maps url and authCode`() =
        runTest {
            coEvery { BiliPassportHttpApi.getAppQRUrl(any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        AppQRDataRequest(
                            url = "https://example.com/app-qr",
                            authCode = "auth-code-xyz",
                        ),
                )

            val result = repository.requestAppQrLogin()

            assertThat(result.url).isEqualTo("https://example.com/app-qr")
            assertThat(result.key).isEqualTo("auth-code-xyz")
        }

    // ------------------------------------------------------------------
    // checkAppQrLoginState
    // ------------------------------------------------------------------

    @Test
    fun `checkAppQrLoginState returns WaitingForScan when response code is 86039`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithAppQR(any(), any(), any()) } returns
                BiliResponse(code = 86039, message = "请使用B站手机APP扫码登录")

            val result = repository.checkAppQrLoginState("test-auth-code")

            assertThat(result.state).isEqualTo(QrLoginState.WaitingForScan)
            assertThat(result.cookies).isNull()
        }

    @Test
    fun `checkAppQrLoginState returns WaitingForConfirm when response code is 86090`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithAppQR(any(), any(), any()) } returns
                BiliResponse(code = 86090, message = "请确认登录")

            val result = repository.checkAppQrLoginState("test-auth-code")

            assertThat(result.state).isEqualTo(QrLoginState.WaitingForConfirm)
        }

    @Test
    fun `checkAppQrLoginState returns Expired when response code is 86038`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithAppQR(any(), any(), any()) } returns
                BiliResponse(code = 86038, message = "二维码已失效")

            val result = repository.checkAppQrLoginState("test-auth-code")

            assertThat(result.state).isEqualTo(QrLoginState.Expired)
        }

    @Test
    fun `checkAppQrLoginState returns Unknown for unrecognized response code`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithAppQR(any(), any(), any()) } returns
                BiliResponse(code = 99999, message = "未知状态")

            val result = repository.checkAppQrLoginState("test-auth-code")

            assertThat(result.state).isEqualTo(QrLoginState.Unknown)
        }

    @Test
    fun `checkAppQrLoginState returns Success with cookies and tokens when response code is 0`() =
        runTest {
            val appQrLoginData =
                AppQRLoginData(
                    isNew = false,
                    mid = 12345L,
                    accessToken = "access-token-123",
                    refreshToken = "refresh-token-456",
                    expiresIn = 7776000,
                    tokenInfo =
                        AppQRLoginData.TokenInfo(
                            mid = 12345L,
                            expiresIn = 7776000,
                            accessToken = "access-token-123",
                            refreshToken = "refresh-token-456",
                        ),
                    cookieInfo =
                        AppQRLoginData.CookieInfo(
                            cookies =
                                listOf(
                                    AppQRLoginData.CookieInfo.Cookie(
                                        name = "DedeUserID",
                                        value = "12345",
                                        httpOnly = 1,
                                        expires = 1700000000,
                                        secure = 1,
                                    ),
                                    AppQRLoginData.CookieInfo.Cookie(
                                        name = "DedeUserID__ckMd5",
                                        value = "ckmd5abc",
                                        httpOnly = 1,
                                        expires = 0,
                                        secure = 1,
                                    ),
                                    AppQRLoginData.CookieInfo.Cookie(
                                        name = "sid",
                                        value = "sid123",
                                        httpOnly = 1,
                                        expires = 0,
                                        secure = 1,
                                    ),
                                    AppQRLoginData.CookieInfo.Cookie(
                                        name = "bili_jct",
                                        value = "jct456",
                                        httpOnly = 1,
                                        expires = 0,
                                        secure = 1,
                                    ),
                                    AppQRLoginData.CookieInfo.Cookie(
                                        name = "SESSDATA",
                                        value = "sess789",
                                        httpOnly = 1,
                                        expires = 0,
                                        secure = 1,
                                    ),
                                ),
                            domains = listOf(".bilibili.com"),
                        ),
                )
            coEvery { BiliPassportHttpApi.loginWithAppQR(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "成功", data = appQrLoginData)

            val result = repository.checkAppQrLoginState("test-auth-code")

            assertThat(result.state).isEqualTo(QrLoginState.Success)
            assertThat(result.cookies).isNotNull()
            assertThat(result.cookies!!.dedeUserId).isEqualTo(12345L)
            assertThat(result.cookies!!.dedeUserIdCkMd5).isEqualTo("ckmd5abc")
            assertThat(result.cookies!!.sid).isEqualTo("sid123")
            assertThat(result.cookies!!.biliJct).isEqualTo("jct456")
            assertThat(result.cookies!!.sessData).isEqualTo("sess789")
            assertThat(result.accessToken).isEqualTo("access-token-123")
            assertThat(result.refreshToken).isEqualTo("refresh-token-456")
        }

    // ------------------------------------------------------------------
    // checkWebQrLoginState - error cases
    // ------------------------------------------------------------------

    @Test
    fun `checkWebQrLoginState throws IllegalArgumentException when DedeUserID cookie is missing on success`() =
        runTest {
            val cookies =
                listOf(
                    Cookie(name = "DedeUserID__ckMd5", value = "abc", expires = null),
                    Cookie(name = "sid", value = "sid123", expires = null),
                    Cookie(name = "bili_jct", value = "jct456", expires = null),
                    Cookie(name = "SESSDATA", value = "sess789", expires = null),
                )
            coEvery { BiliPassportHttpApi.loginWithWebQR(any()) } returns
                Pair(
                    BiliResponse(
                        code = 0,
                        message = "成功",
                        data =
                            WebQRLoginData(
                                url = "https://example.com",
                                refreshToken = "rt",
                                timestamp = 0L,
                                code = 0,
                                message = "成功",
                            ),
                    ),
                    cookies,
                )

            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                repository.checkWebQrLoginState("test-key")
            }
        }

    // ------------------------------------------------------------------
    // getCaptcha
    // ------------------------------------------------------------------

    @Test
    fun `getCaptcha maps token challenge and gt`() =
        runTest {
            coEvery { BiliPassportHttpApi.getCaptcha(any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        CaptchaData(
                            type = "geetest",
                            token = "captcha-token",
                            geetest = CaptchaData.Geetest(challenge = "challenge-key", gt = "gt-id"),
                            tencent = CaptchaData.Tencent(appId = ""),
                        ),
                )

            val result = repository.getCaptcha()

            assertThat(result.token).isEqualTo("captcha-token")
            assertThat(result.challenge).isEqualTo("challenge-key")
            assertThat(result.gt).isEqualTo("gt-id")
        }

    // ------------------------------------------------------------------
    // generateLoginSessionId
    // ------------------------------------------------------------------

    @Test
    fun `generateLoginSessionId returns UUID without dashes`() {
        val sessionId = repository.generateLoginSessionId()

        assertThat(sessionId).doesNotContain("-")
        assertThat(sessionId.length).isEqualTo(32)
    }

    // ------------------------------------------------------------------
    // requestSms
    // ------------------------------------------------------------------

    @Test
    fun `requestSms returns Success when captchaKey is not empty`() =
        runTest {
            coEvery {
                BiliPassportHttpApi.sendSms(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = SendSmsResponse(captchaKey = "sms-key-123", recaptchaUrl = ""),
                )

            val result =
                repository.requestSms(
                    phone = 13800138000L,
                    loginSessionId = "session-id",
                    buvid = "buvid-xxx",
                )

            assertThat(result.state).isEqualTo(SendSmsState.Success)
            assertThat(result.captchaKey).isEqualTo("sms-key-123")
        }

    @Test
    fun `requestSms returns RecaptchaRequire when captchaKey is empty`() =
        runTest {
            coEvery {
                BiliPassportHttpApi.sendSms(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = SendSmsResponse(captchaKey = "", recaptchaUrl = "https://recaptcha.example.com"),
                )

            val result =
                repository.requestSms(
                    phone = 13800138000L,
                    loginSessionId = "session-id",
                    buvid = "buvid-xxx",
                )

            assertThat(result.state).isEqualTo(SendSmsState.RecaptchaRequire)
            assertThat(result.recaptchaUrl).isEqualTo("https://recaptcha.example.com")
        }

    @Test
    fun `requestSms returns Error when response code is non-zero`() =
        runTest {
            coEvery {
                BiliPassportHttpApi.sendSms(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = -105,
                    message = "手机号格式错误",
                    data = null,
                )

            val result =
                repository.requestSms(
                    phone = 123L,
                    loginSessionId = "session-id",
                    buvid = "buvid-xxx",
                )

            assertThat(result.state).isEqualTo(SendSmsState.Error)
            assertThat(result.message).isEqualTo("手机号格式错误")
        }

    // ------------------------------------------------------------------
    // loginWithSms
    // ------------------------------------------------------------------

    @Test
    fun `loginWithSms maps response to SmsLoginResult`() =
        runTest {
            coEvery { BiliPassportHttpApi.loginWithSms(any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        SmsLoginResponse(
                            status = 0,
                            message = "",
                            url = "",
                            tokenInfo =
                                SmsLoginResponse.TokenInfo(
                                    mid = 12345L,
                                    expiresIn = 7776000,
                                    accessToken = "access-token-123",
                                    refreshToken = "refresh-token-456",
                                ),
                            cookieInfo =
                                SmsLoginResponse.CookieInfo(
                                    cookies =
                                        listOf(
                                            SmsLoginResponse.CookieInfo.Cookie(
                                                name = "DedeUserID",
                                                value = "12345",
                                                httpOnly = 1,
                                                expires = 1700000000,
                                                secure = 1,
                                                sameSite = 0,
                                            ),
                                            SmsLoginResponse.CookieInfo.Cookie(
                                                name = "DedeUserID__ckMd5",
                                                value = "ckmd5abc",
                                                httpOnly = 1,
                                                expires = 0,
                                                secure = 1,
                                                sameSite = 0,
                                            ),
                                            SmsLoginResponse.CookieInfo.Cookie(
                                                name = "sid",
                                                value = "sid123",
                                                httpOnly = 1,
                                                expires = 0,
                                                secure = 1,
                                                sameSite = 0,
                                            ),
                                            SmsLoginResponse.CookieInfo.Cookie(
                                                name = "bili_jct",
                                                value = "jct456",
                                                httpOnly = 1,
                                                expires = 0,
                                                secure = 1,
                                                sameSite = 0,
                                            ),
                                            SmsLoginResponse.CookieInfo.Cookie(
                                                name = "SESSDATA",
                                                value = "sess789",
                                                httpOnly = 1,
                                                expires = 0,
                                                secure = 1,
                                                sameSite = 0,
                                            ),
                                        ),
                                    domains = listOf(".bilibili.com"),
                                ),
                            sso = emptyList(),
                            isNew = false,
                            isTourist = false,
                        ),
                )

            val result =
                repository.loginWithSms(
                    phone = 13800138000L,
                    loginSessionId = "session-id",
                    code = 123456,
                    captchaKey = "captcha-key",
                )

            assertThat(result.status).isEqualTo(0)
            assertThat(result.accessToken).isEqualTo("access-token-123")
            assertThat(result.refreshToken).isEqualTo("refresh-token-456")
            assertThat(result.sessData).isEqualTo("sess789")
            assertThat(result.biliJct).isEqualTo("jct456")
            assertThat(result.dedeUserId).isEqualTo(12345L)
            assertThat(result.dedeUserIdCkMd5).isEqualTo("ckmd5abc")
            assertThat(result.sid).isEqualTo("sid123")
        }
}
