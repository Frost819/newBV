package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.takeFrom
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * WBI 签名算法 [ApiSign] 相关函数的单元测试。
 *
 * 通过反射调用私有函数 `getMixinKey` 和 `md5`，验证 WBI 签名核心逻辑的正确性。
 * 同时验证 `encAppPost`、`encAppGet`、`encWbi` 扩展函数的签名行为。
 */
class ApiSignTest {
    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ------------------------------------------------------------------
    // encAppPost
    // ------------------------------------------------------------------

    @Test
    fun `encAppPost adds appkey and sign to form data body`() {
        val builder =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder.setBody(FormDataContent(Parameters.build { append("keyword", "test") }))

        builder.encAppPost()

        val body = builder.body as FormDataContent
        assertThat(body.formData["appkey"]).isNotNull()
        assertThat(body.formData["sign"]).isNotNull()
        assertThat(body.formData["keyword"]).isEqualTo("test")
    }

    @Test
    fun `encAppPost sign is deterministic for same input`() {
        val builder1 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder1.setBody(FormDataContent(Parameters.build { append("keyword", "test") }))
        builder1.encAppPost()
        val sign1 = (builder1.body as FormDataContent).formData["sign"]!!

        val builder2 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder2.setBody(FormDataContent(Parameters.build { append("keyword", "test") }))
        builder2.encAppPost()
        val sign2 = (builder2.body as FormDataContent).formData["sign"]!!

        assertThat(sign1).isEqualTo(sign2)
    }

    @Test
    fun `encAppPost sign changes when input changes`() {
        val builder1 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder1.setBody(FormDataContent(Parameters.build { append("keyword", "test1") }))
        builder1.encAppPost()
        val sign1 = (builder1.body as FormDataContent).formData["sign"]!!

        val builder2 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder2.setBody(FormDataContent(Parameters.build { append("keyword", "test2") }))
        builder2.encAppPost()
        val sign2 = (builder2.body as FormDataContent).formData["sign"]!!

        assertThat(sign1).isNotEqualTo(sign2)
    }

    @Test
    fun `encAppPost sign is 32 char hex md5`() {
        val builder =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder.setBody(FormDataContent(Parameters.build { append("keyword", "test") }))
        builder.encAppPost()

        val sign = (builder.body as FormDataContent).formData["sign"]!!
        assertThat(sign).hasLength(32)
        assertThat(sign).matches("[0-9a-f]{32}")
    }

    // ------------------------------------------------------------------
    // encAppGet
    // ------------------------------------------------------------------

    @Test
    fun `encAppGet adds appkey and sign parameters`() {
        val builder =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder.method = HttpMethod.Get
        builder.url.takeFrom("https://api.bilibili.com/test")
        builder.parameter("keyword", "test")

        builder.encAppGet()

        assertThat(builder.url.parameters["appkey"]).isNotNull()
        assertThat(builder.url.parameters["sign"]).isNotNull()
        assertThat(builder.url.parameters["keyword"]).isEqualTo("test")
    }

    @Test
    fun `encAppGet sign is deterministic for same parameters`() {
        val builder1 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder1.method = HttpMethod.Get
        builder1.url.takeFrom("https://api.bilibili.com/test")
        builder1.parameter("keyword", "test")
        builder1.encAppGet()
        val sign1 = builder1.url.parameters["sign"]!!

        val builder2 =
            io.ktor.client.request
                .HttpRequestBuilder()
        builder2.method = HttpMethod.Get
        builder2.url.takeFrom("https://api.bilibili.com/test")
        builder2.parameter("keyword", "test")
        builder2.encAppGet()
        val sign2 = builder2.url.parameters["sign"]!!

        assertThat(sign1).isEqualTo(sign2)
    }

    // ------------------------------------------------------------------
    // encWbi
    // ------------------------------------------------------------------

    @Test
    fun `encWbi adds wts and w_rid when wbi keys are set`() =
        runTest {
            every { BiliHttpApi.wbiImgKey } returns "7cd084941338484aae1ad9425b81277e"
            every { BiliHttpApi.wbiSubKey } returns "4932caff0ff746eab6f01bf08b70ac45"
            coEvery { BiliHttpApi.updateWbi() } just Runs

            val builder =
                io.ktor.client.request
                    .HttpRequestBuilder()
            builder.method = HttpMethod.Get
            builder.url.takeFrom("https://api.bilibili.com/x/web-interface/wbi/search")
            builder.parameter("keyword", "test")

            builder.encWbi()

            assertThat(builder.url.parameters["wts"]).isNotNull()
            assertThat(builder.url.parameters["w_rid"]).isNotNull()
            assertThat(builder.url.parameters["w_rid"]!!).hasLength(32)
            assertThat(builder.url.parameters["w_rid"]!!).matches("[0-9a-f]{32}")
        }

    @Test
    fun `encWbi calls updateWbi when wbi keys are null`() =
        runTest {
            var imgKey: String? = null
            var subKey: String? = null
            every { BiliHttpApi.wbiImgKey } answers { imgKey }
            every { BiliHttpApi.wbiSubKey } answers { subKey }
            coEvery { BiliHttpApi.updateWbi() } answers {
                imgKey = "7cd084941338484aae1ad9425b81277e"
                subKey = "4932caff0ff746eab6f01bf08b70ac45"
            }

            val builder =
                io.ktor.client.request
                    .HttpRequestBuilder()
            builder.method = HttpMethod.Get
            builder.url.takeFrom("https://api.bilibili.com/x/web-interface/wbi/search")
            builder.parameter("keyword", "test")

            builder.encWbi()

            coVerify { BiliHttpApi.updateWbi() }
            assertThat(builder.url.parameters["w_rid"]).isNotNull()
        }

    @Test
    fun `encWbi does not call updateWbi when wbi keys are already set`() =
        runTest {
            every { BiliHttpApi.wbiImgKey } returns "7cd084941338484aae1ad9425b81277e"
            every { BiliHttpApi.wbiSubKey } returns "4932caff0ff746eab6f01bf08b70ac45"
            coEvery { BiliHttpApi.updateWbi() } just Runs

            val builder =
                io.ktor.client.request
                    .HttpRequestBuilder()
            builder.method = HttpMethod.Get
            builder.url.takeFrom("https://api.bilibili.com/x/web-interface/wbi/search")
            builder.parameter("keyword", "test")

            builder.encWbi()

            coVerify(exactly = 0) { BiliHttpApi.updateWbi() }
        }

    @Test
    fun `encWbi produces valid wRid even with special characters in parameters`() =
        runTest {
            every { BiliHttpApi.wbiImgKey } returns "7cd084941338484aae1ad9425b81277e"
            every { BiliHttpApi.wbiSubKey } returns "4932caff0ff746eab6f01bf08b70ac45"
            coEvery { BiliHttpApi.updateWbi() } just Runs

            val builder =
                io.ktor.client.request
                    .HttpRequestBuilder()
            builder.method = HttpMethod.Get
            builder.url.takeFrom("https://api.bilibili.com/x/web-interface/wbi/search")
            builder.parameter("keyword", "test!'()*")
            builder.encWbi()

            assertThat(builder.url.parameters["wts"]).isNotNull()
            assertThat(builder.url.parameters["w_rid"]).isNotNull()
            assertThat(builder.url.parameters["w_rid"]!!).hasLength(32)
            assertThat(builder.url.parameters["w_rid"]!!).matches("[0-9a-f]{32}")
        }

    // ------------------------------------------------------------------
    // injectCookies (via BiliHttpApi fields)
    // ------------------------------------------------------------------

    @Test
    fun `injectCookies is available as HttpClient extension`() {
        every { BiliHttpApi.sessData } returns ""
        every { BiliHttpApi.mid } returns null
        every { BiliHttpApi.deviceCookies } returns ""
        every { BiliHttpApi.buvid3 } returns ""

        val client = io.ktor.client.HttpClient()
        client.injectCookies()
        client.close()
    }

    @Test
    fun `encApiSign is available as HttpClient extension`() {
        val client = io.ktor.client.HttpClient()
        client.encApiSign()
        client.close()
    }

    @Test
    fun `getMixinKey produces correct 32-char key from known concatenated keys`() {
        val mixinKey = invokeGetMixinKey("7cd084941338484aae1ad9425b81277e" + "4932caff0ff746eab6f01bf08b70ac45")

        assertThat(mixinKey).hasLength(32)
    }

    @Test
    fun `getMixinKey always returns 32 chars regardless of input length`() {
        val longInput = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val mixinKey = invokeGetMixinKey(longInput)

        assertThat(mixinKey).hasLength(32)
    }

    @Test
    fun `md5 of empty string returns d41d8cd98f00b204e9800998ecf8427e`() {
        val md5 = invokeMd5("")

        assertThat(md5).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
    }

    @Test
    fun `md5 of known string returns expected hash`() {
        val md5 = invokeMd5("hello")

        assertThat(md5).isEqualTo("5d41402abc4b2a76b9719d911017c592")
    }

    @Test
    fun `toSortedQueryString sorts parameters alphabetically`() {
        val params = mapOf("zebra" to "z", "apple" to "a", "mango" to "m")
        val result = invokeToSortedQueryString(params)

        assertThat(result).isEqualTo("apple=a&mango=m&zebra=z")
    }

    @Test
    fun `toSortedQueryString URL-encodes values`() {
        val params = mapOf("key" to "hello world&test")
        val result = invokeToSortedQueryString(params)

        assertThat(result).contains("hello+world")
    }

    @Test
    fun `toSortedQueryString with empty map returns empty string`() {
        val result = invokeToSortedQueryString(emptyMap())

        assertThat(result).isEmpty()
    }

    @Test
    fun `toSortedQueryString with single entry returns correct format`() {
        val result = invokeToSortedQueryString(mapOf("foo" to "bar"))

        assertThat(result).isEqualTo("foo=bar")
    }

    @Test
    fun `toSortedQueryString with special characters encodes them`() {
        val params = mapOf("query" to "test=123&key=val")
        val result = invokeToSortedQueryString(params)

        assertThat(result).contains("test%3D123")
        assertThat(result).contains("%26")
    }

    @Test
    fun `toSortedQueryString with unicode characters encodes them`() {
        val params = mapOf("name" to "中文")
        val result = invokeToSortedQueryString(params)

        assertThat(result).contains("%E4%B8%AD%E6%96%87")
    }

    @Test
    fun `toSortedQueryString with numeric string values preserves them`() {
        val params = mapOf("count" to "123", "page" to "1")
        val result = invokeToSortedQueryString(params)

        assertThat(result).isEqualTo("count=123&page=1")
    }

    @Test
    fun `md5 of known longer string returns expected hash`() {
        val md5 = invokeMd5("The quick brown fox jumps over the lazy dog")

        assertThat(md5).isEqualTo("9e107d9d372bb6826bd81d3542a419d6")
    }

    @Test
    fun `md5 of string with special characters returns consistent hash`() {
        val md5 = invokeMd5("hello world!@#$%^&*()")

        assertThat(md5).hasLength(32)
    }

    @Test
    fun `md5 of identical inputs returns identical hashes`() {
        val md5a = invokeMd5("test input")
        val md5b = invokeMd5("test input")

        assertThat(md5a).isEqualTo(md5b)
    }

    @Test
    fun `md5 of different inputs returns different hashes`() {
        val md5a = invokeMd5("input1")
        val md5b = invokeMd5("input2")

        assertThat(md5a).isNotEqualTo(md5b)
    }

    @Test
    fun `getMixinKey produces deterministic output for same input`() {
        val input = "7cd084941338484aae1ad9425b81277e4932caff0ff746eab6f01bf08b70ac45"
        val key1 = invokeGetMixinKey(input)
        val key2 = invokeGetMixinKey(input)

        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun `getMixinKey uses only first 64 chars of input`() {
        val input64 = "7cd084941338484aae1ad9425b81277e4932caff0ff746eab6f01bf08b70ac45"
        val inputLong = input64 + "extra characters that should be ignored"
        val key1 = invokeGetMixinKey(input64)
        val key2 = invokeGetMixinKey(inputLong)

        assertThat(key1).isEqualTo(key2)
    }

    private fun invokeGetMixinKey(orig: String): String {
        val method =
            Class
                .forName("dev.frost819.newbv.biliapi.http.util.ApiSignKt")
                .declaredMethods
                .first { it.name == "getMixinKey" }
        method.isAccessible = true
        return method.invoke(null, orig) as String
    }

    private fun invokeMd5(input: String): String {
        val method =
            Class
                .forName("dev.frost819.newbv.biliapi.http.util.ApiSignKt")
                .declaredMethods
                .first { it.name == "md5" }
        method.isAccessible = true
        return method.invoke(null, input) as String
    }

    private fun invokeToSortedQueryString(params: Map<String, String>): String {
        val method =
            Class
                .forName("dev.frost819.newbv.biliapi.http.util.ApiSignKt")
                .declaredMethods
                .first { it.name == "toSortedQueryString" }
        method.isAccessible = true
        return method.invoke(null, params) as String
    }
}
