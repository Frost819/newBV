package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户关注的主播正在直播的列表响应（单页）。
 *
 * 端点: `GET /xlive/web-ucenter/v1/xfetter/GetWebList`
 * 鉴权: SESSDATA Cookie
 * 参数: `hit_ab=false`（获取真实在线人数和封面），配合 `page` / `page_size` 翻页取全量
 */
@Serializable
data class FollowLiveResponse(
    val rooms: List<FollowLiveRoom> = emptyList(),
    val list: List<FollowLiveRoom> = emptyList(),
    val count: Int = 0,
    @SerialName("not_living_num")
    val notLivingNum: Int = 0,
)

/**
 * 关注直播分页结果。
 *
 * @property rooms 当前页正在直播的房间
 * @property hasMore 是否还有下一页
 * @property total 正在直播的关注人数（服务端 `count`，0 表示服务端未返回）
 */
data class FollowLiveResult(
    val rooms: List<FollowLiveRoom>,
    val hasMore: Boolean,
    val total: Int = 0,
)

/**
 * 关注直播房间信息。
 *
 * 来自 GetWebList 接口的 rooms/list 数组元素。
 */
@Serializable
data class FollowLiveRoom(
    val title: String = "",
    @SerialName("room_id")
    val roomId: Long = 0,
    val uid: Long = 0,
    val online: Int = 0,
    @SerialName("live_time")
    val liveTime: Int = 0,
    @SerialName("live_status")
    val liveStatus: Int = 0,
    @SerialName("short_id")
    val shortId: Int = 0,
    val area: Int = 0,
    @SerialName("area_name")
    val areaName: String = "",
    @SerialName("area_v2_id")
    val areaV2Id: Int = 0,
    @SerialName("area_v2_name")
    val areaV2Name: String = "",
    @SerialName("area_v2_parent_name")
    val areaV2ParentName: String = "",
    @SerialName("area_v2_parent_id")
    val areaV2ParentId: Int = 0,
    val uname: String = "",
    val face: String = "",
    @SerialName("tag_name")
    val tagName: String = "",
    val tags: String = "",
    @SerialName("cover_from_user")
    val coverFromUser: String = "",
    val keyframe: String = "",
    @SerialName("lock_till")
    val lockTill: String = "",
    @SerialName("hidden_till")
    val hiddenTill: String = "",
    @SerialName("broadcast_type")
    val broadcastType: Int = 0,
    @SerialName("is_encrypt")
    val isEncrypt: Boolean = false,
    val link: String = "",
    val nickname: String = "",
    val roomname: String = "",
    @SerialName("liveTime")
    val liveTimestamp: Long = 0,
)
