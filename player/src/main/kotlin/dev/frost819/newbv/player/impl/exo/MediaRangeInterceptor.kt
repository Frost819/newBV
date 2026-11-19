package dev.frost819.newbv.player.impl.exo

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 媒体请求首包的 `Range` 兜底拦截器。
 *
 * B 站海外 CDN（腾讯 EdgeOne，如 `upos-sz-mirrorcosov.bilivideo.com`）对**不带
 * `Range` 的整段 GET** 会返回 `HTTP 567` + HTML 反盗链页；而 media3 打开 `.m4s`
 * 的首包 `position = 0`、长度未知，`OkHttpDataSource` 不会自己加 `Range`。
 * 这里在请求确实没有 `Range` 时补一个开区间，让首包走 206。
 *
 * 之所以不走 `OkHttpDataSource.Factory.setDefaultRequestProperties`：默认头在 media3
 * 1.8.0 里是 `Request.Builder.header()`（替换），而 seek 时 media3 用 `addHeader()`
 * 追加自己算的 `bytes=<pos>-`，两者叠加会让同一个请求带两个 `Range`。拦截器在请求
 * 构建完成之后检查，天然保证「最多一个」：首包由这里补，seek 保持 media3 的原值。
 *
 * HLS 播放列表（`.m3u8`）不是字节流，跳过以免影响直播。
 */
internal object MediaRangeInterceptor : Interceptor {
    /** 开区间 Range：从 0 开始、不限定结尾，CDN 会以 206 正常回片。 */
    const val OPEN_RANGE_VALUE = "bytes=0-"

    /** Range 头键名（HTTP 头大小写不敏感）。 */
    const val RANGE_HEADER = "Range"

    /** HLS 播放列表后缀：列表本身不需要 Range。 */
    private const val HLS_PLAYLIST_SUFFIX = ".m3u8"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val patched =
            when {
                // 播放列表不是字节流，带上 Range 没有意义
                request.url.encodedPath.endsWith(HLS_PLAYLIST_SUFFIX, ignoreCase = true) -> request
                // seek 等场景 media3 已经带了 Range，保持原样，不能再补一个
                request.header(RANGE_HEADER) != null -> request
                else -> request.newBuilder().header(RANGE_HEADER, OPEN_RANGE_VALUE).build()
            }
        return chain.proceed(patched)
    }
}
