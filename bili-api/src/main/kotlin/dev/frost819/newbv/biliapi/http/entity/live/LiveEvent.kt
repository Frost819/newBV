package dev.frost819.newbv.biliapi.http.entity.live

interface LiveEvent

data class DanmakuEvent(
    val content: String,
    val mid: Long,
    val username: String,
    val medalName: String? = null,
    val medalLevel: Int? = null,
) : LiveEvent

/** 进入直播间/关注主播事件。 */
data class InteractWordEvent(
    val uid: Long,
    val uname: String,
    val interactType: InteractType,
) : LiveEvent

enum class InteractType(val code: Int) {
    Enter(1),
    Follow(2),
    Share(3),
    ;

    companion object {
        fun fromCode(code: Int): InteractType? = entries.find { it.code == code }
    }
}

/** 在线人数变化事件。 */
data class OnlineRankCountEvent(
    val count: Int,
) : LiveEvent

/** 看过人数变化事件。 */
data class WatchedChangeEvent(
    val num: Int,
    val textLarge: String,
    val textSmall: String,
) : LiveEvent
