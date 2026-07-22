package dev.frost819.newbv.biliapi.http.entity.live

interface LiveEvent

data class DanmakuEvent(
    val content: String,
    val mid: Long,
    val username: String,
    val medalName: String? = null,
    val medalLevel: Int? = null
) : LiveEvent
