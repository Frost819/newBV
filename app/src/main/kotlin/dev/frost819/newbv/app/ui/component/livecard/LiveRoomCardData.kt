package dev.frost819.newbv.app.ui.component.livecard

/**
 * 直播卡片数据。
 *
 * 用于 [dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard] 的展示数据。
 *
 * @property roomId 直播间 ID。
 * @property title 直播间标题。
 * @property uname 主播名称。
 * @property uid 主播 UID。
 * @property cover 封面 URL。
 * @property face 主播头像 URL。
 * @property areaV2Name 子分区名。
 * @property areaV2ParentName 父分区名。
 * @property onlineString 已格式化的人气字符串（如 "1.2万"）。
 * @property watchedString 已格式化的看过人数字符串。
 */
data class LiveRoomCardData(
    val roomId: Long,
    val title: String,
    val uname: String,
    val uid: Long,
    val cover: String,
    val face: String,
    val areaV2Name: String,
    val areaV2ParentName: String,
    val onlineString: String,
    val watchedString: String,
)

fun formatOnlineCount(online: Int): String {
    return when {
        online >= 10_000_000 -> String.format("%.1f亿", online / 10_000_000.0)
        online >= 10_000 -> String.format("%.1f万", online / 10_000.0)
        else -> online.toString()
    }
}
