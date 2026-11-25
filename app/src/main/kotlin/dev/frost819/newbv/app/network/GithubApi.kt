package dev.frost819.newbv.app.network

import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.biliapi.http.util.useSharedBiliClient
import dev.frost819.newbv.core.log.Loggers
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * 更新包下载源前缀，按顺序尝试；空字符串表示 GitHub 直连（放最后，仅海外网络/代理下可用）。
 *
 * 镜像站可用性经常变动：实测 `ghfast.top` 曾长时间无响应（连接后不返回数据），
 * 单源失败会让所有国内用户卡在 0B 后下载失败，因此保留多个源顺序回退。
 */
internal val DOWNLOAD_SOURCES =
    listOf(
        "https://gh-proxy.com/",
        "https://ghproxy.net/",
        "https://ghfast.top/",
        "",
    )

/** 构造候选下载地址：镜像前缀依次拼接，最后追加 GitHub 直连地址。 */
internal fun buildDownloadUrls(downloadUrl: String): List<String> =
    DOWNLOAD_SOURCES.map { source -> if (source.isEmpty()) downloadUrl else source + downloadUrl }

/** 下载源标识（仅用于日志）：镜像取域名，直连显示「GitHub 直连」。 */
internal fun downloadSourceLabel(url: String): String {
    val host = url.substringAfter("://").substringBefore('/')
    return if (host == "github.com") "GitHub 直连" else host
}

/** 粗略校验文件是 APK：APK 是 ZIP，必然以 `PK\x03\x04` 开头，用于过滤镜像站的错误页。 */
internal fun File.isApkLike(): Boolean =
    runCatching {
        inputStream().use { input ->
            val header = ByteArray(4)
            input.read(header) == 4 &&
                header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        }
    }.getOrDefault(false)

/**
 * GitHub Releases API 封装。
 *
 * 用于检查更新和下载 APK。仓库地址：`Frost819/newBV`。
 *
 * 直连 GitHub 在国内基本不可用，下载按 [DOWNLOAD_SOURCES] 顺序回退到多个镜像，
 * 全部失败后才尝试直连（issue #342：单一镜像失效会导致所有用户卡在 0B）。
 */
object GithubApi {
    private const val OWNER = "Frost819"
    private const val REPO = "newBV"

    /** 单次下载尝试的连接超时：镜像站不可用时尽快切下一个源。 */
    private const val CONNECT_TIMEOUT_MS = 10_000L

    /** 单次读超时：镜像站「连上但不给数据」时（卡 0B）能主动放弃。 */
    private const val SOCKET_TIMEOUT_MS = 20_000L

    /** 单个源的下载总时长上限，避免极慢节点一直占着不放。 */
    private const val REQUEST_TIMEOUT_MS = 10 * 60 * 1000L

    private val logger = Loggers.get("GithubApi")

    private val json =
        Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    private val client: HttpClient =
        HttpClient(OkHttp) {
            engine {
                useSharedBiliClient()
            }
            BrowserUserAgent()
            // 没有安装 HttpTimeout 时，downloadFrom 里的 per-request timeout 不会生效
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0f)
                gzip(0.9f)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.github.com"
                }
            }
        }

    /** 获取最新 Release（非预发布）。 */
    suspend fun getLatestBuild(): GithubRelease {
        val response = client.get("repos/$OWNER/$REPO/releases/latest").bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString(response)
    }

    /** 获取所有 Releases。 */
    suspend fun getReleases(
        pageSize: Int = 30,
        page: Int = 1,
    ): List<GithubRelease> {
        val response =
            client
                .get("repos/$OWNER/$REPO/releases") {
                    parameter("per_page", pageSize)
                    parameter("page", page)
                }.bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString(response)
    }

    /**
     * 下载 Release 中的 APK 附件。
     *
     * 按 [buildDownloadUrls] 的顺序依次尝试镜像，单个源失败（超时、HTTP 错误、
     * 内容不是 APK、下载不完整）时自动切换下一个源。换源对用户不可见：界面上只有
     * 连续进度，切换过程只写入日志，便于事后排查镜像可用性。
     *
     * @param release Release 信息。
     * @param file 目标文件路径。
     * @param onProgress 下载进度回调（已下载字节, 总字节）。
     */
    suspend fun downloadUpdate(
        release: GithubRelease,
        file: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        val downloadUrl =
            release.assets
                .firstOrNull {
                    it.name.contains("release") || it.name.contains("alpha")
                }?.browserDownloadUrl
                ?: throw IllegalStateException("Didn't find download url")

        var lastError: Throwable? = null
        for (url in buildDownloadUrls(downloadUrl)) {
            logger.info { "下载更新：${downloadSourceLabel(url)} $url" }
            try {
                downloadFrom(url, file, onProgress)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.warn(error) { "下载更新失败，切换下载源：$url" }
                lastError = error
            }
        }
        throw IllegalStateException("所有下载源均下载失败，请稍后重试", lastError)
    }

    /** 从单个地址下载到 [file]；HTTP 错误、长度不符或内容不是 APK 时抛出异常交由上层换源。 */
    private suspend fun downloadFrom(
        url: String,
        file: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        client
            .prepareRequest {
                url(url)
                timeout {
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                    socketTimeoutMillis = SOCKET_TIMEOUT_MS
                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                }
                onDownload { downloaded, total ->
                    onProgress(downloaded, total ?: 0)
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    error("HTTP ${response.status.value}")
                }
                val expected = response.contentLength() ?: 0
                val written =
                    file.outputStream().use { output ->
                        response.bodyAsChannel().copyTo(output)
                    }
                if (expected > 0 && written != expected) {
                    error("下载不完整：$written/$expected")
                }
            }
        check(file.isApkLike()) { "下载内容不是 APK" }
    }

    private fun checkErrorMessage(data: String) {
        val responseElement = json.parseToJsonElement(data)
        if (responseElement !is JsonObject) return
        val responseObject = responseElement.jsonObject
        if (responseObject["message"] != null) {
            error(responseObject["message"]!!.jsonPrimitive.content)
        }
    }
}
