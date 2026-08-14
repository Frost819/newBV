package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 分区直播分页列表结果。
 *
 * 由 `GET /room/v1/Area/getRoomList` 返回的数组封装而成。
 * 该端点返回 `data` 直接为数组，不包含 `has_more` 字段，
 * 因此通过返回数量是否达到 `pageSize` 来判断是否还有更多。
 */
data class AreaLiveListResult(
    val list: List<LiveRoomItem>,
    val hasMore: Boolean,
)

/**
 * `second/getList` 端点的响应（已弃用，保留用于数据模型参考）。
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
