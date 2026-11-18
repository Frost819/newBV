package dev.frost819.newbv.player.impl.exo

/**
 * 媒体请求（视频/音频/字幕流）的默认请求头。
 *
 * B 站海外 CDN（腾讯 EdgeOne，如 `upos-sz-mirrorcosov.bilivideo.com`）对
 * **不带 `Range` 的整段 GET** 会直接返回 `HTTP 567` + HTML 反盗链页，播放器侧
 * 表现为 `Source error`；同一条地址带任意 `Range` 就返回 206 并正常出流。
 *
 * media3 的 `ProgressiveMediaSource` 打开 `.m4s` 时 `position = 0` 且长度未知，
 * `OkHttpDataSource` 只在 `position != 0` 或长度已知时才追加 `Range`，因此首包
 * 恰好是「整段 GET」。这里补一个开区间 Range 兜住首包；后续 seek 时 media3 会用
 * 自身计算的 `Range: bytes=<pos>-` 覆盖同名默认头，不影响拖动。
 */
internal object MediaRequestHeaders {
    /** 开区间 Range：从 0 开始、不限定结尾，CDN 会以 206 正常回分片数据。 */
    const val OPEN_RANGE_VALUE = "bytes=0-"

    /** 默认请求头里的 Range 键名（HTTP 头大小写不敏感，OkHttp 会按原样发送）。 */
    const val RANGE_HEADER = "Range"

    /** 默认请求头里的 Referer 键名，B 站 Web 接口签名的地址要求携带。 */
    const val REFERER_HEADER = "referer"

    /**
     * 构造媒体请求的默认请求头。
     *
     * @param referer 自定义 Referer；`null` 表示不发送（App 接口签名的地址带了反而会 403）
     * @return 至少包含 [RANGE_HEADER] 的请求头表
     */
    fun of(referer: String?): Map<String, String> =
        buildMap {
            // 顺序无关，但先放业务头、后放 Range，便于日志阅读
            referer?.takeIf { it.isNotBlank() }?.let { put(REFERER_HEADER, it) }
            put(RANGE_HEADER, OPEN_RANGE_VALUE)
        }
}
