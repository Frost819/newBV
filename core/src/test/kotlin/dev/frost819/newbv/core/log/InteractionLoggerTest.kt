package dev.frost819.newbv.core.log

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [InteractionLogger] 的单元测试。
 *
 * 验证环形缓冲区、文件写入、日志清理逻辑。
 * 使用 [TempDir] 隔离文件系统，[UnconfinedTestDispatcher] 同步执行协程。
 */
class InteractionLoggerTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var logger: InteractionLogger

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        logger = InteractionLogger(
            logDir = tempDir,
            ringBufferSize = 5,
            maxFileSizeBytes = 1024,
            maxAgeDays = 7,
            clock = { 1700000000000L },
            writeDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `log adds entry to ring buffer`() {
        logger.log(LogCategory.NAV, "navigate", mapOf("to" to "home"))
        logger.log(LogCategory.PLAY, "play")

        val entries = logger.getRecentEntriesSnapshot()
        assertThat(entries).hasSize(2)
        assertThat(entries[0]).contains("navigate")
        assertThat(entries[1]).contains("play")
    }

    @Test
    fun `ring buffer keeps only last N entries`() {
        repeat(10) { i ->
            logger.log(LogCategory.CARD, "click_$i")
        }

        val entries = logger.getRecentEntriesSnapshot()
        assertThat(entries).hasSize(5)
        assertThat(entries[0]).contains("click_5")
        assertThat(entries[4]).contains("click_9")
    }

    @Test
    fun `log writes to file`() = runBlocking {
        logger.log(LogCategory.NAV, "test_action")

        val files = logger.listLogFiles()
        assertThat(files).isNotEmpty()

        val content = files.first().readText()
        assertThat(content).contains("test_action")
        assertThat(content).contains("[NAV]")
    }

    @Test
    fun `log file has correct prefix`() = runBlocking {
        logger.log(LogCategory.NAV, "test")

        val files = logger.listLogFiles()
        assertThat(files).isNotEmpty()
        assertThat(files.first().name).startsWith("logs_interaction_")
    }

    @Test
    fun `multiple logs append to same file`() = runBlocking {
        logger.log(LogCategory.NAV, "action1")
        logger.log(LogCategory.NAV, "action2")
        logger.log(LogCategory.NAV, "action3")

        val files = logger.listLogFiles()
        assertThat(files).hasSize(1)

        val content = files.first().readText()
        assertThat(content).contains("action1")
        assertThat(content).contains("action2")
        assertThat(content).contains("action3")
    }

    @Test
    fun `cleanupOldLogs removes files older than maxAgeDays`() {
        val oldFile = File(tempDir, "logs_interaction_20200101.log")
        oldFile.writeText("old log")
        oldFile.setLastModified(0)

        logger.cleanupOldLogs()

        assertThat(oldFile.exists()).isFalse()
    }

    @Test
    fun `cleanupOldLogs keeps recent files`() = runBlocking {
        logger.log(LogCategory.NAV, "recent")
        logger.cleanupOldLogs()

        val files = logger.listLogFiles()
        assertThat(files).isNotEmpty()
    }

    @Test
    fun `getRecentEntries returns suspend version correctly`() = runBlocking {
        logger.log(LogCategory.NAV, "entry1")
        logger.log(LogCategory.NAV, "entry2")

        val entries = logger.getRecentEntries()
        assertThat(entries).hasSize(2)
        assertThat(entries[0]).contains("entry1")
        assertThat(entries[1]).contains("entry2")
    }

    @Test
    fun `writeCrashContext writes device info and recent entries`() = runBlocking {
        logger.log(LogCategory.NAV, "action_before_crash")

        val crashFile = File(tempDir, "test_crash.log")
        val deviceInfo = DeviceInfo(
            appVersion = "1.0.0",
            appVersionCode = 100,
            androidVersion = "14",
            androidSdk = 34,
            device = "Pixel",
            model = "Pixel 8",
            manufacturer = "Google"
        )
        val exception = RuntimeException("Test crash")

        logger.writeCrashContext(crashFile, deviceInfo, exception)

        val content = crashFile.readText()
        assertThat(content).contains("new BV Crash")
        assertThat(content).contains("App Version: 1.0.0 (100)")
        assertThat(content).contains("action_before_crash")
        assertThat(content).contains("RuntimeException")
        assertThat(content).contains("Test crash")
    }

    @Test
    fun `log with different levels formats correctly`() {
        logger.log(LogLevel.WARN, LogCategory.PLAY, "buffering")
        logger.log(LogLevel.ERROR, LogCategory.EXCEPTION, "playback_failed")

        val entries = logger.getRecentEntriesSnapshot()
        assertThat(entries[0]).contains("[WARN]")
        assertThat(entries[1]).contains("[ERROR]")
    }
}
