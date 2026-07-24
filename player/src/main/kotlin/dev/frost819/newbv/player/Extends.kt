package dev.frost819.newbv.player

import java.util.concurrent.TimeUnit

/**
 * 将毫秒时间戳格式化为 mm:ss 字符串。
 *
 * @return 格式化后的时间字符串，负值返回 "..."
 */
fun Long.formatMinSec(): String {
    return if (this < 0L) {
        "..."
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}
