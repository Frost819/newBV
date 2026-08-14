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
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.LiveEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.biliapi.repositories.LiveStreamInfo
import dev.frost819.newbv.biliapi.websocket.LiveDataWebSocket
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.impl.exo.ExoMediaPlayer
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
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
 * 直播播放器状态。
 */
enum class LivePlayerState {
    Idle, Loading, Playing, Paused, Buffering, Error, Ended
}

/**
 * 直播播放器 UI 状态。
 */
data class LivePlayerUiState(
    val title: String = "",
    val uname: String = "",
    val cover: String = "",
    val roomId: Long = 0,
    val realRoomId: Int = 0,
    val liveStatus: Int = 0,
    val areaName: String = "",
    val onlineCount: String = "0",
    val playerState: LivePlayerState = LivePlayerState.Idle,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val availableQualities: List<Pair<Int, String>> = emptyList(),
    val currentQuality: Int = 0,
)

/**
 * 直播播放器 ViewModel。
 *
 * 管理直播流加载、播放控制、WebSocket 弹幕连接。
 * 弹幕播放器实例由 [DanmakuViewModel] 创建并传入，本类仅负责发送直播弹幕数据。
 *
 * @see dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
 */
@HiltViewModel
class LivePlayerViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    private val exoPlayerFactory: ExoPlayerFactory,
) : ViewModel() {

    private val logger = KotlinLogging.logger { }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
        private set

    private var danmakuPlayer: DanmakuPlayer? = null

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private var wsJob: Job? = null
    private var debugInfoJob: Job? = null
    private var danmakuIdCounter = 0L

    private val _debugInfo = MutableStateFlow("")
    /** 调试信息，仅在 [Prefs.showPlayerDebugInfo] 开启时更新。 */
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    /**
     * 初始化直播间信息。
     */
    fun init(roomId: Long, title: String, cover: String) {
        _uiState.update {
            it.copy(
                roomId = roomId,
                title = title,
                cover = cover,
                playerState = LivePlayerState.Loading,
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
        val options = VideoPlayerOptions(
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
            referer = "https://live.bilibili.com",
        )
        videoPlayer = exoPlayerFactory.create(context, options)
        videoPlayer?.setPlayerEventListener(object : VideoPlayerListener {
            override fun onError(error: Exception) {
                logger.error(error) { "Live player error" }

                _uiState.update {
                    it.copy(
                        playerState = LivePlayerState.Error,
                        isBuffering = false,
                        errorMessage = error.message,
                    )
                }
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
        })
        videoPlayer?.initPlayer()
        videoPlayer?.setOptions()
    }

    /**
     * 加载直播间。
     *
     * @param roomId 房间号
     * @param danmakuPlayer 弹幕播放器实例（由 DanmakuViewModel 提供）
     */
    fun loadLive(roomId: Long, danmakuPlayer: DanmakuPlayer? = null) {
        this.danmakuPlayer = danmakuPlayer
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
            _uiState.update { it.copy(playerState = LivePlayerState.Loading) }

            runCatching {
                val roomInit = liveRepository.getRoomInit(roomId.toInt())
                val realRoomId = roomInit.roomId
                _uiState.update { it.copy(realRoomId = realRoomId, liveStatus = roomInit.liveStatus) }

                val roomInfo = liveRepository.getRoomInfo(realRoomId)
                _uiState.update {
                    val newState = it.copy(
                        title = roomInfo.title.ifBlank { _uiState.value.title },
                        uname = roomInfo.uname,
                        cover = roomInfo.cover.ifBlank { roomInfo.keyframe },
                        areaName = roomInfo.areaV2Name,
                        onlineCount = formatOnlineCount(roomInfo.online),
                    )
                    newState
                }

                val qualities = liveRepository.getAvailableQualities(realRoomId)
                _uiState.update { it.copy(availableQualities = qualities) }

                val qn = _uiState.value.currentQuality.takeIf { it > 0 } ?: 0
                val streamInfo = liveRepository.getLiveStreamInfo(realRoomId, qn)
                if (streamInfo.url.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            playerState = LivePlayerState.Error,
                            errorMessage = "获取直播流地址失败",
                        )
                    }
                    return@launch
                }

                if (streamInfo.currentQn > 0) {
                    _uiState.update { it.copy(currentQuality = streamInfo.currentQn) }
                }

                videoPlayer?.playUrl(streamInfo.url)
                videoPlayer?.prepare()
                videoPlayer?.start()
                danmakuPlayer?.start(null)
                startDebugInfoUpdater()

                connectDanmaku(realRoomId)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to load live" }
                _uiState.update {
                    it.copy(
                        playerState = LivePlayerState.Error,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    private fun connectDanmaku(realRoomId: Int) {
        wsJob?.cancel()
        wsJob = viewModelScope.launch(Dispatchers.IO) {
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
        debugInfoJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = videoPlayer as? ExoMediaPlayer ?: break
                val state = _uiState.value
                _debugInfo.value = buildString {
                    appendLine("player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}")
                    appendLine("state: ${state.playerState}")
                    appendLine("stream: ${player.streamProtocol}")
                    val qnDesc = state.availableQualities
                        .firstOrNull { it.first == state.currentQuality }?.second
                    appendLine("quality: ${qnDesc ?: "?"}(${state.currentQuality})")
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
                    sendDanmaku(event.content, event.mid)
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

    private fun sendDanmaku(content: String, mid: Long) {
        val danmakuId = danmakuIdCounter++
        val data = DanmakuItemData(
            danmakuId = danmakuId,
            position = danmakuPlayer?.getCurrentTimeMs() ?: 0,
            content = content,
            mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
            textSize = 25,
            textColor = 0xFFFFFF,
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
     */
    fun changeQuality(qn: Int) {
        _uiState.update { it.copy(currentQuality = qn) }
        val realRoomId = _uiState.value.realRoomId
        if (realRoomId == 0) return

        viewModelScope.launch {
            runCatching {
                val streamInfo = liveRepository.getLiveStreamInfo(realRoomId, qn)
                if (!streamInfo.url.isNullOrBlank()) {
                    videoPlayer?.playUrl(streamInfo.url)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to change quality" }
            }
        }
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
    }

    override fun onCleared() {
        super.onCleared()
        detachPlayer()
    }
}
