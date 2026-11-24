package dev.frost819.newbv.player.impl.exo

/**
 * 媒体请求的默认请求头。
 *
 * 目前只承载 Referer：B 站 Web 接口签名的地址要求携带，App 接口签名的地址带了反而
 * 会 403，因此由播放器选项决定有无。
 *
 * `Range` **刻意不放在默认头里**：media3 1.8.0 的 `OkHttpDataSource` 会把默认头用
 * `Request.Builder.header()` 写入（替换语义），而 seek 时它又用 `addHeader()` 追加
 * 自己算出的 `Range: bytes=<pos>-`。若默认头常驻 `Range: bytes=0-`，一个 seek 请求
 * 就会带两个 Range，最终从哪个字节开始返回取决于 CDN 实现。
 *
 * 首包（`position = 0`、长度未知）的 Range 兜底改由 [MediaRangeInterceptor] 在
 * 「请求确实没有 Range」时补，详见该类注释。
 */
internal object MediaRequestHeaders {
    /** Referer 头键名，B 站 Web 接口签名的地址要求携带。 */
    const val REFERER_HEADER = "referer"

    /**
     * 构造媒体请求的默认请求头。
     *
     * @param referer 自定义 Referer；`null` 或空白表示不发送
     * @return 只包含 Referer 的请求头表，可能为空
     */
    fun of(referer: String?): Map<String, String> =
        referer
            ?.takeIf { it.isNotBlank() }
            ?.let { mapOf(REFERER_HEADER to it) }
            ?: emptyMap()
}
