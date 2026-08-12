package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播间完整信息响应。
 *
 * 端点: `GET /room/v1/Room/get_info?room_id={roomId}`
 */
@Serializable
data class RoomInfoData(
    @SerialName("room_id")
    val roomId: Int,
    @SerialName("short_id")
    val shortId: Int,
    val uid: Long,
    @SerialName("live_status")
    val liveStatus: Int,
    @SerialName("live_time")
    val liveTime: Long,
    @SerialName("live_day_count")
    val liveDayCount: Int = 0,
    val title: String,
    val description: String = "",
    val tags: String = "",
    @SerialName("background")
    val background: String = "",
    @SerialName("uname")
    val uname: String,
    @SerialName("face")
    val face: String = "",
    @SerialName("cover")
    val cover: String = "",
    @SerialName("keyframe")
    val keyframe: String = "",
    @SerialName("online")
    val online: Int,
    @SerialName("area")
    val area: Int,
    @SerialName("area_name")
    val areaName: String = "",
    @SerialName("area_v2_id")
    val areaV2Id: Int,
    @SerialName("area_v2_name")
    val areaV2Name: String = "",
    @SerialName("area_v2_parent_id")
    val areaV2ParentId: Int,
    @SerialName("area_v2_parent_name")
    val areaV2ParentName: String = "",
    @SerialName("parent_area_id")
    val parentAreaId: Int = 0,
    @SerialName("parent_area_name")
    val parentAreaName: String = "",
    @SerialName("watched_show")
    val watchedShow: WatchedShow? = null,
    @SerialName("is_strict_room")
    val isStrictRoom: Boolean = false,
    @SerialName("is_hidden")
    val isHidden: Boolean = false,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
    @SerialName("is_portrait")
    val isPortrait: Boolean = false,
    @SerialName("is_sp")
    val isSp: Int = 0,
    @SerialName("special_type")
    val specialType: Int = 0,
)
