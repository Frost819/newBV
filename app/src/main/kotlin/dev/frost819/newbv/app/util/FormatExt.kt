package dev.frost819.newbv.app.util

import androidx.core.text.HtmlCompat

/**
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
    this?.let {
        if (it < 0) ""
        else if (it < 10_000) it.toString()
        else "${(it / 1000) / 10f}万"
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
fun String.removeHtmlTags(): String = HtmlCompat.fromHtml(
    this, HtmlCompat.FROM_HTML_MODE_LEGACY,
).toString()
