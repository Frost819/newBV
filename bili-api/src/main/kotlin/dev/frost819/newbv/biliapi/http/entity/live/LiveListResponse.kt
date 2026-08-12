package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播首页模块化列表响应。
 *
 * 端点: `GET /xlive/web-interface/v1/index/getList?platform=web`
 */
@Serializable
data class LiveListResponse(
    val modules: List<LiveModule> = emptyList(),
)

@Serializable
data class LiveModule(
    val id: Int,
    val title: String,
    val type: Int,
    val list: List<LiveRoomItem> = emptyList(),
    @SerialName("module_type")
    val moduleType: String = "",
    @SerialName("more_type")
    val moreType: Int = 0,
    @SerialName("more_text")
    val moreText: String = "",
    @SerialName("more_url")
    val moreUrl: String = "",
    @SerialName("more_data")
    val moreData: String = "",
    @SerialName("show_side")
    val showSide: Int = 0,
    @SerialName("slider_data")
    val sliderData: List<SliderItem> = emptyList(),
)

@Serializable
data class SliderItem(
    val id: Int = 0,
    val title: String = "",
    val image: String = "",
    val link: String = "",
    @SerialName("show_id")
    val showId: String = "",
)
