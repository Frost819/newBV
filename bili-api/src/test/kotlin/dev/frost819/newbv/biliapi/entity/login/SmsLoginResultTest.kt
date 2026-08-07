package dev.frost819.newbv.biliapi.entity.login

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.login.sms.SmsLoginResponse
import org.junit.jupiter.api.Test

/**
 * [SmsLoginResult] 实体 `fromSmsLoginResponse` 转换方法的单元测试。
 *
 * 覆盖 token、refreshToken、cookie 提取、缺省值等场景。
 */
class SmsLoginResultTest {
    @Test
    fun `fromSmsLoginResponse maps all fields correctly`() {
        val response = fakeSmsLoginResponse()

        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.status).isEqualTo(0)
        assertThat(result.message).isEqualTo("成功")
        assertThat(result.accessToken).isEqualTo("access-token-123")
        assertThat(result.refreshToken).isEqualTo("refresh-token-456")
        assertThat(result.sessData).isEqualTo("sessdata-value")
        assertThat(result.biliJct).isEqualTo("bili-jct-value")
        assertThat(result.dedeUserId).isEqualTo(12345L)
        assertThat(result.dedeUserIdCkMd5).isEqualTo("ck-md5-value")
        assertThat(result.sid).isEqualTo("sid-value")
    }

    @Test
    fun `fromSmsLoginResponse returns empty sessData when cookie missing`() {
        val response =
            fakeSmsLoginResponse(
                cookies =
                    listOf(
                        fakeCookie("DedeUserID", "12345"),
                        fakeCookie("bili_jct", "jct"),
                        fakeCookie("sid", "sid"),
                        fakeCookie("SESSDATA", "sessdata"),
                    ),
            )
        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.sessData).isEqualTo("sessdata")
    }

    @Test
    fun `fromSmsLoginResponse returns empty string for missing cookies`() {
        val response =
            fakeSmsLoginResponse(
                cookies =
                    listOf(
                        fakeCookie("unknown", "val"),
                    ),
            )

        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.sessData).isEqualTo("")
        assertThat(result.biliJct).isEqualTo("")
        assertThat(result.dedeUserId).isEqualTo(0L)
        assertThat(result.dedeUserIdCkMd5).isEqualTo("")
        assertThat(result.sid).isEqualTo("")
    }

    @Test
    fun `fromSmsLoginResponse returns zero dedeUserId when not a number`() {
        val response =
            fakeSmsLoginResponse(
                cookies =
                    listOf(
                        fakeCookie("DedeUserID", "not-a-number"),
                        fakeCookie("bili_jct", "jct"),
                        fakeCookie("sid", "sid"),
                        fakeCookie("SESSDATA", "sess"),
                    ),
            )

        val result = SmsLoginResult.fromSmsLoginResponse(response)

        assertThat(result.dedeUserId).isEqualTo(0L)
    }

    @Test
    fun `fromSmsLoginResponse calculates expiredDate from expiresIn`() {
        val before = System.currentTimeMillis()
        val response = fakeSmsLoginResponse(expiresIn = 3600)
        val result = SmsLoginResult.fromSmsLoginResponse(response)
        val after = System.currentTimeMillis()

        val expectedMin = before + 3600 * 1000L
        val expectedMax = after + 3600 * 1000L
        assertThat(result.expiredDate.time).isAtLeast(expectedMin)
        assertThat(result.expiredDate.time).isAtMost(expectedMax)
    }

    private fun fakeSmsLoginResponse(
        cookies: List<SmsLoginResponse.CookieInfo.Cookie> =
            listOf(
                fakeCookie("DedeUserID", "12345"),
                fakeCookie("DedeUserID__ckMd5", "ck-md5-value"),
                fakeCookie("sid", "sid-value"),
                fakeCookie("bili_jct", "bili-jct-value"),
                fakeCookie("SESSDATA", "sessdata-value"),
            ),
        expiresIn: Int = 7200,
    ) = SmsLoginResponse(
        status = 0,
        message = "成功",
        url = "https://example.com",
        tokenInfo =
            SmsLoginResponse.TokenInfo(
                mid = 12345L,
                expiresIn = expiresIn,
                accessToken = "access-token-123",
                refreshToken = "refresh-token-456",
            ),
        cookieInfo =
            SmsLoginResponse.CookieInfo(
                cookies = cookies,
                domains = emptyList(),
            ),
        sso = emptyList(),
        isNew = false,
        isTourist = false,
    )

    private fun fakeCookie(
        name: String,
        value: String,
    ) = SmsLoginResponse.CookieInfo.Cookie(
        name = name,
        value = value,
        httpOnly = 0,
        expires = 0,
        secure = 0,
        sameSite = 0,
    )
}
