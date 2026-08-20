package dev.frost819.newbv.core.log

import android.content.Context
import android.os.Build
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
 * 将崩溃日志（含设备信息、应用日志和 logcat 输出）写入文件，
 * 然后转发给原始 handler 执行默认行为（如退出）。
 *
 * 文件命名：`logs_crash_YYYY-MM-dd_HH:mm:ss.log`
 * 日志目录：`filesDir/crash_logs`
 *
 * @param context 应用 Context（用于获取文件目录和包信息）。
 * @param maxLogCount 崩溃日志最大保留数量，超出删除最旧的。
 */
class CrashHandler(
    private val context: Context,
    private val maxLogCount: Int = 10
) {
    private val logger = Loggers.get("CrashHandler")

    companion object {
        const val LOG_DIR = "crash_logs"
        const val CRASH_LOG_PREFIX = "logs_crash"
        const val MANUAL_LOG_PREFIX = "logs_manual"
    }

    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * 崩溃上传器。
     *
     * 由 app 层通过 Hilt DI 注入后设置。
     * 为 null 表示上传功能不可用（token 未配置或未启用）。
     */
    var crashUploader: CrashUploader? = null

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

            val deviceInfo = collectDeviceInfo()

            // 先写入设备信息和异常，再追加 Logcat，确保应用 Logger 输出也被保留。
            writeCrashContext(crashFile, deviceInfo, thread, exception)

            // 收集 logcat 文本（用于文本日志和 JSON 上传）
            val logcatText = collectLogcatText()

            // 追加 logcat 输出到文本日志
            appendLogcatText(crashFile, logcatText)

            // best-effort 同步上传崩溃日志
            crashUploader?.uploadCrash(deviceInfo, thread, exception, logcatText)
        }.onFailure { error ->
            logger.error(error) { "Failed to write crash log" }
        }
    }

    /**
     * 收集 logcat 输出为文本。
     *
     * 读取最近 10000 行 logcat（threadtime 格式）。
     *
     * @return logcat 文本，失败返回空字符串。
     */
    private fun collectLogcatText(): String = runCatching {
        val process = Runtime.getRuntime().exec("logcat -t 10000 -v threadtime")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.appendLine(line)
        }
        reader.close()
        sb.toString()
    }.onFailure {
        logger.error(it) { "Failed to collect logcat" }
    }.getOrDefault("")

    /**
     * 将 logcat 文本追加到文件。
     */
    private fun appendLogcatText(file: File, logcatText: String) {
        OutputStreamWriter(FileOutputStream(file, true)).use { writer ->
            writer.appendLine("======== Logcat ========")
            writer.append(logcatText)
            writer.flush()
        }
    }

    private fun writeCrashContext(
        file: File,
        deviceInfo: DeviceInfo,
        thread: Thread,
        throwable: Throwable,
    ) {
        OutputStreamWriter(FileOutputStream(file, true)).use { writer ->
            writer.append(
                LogFormat.crashHeader(
                    appVersion = deviceInfo.appVersion,
                    appVersionCode = deviceInfo.appVersionCode,
                    androidVersion = deviceInfo.androidVersion,
                    androidSdk = deviceInfo.androidSdk,
                    device = deviceInfo.device,
                    model = deviceInfo.model,
                    manufacturer = deviceInfo.manufacturer,
                )
            )
            writer.appendLine("======== Exception ========")
            writer.appendLine("Thread: ${thread.name}")
            writer.appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            throwable.stackTrace.forEach { writer.appendLine("    at $it") }
            throwable.cause?.let { cause ->
                writer.appendLine("Caused by: ${cause.javaClass.name}: ${cause.message}")
                cause.stackTrace.forEach { writer.appendLine("    at $it") }
            }
            writer.appendLine("================================")
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

/**
 * 崩溃日志中的设备与应用信息。
 *
 * @property appVersion 应用版本名。
 * @property appVersionCode 应用版本号。
 * @property androidVersion Android 版本。
 * @property androidSdk Android SDK 版本。
 * @property device 设备代号。
 * @property model 设备型号。
 * @property manufacturer 设备制造商。
 */
data class DeviceInfo(
    val appVersion: String,
    val appVersionCode: Int,
    val androidVersion: String,
    val androidSdk: Int,
    val device: String,
    val model: String,
    val manufacturer: String,
)
