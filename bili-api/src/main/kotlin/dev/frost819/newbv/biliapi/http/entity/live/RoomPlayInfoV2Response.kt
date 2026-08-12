package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播流地址 v2 响应。
 *
 * 端点: `GET /xlive/web-room/v2/index/getRoomPlayInfo`
 * 返回多协议（FLV/HLS）、多格式（FLV/TS/FMP4）、多编码（AVC/HEVC/AV1）的流地址。
 */
@Serializable
data class RoomPlayInfoV2Data(
    @SerialName("room_id")
    val roomId: Int,
    @SerialName("short_id")
    val shortId: Int,
    val uid: Long,
    @SerialName("live_status")
    val liveStatus: Int,
    @SerialName("playurl_info")
    val playUrlInfo: PlayUrlInfo,
)

@Serializable
data class PlayUrlInfo(
    @SerialName("playurl")
    val playUrl: PlayUrl,
)

@Serializable
data class PlayUrl(
    val cid: Long,
    @SerialName("g_qn_desc")
    val qnDesc: List<QnDesc> = emptyList(),
    val stream: List<PlayStream> = emptyList(),
)

@Serializable
data class QnDesc(
    val qn: Int,
    val desc: String = "",
)

@Serializable
data class PlayStream(
    @SerialName("protocol_name")
    val protocolName: String,
    val format: List<PlayFormat> = emptyList(),
)

@Serializable
data class PlayFormat(
    @SerialName("format_name")
    val formatName: String,
    val codec: List<PlayCodec> = emptyList(),
)

@Serializable
data class PlayCodec(
    @SerialName("codec_name")
    val codecName: String,
    @SerialName("current_qn")
    val currentQn: Int,
    @SerialName("accept_qn")
    val acceptQn: List<Int> = emptyList(),
    @SerialName("base_url")
    val baseUrl: String,
    @SerialName("url_info")
    val urlInfo: List<PlayUrlInfoItem> = emptyList(),
    @SerialName("media_info")
    val mediaInfo: MediaInfo = MediaInfo(),
)

@Serializable
data class PlayUrlInfoItem(
    val host: String,
    val extra: String,
    @SerialName("is_timeout")
    val isTimeout: Boolean = false,
)

@Serializable
data class MediaInfo(
    val height: Int = 0,
    val width: Int = 0,
    @SerialName("realtime_avg_bw")
    val realtimeAvgBw: Long = 0,
)
