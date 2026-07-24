package dev.frost819.newbv.player.entity

/**
 * 视频画质枚举。
 *
 * @param code B 站 API 画质标识（qn 参数）
 * @param displayName 用于 UI 显示的完整名称
 * @param shortName 用于 UI 紧凑显示的简称
 */
enum class VideoResolution(
    val code: Int,
    val displayName: String,
    val shortName: String
) {
    R240P(6, "240P", "240P"),
    R360P(16, "360P", "360P"),
    R480P(32, "480P", "480P"),
    R720P(64, "720P", "720P"),
    R720P60(74, "720P60", "720P60"),
    R1080P(80, "1080P", "1080P"),
    R1080PPlus(112, "1080P+", "1080P+"),
    R1080P60(116, "1080P60", "1080P60"),
    R4K(120, "4K", "4K"),
    RHdr(125, "HDR", "HDR"),
    RDolby(126, "Dolby Vision", "Dolby"),
    R8K(127, "8K", "8K");

    companion object {
        /** 根据 B 站 API 返回的 qn 画质标识获取枚举，未知值默认返回 1080P */
        fun fromCode(code: Int): VideoResolution {
            return entries.find { it.code == code } ?: R1080P
        }
    }
}
