package dev.frost819.newbv.player

/**
 * 视频播放器抽象基类。
 *
 * 定义播放器的统一接口，由具体的播放器引擎实现（如 [impl.exo.ExoMediaPlayer]）。
 * 通过 [VideoPlayerListener] 回调播放状态变化。
 *
 * @see VideoPlayerListener
 * @see impl.exo.ExoMediaPlayer
 */
abstract class AbstractVideoPlayer {

    /** 播放器事件回调，由外部通过 [setPlayerEventListener] 设置 */
    protected var mPlayerEventListener: VideoPlayerListener? = null

    /** 初始化播放器实例 */
    abstract fun initPlayer()

    /** 设置请求头 */
    abstract fun setHeader(headers: Map<String, String>)

    /**
     * 设置播放地址。
     *
     * DASH 流视频与音频分离，需要同时传入 [videoUrl] 和 [audioUrl]；
     * HLS/FLV 直播流只需 [videoUrl]。
     *
     * @param videoUrl 视频流地址，null 表示无视频流
     * @param audioUrl 音频流地址，null 表示无独立音频流（如直播流音视频合一）
     */
    abstract fun playUrl(videoUrl: String? = null, audioUrl: String? = null)

    /** 准备开始播放（加载流、初始化解码器） */
    abstract fun prepare()

    /** 开始播放 */
    abstract fun start()

    /** 暂停播放 */
    abstract fun pause()

    /** 停止播放 */
    abstract fun stop()

    /** 重置播放器状态 */
    abstract fun reset()

    /** 是否正在播放 */
    abstract val isPlaying: Boolean

    /** 跳转到指定播放位置（毫秒） */
    abstract fun seekTo(time: Long)

    /** 释放播放器资源 */
    abstract fun release()

    /** 当前播放位置（毫秒） */
    abstract val currentPosition: Long

    /** 视频总时长（毫秒） */
    abstract val duration: Long

    /** 缓冲百分比（0-100） */
    abstract val bufferedPercentage: Int

    /** 设置播放配置（如自动播放） */
    abstract fun setOptions()

    /** 播放速度（1.0 = 正常速度） */
    abstract var speed: Float

    /** 当前缓冲的网速（字节/秒），部分实现可能返回 0 */
    abstract val tcpSpeed: Long

    /** 调试信息字符串 */
    abstract val debugInfo: String

    /** 视频宽度（像素） */
    abstract val videoWidth: Int

    /** 视频高度（像素） */
    abstract val videoHeight: Int

    /**
     * 绑定播放器事件回调。
     *
     * @param playerEventListener 事件监听器，null 表示取消绑定
     */
    fun setPlayerEventListener(playerEventListener: VideoPlayerListener?) {
        mPlayerEventListener = playerEventListener
    }
}
