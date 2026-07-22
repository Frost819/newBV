package dev.frost819.newbv.biliapi.http.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
/**
 * 智能日期格式化 (兼容低版本 Android)
 * @param timeZone 时区 (默认系统时区)
 */
fun Long.toSmartDate(timeZone: TimeZone = TimeZone.getDefault()): String? {
    if (this <= 0) return null
    try {
        // 自动识别秒级或毫秒级时间戳
        // 秒级时间戳通常小于等于10位数，目前直到2286年都是10位数
        // 毫秒级时间戳通常为13位数
        val timeInMillis = if (this < 10000000000L) this * 1000L else this

        // 创建日历实例
        val cal = Calendar.getInstance(timeZone).apply {
            this.timeInMillis = timeInMillis
        }

        // 获取当前年份
        val currentYear = Calendar.getInstance(timeZone).get(Calendar.YEAR)

        // 动态格式选择
        val pattern = if (cal.get(Calendar.YEAR) == currentYear) {
            "M月d日"
        } else {
            "yyyy年M月d日"
        }

        // 线程安全的日期格式化
        return SimpleDateFormat(pattern, Locale.CHINESE).apply {
            this.timeZone = timeZone
        }.format(cal.time)
    } catch (e: Exception) {
        return null
    }
}

val Int.smartDate: String?
    get() = this.toLong().toSmartDate()