package dev.frost819.newbv.app.util

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 视频解码能力描述。
 *
 * 用于查询本机解码器能否处理某条 DASH 视频流。
 *
 * @property codec 视频编码。
 * @property width 视频宽度（像素）。
 * @property height 视频高度（像素）。
 * @property frameRate 帧率（fps），未知为 null。
 * @property codecs RFC 6381 编码串（如 `avc1.640034`），用于 profile/level 判定。
 */
data class VideoDecodeProfile(
    val codec: VideoCodec,
    val width: Int,
    val height: Int,
    val frameRate: Float?,
    val codecs: String?,
)

/**
 * 设备视频解码能力查询接口。
 *
 * 抽象为接口以便单测注入确定性实现（真实实现依赖 Android MediaCodec）。
 */
interface VideoCapabilityProvider {
    /**
     * 判定该视频格式是否值得尝试解码。
     *
     * 实现应在「确定无法解码」时返回 false（典型是查不到对应 MIME 的解码器）；对能力上报
     * 疑似超限、但无法确证的情况应返回 true，交由播放器运行时的解码失败回退兜底。
     * 过度保守会稳定误降档（issue #335），详见 [isOptimisticallyDecodable]。
     *
     * @param profile 待判定的视频格式。
     * @return 值得尝试解码返回 true。
     */
    fun isDecodable(profile: VideoDecodeProfile): Boolean
}

/**
 * 判定一次格式能力查询结果是否「值得尝试解码」。
 *
 * Media3 的 `MediaCodecVideoRenderer.supportsFormat()` 把所有能力查询失败都归为
 * [C.FORMAT_EXCEEDS_CAPABILITIES]：尺寸 / 帧率 / 码率超出上报能力与 profile / level
 * 不被识别并无区分。而部分老设备（典型是 Android 5.x 电视）的 MediaCodec 能力上报
 * 并不可靠——明明能解 1080P 却上报「超出能力」，若据此硬性降档就会稳定误降
 * （issue #335：默认 1080P 每次都被迫降到 720P，手动切回 1080P 播放正常）。
 *
 * 因此这里只把「完全没有对应 MIME 的解码器」（[C.FORMAT_UNSUPPORTED_SUBTYPE] /
 * [C.FORMAT_UNSUPPORTED_TYPE]）判为不可解码，其余一律乐观放行，
 * 交给播放器运行时的解码失败回退兜底。
 *
 * @param support [MediaCodecVideoRenderer.supportsFormat] 返回的能力位掩码
 * @return 是否值得尝试解码
 */
internal fun isOptimisticallyDecodable(support: Int): Boolean {
    val formatSupport = RendererCapabilities.getFormatSupport(support)
    return formatSupport == C.FORMAT_HANDLED || formatSupport == C.FORMAT_EXCEEDS_CAPABILITIES
}

/**
 * 基于 Media3 [MediaCodecVideoRenderer.supportsFormat] 的实现。
 *
 * 复用播放器（ExoPlayer）自身的格式支持判定，涵盖 mime、profile/level、
 * 尺寸×帧率（宏块率）等。能力判定**只用于排除确定性不可解码**（无对应 MIME 解码器），
 * 尺寸 / 帧率 / profile 超限按乐观处理，详见 [isOptimisticallyDecodable]。
 * 查询异常时同样返回 true，避免因查询失败误伤正常播放（运行时回退作为兜底）。
 *
 * @param context 应用上下文（查询系统解码器能力）。
 * @param useSoftwareDecoder 是否按软件解码选择器判定；默认读取用户偏好。测试可注入固定值。
 */
class MediaCodecVideoCapabilityProvider(
    private val context: Context,
    private val useSoftwareDecoder: () -> Boolean = { Prefs.enableSoftwareVideoDecoder },
) : VideoCapabilityProvider {
    private val logger = Loggers.get("VideoCapability")
    private val cache = mutableMapOf<String, Boolean>()

    override fun isDecodable(profile: VideoDecodeProfile): Boolean {
        val mime = profile.codec.mimeType ?: return true
        val software = useSoftwareDecoder()
        val key =
            "$mime|${profile.width}x${profile.height}|" +
                "${profile.frameRate}|${profile.codecs}|$software"
        return cache.getOrPut(key) { query(mime, profile, software) }
    }

    private fun query(
        mime: String,
        profile: VideoDecodeProfile,
        software: Boolean,
    ): Boolean =
        runCatching {
            val format =
                Format
                    .Builder()
                    .setSampleMimeType(mime)
                    .setWidth(profile.width)
                    .setHeight(profile.height)
                    .apply { profile.frameRate?.let { setFrameRate(it) } }
                    .apply { profile.codecs?.let { setCodecs(it) } }
                    .build()
            val selector = if (software) softwareSelector else MediaCodecSelector.DEFAULT
            val support = MediaCodecVideoRenderer.supportsFormat(context, selector, format)
            // 记录被乐观放行的「超出上报能力」结果，便于从用户日志定位厂商能力误报（如 #335）
            if (RendererCapabilities.getFormatSupport(support) == C.FORMAT_EXCEEDS_CAPABILITIES) {
                logger.info {
                    "Optimistically decodable: ${profile.codec} ${profile.width}x${profile.height}" +
                        "@${profile.frameRate} ${profile.codecs} (support=$support)"
                }
            }
            isOptimisticallyDecodable(support)
        }.getOrDefault(true)

    /**
     * 软件解码选择器，与 [dev.frost819.newbv.player.impl.exo.ExoMediaPlayer] 保持一致：
     * 仅选 `OMX.google.*` / `c2.android.*`，无软解时回退全部解码器。
     */
    private val softwareSelector =
        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val all =
                MediaCodecUtil.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder,
                )
            all
                .filter { it.name.startsWith("OMX.google.") || it.name.startsWith("c2.android.") }
                .ifEmpty { all }
        }

    /** [VideoCodec] 对应的视频 MIME 类型，未知为 null。 */
    private val VideoCodec.mimeType: String?
        get() =
            when (this) {
                VideoCodec.AVC -> "video/avc"
                VideoCodec.HEVC -> "video/hevc"
                VideoCodec.AV1 -> "video/av01"
                VideoCodec.DVH1 -> "video/dolby-vision"
            }
}
