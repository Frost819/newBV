package dev.frost819.newbv.biliapi.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * [BiliLogger] 的单元测试。
 *
 * 验证 JVM 模块使用标准输出记录日志，并保留级别与异常上下文。
 */
class BiliLoggerTest {
    private lateinit var originalOut: PrintStream
    private lateinit var output: ByteArrayOutputStream

    @BeforeEach
    fun setUp() {
        originalOut = System.out
        output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `info and warn include bili api prefix`() {
        BiliLogger.info { "request started" }
        BiliLogger.warn { "fallback" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][INFO] request started")
        assertThat(text).contains("[bili-api][WARN] fallback")
    }

    @Test
    fun `warn with throwable includes stack trace`() {
        BiliLogger.warn(IllegalStateException("warn-broken")) { "warn with exception" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][WARN] warn with exception")
        assertThat(text).contains("IllegalStateException: warn-broken")
    }

    @Test
    fun `error without throwable includes prefix`() {
        BiliLogger.error { "simple error" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][ERROR] simple error")
    }

    @Test
    fun `error with throwable includes stack trace`() {
        BiliLogger.error(IllegalStateException("broken")) { "request failed" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][ERROR] request failed")
        assertThat(text).contains("IllegalStateException: broken")
    }
}
