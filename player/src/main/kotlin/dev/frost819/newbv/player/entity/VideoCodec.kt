package dev.frost819.newbv.player.entity

import dev.frost819.newbv.biliapi.entity.CodeType

/**
 * 视频编码格式枚举。
 *
 * @param displayName 用于 UI 显示的名称
 * @param prefix codec 前缀，用于从 B 站返回的 codecs 字符串中匹配编码类型
 * @param codecId B 站 API 返回的编码 ID
 */
enum class VideoCodec(
    val displayName: String,
    val prefix: String,
    val codecId: Int
) {
    AVC("AVC/H.264", "avc1", 7),
    HEVC("HEVC/H.265", "hev1", 12),
    AV1("AV1", "av01", 13),
    DVH1("Dolby Vision", "dvh1", 0),
    HVC1("HVC1", "hvc", 0);

    companion object {
        /** 根据枚举序号获取编码（从 PlayData 的编码列表选择） */
        fun fromCode(code: Int?): VideoCodec {
            return entries.find { it.ordinal == code } ?: AVC
        }

        /** 从 B 站返回的 codec 字符串（如 "avc1.640034"）匹配编码类型 */
        fun fromCodecString(codec: String) = runCatching {
            entries.forEach {
                if (codec.startsWith(it.prefix)) return@runCatching it
            }
            return@runCatching null
        }.getOrNull()

        /** 根据 B 站 API 返回的 codecId 匹配编码 */
        fun fromCodecId(codecId: Int) = runCatching {
            entries.find { it.codecId == codecId }!!
        }.getOrDefault(AVC)
    }

    /** 转换为 bili-api 的 CodeType */
    fun toBiliApiCodeType() = when (this) {
        AVC -> CodeType.Code264
        HEVC -> CodeType.Code265
        AV1 -> CodeType.CodeAv1
        DVH1, HVC1 -> CodeType.Code265
    }
}
