package dev.frost819.newbv.app.viewmodel.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.network.HttpServer
import dev.frost819.newbv.core.log.CrashHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

/**
 * 日志查看页 ViewModel。
 *
 * 管理 [HttpServer] 生命周期和日志文件列表。
 * 进入页面时启动 HTTP 服务器，离开时停止。
 *
 * @param application 用于获取 filesDir 和 assets。
 * @param httpServer 本地 HTTP 日志服务器。
 * @param crashHandler 崩溃处理器（提供崩溃/手动日志列表）。
 */
@HiltViewModel
class LogViewerViewModel @Inject constructor(
    application: Application,
    private val httpServer: HttpServer,
    private val crashHandler: CrashHandler,
) : AndroidViewModel(application) {

    private val logger = KotlinLogging.logger("LogViewerViewModel")

    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()

    init {
        startServer()
        refreshLogs()
    }

    /**
     * 启动 HTTP 服务器并解析本地 IP。
     *
     * 在 IO 线程启动服务器，获取端口后更新 UI 状态。
     * 同时扫描 wlan0 接口获取 IPv4 地址，用于生成二维码 URL。
     */
    private fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            httpServer.start()
            val port = httpServer.getPort() ?: 0
            val host = getLocalIpAddress()
            _uiState.value = _uiState.value.copy(
                serverAddress = "$host:$port",
                isServerReady = port > 0,
            )
        }
    }

    /**
     * 刷新日志文件列表。
     *
     * 合并崩溃日志和手动日志，按最后修改时间降序排列。
     */
    fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = (
                crashHandler.listManualLogs() +
                    crashHandler.listCrashLogs()
                ).sortedByDescending { it.lastModified() }
            _uiState.value = _uiState.value.copy(logFiles = logs)
        }
    }

    /**
     * 创建手动日志（含 logcat 输出）。
     *
     * 创建后刷新列表。
     */
    fun createManualLog() {
        viewModelScope.launch(Dispatchers.IO) {
            crashHandler.createManualLog()
            refreshLogs()
        }
    }

    /**
     * 获取当前选中日志文件的下载 URL。
     *
     * @param file 日志文件。
     * @return 形如 `http://192.168.1.100:12345/api/logs/filename.log` 的 URL。
     */
    fun getFileUrl(file: File): String {
        val addr = _uiState.value.serverAddress
        return "http://$addr/api/logs/${file.name}"
    }

    /**
     * 获取服务器首页 URL（用于生成二维码）。
     *
     * @return 形如 `http://192.168.1.100:12345/` 的 URL。
     */
    fun getServerUrl(): String {
        val addr = _uiState.value.serverAddress
        return "http://$addr/"
    }

    override fun onCleared() {
        httpServer.stop()
        super.onCleared()
    }

    /**
     * 扫描 wlan0 接口获取本地 IPv4 地址。
     *
     * @return IPv4 地址字符串，获取失败返回空字符串。
     */
    private fun getLocalIpAddress(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull()
                ?.hostAddress ?: ""
        }.getOrDefault("")
    }

    /**
     * 获取日志文件的类型显示名。
     */
    fun getLogTypeDisplayName(file: File): String = when {
        file.name.startsWith(CrashHandler.MANUAL_LOG_PREFIX) -> "手动日志"
        file.name.startsWith(CrashHandler.CRASH_LOG_PREFIX) -> "崩溃日志"
        else -> "未知"
    }
}

/**
 * 日志查看页 UI 状态。
 *
 * @property logFiles 日志文件列表（按时间降序）。
 * @property serverAddress HTTP 服务器地址（`host:port`）。
 * @property isServerReady 服务器是否已启动并获取到端口。
 */
data class LogViewerUiState(
    val logFiles: List<File> = emptyList(),
    val serverAddress: String = "",
    val isServerReady: Boolean = false,
)
