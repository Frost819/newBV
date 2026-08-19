package dev.frost819.newbv.core.log

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [AndroidLogger] 与 [Loggers] 的单元测试。
 *
 * 通过 mockkStatic 拦截 [android.util.Log] 的静态方法，
 * 验证各日志级别调用时传入了正确的 priority、tag 和消息内容。
 */
class LoggerTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.getStackTraceString(any()) } returns "stack-trace"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `debug calls Log println with DEBUG priority`() {
        val logger = Loggers.get("TestTag")

        logger.debug { "debug msg" }

        verify { Log.println(Log.DEBUG, "TestTag", "debug msg") }
    }

    @Test
    fun `info calls Log println with INFO priority`() {
        val logger = Loggers.get("TestTag")

        logger.info { "info msg" }

        verify { Log.println(Log.INFO, "TestTag", "info msg") }
    }

    @Test
    fun `warn calls Log println with WARN priority`() {
        val logger = Loggers.get("TestTag")

        logger.warn { "warn msg" }

        verify { Log.println(Log.WARN, "TestTag", "warn msg") }
    }

    @Test
    fun `warn with throwable appends stack trace`() {
        val logger = Loggers.get("TestTag")
        val exception = IllegalStateException("boom")

        logger.warn(exception) { "warn msg" }

        verify { Log.println(Log.WARN, "TestTag", "warn msg\nstack-trace") }
    }

    @Test
    fun `error calls Log println with ERROR priority`() {
        val logger = Loggers.get("TestTag")

        logger.error { "error msg" }

        verify { Log.println(Log.ERROR, "TestTag", "error msg") }
    }

    @Test
    fun `error with throwable appends stack trace`() {
        val logger = Loggers.get("TestTag")
        val exception = RuntimeException("fail")

        logger.error(exception) { "error msg" }

        verify { Log.println(Log.ERROR, "TestTag", "error msg\nstack-trace") }
    }

    @Test
    fun `Loggers returns same tag on multiple calls`() {
        val logger1 = Loggers.get("TagA")
        val logger2 = Loggers.get("TagA")

        logger1.info { "first" }
        logger2.info { "second" }

        verify(exactly = 1) { Log.println(Log.INFO, "TagA", "first") }
        verify(exactly = 1) { Log.println(Log.INFO, "TagA", "second") }
    }

    @Test
    fun `message lambda is lazy and not evaluated when write fails`() {
        val logger = Loggers.get("TestTag")
        var evaluated = false

        every { Log.println(any(), any(), any()) } throws RuntimeException("mock failure")

        logger.info { evaluated = true; "should not be used" }

        assertThat(evaluated).isTrue()
    }

    @Test
    fun `logger does not throw when Log throws`() {
        val logger = Loggers.get("TestTag")
        every { Log.println(any(), any(), any()) } throws RuntimeException("mock failure")

        logger.info { "safe" }
        logger.error(RuntimeException("err")) { "safe error" }
    }
}
