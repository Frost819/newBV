package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 分区直播分页列表响应。
 *
 * 端点: `GET /xlive/web-interface/v1/second/getList`
 */
@Serializable
data class AreaLiveListResponse(
    val count: Int = 0,
    @SerialName("has_more")
    val hasMore: Int = 0,
    val list: List<LiveRoomItem> = emptyList(),
    @SerialName("new_tags")
    val newTags: List<AreaSortTag> = emptyList(),
    val banner: List<AreaBanner> = emptyList(),
)

@Serializable
data class AreaSortTag(
    val id: Int,
    val name: String,
    @SerialName("sort_type")
    val sortType: String = "",
)

@Serializable
data class AreaBanner(
    val id: Int = 0,
    val title: String = "",
    val image: String = "",
    val link: String = "",
)
