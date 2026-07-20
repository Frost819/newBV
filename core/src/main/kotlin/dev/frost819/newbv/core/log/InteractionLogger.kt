package dev.frost819.newbv.core.log

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 交互日志记录器。
 *
 * 记录用户交互事件（导航、播放、设置等），异步写入文件，
 * 同时在内存维护最近 [ringBufferSize] 条日志供崩溃报告引用。
 *
 * 文件命名：`logs_interaction_YYYYMMDD.log`
 * 单文件上限 [maxFileSizeBytes]（默认 10MB），超出后滚动到新文件。
 * 保留最近 [maxAgeDays] 天的日志（默认 7 天）。
 *
 * @param logDir 日志目录。
 * @param ringBufferSize 内存环形缓冲区大小（供崩溃报告引用）。
 * @param maxFileSizeBytes 单文件大小上限。
 * @param maxAgeDays 日志保留天数。
 * @param clock 时间源，便于测试。
 */
class InteractionLogger(
    private val logDir: File,
    private val ringBufferSize: Int = 100,
    private val maxFileSizeBytes: Long = 10 * 1024 * 1024,
    private val maxAgeDays: Int = 7,
    private val clock: () -> Long = System::currentTimeMillis,
    private val writeDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = KotlinLogging.logger("InteractionLogger")
    private val scope = CoroutineScope(SupervisorJob() + writeDispatcher)
    private val writeMutex = Mutex()
    private val ringBuffer = ArrayDeque<String>()
    private val ringMutex = Mutex()

    companion object {
        const val FILE_PREFIX = "logs_interaction"
        private val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    }

    /**
     * 记录一条交互日志。
     *
     * @param level 日志级别。
     * @param category 日志类别。
     * @param action 操作名称。
     * @param detail 附加详情键值对。
     */
    fun log(
        level: LogLevel = LogLevel.INFO,
        category: LogCategory,
        action: String,
        detail: Map<String, String> = emptyMap()
    ) {
        val entry = LogFormat.interactionEntry(clock(), level, category.name, action, detail)
        addToRingBuffer(entry)
        writeToEntry(entry)
    }

    /**
     * 记录一条交互日志（简化版）。
     */
    fun log(category: LogCategory, action: String, detail: Map<String, String> = emptyMap()) {
        log(LogLevel.INFO, category, action, detail)
    }

    /**
     * 获取最近 [count] 条交互日志（用于崩溃报告）。
     */
    suspend fun getRecentEntries(count: Int = ringBufferSize): List<String> {
        ringMutex.withLock {
            return ringBuffer.takeLast(minOf(count, ringBuffer.size)).toList()
        }
    }

    /**
     * 同步获取最近 [count] 条交互日志（非挂起版本）。
     */
    fun getRecentEntriesSnapshot(count: Int = ringBufferSize): List<String> {
        return synchronized(ringBuffer) {
            ringBuffer.takeLast(minOf(count, ringBuffer.size)).toList()
        }
    }

    /**
     * 列出所有交互日志文件（按修改时间排序）。
     */
    fun listLogFiles(): List<File> =
        logDir.listFiles { file -> file.name.startsWith(FILE_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /**
     * 清理过期日志文件。
     *
     * 删除超过 [maxAgeDays] 天的日志文件。
     */
    fun cleanupOldLogs() {
        val cutoff = clock() - maxAgeDays * 24 * 60 * 60 * 1000L
        logDir.listFiles { file -> file.name.startsWith(FILE_PREFIX) }
            ?.filter { it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    /**
     * 生成崩溃上下文日志。
     *
     * 包含设备信息和最近交互日志，写入崩溃日志文件。
     *
     * @param crashFile 崩溃日志文件。
     * @param deviceInfo 设备信息。
     * @param throwable 异常对象。
     */
    suspend fun writeCrashContext(
        crashFile: File,
        deviceInfo: DeviceInfo,
        throwable: Throwable
    ) {
        val recentEntries = getRecentEntries()
        writeMutex.withLock {
            PrintWriter(FileWriter(crashFile, true)).use { writer ->
                writer.println(LogFormat.crashHeader(
                    appVersion = deviceInfo.appVersion,
                    appVersionCode = deviceInfo.appVersionCode,
                    androidVersion = deviceInfo.androidVersion,
                    androidSdk = deviceInfo.androidSdk,
                    device = deviceInfo.device,
                    model = deviceInfo.model,
                    manufacturer = deviceInfo.manufacturer,
                    recentInteractionCount = recentEntries.size
                ))
                recentEntries.forEach { writer.println(it) }
                writer.println("======== Exception ========")
                writer.println("Thread: ${Thread.currentThread().name}")
                writer.println("Exception: ${throwable.javaClass.name}: ${throwable.message}")
                throwable.stackTrace.forEach { writer.println("    at $it") }
                throwable.cause?.let { cause ->
                    writer.println("Caused by: ${cause.javaClass.name}: ${cause.message}")
                    cause.stackTrace.forEach { writer.println("    at $it") }
                }
                writer.println("================================")
            }
        }
    }

    private fun addToRingBuffer(entry: String) {
        synchronized(ringBuffer) {
            ringBuffer.addLast(entry)
            while (ringBuffer.size > ringBufferSize) {
                ringBuffer.removeFirst()
            }
        }
    }

    private fun writeToEntry(entry: String) {
        scope.launch {
            writeMutex.withLock {
                runCatching {
                    if (!logDir.exists()) logDir.mkdirs()
                    val targetFile = getTargetFile()
                    if (targetFile.length() >= maxFileSizeBytes) {
                        // 当前文件已满，创建新文件
                        val newFile = File(logDir, createFilename())
                        targetFile.copyTo(newFile, overwrite = true)
                        targetFile.writeText(entry + "\n")
                    } else {
                        targetFile.appendText(entry + "\n")
                    }
                }.onFailure { error ->
                    logger.error(error) { "Failed to write interaction log" }
                }
            }
        }
    }

    private fun getTargetFile(): File {
        val today = dateFormatter.format(Date(clock()))
        val filename = "${FILE_PREFIX}_$today.log"
        return File(logDir, filename)
    }

    private fun createFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(clock()))
        return "${FILE_PREFIX}_$timestamp.log"
    }
}

/**
 * 设备信息，供日志记录使用。
 */
data class DeviceInfo(
    val appVersion: String,
    val appVersionCode: Int,
    val androidVersion: String,
    val androidSdk: Int,
    val device: String,
    val model: String,
    val manufacturer: String
)
