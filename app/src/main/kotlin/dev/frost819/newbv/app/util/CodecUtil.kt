package dev.frost819.newbv.app.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Range
import androidx.core.util.toRange

/**
 * 编解码器工具类。
 *
 * 解析设备上所有 [MediaCodecInfo]，分类为编码器/解码器、硬件/软件、音频/视频。
 */
object CodecUtil {
    /** 解析设备上所有编解码器信息。 */
    fun parseCodecs(): List<CodecInfoData> =
        MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .toList()
            .map { CodecInfoData.fromCodecInfo(it) }
}

/**
 * 编解码器详细信息。
 *
 * @property name 编解码器名称。
 * @property mimeType MIME 类型。
 * @property type 编码器/解码器。
 * @property mode 硬件/软件。
 * @property media 音频/视频。
 * @property maxSupportedInstances 最大并发实例数（API 23+）。
 * @property colorFormats 支持的颜色格式列表。
 * @property audioBitrateRange 音频码率范围。
 * @property videoBitrateRange 视频码率范围。
 * @property videoFrame 视频帧率范围。
 * @property supportedFrameRates 各分辨率支持的帧率。
 * @property achievableFrameRates 各分辨率可达帧率（API 23+）。
 */
data class CodecInfoData(
    val name: String,
    val mimeType: String,
    val type: CodecType,
    val mode: CodecMode,
    val media: CodecMedia,
    val maxSupportedInstances: Int?,
    val colorFormats: List<Int>,
    val audioBitrateRange: IntRange?,
    val videoBitrateRange: IntRange?,
    val videoFrame: IntRange?,
    val supportedFrameRates: List<SupportedFrameRate>,
    val achievableFrameRates: List<SupportedFrameRate>,
) {
    companion object {
        fun fromCodecInfo(codecInfo: MediaCodecInfo): CodecInfoData {
            val capabilities = codecInfo.getCapabilitiesForType(codecInfo.supportedTypes.first())
            return CodecInfoData(
                name = codecInfo.name,
                mimeType = capabilities.mimeType,
                type = CodecType.fromMediaCodecInfo(codecInfo),
                mode = CodecMode.fromMediaCodecInfo(codecInfo),
                media = CodecMedia.fromMediaCodecInfo(codecInfo),
                maxSupportedInstances =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        capabilities.maxSupportedInstances
                    } else {
                        null
                    },
                colorFormats = capabilities.colorFormats.toList(),
                audioBitrateRange =
                    runCatching {
                        with(capabilities.audioCapabilities.bitrateRange) { lower..upper }
                    }.getOrNull(),
                videoBitrateRange =
                    runCatching {
                        with(capabilities.videoCapabilities.bitrateRange) { lower..upper }
                    }.getOrNull(),
                videoFrame =
                    runCatching {
                        with(capabilities.videoCapabilities.supportedFrameRates) { lower..upper }
                    }.getOrNull(),
                supportedFrameRates =
                    runCatching {
                        codecInfo.getSupportedFrameRates()
                    }.getOrDefault(emptyList()),
                achievableFrameRates =
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            codecInfo.getAchievableFrameRates()
                        } else {
                            emptyList()
                        }
                    }.getOrDefault(emptyList()),
            )
        }
    }
}

/** 编解码器类型。 */
enum class CodecType {
    Encoder,
    Decoder,
    ;

    companion object {
        fun fromMediaCodecInfo(info: MediaCodecInfo): CodecType = if (info.isEncoder) Encoder else Decoder
    }
}

/** 编解码媒体类型。 */
enum class CodecMedia {
    Audio,
    Video,
    ;

    companion object {
        fun fromMediaCodecInfo(info: MediaCodecInfo): CodecMedia = if (info.isAudioCodec()) Audio else Video
    }
}

/** 编解码模式（硬件/软件）。 */
enum class CodecMode {
    Hardware,
    Software,
    ;

    companion object {
        private val softwareCodecPrefixes =
            listOf(
                "omx.google.",
                "c2.android.",
                "c2.google.",
                "omx.sprd.soft.",
                "omx.avcodec.",
                "omx.pv",
            )

        private val softwareCodecSuffixes = listOf("sw", "sw.dec", "sw_vd")

        fun fromMediaCodecInfo(info: MediaCodecInfo): CodecMode {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return if (info.isSoftwareOnly) Software else Hardware
            }
            if (info.isAudioCodec()) return Software
            val name = info.name
            if (name.contains("omx.brcm.video", true) && name.contains("hw", true)) return Hardware
            if (name.startsWith("omx.marvell.video.hw", true)) return Hardware
            if (name.startsWith("omx.intel.hw_vd", true)) return Hardware
            if (name.startsWith("omx.qcom", true) && name.endsWith("hw")) return Hardware
            if (name.startsWith("c2.vda.arc", true) || name.startsWith("arc.")) return Hardware
            return if (isSoftwareCodec(name)) Software else Hardware
        }

        private fun isSoftwareCodec(name: String): Boolean {
            val matchesPrefix = softwareCodecPrefixes.any { name.startsWith(it, true) }
            val matchesSuffix = softwareCodecSuffixes.any { name.endsWith(it, true) }
            val matchesSpecific =
                name.contains("ffmpeg", true) ||
                    (name.startsWith("omx.sec.", true) && name.contains(".sw.", true)) ||
                    name.equals("omx.qcom.video.decoder.hevcswvdec", true)
            val isUnknownVendor = !name.startsWith("omx.", true) && !name.startsWith("c2.", true)
            return matchesPrefix || matchesSuffix || matchesSpecific || isUnknownVendor
        }
    }
}

/** 特定分辨率下支持的帧率范围。 */
data class SupportedFrameRate(
    val resolution: Pair<Int, Int>,
    val frameRate: Range<Double>,
    val unsupported: Boolean,
)

private fun MediaCodecInfo.isAudioCodec(): Boolean = supportedTypes.joinToString().contains("audio")

private val resolutions =
    mapOf(
        480 to 360,
        720 to 480,
        1280 to 720,
        1920 to 1080,
        2560 to 1440,
        3840 to 2160,
        7680 to 4320,
    )

private fun MediaCodecInfo.getSupportedFrameRates(): List<SupportedFrameRate> =
    resolutions.map { (width, height) ->
        val frameRates =
            runCatching {
                val videoCapabilities = getCapabilitiesForType(supportedTypes.first()).videoCapabilities
                videoCapabilities.getSupportedFrameRatesFor(width, height)
            }.getOrNull()
        SupportedFrameRate(
            resolution = width to height,
            frameRate = frameRates ?: ((0.0..0.0).toRange()),
            unsupported = frameRates == null,
        )
    }

private fun MediaCodecInfo.getAchievableFrameRates(): List<SupportedFrameRate> =
    resolutions.map { (width, height) ->
        val frameRates =
            runCatching {
                val videoCapabilities = getCapabilitiesForType(supportedTypes.first()).videoCapabilities
                videoCapabilities.getAchievableFrameRatesFor(width, height)
            }.getOrNull()
        SupportedFrameRate(
            resolution = width to height,
            frameRate = frameRates ?: ((0.0..0.0).toRange()),
            unsupported = frameRates == null,
        )
    }
