package dev.frost819.newbv.app.ui.state.live

import dev.frost819.newbv.biliapi.repositories.LivePlayLine
import dev.frost819.newbv.biliapi.repositories.LiveQuality

/**
 * 直播播放状态。
 *
 * 与点播的 [dev.frost819.newbv.app.ui.state.player.PlayerState] 保持同构：
 * 错误文案随状态一起携带，离开错误态即自然消失，因此不需要额外的
 * `errorMessage` 字段，也不会出现「状态已恢复但旧文案残留」。
 */
sealed class LivePlayerState {
    /** 未开始。 */
    data object Idle : LivePlayerState()

    /** 加载中（首次拉流 / 切换画质或线路）。 */
    data object Loading : LivePlayerState()

    /** 正在播放。 */
    data object Playing : LivePlayerState()

    /** 已暂停。 */
    data object Paused : LivePlayerState()

    /** 播放结束。 */
    data object Ended : LivePlayerState()

    /**
     * 发生错误。
     *
     * @param message 面向用户的错误文案
     */
    data class Error(
        val message: String,
    ) : LivePlayerState()
}

/** 当前状态的错误文案；非错误态为 null。 */
val LivePlayerState.errorMessage: String?
    get() = (this as? LivePlayerState.Error)?.message

/**
 * 直播播放器 UI 状态。
 */
data class LivePlayerUiState(
    val title: String = "",
    val cover: String = "",
    val roomId: Long = 0,
    val realRoomId: Int = 0,
    val liveStatus: Int = 0,
    val areaName: String = "",
    val onlineCount: String = "0",
    /** 主播 UID（用于跳转 UP 主主页）。 */
    val upMid: Long = 0L,
    /** 主播昵称。 */
    val upName: String = "",
    /** 主播头像 URL。 */
    val upFace: String = "",
    val playerState: LivePlayerState = LivePlayerState.Idle,
    val isBuffering: Boolean = false,
    val availableQualities: List<LiveQuality> = emptyList(),
    /** 当前画质档位标识（[LiveQuality.key]，qn * 100 + hdrType * 10 + eotf）。 */
    val currentQuality: Int = 0,
    val availableLines: List<LivePlayLine> = emptyList(),
    val currentLine: Int = 0,
)
