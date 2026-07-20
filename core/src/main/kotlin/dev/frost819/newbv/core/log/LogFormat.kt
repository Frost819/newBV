package dev.frost819.newbv.core.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志格式定义。
 *
 * 统一 new BV 所有日志文件的格式规范，
 * 包括交互日志、崩溃日志、手动日志。
 */
object LogFormat {
    private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 交互日志条目格式。
     *
     * 格式：`[HH:mm:ss.SSS] [LEVEL] [category] action: key1=value1, key2=value2`
     */
    fun interactionEntry(
        timestamp: Long,
        level: LogLevel,
        category: String,
        action: String,
        detail: Map<String, String> = emptyMap()
    ): String {
        val ts = timestampFormatter.format(Date(timestamp))
        val detailStr = if (detail.isNotEmpty()) {
            detail.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else ""
        return buildString {
            append("[$ts] [$level] [$category] $action")
            if (detailStr.isNotEmpty()) append(": $detailStr")
        }
    }

    /**
     * 日志文件头部格式。
     *
     * @param appVersion 应用版本名
     * @param appVersionCode 应用版本号
     * @param androidVersion Android 版本
     * @param androidSdk Android SDK 版本
     * @param device 设备型号
     * @param model 设备型号
     * @param manufacturer 制造商
     */
    fun header(
        appVersion: String,
        appVersionCode: Int,
        androidVersion: String,
        androidSdk: Int,
        device: String,
        model: String,
        manufacturer: String
    ): String = buildString {
        appendLine("======== new BV Log ========")
        appendLine("App Version: $appVersion ($appVersionCode)")
        appendLine("Android Version: $androidVersion ($androidSdk)")
        appendLine("Device: $device")
        appendLine("Model: $model")
        appendLine("Manufacturer: $manufacturer")
        appendLine("================================")
    }

    /**
     * 崩溃日志头部。
     */
    fun crashHeader(
        appVersion: String,
        appVersionCode: Int,
        androidVersion: String,
        androidSdk: Int,
        device: String,
        model: String,
        manufacturer: String,
        recentInteractionCount: Int
    ): String = buildString {
        appendLine("======== new BV Crash ========")
        appendLine("App Version: $appVersion ($appVersionCode)")
        appendLine("Android Version: $androidVersion ($androidSdk)")
        appendLine("Device: $device")
        appendLine("Model: $model")
        appendLine("Manufacturer: $manufacturer")
        appendLine("================================")
        appendLine("Recent interaction logs ($recentInteractionCount entries):")
    }
}

/**
 * 日志级别。
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR;

    companion object {
        fun fromString(value: String): LogLevel =
            entries.find { it.name == value } ?: INFO
    }
}

/**
 * 交互日志类别。
 */
enum class LogCategory(val displayName: String) {
    NAV("导航"),
    PLAY("播放"),
    SETTINGS("设置"),
    CARD("视频卡片"),
    INPUT("输入"),
    EXCEPTION("异常"),
    LIFECYCLE("生命周期");

    companion object {
        fun fromString(value: String): LogCategory =
            entries.find { it.name == value } ?: NAV
    }
}
