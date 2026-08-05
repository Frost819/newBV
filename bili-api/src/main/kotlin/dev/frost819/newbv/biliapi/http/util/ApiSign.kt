package dev.frost819.newbv.biliapi.http.util

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.clone
import io.ktor.http.encodedPath
import io.ktor.http.plus
import java.net.URLEncoder
import java.security.MessageDigest

private const val APP_KEY = "dfca71928277209b"
private const val APP_SEC = "b5475a8825547a4fc26c7d518eaaa02e"
private val mixinKeyEncTab =
    listOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61,
        26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36,
        20, 34, 44, 52,
    )

private fun String.md5(): String =
    MessageDigest.getInstance("MD5")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

private fun Map<String, String>.toSortedQueryString(): String =
    toSortedMap()
        .map { (k, v) -> "$k=${URLEncoder.encode(v, "utf-8")}" }
        .joinToString("&")

private fun getMixinKey(orig: String): String = mixinKeyEncTab.fold("") { s, i -> s + orig[i] }.substring(0, 32)

private val HttpRequestBuilder.isAppRequest: Boolean
    get() = url.parameters.contains("access_key") || url.host == "app.bilibili.com"

fun HttpRequestBuilder.encAppPost() {
    var parameters = (body as FormDataContent).formData
    parameters += Parameters.build { append("appkey", APP_KEY) }

    val sortedQueryString =
        parameters.entries()
            .associate { it.key to it.value.first() }
            .toSortedQueryString()

    val sign = (sortedQueryString + APP_SEC).md5()
    parameters += Parameters.build { append("sign", sign) }
    setBody(FormDataContent(parameters))
    println("sign: $sign")
}

fun HttpRequestBuilder.encAppGet() {
    parameter("appkey", APP_KEY)

    val sortedQueryString =
        url.encodedParameters.entries()
            .associate { it.key to it.value.first() }
            .toSortedQueryString()

    val sign = (sortedQueryString + APP_SEC).md5()
    parameter("sign", sign)
    println("sign: $sign")
}

suspend fun HttpRequestBuilder.encWbi() {
    if (BiliHttpApi.wbiImgKey == null || BiliHttpApi.wbiSubKey == null) BiliHttpApi.updateWbi()
    val mixinKey =
        getMixinKey(
            requireNotNull(BiliHttpApi.wbiImgKey) { "wbiImgKey can't be null!" } +
                requireNotNull(BiliHttpApi.wbiSubKey) { "wbiSubKey can't be null!" },
        )

    val wts = (System.currentTimeMillis() / 1000).toInt()
    parameter("wts", wts)

    val sortedParams =
        url.encodedParameters.entries()
            .associate { it.key to it.value.first() }
            .toSortedMap()
            .map { (key, value) ->
                // 过滤特殊字符 !"!'()*
                val filteredValue = value.filter { c -> c !in setOf('!', '\'', '(', ')', '*') }
                "$key=$filteredValue"
            }
            .joinToString("&")

    val wRid = (sortedParams + mixinKey).md5()
    parameter("w_rid", wRid)
}

fun HttpClient.encApiSign() =
    plugin(HttpSend)
        .intercept { request ->
            if (request.url.encodedPath.startsWith("bilibili.")) {
                return@intercept execute(request)
            }

            val getUrlWithoutAccessToken: (URLBuilder) -> String = { urlBuilder ->
                urlBuilder.clone().apply {
                    if (parameters.contains("access_key") && !parameters["access_key"].isNullOrBlank()) {
                        parameters["access_key"] = "HIDDEN_ACCESS_TOKEN"
                    }
                }.toString()
            }

            when (request.method) {
                HttpMethod.Get -> {
                    val isWbiRequest =
                        request.url.encodedPath.contains("wbi") ||
                            request.url.encodedPath.contains("/pgc/player/web/playurl") ||
                            request.url.encodedPath.contains("/pgc/player/web/v2/playurl")
                    if (isWbiRequest) {
                        println("Enc wbi for get request: ${getUrlWithoutAccessToken(request.url)}")
                        request.encWbi()
                    } else if (request.isAppRequest) {
                        println("Enc app sign for get request: ${getUrlWithoutAccessToken(request.url)}")
                        request.encAppGet()
                        println(getUrlWithoutAccessToken(request.url))
                    }
                }

                HttpMethod.Post -> {
                    if (request.body is EmptyContent) return@intercept execute(request)
                    val parameters = (request.body as FormDataContent).formData
                    val isParametersContainKeywords = parameters.contains("access_key")
                    val isPathContainKeywords = request.url.encodedPath.contains("passport")
                    if (isParametersContainKeywords || isPathContainKeywords) {
                        println("Enc app sign for post request: ${getUrlWithoutAccessToken(request.url)}")
                        request.encAppPost()
                    }
                }
            }
            execute(request)
        }

/**
 * 统一 Cookie 注入拦截器。
 *
 * 像浏览器一样，为所有非 App 请求自动携带完整 Cookie（SESSDATA + DedeUserID + buvid3 + b_nut 等）。
 * 各 API 函数不再需要手动拼接 Cookie header。
 *
 * - App 请求（含 access_key）不注入 Cookie，使用 access_key 鉴权
 * - SPI/导航等无登录请求：buvid3 + b_nut 仍会被注入（设备标识）
 */
fun HttpClient.injectCookies() =
    plugin(HttpSend).intercept { request ->
        val isPlayUrlRequest =
            request.url.encodedPath.contains("/x/player/playurl") ||
                request.url.encodedPath.contains("/x/player/wbi/playurl")

        if (!request.isAppRequest && !isPlayUrlRequest) {
            val cookieParts = mutableListOf<String>()

            // 登录凭证
            if (BiliHttpApi.sessData.isNotBlank()) {
                cookieParts.add("SESSDATA=${BiliHttpApi.sessData}")
            }
            if (BiliHttpApi.mid != null) {
                cookieParts.add("DedeUserID=${BiliHttpApi.mid}")
            }

            // 设备标识 cookie（buvid3 + b_nut 等）
            if (BiliHttpApi.deviceCookies.isNotBlank()) {
                cookieParts.add(BiliHttpApi.deviceCookies)
            } else if (BiliHttpApi.buvid3.isNotBlank()) {
                cookieParts.add("buvid3=${BiliHttpApi.buvid3}")
            }

            if (cookieParts.isNotEmpty()) {
                request.headers["Cookie"] = cookieParts.joinToString("; ")
            }
        }
        execute(request)
    }
