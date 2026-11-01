package dev.frost819.newbv.app.network

import dev.frost819.newbv.app.network.entity.SponsorHashVideo
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest

/**
 * SponsorBlock B 站镜像（bsbsb.top）API 封装。
 *
 * 空降助手数据源：社区贡献的跳过片段（恰饭/开场/片尾等），按 videoID 的
 * SHA-256 哈希前缀查询以节省带宽，客户端在响应内筛选目标视频。
 *
 * 独立于 [BiliHttpApi]：该服务非 B 站域名，不需要 WBI 签名与 Cookie 注入。
 * 主域名在国内可能不可达，网络层失败（[IOException]）时回退一次备用 IP 直连。
 */
object SponsorBlockApi {
    private const val PRIMARY_HOST = "bsbsb.top"

    /** 备用入口（IP 直连，国内可访问），仅在主域名网络不可达时使用。 */
    private const val FALLBACK_HOST = "154.222.28.109"

    private const val HASH_PREFIX_LENGTH = 4

    private val json =
        Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

    private val client: HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0f)
                gzip(0.9f)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000L
                requestTimeoutMillis = 15_000L
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = PRIMARY_HOST
                }
            }
        }

    /**
     * 查询视频的空降片段。
     *
     * @param bvid 视频 BV 号（哈希输入，服务端按前缀匹配）
     * @param categories 需要查询的分类列表；调用方需保证非空（空列表在仓库层拦截）
     * @return 该哈希前缀下的视频条目列表（需按 videoID 筛选）；目标视频无片段时响应为空列表
     * @throws IOException 主备两个入口均网络不可达时抛出
     * @throws IllegalStateException 服务端返回非成功状态码
     */
    suspend fun getSkipSegments(
        bvid: String,
        categories: List<String>,
    ): List<SponsorHashVideo> {
        val hashPrefix = sha256Prefix(bvid)
        return try {
            requestSegments(PRIMARY_HOST, URLProtocol.HTTPS, hashPrefix, categories)
        } catch (e: IOException) {
            requestSegments(FALLBACK_HOST, URLProtocol.HTTP, hashPrefix, categories)
        }
    }

    /**
     * 计算视频 ID 的 SHA-256 哈希十六进制前缀（SponsorBlock 标准哈希前缀查询协议）。
     */
    private fun sha256Prefix(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(HASH_PREFIX_LENGTH)
    }

    /**
     * 向指定 host 发起一次片段查询。
     *
     * 404 语义为该前缀下无数据（目标视频无片段），归一化为空列表成功返回。
     */
    private suspend fun requestSegments(
        targetHost: String,
        targetProtocol: URLProtocol,
        hashPrefix: String,
        categories: List<String>,
    ): List<SponsorHashVideo> {
        val response =
            client.get("api/skipSegments/$hashPrefix") {
                header(HttpHeaders.UserAgent, "newBV/1.0")
                header("origin", "newbv")
                categories.forEach { parameter("category", it) }
                url {
                    protocol = targetProtocol
                    host = targetHost
                }
            }
        if (response.status.value == 404) return emptyList()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("SponsorBlock HTTP ${response.status.value}")
        }
        return json.decodeFromString(response.bodyAsText())
    }
}
