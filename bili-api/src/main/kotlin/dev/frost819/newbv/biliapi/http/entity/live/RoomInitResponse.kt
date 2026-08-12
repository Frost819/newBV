package dev.frost819.newbv.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播间初始化响应（短号→长号转换）。
 *
 * 端点: `GET /room/v1/Room/room_init?id={roomId}`
 */
@Serializable
data class RoomInitData(
    @SerialName("room_id")
    val roomId: Int,
    @SerialName("short_id")
    val shortId: Int,
    val uid: Long,
    @SerialName("need_p2p")
    val needP2P: Int,
    @SerialName("is_hidden")
    val isHidden: Boolean,
    @SerialName("is_locked")
    val isLocked: Boolean,
    @SerialName("is_portrait")
    val isPortrait: Boolean,
    @SerialName("live_status")
    val liveStatus: Int,
    @SerialName("hidden_till")
    val hiddenTill: Int,
    @SerialName("lock_till")
    val lockTill: Int,
    val encrypted: Boolean,
    @SerialName("pwd_verified")
    val pwdVerified: Boolean,
    @SerialName("live_time")
    val liveTime: Long,
    @SerialName("room_shield")
    val roomShield: Int,
    @SerialName("is_sp")
    val isSp: Int,
    @SerialName("special_type")
    val specialType: Int,
)
