package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 推荐直播间列表响应。
 *
 * 端点: `GET /xlive/web-interface/v1/webMain/getMoreRecList?platform=web`
 */
@Serializable
data class LiveRecommendResponse(
    @SerialName("count")
    val count: Int = 0,
    @SerialName("recommend_room_list")
    val list: List<LiveRoomItem> = emptyList(),
    @SerialName("has_more")
    val hasMore: Int = 0,
    @SerialName("is_nft")
    val isNft: Boolean = false,
    @SerialName("new_tags")
    val newTags: List<RecommendTag> = emptyList(),
)

@Serializable
data class RecommendTag(
    val id: Int,
    val name: String,
    @SerialName("sort_type")
    val sortType: String = "",
)
