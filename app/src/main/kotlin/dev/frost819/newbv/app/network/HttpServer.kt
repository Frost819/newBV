package dev.frost819.newbv.app.network

import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.InteractionLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 本地 HTTP 日志服务器。
 *
 * 使用 Ktor CIO 引擎在随机端口启动，提供日志管理 Web UI 和 REST API。
 * 浏览器访问 `http://<设备IP>:<端口>/` 即可查看和下载日志文件。
 *
 * 路由：
 * - `GET /` — 日志管理首页（index.html）
 * - `GET /logs_ui/{path...}` — 静态资源（CSS/JS/字体）
 * - `GET /api/logs/list` — 日志文件列表 JSON
 * - `GET /api/logs/{filename}` — 下载指定日志文件
 * - `GET /api/logs/create-manual-and-download` — 创建手动日志并下载
 *
 * @param assetProvider 资源读取函数，传入 assets 路径返回字节流，不存在返回 null。
 * @param logFileProvider 日志文件列表函数，返回所有可管理的日志文件。
 * @param manualLogCreator 手动日志创建函数，返回生成的文件，失败返回 null。
 */
class HttpServer(
    private val assetProvider: (String) -> ByteArray?,
    private val logFileProvider: () -> List<File>,
    private val manualLogCreator: () -> File?
) {
    private val logger = KotlinLogging.logger("HttpServer")

    @Volatile
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    @Volatile
    private var resolvedPort: Int? = null

    /**
     * 启动 HTTP 服务器。
     *
     * 在随机端口（port = 0）上启动 CIO 引擎，非阻塞模式。
     * 重复调用安全（已运行时直接返回）。
     */
    fun start() {
        if (server != null) {
            logger.warn { "HttpServer already running" }
            return
        }
        server = embeddedServer(CIO, port = 0) {
            configureRoutes()
        }.also { it.start(wait = false) }

        // resolvedConnectors() 是 suspend 函数，用 runBlocking 同步获取端口
        resolvedPort = runBlocking {
            server?.engine?.resolvedConnectors()?.firstOrNull()?.port
        }
        logger.info { "HttpServer started on port $resolvedPort" }
    }

    /**
     * 停止 HTTP 服务器。
     */
    fun stop() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        server = null
        resolvedPort = null
        logger.info { "HttpServer stopped" }
    }

    /** 服务器是否正在运行。 */
    fun isRunning(): Boolean = server != null

    /**
     * 获取实际监听端口。
     *
     * @return 端口号，未运行时返回 null。
     */
    fun getPort(): Int? = resolvedPort

    // ── 路由配置 ──────────────────────────────────────────────────────

    private fun Application.configureRoutes() {
        routing {
            homeRoute()
            staticAssetsRoute()
            logsApiRoute()
        }
    }

    /** `GET /` — 返回日志管理首页。 */
    private fun Route.homeRoute() {
        get("/") {
            val bytes = assetProvider("logs_ui/index.html")
            if (bytes != null) {
                call.respondBytes(bytes, contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8))
            } else {
                call.respondText("logs_ui/index.html not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    /** `GET /logs_ui/{path...}` — 静态资源服务（含路径穿越防护）。 */
    private fun Route.staticAssetsRoute() {
        get("/logs_ui/{path...}") {
            val segments = call.parameters.getAll("path").orEmpty()
            val relPath = segments.joinToString("/").ifBlank { "index.html" }

            if (relPath.contains("..") || relPath.contains("\\")) {
                return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
            }

            val bytes = assetProvider("logs_ui/$relPath")
            if (bytes != null) {
                call.respondBytes(bytes, contentType = contentTypeFor(relPath))
            } else {
                call.respondText("not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    /** 日志 API 路由。 */
    private fun Route.logsApiRoute() {
        // 列出所有日志文件
        get("/api/logs/list") {
            val files = logFileProvider()
            val items = files.map { it.toLogItem() }
                .sortedByDescending { it.lastModified }

            call.respondText(
                text = json.encodeToString(items),
                contentType = ContentType.Application.Json
            )
        }

        // 下载指定日志文件（白名单校验）
        get("/api/logs/{filename}") {
            val filename = call.parameters["filename"]
                ?: return@get call.respondText("filename is null", status = HttpStatusCode.NotFound)

            if (isPathTraversal(filename)) {
                return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
            }

            if (!isAllowedLogFilename(filename)) {
                return@get call.respondText("forbidden", status = HttpStatusCode.Forbidden)
            }

            val file = logFileProvider().find { it.name == filename }
                ?: return@get call.respondText("file not found", status = HttpStatusCode.NotFound)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName, file.name
                ).toString()
            )
            call.respondFile(file)
        }

        // 创建手动日志并下载
        get("/api/logs/create-manual-and-download") {
            val file = manualLogCreator()
            if (file == null || !file.exists()) {
                return@get call.respondText(
                    "create manual log failed",
                    status = HttpStatusCode.InternalServerError
                )
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName, file.name
                ).toString()
            )
            call.respondFile(file)
        }
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────

    /**
     * 日志文件列表项。
     *
     * @param name 文件名
     * @param size 文件大小（字节）
     * @param lastModified 最后修改时间戳（毫秒）
     * @param type 日志类型：`manual` / `crash` / `interaction`
     */
    @Serializable
    data class LogItem(
        val name: String,
        val size: Long,
        val lastModified: Long,
        val type: String
    )

    private fun File.toLogItem(): LogItem {
        val type = when {
            name.startsWith(CrashHandler.MANUAL_LOG_PREFIX) -> "manual"
            name.startsWith(CrashHandler.CRASH_LOG_PREFIX) -> "crash"
            name.startsWith(InteractionLogger.FILE_PREFIX) -> "interaction"
            else -> "unknown"
        }
        return LogItem(name = name, size = length(), lastModified = lastModified(), type = type)
    }

    /** 检查文件名是否包含路径穿越攻击模式。 */
    private fun isPathTraversal(filename: String): Boolean =
        filename.contains("..") || filename.contains("/") || filename.contains("\\")

    /** 检查文件名是否符合日志白名单（前缀 + 后缀）。 */
    private fun isAllowedLogFilename(filename: String): Boolean {
        val allowedPrefix = filename.startsWith(CrashHandler.MANUAL_LOG_PREFIX)
            || filename.startsWith(CrashHandler.CRASH_LOG_PREFIX)
            || filename.startsWith(InteractionLogger.FILE_PREFIX)
        val allowedSuffix = filename.endsWith(".log")
        return allowedPrefix && allowedSuffix
    }

    private fun contentTypeFor(path: String): ContentType =
        when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> ContentType.Text.Html.withCharset(Charsets.UTF_8)
            "css" -> ContentType.Text.CSS.withCharset(Charsets.UTF_8)
            "js" -> ContentType.Application.JavaScript.withCharset(Charsets.UTF_8)
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "svg" -> ContentType.Image.SVG
            "ico" -> ContentType.Image.XIcon
            "woff2" -> ContentType.Application.OctetStream
            else -> ContentType.Application.OctetStream
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}
