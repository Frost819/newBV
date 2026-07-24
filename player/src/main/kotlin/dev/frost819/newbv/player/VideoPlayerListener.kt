package dev.frost819.newbv.player

/**
 * 播放器事件回调接口。
 *
 * 由 [AbstractVideoPlayer] 的使用者实现，接收播放器状态变化通知。
 */
interface VideoPlayerListener {
    /** 播放器发生异常 */
    fun onError(error: Exception)

    /** 播放器准备就绪，可以开始播放 */
    fun onReady()

    /** 播放器开始播放 */
    fun onPlay()

    /** 播放器暂停 */
    fun onPause()

    /** 播放器正在缓冲 */
    fun onBuffering()

    /** 播放结束（播放到末尾） */
    fun onEnd()

    /** 后退跳跃（由遥控器触发） */
    fun onSeekBack(seekBackIncrementMs: Long)

    /** 前进跳跃（由遥控器触发） */
    fun onSeekForward(seekForwardIncrementMs: Long)
}
