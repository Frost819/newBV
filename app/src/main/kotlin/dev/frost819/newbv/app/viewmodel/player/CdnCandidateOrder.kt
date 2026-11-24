package dev.frost819.newbv.app.viewmodel.player

import java.net.URI

/** 只接受封口 `Range` 的 CDN 域名后缀。 */
private val OPEN_RANGE_REJECTING_HOST_SUFFIXES = listOf(".akamaized.net")

/**
 * 把拒绝开区间 `Range` 的 CDN 排到候选末尾（稳定排序，其余保持原有顺序）。
 *
 * 海外下发的 `upos-hz-mirrorakam.akamaized.net` 只接受封口的 `Range: bytes=a-b`，
 * 对 `bytes=a-` 和不带 `Range` 的请求一律返回 403；而播放器走渐进式数据源，
 * 只会发开区间请求。接口常把它排在视频或音频的首位，导致海外起播直接 403。
 * 只有这一个候选时顺序不变。
 */
internal fun List<String>.openRangeRejectingHostsLast(): List<String> =
    sortedBy { url ->
        val host = runCatching { URI(url).host }.getOrNull().orEmpty()
        OPEN_RANGE_REJECTING_HOST_SUFFIXES.any { host.endsWith(it, ignoreCase = true) }
    }
