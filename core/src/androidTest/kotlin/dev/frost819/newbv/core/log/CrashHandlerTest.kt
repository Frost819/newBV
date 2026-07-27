package dev.frost819.newbv.core.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * [CrashHandler] 的插桩测试。
 *
 * 在真实 Android 环境中验证崩溃日志文件创建、手动日志生成、
 * 日志列表与清理逻辑。
 */
@RunWith(AndroidJUnit4::class)
class CrashHandlerTest {

    private lateinit var context: Context
    private lateinit var crashHandler: CrashHandler
    private lateinit var interactionLogger: InteractionLogger
    private lateinit var logDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        logDir = File(context.filesDir, CrashHandler.LOG_DIR)
        logDir.deleteRecursively()
        logDir.mkdirs()

        interactionLogger = InteractionLogger(
            logDir = logDir,
            ringBufferSize = 50,
            writeDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )
        crashHandler = CrashHandler(
            context = context,
            interactionLogger = interactionLogger,
            maxLogCount = 3
        )
    }

    @After
    fun teardown() {
        crashHandler.uninstall()
        logDir.deleteRecursively()
    }

    @Test
    fun createManualLog_creates_file_with_device_info() {
        val logFile = crashHandler.createManualLog()

        assertThat(logFile).isNotNull()
        assertThat(logFile!!.exists()).isTrue()

        val content = logFile.readText()
        assertThat(content).contains("======== Device info ========")
        assertThat(content).contains("App Version:")
        assertThat(content).contains("Android Version:")
        assertThat(content).contains("Device:")
        assertThat(content).contains("======== Logs ========")
    }

    @Test
    fun listCrashLogs_returns_empty_when_no_crashes() {
        val logs = crashHandler.listCrashLogs()
        assertThat(logs).isEmpty()
    }

    @Test
    fun listManualLogs_returns_created_manual_logs() {
        crashHandler.createManualLog()
        // 文件名以时间戳命名，同秒内可能覆盖，确保至少 1 个
        val logs = crashHandler.listManualLogs()
        assertThat(logs).isNotEmpty()
        assertThat(logs.first().name).startsWith(CrashHandler.MANUAL_LOG_PREFIX)
    }

    @Test
    fun cleanupOldLogFiles_removes_files_exceeding_maxLogCount() {
        // 创建 5 个崩溃日志文件（超过 maxLogCount=3）
        repeat(5) { index ->
            val file = File(logDir, "${CrashHandler.CRASH_LOG_PREFIX}_2024-01-0${index}_10:00:00.log")
            file.writeText("fake crash log $index")
            // 设置不同的修改时间以确保排序
            file.setLastModified(System.currentTimeMillis() - (5 - index) * 1000L)
        }

        crashHandler.cleanupOldLogFiles()

        val remaining = crashHandler.listCrashLogs()
        assertThat(remaining).hasSize(3)
    }

    @Test
    fun install_sets_default_uncaught_exception_handler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        crashHandler.install()

        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        assertThat(currentHandler).isNotSameInstanceAs(originalHandler)

        crashHandler.uninstall()
        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isSameInstanceAs(originalHandler)
    }

    @Test
    fun uninstall_restores_original_handler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        crashHandler.install()
        crashHandler.uninstall()

        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isSameInstanceAs(originalHandler)
    }

    @Test
    fun handleCrash_creates_crash_log_file() {
        crashHandler.install()

        // 记录一些交互日志
        interactionLogger.log(LogCategory.PLAY, "play_video", mapOf("aid" to "123"))
        interactionLogger.log(LogCategory.NAV, "navigate", mapOf("route" to "detail"))

        // 模拟崩溃
        val exception = RuntimeException("Test crash for instrumentation test")
        val thread = Thread.currentThread()
        // 直接调用 uncaughtExceptionHandler 触发处理（不真正崩溃进程）
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)

        val crashLogs = crashHandler.listCrashLogs()
        assertThat(crashLogs).isNotEmpty()

        val content = crashLogs.first().readText()
        assertThat(content).contains("Test crash for instrumentation test")
        assertThat(content).contains("Exception")
        assertThat(content).contains("======== Logcat ========")
    }

    @Test
    fun handleCrash_includes_recent_interaction_logs() {
        crashHandler.install()

        interactionLogger.log(LogCategory.PLAY, "play_video", mapOf("aid" to "123"))
        interactionLogger.log(LogCategory.NAV, "navigate", mapOf("route" to "detail"))

        val exception = IllegalStateException("Crash with context")
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(Thread.currentThread(), exception)

        val crashLogs = crashHandler.listCrashLogs()
        assertThat(crashLogs).isNotEmpty()
        val content = crashLogs.first().readText()
        // 崩溃日志应包含交互记录的头部标识
        assertThat(content).contains("play_video")
        assertThat(content).contains("navigate")
    }

    @Test
    fun handleCrash_writes_device_info_section() {
        crashHandler.install()

        val exception = RuntimeException("Device info test")
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(Thread.currentThread(), exception)

        val crashLogs = crashHandler.listCrashLogs()
        assertThat(crashLogs).isNotEmpty()
        val content = crashLogs.first().readText()
        assertThat(content).contains("App Version")
        assertThat(content).contains("Android Version")
        assertThat(content).contains("Manufacturer")
    }

    @Test
    fun createManualLog_creates_log_directory_if_not_exists() {
        logDir.deleteRecursively()
        assertThat(logDir.exists()).isFalse()

        val logFile = crashHandler.createManualLog()

        assertThat(logFile).isNotNull()
        assertThat(logDir.exists()).isTrue()
    }
}
