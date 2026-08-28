package dev.frost819.newbv.core.log

import android.util.Log

/**
 * 应用诊断日志接口。
 *
 * 消息使用惰性 lambda 构造，未启用对应日志级别时不会执行字符串插值。
 */
interface Logger {
    /** 记录 Debug 级别日志。 */
    fun debug(message: () -> String)

    /** 记录 Info 级别日志。 */
    fun info(message: () -> String)

    /** 记录 Warn 级别日志。 */
    fun warn(message: () -> String)

    /** 记录带异常的 Warn 级别日志。 */
    fun warn(
        throwable: Throwable,
        message: () -> String,
    )

    /** 记录 Error 级别日志。 */
    fun error(message: () -> String)

    /** 记录带异常的 Error 级别日志。 */
    fun error(
        throwable: Throwable,
        message: () -> String,
    )
}

/**
 * Android Logcat Logger 实现。
 *
 * Android 单条 Log 消息存在长度限制，因此过长消息会拆分后输出。
 *
 * @param tag Logcat 标签。
 */
private class AndroidLogger(
    private val tag: String,
) : Logger {
    override fun debug(message: () -> String) {
        write(Log.DEBUG, message)
    }

    override fun info(message: () -> String) {
        write(Log.INFO, message)
    }

    override fun warn(message: () -> String) {
        write(Log.WARN, message)
    }

    override fun warn(
        throwable: Throwable,
        message: () -> String,
    ) {
        write(Log.WARN, message, throwable)
    }

    override fun error(message: () -> String) {
        write(Log.ERROR, message)
    }

    override fun error(
        throwable: Throwable,
        message: () -> String,
    ) {
        write(Log.ERROR, message, throwable)
    }

    private fun write(
        priority: Int,
        message: () -> String,
        throwable: Throwable? = null,
    ) {
        runCatching {
            val text = message()
            val detail = throwable?.let { "\n${Log.getStackTraceString(it)}" }.orEmpty()
            Log.println(priority, tag, text + detail)
        }
    }
}

/**
 * Logger 创建入口。
 *
 * 这是日志基础设施的轻量工厂，不承载业务状态，也不替代 Hilt 业务依赖注入。
 */
object Loggers {
    /**
     * 获取指定标签的 Logger。
     *
     * @param tag Logcat 标签。
     * @return Android Logcat Logger。
     */
    fun get(tag: String): Logger = AndroidLogger(tag)
}
