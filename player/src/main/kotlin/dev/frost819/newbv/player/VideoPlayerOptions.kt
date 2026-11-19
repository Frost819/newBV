package dev.frost819.newbv.player

/**
 * 播放器配置选项。
 *
 * @param userAgent 自定义 User-Agent，用于 HTTP 请求
 * @param referer 自定义 Referer，用于 B 站防盗链验证
 * @param enableSoftwareVideoDecoder 是否强制使用软件视频解码（用于不支持硬解的编码）
 * @param enableFrameRateMatching 是否允许 Media3 向系统请求匹配片源帧率
 */
data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableSoftwareVideoDecoder: Boolean = false,
    val enableFrameRateMatching: Boolean = true,
)
