package dev.frost819.newbv.app.network

import dev.frost819.newbv.app.network.entity.SponsorHashVideo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SponsorBlock B 站镜像（bsbsb.top）API 封装。
 *
 * 空降助手数据源：社区贡献的跳过片段（恰饭/开场/片尾等），按 videoID 的
 * SHA-256 哈希前缀查询以节省带宽，客户端在响应内筛选目标视频。
 *
 * 独立于 [BiliHttpApi]：该服务非 B 站域名，不需要 WBI 签名与 Cookie 注入；
 * 复用全局注入的共享 [HttpClient]（OkHttp 连接池 + [HttpTimeout] 插件），
 * 并为每次查询施加短超时，以便主域名国内不可达时快速回退备用 IP 直连。
 *
 * @param client 全局共享 Ktor [HttpClient]
 */
@Singleton
class SponsorBlockApi
    @Inject
    constructor(
        private val client: HttpClient,
    ) {
        private val json =
            Json {
                coerceInputValues = true
                ignoreUnknownKeys = true
            }

        /**
         * 查询视频的空降片段。
         *
         * @param bvid 视频 BV 号（哈希输入，服务端按前缀匹配）
         * @param categories 需要查询的分类列表；调用方需保证非空（空列表在仓库层拦截）
         * @return 该哈希前缀下的视频条目列表（需按 videoID 筛选）；目标视频无片段时响应为空列表
         * @throws IOException 主备两个入口均网络不可达或超时时抛出
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
                client.get {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header("origin", ORIGIN_HEADER)
                    timeout {
                        connectTimeoutMillis = CONNECT_TIMEOUT_MS
                        requestTimeoutMillis = REQUEST_TIMEOUT_MS
                    }
                    url {
                        protocol = targetProtocol
                        host = targetHost
                        path("api", "skipSegments", hashPrefix)
                        categories.forEach { parameters.append("category", it) }
                    }
                }
            if (response.status.value == HTTP_NOT_FOUND) return emptyList()
            if (!response.status.isSuccess()) {
                throw IllegalStateException("SponsorBlock HTTP ${response.status.value}")
            }
            return json.decodeFromString(response.bodyAsText())
        }

        internal companion object {
            private const val PRIMARY_HOST = "bsbsb.top"

            /** 备用入口（IP 直连，国内可访问），仅在主域名网络不可达或超时时使用。 */
            private const val FALLBACK_HOST = "154.222.28.109"

            private const val HASH_PREFIX_LENGTH = 4
            private const val CONNECT_TIMEOUT_MS = 4_000L
            private const val REQUEST_TIMEOUT_MS = 8_000L
            private const val HTTP_NOT_FOUND = 404
            private const val USER_AGENT = "newBV/1.0"
            private const val ORIGIN_HEADER = "newbv"

            /**
             * 计算视频 ID 的 SHA-256 哈希十六进制前缀（SponsorBlock 标准哈希前缀查询协议）。
             *
             * @param input 视频 BV 号
             * @return 小写十六进制前 4 位前缀
             */
            fun sha256Prefix(input: String): String {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
                return hash.joinToString("") { "%02x".format(it) }.take(HASH_PREFIX_LENGTH)
            }
        }
    }
