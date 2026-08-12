package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 简单单画质直播流地址响应（fallback 用）。
 *
 * 端点: `GET /room/v1/Room/playUrl`
 */
@Serializable
data class SimplePlayUrlData(
    @SerialName("current_quality")
    val currentQuality: Int,
    @SerialName("quality_description")
    val qualityDescription: List<QualityDesc> = emptyList(),
    val durl: List<SimpleDurl> = emptyList(),
)

@Serializable
data class QualityDesc(
    val qn: Int,
    val desc: String = "",
)

@Serializable
data class SimpleDurl(
    val url: String,
    val length: Int = 0,
    val order: Int = 0,
    @SerialName("stream_type")
    val streamType: Int = 0,
)
