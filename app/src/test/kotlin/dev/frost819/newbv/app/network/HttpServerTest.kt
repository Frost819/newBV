package dev.frost819.newbv.app.network

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

/**
 * [HttpServer] 的单元测试。
 *
 * 使用真实的 Ktor CIO 引擎启动服务器，通过 HTTP 请求验证路由、白名单、文件下载等功能。
 * 不依赖 Android Context（通过函数参数注入依赖）。
 */
class HttpServerTest {

    private lateinit var tempDir: File
    private lateinit var server: HttpServer
    private lateinit var logFiles: MutableList<File>

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("httpserver_test").toFile()
        logFiles = mutableListOf()

        // 创建测试日志文件
        logFiles.add(createLog("logs_manual_2026-01-01_10:00:00.log", "manual log content"))
        logFiles.add(createLog("logs_crash_2026-01-02_11:30:00.log", "crash log content"))
        logFiles.add(createLog("logs_interaction_20260101.log", "interaction log content"))

        server = HttpServer(
            assetProvider = { path ->
                if (path == "logs_ui/index.html") "<html>test</html>".toByteArray()
                else null
            },
            logFileProvider = { logFiles.toList() },
            manualLogCreator = {
                val file = File(tempDir, "logs_manual_2026-01-03_12:00:00.log")
                file.writeText("generated manual log")
                logFiles.add(file)
                file
            }
        )
        server.start()

        // 等待服务器就绪
        Thread.sleep(200)
    }

    @AfterEach
    fun tearDown() {
        server.stop()
        tempDir.deleteRecursively()
    }

    // ── 基础功能 ──────────────────────────────────────────────────────

    @Test
    fun `server starts and port is resolved`() {
        assertThat(server.isRunning()).isTrue()
        assertThat(server.getPort()).isNotNull()
        assertThat(server.getPort()!!).isGreaterThan(0)
    }

    @Test
    fun `stop sets isRunning to false`() {
        server.stop()
        assertThat(server.isRunning()).isFalse()
        assertThat(server.getPort()).isNull()
    }

    // ── 首页路由 ──────────────────────────────────────────────────────

    @Test
    fun `GET root returns index html`() {
        val (status, body) = httpGet("/")
        assertThat(status).isEqualTo(200)
        assertThat(body).contains("test")
    }

    @Test
    fun `GET root returns 404 when asset not found`() {
        server.stop()
        server = HttpServer(
            assetProvider = { null },
            logFileProvider = { emptyList() },
            manualLogCreator = { null }
        )
        server.start()
        Thread.sleep(200)

        val (status, _) = httpGet("/")
        assertThat(status).isEqualTo(404)
    }

    // ── 静态资源路由 ──────────────────────────────────────────────────

    @Test
    fun `GET logs_ui serves static asset`() {
        server.stop()
        server = HttpServer(
            assetProvider = { path ->
                if (path == "logs_ui/index.html") "<html></html>".toByteArray()
                else if (path == "logs_ui/test.css") "body { }".toByteArray()
                else null
            },
            logFileProvider = { emptyList() },
            manualLogCreator = { null }
        )
        server.start()
        Thread.sleep(200)

        val (status, body) = httpGet("/logs_ui/test.css")
        assertThat(status).isEqualTo(200)
        assertThat(body).contains("body")
    }

    @Test
    fun `GET logs_ui with path traversal returns 403`() {
        val (status, _) = httpGet("/logs_ui/../../etc/passwd")
        assertThat(status).isEqualTo(403)
    }

    @Test
    fun `GET logs_ui with non-existent asset returns 404`() {
        val (status, _) = httpGet("/logs_ui/nonexistent.js")
        assertThat(status).isEqualTo(404)
    }

    // ── API: /api/logs/list ───────────────────────────────────────────

    @Test
    fun `GET api logs list returns all log files as JSON`() {
        val (status, body) = httpGet("/api/logs/list")
        assertThat(status).isEqualTo(200)

        val items = json.decodeFromString<List<HttpServer.LogItem>>(body)
        assertThat(items).hasSize(3)

        val types = items.map { it.type }.toSet()
        assertThat(types).containsExactly("manual", "crash", "interaction")
    }

    @Test
    fun `GET api logs list returns sorted by lastModified desc`() {
        val (status, body) = httpGet("/api/logs/list")
        assertThat(status).isEqualTo(200)

        val items = json.decodeFromString<List<HttpServer.LogItem>>(body)
        assertThat(items).hasSize(3)
        assertThat(items[0].lastModified).isAtLeast(items[1].lastModified)
        assertThat(items[1].lastModified).isAtLeast(items[2].lastModified)
    }

    @Test
    fun `GET api logs list returns empty array when no logs`() {
        server.stop()
        server = HttpServer(
            assetProvider = { null },
            logFileProvider = { emptyList() },
            manualLogCreator = { null }
        )
        server.start()
        Thread.sleep(200)

        val (status, body) = httpGet("/api/logs/list")
        assertThat(status).isEqualTo(200)

        val items = json.decodeFromString<List<HttpServer.LogItem>>(body)
        assertThat(items).isEmpty()
    }

    // ── API: /api/logs/{filename} ─────────────────────────────────────

    @Test
    fun `GET api logs filename downloads existing log file`() {
        val (status, body) = httpGet("/api/logs/logs_manual_2026-01-01_10:00:00.log")
        assertThat(status).isEqualTo(200)
        assertThat(body).contains("manual log content")
    }

    @Test
    fun `GET api logs filename returns 404 for non-existent file`() {
        val (status, _) = httpGet("/api/logs/logs_manual_9999-01-01_00:00:00.log")
        assertThat(status).isEqualTo(404)
    }

    @Test
    fun `GET api logs filename returns 403 for non-whitelisted prefix`() {
        val (status, _) = httpGet("/api/logs/secret_file.log")
        assertThat(status).isEqualTo(403)
    }

    @Test
    fun `GET api logs filename returns 403 for non-log suffix`() {
        val (status, _) = httpGet("/api/logs/logs_manual_test.txt")
        assertThat(status).isEqualTo(403)
    }

    @Test
    fun `GET api logs filename returns 403 for path traversal`() {
        // java.net.URL 会标准化完整 `..` 路径段，所以用包含 `..` 的文件名测试
        val (status, _) = httpGet("/api/logs/logs_manual_..test.log")
        assertThat(status).isEqualTo(403)
    }

    @Test
    fun `GET api logs interaction file downloads successfully`() {
        val (status, body) = httpGet("/api/logs/logs_interaction_20260101.log")
        assertThat(status).isEqualTo(200)
        assertThat(body).contains("interaction log content")
    }

    // ── API: /api/logs/create-manual-and-download ─────────────────────

    @Test
    fun `GET create-manual-and-download creates and returns file`() {
        val (status, body) = httpGet("/api/logs/create-manual-and-download")
        assertThat(status).isEqualTo(200)
        assertThat(body).contains("generated manual log")
    }

    @Test
    fun `GET create-manual-and-download returns 500 when creator fails`() {
        server.stop()
        server = HttpServer(
            assetProvider = { null },
            logFileProvider = { emptyList() },
            manualLogCreator = { null }
        )
        server.start()
        Thread.sleep(200)

        val (status, _) = httpGet("/api/logs/create-manual-and-download")
        assertThat(status).isEqualTo(500)
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    private fun createLog(name: String, content: String): File {
        val file = File(tempDir, name)
        file.writeText(content)
        // 确保每个文件的 lastModified 不同
        file.setLastModified(System.currentTimeMillis() + logFiles.size * 1000)
        return file
    }

    /** 发送 GET 请求，返回状态码和响应体。 */
    private fun httpGet(path: String): Pair<Int, String> {
        val port = server.getPort() ?: return -1 to "server not running"
        val url = URL("http://localhost:$port$path")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val status = conn.responseCode
            val body = if (status in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            status to body
        } catch (e: Exception) {
            -1 to (e.message ?: "error")
        } finally {
            conn.disconnect()
        }
    }
}
