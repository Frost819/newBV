package dev.frost819.newbv.app.viewmodel.live

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.component.livecard.formatOnlineCount
import dev.frost819.newbv.app.ui.state.live.LivePlayerState
import dev.frost819.newbv.app.ui.state.live.LivePlayerUiState
import dev.frost819.newbv.app.util.VideoCapabilityProvider
import dev.frost819.newbv.app.util.VideoDecodeProfile
import dev.frost819.newbv.app.util.toPlaybackLoadErrorMessage
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.LiveEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.repositories.LivePlayInfo
import dev.frost819.newbv.biliapi.repositories.LivePlayLine
import dev.frost819.newbv.biliapi.repositories.LiveQuality
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.biliapi.websocket.LiveDataWebSocket
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.impl.exo.ExoMediaPlayer
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import dev.frost819.newbv.player.toPlaybackErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 直播播放器 ViewModel。
 *
 * 管理直播流加载、播放控制、WebSocket 弹幕连接。
 * 弹幕播放器实例由 [DanmakuViewModel] 创建并传入，本类仅负责发送直播弹幕数据。
 *
 * @see dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
 */
@HiltViewModel
class LivePlayerViewModel
    @Inject
    constructor(
        private val liveRepository: LiveRepository,
        private val exoPlayerFactory: ExoPlayerFactory,
        private val videoCapabilityProvider: VideoCapabilityProvider,
    ) : ViewModel() {
        private val logger = Loggers.get("LivePlayerViewModel")

        var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
            private set

        private var danmakuPlayer: DanmakuPlayer? = null

        /** 直播弹幕单条过滤函数（账号云端屏蔽规则），由 [loadLive] 注入。 */
        private var danmakuFilter: ((content: String, mid: Long) -> Boolean)? = null

        private val _uiState = MutableStateFlow(LivePlayerUiState())
        val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

        private var wsJob: Job? = null
        private var debugInfoJob: Job? = null
        private var danmakuIdCounter = 0L

        private val _debugInfo = MutableStateFlow("")

        /** 调试信息，仅在 [Prefs.showPlayerDebugInfo] 开启时更新。 */
        val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

        /** 提成字段便于单测直接驱动回调；错误态统一由 [failPlayback] 写入。 */
        private val videoPlayerListener =
            object : VideoPlayerListener {
                override fun onError(error: Exception) {
                    logger.error(error) { "Live player error" }
                    failPlayback(error.toPlaybackErrorMessage())
                }

                override fun onVideoDecodeUnsupported() {
                    logger.error { "Live video decode unsupported: quality=${_uiState.value.currentQuality}" }
                    // 直播是单条流，无法换编码；提示用户切画质或线路自救
                    failPlayback("解码失败，请尝试切换画质或线路")
                }

                override fun onAudioDecodeUnsupported() {
                    logger.error { "Live audio decode unsupported: quality=${_uiState.value.currentQuality}" }
                    failPlayback("音频解码失败，请尝试切换线路")
                }

                override fun onReady() {
                    _uiState.update {
                        it.copy(playerState = LivePlayerState.Playing, isBuffering = false)
                    }
                }

                override fun onPlay() {
                    _uiState.update { it.copy(playerState = LivePlayerState.Playing) }
                }

                override fun onPause() {
                    // 出错后 ExoPlayer 会紧跟着回调 isPlaying=false，不能把错误态覆盖成暂停态
                    if (_uiState.value.playerState is LivePlayerState.Error) return
                    _uiState.update { it.copy(playerState = LivePlayerState.Paused) }
                }

                override fun onBuffering() {
                    _uiState.update { it.copy(isBuffering = true) }
                }

                override fun onEnd() {
                    _uiState.update { it.copy(playerState = LivePlayerState.Ended) }
                }

                override fun onSeekBack(seekBackIncrementMs: Long) {}

                override fun onSeekForward(seekForwardIncrementMs: Long) {}
            }

        /**
         * 初始化直播间信息。
         */
        fun init(
            roomId: Long,
            title: String,
            cover: String,
        ) {
            _uiState.update {
                it.copy(
                    roomId = roomId,
                    title = title,
                    cover = cover,
                )
            }
        }

        /**
         * 初始化播放器。
         *
         * 不创建弹幕播放器，弹幕播放器由 [DanmakuViewModel] 管理，
         * 在 [loadLive] 时传入。
         */
        fun initVideoPlayer(context: Context) {
            val options =
                VideoPlayerOptions(
                    userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
                    referer = "https://live.bilibili.com",
                )
            videoPlayer = exoPlayerFactory.create(context, options)
            videoPlayer?.setPlayerEventListener(videoPlayerListener)
            videoPlayer?.initPlayer()
            videoPlayer?.setOptions()
        }

        /**
         * 加载直播间。
         *
         * @param roomId 房间号
         * @param danmakuPlayer 弹幕播放器实例（由 DanmakuViewModel 提供）
         * @param danmakuFilter 直播弹幕单条过滤函数，返回 true 表示丢弃
         */
        fun loadLive(
            roomId: Long,
            danmakuPlayer: DanmakuPlayer? = null,
            danmakuFilter: ((content: String, mid: Long) -> Boolean)? = null,
        ) {
            this.danmakuPlayer = danmakuPlayer
            this.danmakuFilter = danmakuFilter
            loadLiveInternal(roomId)
        }

        /**
         * 刷新直播间。
         *
         * 重新拉取房间信息、直播流地址、重连 WebSocket。
         */
        fun refresh() {
            val roomId = _uiState.value.roomId
            if (roomId == 0L) return
            wsJob?.cancel()
            stopDebugInfoUpdater()
            loadLiveInternal(roomId)
        }

        private fun loadLiveInternal(roomId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(playerState = LivePlayerState.Loading, isBuffering = true) }

                runCatching {
                    val roomInit = liveRepository.getRoomInit(roomId.toInt())
                    val realRoomId = roomInit.roomId
                    _uiState.update { it.copy(realRoomId = realRoomId, liveStatus = roomInit.liveStatus) }

                    val roomInfo = liveRepository.getRoomInfo(realRoomId)
                    _uiState.update {
                        val newState =
                            it.copy(
                                title = roomInfo.title.ifBlank { _uiState.value.title },
                                cover = roomInfo.cover.ifBlank { roomInfo.keyframe },
                                areaName = roomInfo.areaV2Name,
                                onlineCount = formatOnlineCount(roomInfo.online),
                            )
                        newState
                    }

                    val requestedKey = _uiState.value.currentQuality.takeIf { it > 0 } ?: 0
                    val playInfo = liveRepository.getLivePlayInfo(realRoomId, requestedKey)
                    val line = resolveLine(playInfo, _uiState.value.currentLine)
                    if (line == null) {
                        failPlayback("获取直播流地址失败")
                        return@launch
                    }

                    _uiState.update {
                        val qualities = filterDecodableQualities(playInfo.qualities)
                        it.copy(
                            availableQualities = qualities,
                            currentQuality = resolveCurrentKey(qualities, playInfo.currentKey, requestedKey),
                            availableLines = playInfo.lines,
                            currentLine = line.order,
                        )
                    }

                    videoPlayer?.playUrl(line.url)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                    danmakuPlayer?.start(null)
                    startDebugInfoUpdater()

                    connectDanmaku(realRoomId)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to load live" }
                    failPlayback(error.toPlaybackLoadErrorMessage())
                }
            }
        }

        /** 置错误态并清掉缓冲标记（错误浮层优先于转圈，残留缓冲会让重连误判）。 */
        private fun failPlayback(message: String) {
            _uiState.update {
                it.copy(
                    playerState = LivePlayerState.Error(message),
                    isBuffering = false,
                )
            }
        }

        private fun connectDanmaku(realRoomId: Int) {
            wsJob?.cancel()
            wsJob =
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        LiveDataWebSocket.connectLiveEvent(realRoomId).collect { event ->
                            handleLiveEvent(event)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) { "WebSocket error" }
                    }
                }

            viewModelScope.launch {
                runCatching {
                    dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
                        .getLiveDanmuHistory(realRoomId)
                        .data
                }.onSuccess { history ->
                    history?.room?.forEach { item ->
                        sendDanmaku(item.text, item.uid)
                    }
                }
            }
        }

        /**
         * 启动调试信息轮询（仅 [Prefs.showPlayerDebugInfo] 开启时）。
         *
         * 以 500ms 间隔从播放器读取属性，组合成 debug 字符串。
         * 设置关闭时不启动，避免无谓开销。
         */
        private fun startDebugInfoUpdater() {
            if (!Prefs.showPlayerDebugInfo) return
            if (debugInfoJob?.isActive == true) return
            debugInfoJob =
                viewModelScope.launch(Dispatchers.Main) {
                    while (isActive) {
                        val player = videoPlayer as? ExoMediaPlayer ?: break
                        val state = _uiState.value
                        _debugInfo.value =
                            buildString {
                                appendLine("player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}")
                                appendLine("state: ${state.playerState}")
                                appendLine("stream: ${player.streamProtocol}")
                                val qnDesc =
                                    state.availableQualities
                                        .firstOrNull { it.key == state.currentQuality }
                                        ?.displayDesc
                                appendLine("quality: ${qnDesc ?: "?"}(${state.currentQuality})")
                                appendLine("line: ${state.currentLine}/${state.availableLines.size}")
                                appendLine("roomId: ${state.realRoomId}")
                                appendLine("resolution: ${player.videoWidth} x ${player.videoHeight}")
                                appendLine("buffered: ${player.bufferedPercentage}%")
                                appendLine("video: ${player.mPlayer?.videoFormat?.sampleMimeType ?: "null"}")
                                val audioCodec = player.mPlayer?.audioFormat?.sampleMimeType ?: "null"
                                appendLine("audio: $audioCodec (${player.audioRendererName})")
                            }.trimEnd()
                        delay(500)
                    }
                }
        }

        private fun stopDebugInfoUpdater() {
            debugInfoJob?.cancel()
            debugInfoJob = null
        }

        /**
         * 处理直播弹幕事件。
         *
         * WebSocket 回调运行在 IO 线程，需切换到主线程操作弹幕播放器。
         */
        private fun handleLiveEvent(event: LiveEvent) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                when (event) {
                    is DanmakuEvent -> {
                        sendDanmaku(event.content, event.mid, event.mode, event.color)
                    }
                    is OnlineRankCountEvent -> {
                        _uiState.update {
                            it.copy(onlineCount = formatOnlineCount(event.count))
                        }
                    }
                    else -> {}
                }
            }
        }

        /** 将直播弹幕送入引擎，先套用账号云端屏蔽规则；mode 7+ 折叠为滚动。 */
        private fun sendDanmaku(
            content: String,
            mid: Long,
            mode: Int = 1,
            textColor: Int = 0xFFFFFF,
        ) {
            if (danmakuFilter?.invoke(content, mid) == true) return
            val danmakuId = danmakuIdCounter++
            val data =
                DanmakuItemData(
                    danmakuId = danmakuId,
                    position = danmakuPlayer?.getCurrentTimeMs() ?: 0,
                    content = content,
                    mode =
                        when (mode) {
                            4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                            5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                            else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                        },
                    textSize = 25,
                    textColor = textColor,
                    score = 0,
                    danmakuStyle = 0,
                    rank = 0,
                    userId = mid,
                    mergedType = DanmakuItemData.MERGED_TYPE_NORMAL,
                )
            danmakuPlayer?.send(data)
        }

        /**
         * 播放/暂停切换。
         */
        fun togglePlayPause() {
            val player = videoPlayer ?: return
            if (player.isPlaying) {
                player.pause()
            } else {
                player.start()
            }
        }

        /**
         * 切换画质。
         *
         * 切换后线路重置为第一条（档位变化会导致可用线路集合变化）。
         *
         * @param key 目标画质档位标识（[LiveQuality.key]）
         */
        fun changeQuality(key: Int) {
            _uiState.update { it.copy(currentQuality = key, currentLine = 0) }
            reloadAndPlay(key)
        }

        /**
         * 手动切换线路。
         *
         * 重新请求直播流信息以刷新签名 URL（signed URL 会过期），再按 [order] 播放。
         * 若服务端返回的线路数变化导致 [order] 越界，则回退到第一条线路。
         *
         * @param order 目标线路序号（从 1 开始）
         */
        fun changeLine(order: Int) {
            _uiState.update { it.copy(currentLine = order) }
            reloadAndPlay(_uiState.value.currentQuality)
        }

        /**
         * 重新拉取直播流信息并播放当前选中的线路。
         *
         * @param key 目标画质档位标识（[LiveQuality.key]，0 = 自动）
         */
        private fun reloadAndPlay(key: Int) {
            val realRoomId = _uiState.value.realRoomId
            if (realRoomId == 0) return

            viewModelScope.launch {
                // 换档/换线路期间进入加载态并清掉旧错误，避免错误浮层压住新的加载状态
                _uiState.update {
                    it.copy(playerState = LivePlayerState.Loading, isBuffering = true)
                }
                runCatching {
                    val playInfo = liveRepository.getLivePlayInfo(realRoomId, key)
                    val line =
                        resolveLine(playInfo, _uiState.value.currentLine)
                            ?: error("获取直播流地址失败")
                    _uiState.update {
                        // 响应未下发画质时保留既有列表；否则以过滤结果为准，避免残留已判定不可解的 HDR 档
                        val qualities =
                            if (playInfo.qualities.isEmpty()) {
                                it.availableQualities
                            } else {
                                filterDecodableQualities(playInfo.qualities)
                            }
                        it.copy(
                            availableQualities = qualities,
                            currentQuality = resolveCurrentKey(qualities, playInfo.currentKey, key),
                            availableLines = playInfo.lines,
                            currentLine = line.order,
                        )
                    }
                    videoPlayer?.playUrl(line.url)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to reload live stream" }
                    failPlayback(error.toPlaybackLoadErrorMessage())
                }
            }
        }

        /** 隐藏本机无法解码的 HDR 档（SDR 始终保留），避免选中后播放失败。 */
        private fun filterDecodableQualities(qualities: List<LiveQuality>): List<LiveQuality> =
            qualities.filter { quality ->
                if (!quality.isHdr) return@filter true
                val decodable = videoCapabilityProvider.isDecodable(quality.toDecodeProfile())
                if (!decodable) {
                    logger.info { "HDR quality ${quality.key}(${quality.displayDesc}) filtered: not decodable" }
                }
                decodable
            }

        /** 档位被能力过滤或服务端降级移除时回退到首项，保证选中态与请求 key 有效。 */
        private fun resolveCurrentKey(
            qualities: List<LiveQuality>,
            currentKey: Int,
            fallbackKey: Int,
        ): Int =
            currentKey.takeIf { k -> k > 0 && qualities.any { it.key == k } }
                ?: qualities.firstOrNull()?.key
                ?: fallbackKey

        /** 从 HDR 档的解码提示构造 [VideoDecodeProfile]；dvh1/dvhe 为杜比视界。 */
        private fun LiveQuality.toDecodeProfile(): VideoDecodeProfile {
            val codec =
                when {
                    codecs?.startsWith("dvh1") == true || codecs?.startsWith("dvhe") == true -> VideoCodec.DVH1
                    codecName?.contains("hevc") == true -> VideoCodec.HEVC
                    codecName?.contains("av1") == true -> VideoCodec.AV1
                    else -> VideoCodec.AVC
                }
            return VideoDecodeProfile(
                codec = codec,
                width = width,
                height = height,
                frameRate = null,
                codecs = codecs,
            )
        }

        /**
         * 按线路序号解析目标线路；序号越界时回退到第一条。
         *
         * @param playInfo 直播流信息
         * @param preferredOrder 期望的线路序号（从 1 开始），0 表示默认第一条
         */
        private fun resolveLine(
            playInfo: LivePlayInfo,
            preferredOrder: Int,
        ): LivePlayLine? =
            if (playInfo.lines.isEmpty()) {
                null
            } else {
                playInfo.lines.firstOrNull { it.order == preferredOrder } ?: playInfo.lines.first()
            }

        /**
         * 释放播放器资源。
         *
         * 弹幕播放器由 [DanmakuViewModel] 管理释放，此处不释放。
         */
        fun detachPlayer() {
            wsJob?.cancel()
            stopDebugInfoUpdater()
            videoPlayer?.release()
            videoPlayer = null
            danmakuPlayer = null
            danmakuFilter = null
        }

        override fun onCleared() {
            super.onCleared()
            detachPlayer()
        }
    }
