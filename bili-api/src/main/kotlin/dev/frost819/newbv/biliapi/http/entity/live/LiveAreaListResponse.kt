package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播两级分区列表响应。
 *
 * 端点: `GET /room/v1/area/getList`
 */
@Serializable
data class LiveAreaListResponse(
    val data: List<LiveAreaParent> = emptyList(),
)

@Serializable
data class LiveAreaParent(
    val id: Int,
    val name: String,
    @SerialName("list")
    val list: List<LiveAreaChild> = emptyList(),
)

@Serializable
data class LiveAreaChild(
    val id: String = "",
    val name: String,
    @SerialName("parent_id")
    val parentId: String = "",
    @SerialName("parent_name")
    val parentName: String = "",
    @SerialName("pic")
    val pic: String = "",
    @SerialName("area_type")
    val areaType: Int = 0,
    @SerialName("hot_status")
    val hotStatus: Int = 0,
)
