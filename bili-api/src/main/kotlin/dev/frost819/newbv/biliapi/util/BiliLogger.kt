package dev.frost819.newbv.biliapi.util

/**
 * bili-api 的轻量 JVM 日志包装。
 *
 * bili-api 是独立 JVM 模块，不依赖 Android Core Logger；Android 运行时会将标准输出
 * 重定向到 Logcat，CrashHandler 也能捕获这些输出。
 */
internal object BiliLogger {
    /** 记录 Info 级别日志。 */
    fun info(message: () -> String) {
        println("[bili-api][INFO] ${message()}")
    }

    /** 记录 Warn 级别日志。 */
    fun warn(message: () -> String) {
        println("[bili-api][WARN] ${message()}")
    }

    /** 记录带异常的 Warn 级别日志。 */
    fun warn(
        throwable: Throwable,
        message: () -> String,
    ) {
        println("[bili-api][WARN] ${message()}")
        throwable.printStackTrace(System.out)
    }

    /** 记录 Error 级别日志。 */
    fun error(message: () -> String) {
        println("[bili-api][ERROR] ${message()}")
    }

    /** 记录带异常的 Error 级别日志。 */
    fun error(
        throwable: Throwable,
        message: () -> String,
    ) {
        println("[bili-api][ERROR] ${message()}")
        throwable.printStackTrace(System.out)
    }
}
