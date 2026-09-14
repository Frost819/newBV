package dev.frost819.newbv.biliapi.entity.login

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Date

/**
 * [QrLoginData]、[QrLoginResult]、[WebCookies]、[Captcha]、[SmsLoginResult]
 * 实体的单元测试。
 */
class LoginEntityTest {
    @Test
    fun `QrLoginData holds url and key`() {
        val data = QrLoginData(url = "https://example.com/qr", key = "abc123")
        assertThat(data.url).isEqualTo("https://example.com/qr")
        assertThat(data.key).isEqualTo("abc123")
    }

    @Test
    fun `QrLoginResult Success state has optional fields null by default`() {
        val result = QrLoginResult(state = QrLoginState.Success)
        assertThat(result.state).isEqualTo(QrLoginState.Success)
        assertThat(result.accessToken).isNull()
        assertThat(result.refreshToken).isNull()
        assertThat(result.cookies).isNull()
    }

    @Test
    fun `QrLoginResult with all fields populated`() {
        val cookies =
            WebCookies(
                dedeUserId = 123L,
                dedeUserIdCkMd5 = "md5hash",
                sid = "session-id",
                biliJct = "jct-value",
                sessData = "sess-value",
                expiredDate = Date(1700000000000L),
            )
        val result =
            QrLoginResult(
                state = QrLoginState.Success,
                accessToken = "at",
                refreshToken = "rt",
                cookies = cookies,
            )
        assertThat(result.accessToken).isEqualTo("at")
        assertThat(result.refreshToken).isEqualTo("rt")
        assertThat(result.cookies!!.dedeUserId).isEqualTo(123L)
        assertThat(result.cookies!!.sessData).isEqualTo("sess-value")
    }

    @Test
    fun `QrLoginState has all expected values`() {
        assertThat(QrLoginState.entries).containsAtLeast(
            QrLoginState.Ready,
            QrLoginState.WaitingForScan,
            QrLoginState.WaitingForConfirm,
            QrLoginState.Expired,
            QrLoginState.Success,
            QrLoginState.Error,
            QrLoginState.Unknown,
        )
    }

    @Test
    fun `Captcha holds all fields`() {
        val captcha = Captcha(token = "tok", challenge = "ch", gt = "gt-id")
        assertThat(captcha.token).isEqualTo("tok")
        assertThat(captcha.challenge).isEqualTo("ch")
        assertThat(captcha.gt).isEqualTo("gt-id")
    }

    @Test
    fun `SmsLoginResult fromSmsLoginResponse maps all fields`() {
        val response =
            dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse(
                status = 0,
                message = "ok",
                url = "",
                tokenInfo =
                    dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.TokenInfo(
                        mid = 100L,
                        expiresIn = 3600,
                        accessToken = "access-tok",
                        refreshToken = "refresh-tok",
                    ),
                cookieInfo =
                    dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo(
                        cookies =
                            listOf(
                                dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo.Cookie(
                                    name = "SESSDATA",
                                    value = "sess-value",
                                    httpOnly = 1,
                                    expires = 0,
                                    secure = 1,
                                    sameSite = 0,
                                ),
                                dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo.Cookie(
                                    name = "bili_jct",
                                    value = "jct-value",
                                    httpOnly = 1,
                                    expires = 0,
                                    secure = 1,
                                    sameSite = 0,
                                ),
                                dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo.Cookie(
                                    name = "DedeUserID",
                                    value = "123456",
                                    httpOnly = 1,
                                    expires = 0,
                                    secure = 1,
                                    sameSite = 0,
                                ),
                                dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo.Cookie(
                                    name = "DedeUserID__ckMd5",
                                    value = "ckmd5",
                                    httpOnly = 1,
                                    expires = 0,
                                    secure = 1,
                                    sameSite = 0,
                                ),
                                dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo.Cookie(
                                    name = "sid",
                                    value = "sid-val",
                                    httpOnly = 1,
                                    expires = 0,
                                    secure = 1,
                                    sameSite = 0,
                                ),
                            ),
                        domains = emptyList(),
                    ),
                sso = emptyList(),
                isNew = false,
                isTourist = false,
            )

        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.status).isEqualTo(0)
        assertThat(result.message).isEqualTo("ok")
        assertThat(result.accessToken).isEqualTo("access-tok")
        assertThat(result.refreshToken).isEqualTo("refresh-tok")
        assertThat(result.sessData).isEqualTo("sess-value")
        assertThat(result.biliJct).isEqualTo("jct-value")
        assertThat(result.dedeUserId).isEqualTo(123456L)
        assertThat(result.dedeUserIdCkMd5).isEqualTo("ckmd5")
        assertThat(result.sid).isEqualTo("sid-val")
    }

    @Test
    fun `SmsLoginResult fromSmsLoginResponse uses empty string when cookie missing`() {
        val response =
            dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse(
                status = 0,
                message = "ok",
                url = "",
                tokenInfo =
                    dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.TokenInfo(
                        mid = 1L,
                        expiresIn = 7200,
                        accessToken = "at",
                        refreshToken = "rt",
                    ),
                cookieInfo =
                    dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse.CookieInfo(
                        cookies = emptyList(),
                        domains = emptyList(),
                    ),
                sso = emptyList(),
                isNew = false,
                isTourist = false,
            )

        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.sessData).isEmpty()
        assertThat(result.biliJct).isEmpty()
        assertThat(result.dedeUserId).isEqualTo(0L)
        assertThat(result.dedeUserIdCkMd5).isEmpty()
        assertThat(result.sid).isEmpty()
    }
}
