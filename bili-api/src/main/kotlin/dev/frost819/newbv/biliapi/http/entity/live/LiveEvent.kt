package dev.frost819.newbv.biliapi.http.entity.live

interface LiveEvent

/**
 * 直播弹幕事件（`DANMU_MSG`）。
 *
 * @property content 弹幕文本
 * @property mid 发送者 UID
 * @property username 发送者用户名
 * @property mode 弹幕模式（WS `info[0][1]`：1-3 滚动、4 底部、5 顶部、6 逆向）
 * @property color 弹幕颜色（WS `info[0][3]`，十进制 RGB，如 0xFFFFFF 为白色；
 *   舰长/提督/总督等身份弹幕带自定义颜色）
 * @property medalName 发送者佩戴粉丝勋章名
 * @property medalLevel 发送者佩戴粉丝勋章等级
 */
data class DanmakuEvent(
    val content: String,
    val mid: Long,
    val username: String,
    val mode: Int = 1,
    val color: Int = 0xFFFFFF,
    val medalName: String? = null,
    val medalLevel: Int? = null,
) : LiveEvent

/** 进入直播间/关注主播事件。 */
data class InteractWordEvent(
    val uid: Long,
    val uname: String,
    val interactType: InteractType,
) : LiveEvent

enum class InteractType(
    val code: Int,
) {
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
