package dev.frost819.newbv.core.log

import android.content.Context
import android.os.Build
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃处理器。
 *
 * 通过 [Thread.setDefaultUncaughtExceptionHandler] 捕获未处理异常，
 * 将崩溃日志（含设备信息、最近交互日志、logcat 输出）写入文件，
 * 然后转发给原始 handler 执行默认行为（如退出）。
 *
 * 文件命名：`logs_crash_YYYY-MM-dd_HH:mm:ss.log`
 * 日志目录：`filesDir/crash_logs`
 *
 * @param context 应用 Context（用于获取文件目录和包信息）。
 * @param interactionLogger 交互日志记录器（提供最近交互上下文）。
 * @param maxLogCount 崩溃日志最大保留数量，超出删除最旧的。
 */
class CrashHandler(
    private val context: Context,
    private val interactionLogger: InteractionLogger,
    private val maxLogCount: Int = 10
) {
    private val logger = KotlinLogging.logger("CrashHandler")

    companion object {
        const val LOG_DIR = "crash_logs"
        const val CRASH_LOG_PREFIX = "logs_crash"
        const val MANUAL_LOG_PREFIX = "logs_manual"
    }

    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * 安装崩溃处理器。
     *
     * 清空 logcat 缓冲，设置全局异常处理器。
     * 仅应调用一次。
     */
    fun install() {
        runCatching {
            Runtime.getRuntime().exec("logcat -c")
            logger.info { "Cleared logcat buffer" }
        }

        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            logger.error(exception) { "======== UncaughtException on ${thread.name} ========" }
            handleCrash(thread, exception)
            originalHandler?.uncaughtException(thread, exception)
        }

        cleanupOldLogFiles()
    }

    /**
     * 卸载崩溃处理器，恢复原始 handler。
     */
    fun uninstall() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    /**
     * 手动生成日志文件（含 logcat 输出）。
     *
     * @return 生成的日志文件，失败返回 null。
     */
    fun createManualLog(): File? = runCatching {
        val process = Runtime.getRuntime().exec("logcat -t 10000 -v threadtime")
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) logDir.mkdirs()

        val logFile = File(logDir, createFilename(manual = true))
        logFile.createNewFile()

        with(logFile.writer()) {
            writeDeviceInfo(this)
            appendLine("======== Logs ========")
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                appendLine(line)
            }
            flush()
            close()
            reader.close()
        }
        logFile
    }.onFailure {
        logger.error(it) { "Failed to create manual log" }
    }.getOrNull()

    /**
     * 列出所有崩溃日志文件。
     */
    fun listCrashLogs(): List<File> =
        File(context.filesDir, LOG_DIR)
            .listFiles { it.name.startsWith(CRASH_LOG_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /**
     * 列出所有手动日志文件。
     */
    fun listManualLogs(): List<File> =
        File(context.filesDir, LOG_DIR)
            .listFiles { it.name.startsWith(MANUAL_LOG_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /**
     * 刷新日志文件列表并清理超出 [maxLogCount] 的旧文件。
     */
    fun cleanupOldLogFiles() {
        val crashFiles = listCrashLogs()
        if (crashFiles.size > maxLogCount) {
            crashFiles.takeLast(crashFiles.size - maxLogCount).forEach { it.delete() }
        }
        val manualFiles = listManualLogs()
        if (manualFiles.size > maxLogCount) {
            manualFiles.takeLast(manualFiles.size - maxLogCount).forEach { it.delete() }
        }
    }

    private fun handleCrash(thread: Thread, exception: Throwable) {
        runCatching {
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) logDir.mkdirs()

            val crashFile = File(logDir, createFilename(manual = false))
            crashFile.createNewFile()

            // 写入崩溃上下文（设备信息 + 最近交互日志 + 异常堆栈）
            val deviceInfo = collectDeviceInfo()
            kotlinx.coroutines.runBlocking {
                interactionLogger.writeCrashContext(crashFile, deviceInfo, exception)
            }

            // 追加 logcat 输出
            appendLogcat(crashFile)
        }.onFailure { error ->
            logger.error(error) { "Failed to write crash log" }
        }
    }

    private fun appendLogcat(file: File) {
        runCatching {
            val process = Runtime.getRuntime().exec("logcat -t 10000 -v threadtime")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            // 使用 append 模式，避免覆盖 writeCrashContext 已写入的内容
            OutputStreamWriter(FileOutputStream(file, true)).use { writer ->
                writer.appendLine("======== Logcat ========")
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    writer.appendLine(line)
                }
                writer.flush()
            }
            reader.close()
        }
    }

    private fun collectDeviceInfo(): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return DeviceInfo(
            appVersion = packageInfo.versionName ?: "unknown",
            appVersionCode = packageInfo.longVersionCode.toInt(),
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            androidSdk = Build.VERSION.SDK_INT,
            device = Build.DEVICE ?: "unknown",
            model = Build.MODEL ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown"
        )
    }

    private fun writeDeviceInfo(writer: OutputStreamWriter) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        writer.apply {
            appendLine("======== Device info ========")
            appendLine("App Version: ${info.versionName} (${info.longVersionCode})")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Product: ${Build.PRODUCT}")
        }
    }

    private fun createFilename(manual: Boolean): String {
        val prefix = if (manual) MANUAL_LOG_PREFIX else CRASH_LOG_PREFIX
        val date = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(Date())
        return "${prefix}_$date.log"
    }
}
