package dev.frost819.newbv.core.log

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Logcat Logger 的插桩测试。
 *
 * 验证各日志级别和异常重载在真实 Android 环境中不会抛出异常。
 */
@RunWith(AndroidJUnit4::class)
class LoggerTest {
    @Test
    fun logger_writes_all_supported_levels() {
        val logger = Loggers.get("LoggerTest")
        val exception = IllegalStateException("test")

        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.warn(exception) { "warn with exception" }
        logger.error { "error" }
        logger.error(exception) { "error with exception" }
    }
}
