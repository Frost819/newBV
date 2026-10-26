package dev.frost819.newbv.app.util

import androidx.core.text.HtmlCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * 数字与时间格式化扩展函数。
 */

/**
 * 将播放数/弹幕数格式化为"万"单位字符串。
 *
 * - null 或 -1 返回空字符串（表示数据缺失）
 * - < 10000 直接显示数字
 * - >= 10000 显示 "n.n万"
 */
fun Int?.toWanString(): String =
    this
        ?.let {
            if (it < 0) {
                ""
            } else if (it < 10_000) {
                it.toString()
            } else {
                "${(it / 1000) / 10f}万"
            }
        }.orEmpty()

/**
 * 将毫秒时长格式化为 "HH:MM:SS" 或 "MM:SS" 字符串。
 *
 * 小时为 0 时省略小时部分。
 */
fun Long.formatHourMinSec(): String {
    if (this < 0L) return "..."

    val s = this / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60

    val sb = StringBuilder(8)

    if (h > 0) {
        if (h < 10) sb.append('0')
        sb.append(h).append(':')
    }

    if (m < 10) sb.append('0')
    sb.append(m).append(':')

    if (sec < 10) sb.append('0')
    sb.append(sec)

    return sb.toString()
}

/**
 * 将秒数时长格式化为 "HH:MM:SS" 或 "MM:SS" 字符串。
 */
fun Int.formatHourMinSec(): String = (this * 1000L).formatHourMinSec()

/**
 * 去除字符串中的 HTML 标签（如搜索结果标题中的 <em> 高亮标签）。
 */
fun String.removeHtmlTags(): String =
    HtmlCompat
        .fromHtml(
            this,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        ).toString()

/**
 * 将评论发布时间戳格式化为相对时间文案。
 *
 * 规则与 B 站评论区一致：1 分钟内显示「刚刚」，1 小时内显示「x分钟前」，
 * 24 小时内显示「x小时前」，48 小时内显示「昨天」，同年显示「M月d日」，
 * 往年显示「yyyy年M月d日」。
 *
 * 自动兼容秒级（10 位）与毫秒级（13 位）时间戳。设备时间早于评论时间
 * （时间戳在未来）时按「刚刚」处理。
 *
 * @param now 当前时间戳（毫秒），提供参数便于测试注入
 * @return 格式化文案；时间戳非法（<=0）时返回空串
 */
fun Long.formatCommentTime(now: Long = System.currentTimeMillis()): String {
    if (this <= 0L) return ""
    val timeMs = if (this < 10_000_000_000L) this * 1000L else this
    val elapsed = (now - timeMs).coerceAtLeast(0L)
    return when {
        elapsed < 60_000L -> "刚刚"
        elapsed < 3_600_000L -> "${elapsed / 60_000L}分钟前"
        elapsed < 86_400_000L -> "${elapsed / 3_600_000L}小时前"
        elapsed < 172_800_000L -> "昨天"
        else -> formatCommentDate(timeMs, now)
    }
}

/** 评论时间超过两天时的日期文案：同年省略年份，往年带年份。 */
private fun formatCommentDate(
    timeMs: Long,
    nowMs: Long,
): String {
    val time = Calendar.getInstance().apply { timeInMillis = timeMs }
    val current = Calendar.getInstance().apply { timeInMillis = nowMs }
    val pattern = if (time.get(Calendar.YEAR) == current.get(Calendar.YEAR)) "M月d日" else "yyyy年M月d日"
    return SimpleDateFormat(pattern, Locale.CHINESE).format(time.time)
}
