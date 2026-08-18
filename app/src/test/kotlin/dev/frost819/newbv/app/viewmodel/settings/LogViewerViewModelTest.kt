package dev.frost819.newbv.app.viewmodel.settings

import android.app.Application
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.network.HttpServer
import dev.frost819.newbv.core.log.CrashHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

/**
 * [LogViewerViewModel] 的单元测试。
 *
 * 通过 MockK 模拟 [HttpServer] 和 [CrashHandler]，
 * 验证日志类型判断、URL 生成、日志刷新与手动日志创建逻辑。
 * init 块中的协程运行在 Dispatchers.IO 上，测试中使用 Thread.sleep 等待完成。
 */
class LogViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var httpServer: HttpServer
    private lateinit var crashHandler: CrashHandler
    private lateinit var viewModel: LogViewerViewModel
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempDir = Files.createTempDirectory("logviewer_test").toFile()

        httpServer = mockk(relaxed = true)
        every { httpServer.getPort() } returns 8080
        crashHandler = mockk(relaxed = true)
        every { crashHandler.listManualLogs() } returns emptyList()
        every { crashHandler.listCrashLogs() } returns emptyList()
        val app = mockk<Application>(relaxed = true)
        viewModel = LogViewerViewModel(app, httpServer, crashHandler)

        Thread.sleep(300)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        tempDir.deleteRecursively()
    }

    // ── LogViewerUiState ──────────────────────────────────────────────

    @Test
    fun `LogViewerUiState default values`() {
        val state = LogViewerUiState()
        assertThat(state.logFiles).isEmpty()
        assertThat(state.serverAddress).isEmpty()
        assertThat(state.isServerReady).isFalse()
    }

    @Test
    fun `LogViewerUiState with values`() {
        val files = listOf(File("test.log"))
        val state = LogViewerUiState(
            logFiles = files,
            serverAddress = "192.168.1.1:8080",
            isServerReady = true,
        )
        assertThat(state.logFiles).hasSize(1)
        assertThat(state.serverAddress).isEqualTo("192.168.1.1:8080")
        assertThat(state.isServerReady).isTrue()
    }

    // ── getLogTypeDisplayName ─────────────────────────────────────────

    @Test
    fun `getLogTypeDisplayName returns manual for manual log prefix`() {
        val file = File("logs_manual_2024-01-01_10:00:00.log")

        assertThat(viewModel.getLogTypeDisplayName(file)).isEqualTo("手动日志")
    }

    @Test
    fun `getLogTypeDisplayName returns crash for crash log prefix`() {
        val file = File("logs_crash_2024-01-01_11:00:00.log")

        assertThat(viewModel.getLogTypeDisplayName(file)).isEqualTo("崩溃日志")
    }

    @Test
    fun `getLogTypeDisplayName returns unknown for unrecognized prefix`() {
        val file = File("random_file.log")

        assertThat(viewModel.getLogTypeDisplayName(file)).isEqualTo("未知")
    }

    @Test
    fun `getLogTypeDisplayName returns unknown for file without prefix`() {
        val file = File("no_prefix.txt")

        assertThat(viewModel.getLogTypeDisplayName(file)).isEqualTo("未知")
    }

    // ── getFileUrl & getServerUrl ─────────────────────────────────────

    @Test
    fun `getFileUrl returns correct URL format with server address`() {
        val file = File("logs_manual_test.log")
        val url = viewModel.getFileUrl(file)

        assertThat(url).contains("http://")
        assertThat(url).contains("/api/logs/logs_manual_test.log")
    }

    @Test
    fun `getServerUrl returns correct URL format`() {
        val url = viewModel.getServerUrl()

        assertThat(url).startsWith("http://")
        assertThat(url).endsWith("/")
    }

    // ── refreshLogs ───────────────────────────────────────────────────

    @Test
    fun `refreshLogs merges all log sources sorted by lastModified desc`() {
        val manualLog = File(tempDir, "logs_manual_2024-01-01.log")
        manualLog.writeText("manual")
        manualLog.setLastModified(1000L)
        val crashLog = File(tempDir, "logs_crash_2024-01-02.log")
        crashLog.writeText("crash")
        crashLog.setLastModified(2000L)
        every { crashHandler.listManualLogs() } returns listOf(manualLog)
        every { crashHandler.listCrashLogs() } returns listOf(crashLog)

        viewModel.refreshLogs()
        Thread.sleep(300)

        val state = viewModel.uiState.value
        assertThat(state.logFiles).hasSize(2)
        assertThat(state.logFiles[0]).isEqualTo(crashLog)
        assertThat(state.logFiles[1]).isEqualTo(manualLog)
    }

    @Test
    fun `refreshLogs with no logs returns empty list`() {
        every { crashHandler.listManualLogs() } returns emptyList()
        every { crashHandler.listCrashLogs() } returns emptyList()
        viewModel.refreshLogs()
        Thread.sleep(300)

        assertThat(viewModel.uiState.value.logFiles).isEmpty()
    }

    // ── createManualLog ───────────────────────────────────────────────

    @Test
    fun `createManualLog calls crashHandler createManualLog`() {
        val manualLog = File(tempDir, "logs_manual_2024-01-03.log")
        manualLog.writeText("generated")
        manualLog.setLastModified(5000L)
        every { crashHandler.createManualLog() } returns manualLog
        every { crashHandler.listManualLogs() } returns listOf(manualLog)

        viewModel.createManualLog()
        Thread.sleep(300)

        verify { crashHandler.createManualLog() }
        assertThat(viewModel.uiState.value.logFiles).isNotEmpty()
    }

    // ── server lifecycle ──────────────────────────────────────────────

    @Test
    fun `onCleared stops httpServer`() {
        httpServer.stop()
        verify { httpServer.stop() }
    }
}
