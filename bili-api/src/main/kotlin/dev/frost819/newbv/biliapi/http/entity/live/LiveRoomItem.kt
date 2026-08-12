package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播间列表项，用于推荐列表、分区列表、关注列表等。
 */
@Serializable
data class LiveRoomItem(
    @SerialName("roomid")
    val roomId: Int,
    val uid: Long,
    val title: String,
    val uname: String,
    val online: Int,
    val cover: String = "",
    @SerialName("user_cover")
    val userCover: String = "",
    val face: String = "",
    val keyframe: String = "",
    @SerialName("area_v2_id")
    val areaV2Id: Int = 0,
    @SerialName("area_v2_name")
    val areaV2Name: String = "",
    @SerialName("area_v2_parent_id")
    val areaV2ParentId: Int = 0,
    @SerialName("area_v2_parent_name")
    val areaV2ParentName: String = "",
    @SerialName("live_status")
    val liveStatus: Int = 0,
    @SerialName("short_id")
    val shortId: Int = 0,
    @SerialName("watched_show")
    val watchedShow: WatchedShow? = null,
    @SerialName("pendant_info")
    val pendantInfo: Map<String, PendantInfo> = emptyMap(),
)

@Serializable
data class WatchedShow(
    val num: Int = 0,
    @SerialName("text_small")
    val textSmall: String = "",
    @SerialName("text_large")
    val textLarge: String = "",
    @SerialName("icon")
    val icon: String = "",
    @SerialName("icon_location")
    val iconLocation: Int = 0,
    @SerialName("icon_web")
    val iconWeb: String = "",
)

@Serializable
data class PendantInfo(
    @SerialName("pendant_id")
    val pendantId: Int = 0,
    val name: String = "",
    @SerialName("image")
    val image: String = "",
)
