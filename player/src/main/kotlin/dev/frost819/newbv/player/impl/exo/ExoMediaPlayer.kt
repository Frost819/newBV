package dev.frost819.newbv.player.impl.exo

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.ParsingLoadable
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.OkHttpUtil
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.formatMinSec
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale

/**
 * 基于 Media3 (ExoPlayer) 的播放器实现。
 *
 * 支持：
 * - DASH 视频流（视频+音频分离，使用 [MergingMediaSource] 合并）
 * - HLS / FLV 直播流（使用 [ProgressiveMediaSource]）
 * - 软件解码 / 硬件解码切换
 * - FFmpeg 音频渲染器（需配合 ffmpegDecoder 库）
 *
 * @param context Android Context
 * @param options 播放器配置
 */
@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions,
) : AbstractVideoPlayer(),
    Player.Listener {
    companion object {
        private const val BEHIND_LIVE_WINDOW_RECOVER_WINDOW_MS = 15_000L
        private const val BEHIND_LIVE_WINDOW_RECOVER_MIN_INTERVAL_MS = 1_200L
        private const val BEHIND_LIVE_WINDOW_MAX_RECOVERS = 2
    }

    /** ExoPlayer 实例，在 [initPlayer] 中创建 */
    var mPlayer: ExoPlayer? = null
        private set

    /** 当前 MediaSource，在 [playUrl] 中创建 */
    protected var mMediaSource: MediaSource? = null

    /** 当前播放的流协议（"FLV" / "HLS" / "DASH" / "Unknown"），在 [playUrl] 中赋值 */
    var streamProtocol: String = "Unknown"
        private set

    // --- BEHIND_LIVE_WINDOW 恢复状态（参考 blbl tryRecoverBehindLiveWindow） ---

    private var behindLiveWindowWindowStartAtMs = 0L
    private var behindLiveWindowRecoverCount = 0
    private var behindLiveWindowLastRecoverAtMs = 0L

    private val httpDataSourceFactory =
        OkHttpDataSource.Factory(OkHttpUtil.generateCustomSslOkHttpClient(context)).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    init {
        initPlayer()
    }

    override fun initPlayer() {
        val renderersFactory =
            DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(
                    when (options.enableFfmpegAudioRenderer) {
                        true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                        false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    },
                )
                if (options.enableSoftwareVideoDecoder) {
                    // 强制软件解码：只选择 OMX.google.* / c2.android.* 开头的解码器
                    setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                        val allDecoders =
                            MediaCodecUtil.getDecoderInfos(
                                mimeType,
                                requiresSecureDecoder,
                                requiresTunnelingDecoder,
                            )
                        val softwareDecoders =
                            allDecoders.filter {
                                it.name.startsWith("OMX.google.") || it.name.startsWith("c2.android.")
                            }
                        // 兜底回退到硬解
                        softwareDecoders.ifEmpty { allDecoders }
                    }
                } else {
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                }
            }
        mPlayer =
            ExoPlayer
                .Builder(context)
                .setRenderersFactory(renderersFactory)
                .setSeekForwardIncrementMs(1000 * 10)
                .setSeekBackIncrementMs(1000 * 5)
                .build()

        mPlayer?.addListener(this)
    }

    override fun setHeader(headers: Map<String, String>) {
        // ExoPlayer 通过 dataSourceFactory 设置默认请求头，此方法预留
    }

    override fun playUrl(
        videoUrl: String?,
        audioUrl: String?,
    ) {
        streamProtocol = resolveStreamProtocol(videoUrl, audioUrl)
        val videoMediaSource = videoUrl?.let { createMediaSource(it) }
        val audioMediaSource = audioUrl?.let { createMediaSource(it) }

        val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
        mMediaSource =
            if (mediaSources.size > 1) {
                @Suppress("SpreadOperator")
                MergingMediaSource(*mediaSources.toTypedArray())
            } else {
                mediaSources.firstOrNull()
            }
    }

    /**
     * 从 URL 推断流协议类型。
     *
     * - 双 URL（video + audio）→ DASH
     * - `.m3u8` → HLS
     * - 其余（`.flv` / progressive）→ FLV
     */
    private fun resolveStreamProtocol(
        videoUrl: String?,
        audioUrl: String?,
    ): String {
        if (videoUrl != null && audioUrl != null) return "DASH"
        val url = videoUrl ?: return "Unknown"
        val isHls =
            url
                .substringBefore('?')
                .trim()
                .lowercase(Locale.US)
                .endsWith(".m3u8")
        return if (isHls) "HLS" else "FLV"
    }

    /**
     * 根据 URL 创建对应的 [MediaSource]。
     *
     * B 站直播 HLS 流的 playlist 包含 `#EXT-X-START` 标签，
     * 会导致 ExoPlayer 错误计算直播窗口位置，触发 `ERROR_CODE_BEHIND_LIVE_WINDOW`。
     * 因此对 `.m3u8` URL 使用 [HlsMediaSource] 并通过自定义 [HlsPlaylistParserFactory]
     * 剥离该标签；其余格式（DASH / FLV / progressive）走 [DefaultMediaSourceFactory]。
     */
    private fun createMediaSource(url: String): MediaSource {
        val isHls =
            url
                .substringBefore('?')
                .trim()
                .lowercase(Locale.US)
                .endsWith(".m3u8")
        return if (isHls) {
            HlsMediaSource
                .Factory(dataSourceFactory)
                .setPlaylistParserFactory(ExtXStartStrippingHlsPlaylistParserFactory())
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        }
    }

    override fun prepare() {
        mMediaSource?.let {
            mPlayer?.setMediaSource(it)
            mPlayer?.prepare()
        }
    }

    override fun start() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        mPlayer?.seekTo(time)
    }

    override fun release() {
        mPlayer?.release()
    }

    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val bufferedPercentage: Int
        get() = mPlayer?.bufferedPercentage ?: 0

    override fun setOptions() {
        mPlayer?.playWhenReady = true
    }

    override var speed: Float
        get() = mPlayer?.playbackParameters?.speed ?: 1f
        set(value) {
            mPlayer?.setPlaybackSpeed(value)
        }

    override val tcpSpeed: Long
        get() = 0L

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {}
            Player.STATE_BUFFERING -> mPlayerEventListener?.onBuffering()
            Player.STATE_READY -> mPlayerEventListener?.onReady()
            Player.STATE_ENDED -> mPlayerEventListener?.onEnd()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayerEventListener?.onPlay()
        } else {
            mPlayerEventListener?.onPause()
        }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        mPlayerEventListener?.onSeekBack(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        mPlayerEventListener?.onSeekForward(seekForwardIncrementMs)
    }

    override val debugInfo: String
        get() {
            return """
                player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}
                time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}
                speed: ${speed}x
                buffered: $bufferedPercentage%
                resolution: ${mPlayer?.videoSize?.width} x ${mPlayer?.videoSize?.height}
                audio: ${mPlayer?.audioFormat?.bitrate ?: 0} kbps
                video codec: ${mPlayer?.videoFormat?.sampleMimeType ?: "null"}
                audio codec: ${mPlayer?.audioFormat?.sampleMimeType ?: "null"} ($audioRendererName)
                """.trimIndent()
        }

    /** 当前活跃的音频渲染器名称（如 "OMX.google.aac.decoder"）。 */
    val audioRendererName: String
        get() = findAudioRendererName()

    private fun findAudioRendererName(): String {
        val rendererCount = mPlayer?.rendererCount ?: return "UnknownRenderer"
        for (i in 0 until rendererCount) {
            val renderer = mPlayer!!.getRenderer(i)
            if (renderer.trackType == C.TRACK_TYPE_AUDIO && renderer.state == Renderer.STATE_STARTED) {
                return renderer.name
            }
        }
        return "UnknownRenderer"
    }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    override fun onPlayerError(error: PlaybackException) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            tryRecoverBehindLiveWindow(error)
            return
        }
        mPlayerEventListener?.onError(error)
    }

    /**
     * BEHIND_LIVE_WINDOW 两级恢复 + 频率限制（参考 blbl `tryRecoverBehindLiveWindow`）。
     *
     * - 第 1 次：`seekToDefaultPosition()` + `prepare()`（轻量恢复）
     * - 第 2 次：`stop()` + 重新 `setMediaSource()` + `prepare()`（重建 MediaSource）
     * - 超过 2 次（15 秒窗口内）：上报 error，不再自行恢复
     *
     * 频率限制：15 秒滑动窗口内最多 2 次，最小间隔 1.2 秒。
     */
    private fun tryRecoverBehindLiveWindow(error: PlaybackException) {
        val player =
            mPlayer ?: run {
                mPlayerEventListener?.onError(error)
                return
            }
        val nowMs = android.os.SystemClock.elapsedRealtime()

        // 重置窗口（超过 15 秒或首次）
        if (behindLiveWindowWindowStartAtMs <= 0L ||
            nowMs - behindLiveWindowWindowStartAtMs > BEHIND_LIVE_WINDOW_RECOVER_WINDOW_MS
        ) {
            behindLiveWindowWindowStartAtMs = nowMs
            behindLiveWindowRecoverCount = 0
        }

        // 最小间隔检查（防紧密循环）
        if (nowMs - behindLiveWindowLastRecoverAtMs < BEHIND_LIVE_WINDOW_RECOVER_MIN_INTERVAL_MS) {
            return
        }

        // 超过最大恢复次数，上报 error
        if (behindLiveWindowRecoverCount >= BEHIND_LIVE_WINDOW_MAX_RECOVERS) {
            mPlayerEventListener?.onError(error)
            return
        }

        behindLiveWindowRecoverCount++
        behindLiveWindowLastRecoverAtMs = nowMs

        if (behindLiveWindowRecoverCount == 1) {
            // 第 1 次：轻量恢复 — seekToDefaultPosition + prepare
            player.seekToDefaultPosition()
            player.prepare()
            player.playWhenReady = true
        } else {
            // 第 2 次：重建 MediaSource — stop + setMediaSource + prepare
            mMediaSource?.let { source ->
                player.stop()
                player.setMediaSource(source)
                player.prepare()
                player.playWhenReady = true
            } ?: run {
                // 无 MediaSource 可重建，直接上报
                mPlayerEventListener?.onError(error)
            }
        }
    }
}

/**
 * 剥离 `#EXT-X-START` 标签的 HLS Playlist 解析器工厂。
 *
 * B 站直播 HLS playlist 包含 `#EXT-X-START` 标签，ExoPlayer 解析后
 * 会错误计算直播窗口的起始位置，导致播放几秒后触发
 * [PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW]。
 * 此工厂创建的解析器会在交给 ExoPlayer 默认解析器之前将该标签过滤掉。
 */
@OptIn(UnstableApi::class)
private class ExtXStartStrippingHlsPlaylistParserFactory(
    private val delegate: HlsPlaylistParserFactory = DefaultHlsPlaylistParserFactory(),
) : HlsPlaylistParserFactory {
    override fun createPlaylistParser(): ParsingLoadable.Parser<HlsPlaylist> =
        ExtXStartStrippingParser(delegate.createPlaylistParser())

    override fun createPlaylistParser(
        multivariantPlaylist: HlsMultivariantPlaylist,
        previousMediaPlaylist: HlsMediaPlaylist?,
    ): ParsingLoadable.Parser<HlsPlaylist> =
        ExtXStartStrippingParser(
            delegate.createPlaylistParser(multivariantPlaylist, previousMediaPlaylist),
        )
}

/**
 * 解析 HLS playlist 时剥离 `#EXT-X-START` 行。
 */
@OptIn(UnstableApi::class)
private class ExtXStartStrippingParser(
    private val delegate: ParsingLoadable.Parser<HlsPlaylist>,
) : ParsingLoadable.Parser<HlsPlaylist> {
    override fun parse(
        uri: android.net.Uri,
        inputStream: InputStream,
    ): HlsPlaylist {
        val bytes = inputStream.readBytes()
        val text = String(bytes, Charsets.UTF_8)
        if (!text.contains("#EXT-X-START", ignoreCase = true)) {
            return delegate.parse(uri, ByteArrayInputStream(bytes))
        }
        val filtered =
            text
                .lineSequence()
                .filterNot { it.trimStart().startsWith("#EXT-X-START", ignoreCase = true) }
                .joinToString("\n")
        return delegate.parse(uri, ByteArrayInputStream(filtered.toByteArray(Charsets.UTF_8)))
    }
}
