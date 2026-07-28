package dev.frost819.newbv.app.ui.component.videocard

/**
 * 视频卡片数据。
 *
 * 用于 [SmallVideoCard] 的展示数据，由 [dev.frost819.newbv.biliapi.entity.ugc.UgcItem]
 * 或 [dev.frost819.newbv.biliapi.entity.user.DynamicVideo] 转换而来。
 *
 * @property avid 视频 AV 号。
 * @property cid 视频 CID。
 * @property epid 番剧 EP ID（PGC 动态时有值）。
 * @property title 视频标题。
 * @property cover 封面 URL。
 * @property upName UP 主名称。
 * @property upMid UP 主 MID。
 * @property playString 播放数显示字符串（已格式化）。
 * @property danmakuString 弹幕数显示字符串（已格式化）。
 * @property timeString 时长显示字符串（已格式化）。
 * @property pubTime 发布时间显示字符串。
 */
data class VideoCardData(
    val avid: Long,
    val cid: Long? = null,
    val epid: Int? = null,
    val title: String,
    val cover: String,
    val upName: String,
    val upMid: Long? = null,
    val playString: String = "",
    val danmakuString: String = "",
    val timeString: String = "",
    val pubTime: String? = null,
)
