package dev.frost819.newbv.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PopularVideoData(
    val list: List<VideoInfo>,
    @SerialName("no_more")
    val noMore: Boolean,
)
