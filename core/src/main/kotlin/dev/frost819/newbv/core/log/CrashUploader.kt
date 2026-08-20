package dev.frost819.newbv.core.log

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 崩溃日志上传器。
 *
 * 将崩溃日志转为结构化 JSON 并上传到 Cloudflare Worker（R2 Storage）。
 * 采用双重保障策略：
 * 1. 崩溃时 best-effort 同步上传（应用即将退出，可能失败）
 * 2. 下次启动时检查未上传的 JSON 文件并异步上传
 *
 * 上传失败静默忽略，不影响用户体验。
 *
 * JSON 文件存储在 `filesDir/crash_logs/` 下，命名为 `logs_crash_*.json`。
 * 上传成功后删除 JSON 文件；`.log` 文本日志保留供本地查看。
 *
 * @param context 应用 Context（用于获取文件目录和包信息）
 * @param authToken Worker 认证 token（为空则不上传）
 */
class CrashUploader(
    private val context: Context,
    private val authToken: String,
) {
    private val logger = Loggers.get("CrashUploader")

    /**
     * 是否启用上传。
     *
     * 由 app 层根据用户设置 ([dev.frost819.newbv.data.datastore.Prefs.crashReportEnabled]) 控制。
     * 默认关闭，用户在设置中手动开启后才会上传。
     */
    @Volatile
    var enabled: Boolean = false

    companion object {
        private const val WORKER_URL = "https://crash-log.frost819.site"
        private const val UPLOAD_PATH = "/api/crash/upload"
        private const val UPLOAD_TIMEOUT_MS = 5_000
        private const val CRASH_JSON_PREFIX = "logs_crash"
        private const val CRASH_JSON_SUFFIX = ".json"
    }

    /**
     * 崩溃时同步上传（best-effort）。
     *
     * 将崩溃数据转为 JSON 保存到文件，然后尝试同步上传。
     * 如果上传失败，JSON 文件保留在磁盘上，下次启动时重试。
     *
     * @param deviceInfo 设备信息
     * @param thread 崩溃线程
     * @param throwable 异常
     * @param logcatText 崩溃前的 logcat 输出
     */
    fun uploadCrash(
        deviceInfo: DeviceInfo,
        thread: Thread,
        throwable: Throwable,
        logcatText: String,
    ) {
        if (!enabled || authToken.isBlank()) return

        val json = buildCrashJson(deviceInfo, thread, throwable, logcatText)
        val jsonFile = saveJsonFile(json)

        if (uploadJson(json)) {
            jsonFile.delete()
            logger.info { "Crash log uploaded successfully" }
        } else {
            logger.warn { "Crash log upload failed, will retry on next launch" }
        }
    }

    /**
     * 上传所有未发送的崩溃日志。
     *
     * 在应用启动时调用，扫描 `crash_logs/` 目录下的 `.json` 文件，
     * 逐个上传，成功后删除。
     */
    fun uploadPendingCrashLogs() {
        if (!enabled || authToken.isBlank()) return

        val logDir = File(context.filesDir, CrashHandler.LOG_DIR)
        val jsonFiles = logDir.listFiles { file ->
            file.name.startsWith(CRASH_JSON_PREFIX) &&
                file.name.endsWith(CRASH_JSON_SUFFIX)
        }?.sortedBy { it.lastModified() } ?: return

        if (jsonFiles.isEmpty()) return

        logger.info { "Found ${jsonFiles.size} unsent crash log(s)" }

        for (file in jsonFiles) {
            val json = runCatching { file.readText() }.getOrNull() ?: continue
            if (uploadJson(json)) {
                file.delete()
                logger.info { "Uploaded pending crash log: ${file.name}" }
            } else {
                logger.warn { "Failed to upload: ${file.name}, will retry later" }
                break
            }
        }
    }

    /**
     * 构建 CrashUploader 是否可用（enabled + token 非空）。
     */
    fun canUpload(): Boolean = enabled && authToken.isNotBlank()

    // -----------------------------------------------------------------------
    // JSON 构建
    // -----------------------------------------------------------------------

    /**
     * 将崩溃数据构建为结构化 JSON 字符串。
     */
    private fun buildCrashJson(
        deviceInfo: DeviceInfo,
        thread: Thread,
        throwable: Throwable,
        logcatText: String,
    ): String {
        val json = JSONObject()

        json.put("id", UUID.randomUUID().toString())
        json.put("versionCode", deviceInfo.appVersionCode)
        json.put("versionName", deviceInfo.appVersion)
        json.put("crashTime", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()))

        val device = JSONObject()
        device.put("manufacturer", deviceInfo.manufacturer)
        device.put("model", deviceInfo.model)
        device.put("androidVersion", deviceInfo.androidVersion)
        device.put("sdk", deviceInfo.androidSdk)
        device.put("device", deviceInfo.device)
        json.put("deviceInfo", device)

        val exception = JSONObject()
        exception.put("thread", thread.name)
        exception.put("type", throwable.javaClass.name)
        exception.put("message", throwable.message ?: "")

        val stackTrace = StringBuilder()
        stackTrace.append("${throwable.javaClass.name}: ${throwable.message}")
        stackTrace.append("\n")
        throwable.stackTrace.forEach {
            stackTrace.append("    at $it\n")
        }
        throwable.cause?.let { cause ->
            stackTrace.append("Caused by: ${cause.javaClass.name}: ${cause.message}\n")
            cause.stackTrace.forEach {
                stackTrace.append("    at $it\n")
            }
        }
        exception.put("stackTrace", stackTrace.toString().trimEnd())
        json.put("exception", exception)

        json.put("logcat", logcatText)

        return json.toString()
    }

    /**
     * 将 JSON 保存到 `crash_logs/` 目录下的文件。
     *
     * @return 保存的文件
     */
    private fun saveJsonFile(json: String): File {
        val logDir = File(context.filesDir, CrashHandler.LOG_DIR)
        if (!logDir.exists()) logDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(Date())
        val jsonFile = File(logDir, "${CrashHandler.CRASH_LOG_PREFIX}_$timestamp$CRASH_JSON_SUFFIX")
        jsonFile.writeText(json)
        return jsonFile
    }

    // -----------------------------------------------------------------------
    // HTTP 上传
    // -----------------------------------------------------------------------

    /**
     * 同步上传 JSON 到 Worker。
     *
     * @param json JSON 字符串
     * @return 是否上传成功
     */
    private fun uploadJson(json: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$WORKER_URL$UPLOAD_PATH")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = UPLOAD_TIMEOUT_MS
                readTimeout = UPLOAD_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Authorization", "Bearer $authToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                true
            } else {
                logger.warn { "Upload failed: HTTP $responseCode" }
                false
            }
        } catch (e: Exception) {
            logger.warn { "Upload exception: ${e.message}" }
            false
        } finally {
            connection?.disconnect()
        }
    }
}
