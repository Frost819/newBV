package dev.frost819.newbv.player

import java.util.concurrent.TimeUnit

/**
 * 将毫秒时间戳格式化为 mm:ss 字符串。
 *
 * @return 格式化后的时间字符串，负值返回 "..."
 */
fun Long.formatMinSec(): String =
    if (this < 0L) {
        "..."
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(this),
                ),
        )
    }

/**
 * 将码率（bps）格式化为 kbps 文本。
 *
 * [androidx.media3.common.Format.bitrate] 的单位是 bps，直接当作 kbps 展示会大 1000 倍；
 * [androidx.media3.common.Format.NO_VALUE]（-1）与 0 均表示无法获取，统一显示「未知」。
 *
 * @return 形如 `192 kbps` 的文本；码率未知时返回「未知」
 */
fun Int.formatBitrateKbps(): String =
    if (this <= 0) {
        "未知"
    } else {
        "${this / 1000} kbps"
    }
