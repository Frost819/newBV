package dev.frost819.newbv.app.viewmodel.live

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.component.livecard.formatOnlineCount
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.LiveEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.biliapi.websocket.LiveDataWebSocket
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LivePlayerState {
    Idle, Loading, Playing, Paused, Buffering, Error, Ended
}

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
    val danmakuEnabled: Boolean = true,
    val availableQualities: List<Pair<Int, String>> = emptyList(),
    val currentQuality: Int = 0,
    val showController: Boolean = true,
)

@HiltViewModel
class LivePlayerViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    private val exoPlayerFactory: ExoPlayerFactory,
) : ViewModel() {

    private val logger = KotlinLogging.logger { }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
        private set

    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private var wsJob: Job? = null
    private var danmakuIdCounter = 0L

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

        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
    }

    fun loadLive(roomId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(playerState = LivePlayerState.Loading) }

            runCatching {
                val roomInit = liveRepository.getRoomInit(roomId.toInt())
                val realRoomId = roomInit.roomId
                _uiState.update { it.copy(realRoomId = realRoomId, liveStatus = roomInit.liveStatus) }

                val roomInfo = liveRepository.getRoomInfo(realRoomId)
                _uiState.update {
                    it.copy(
                        title = roomInfo.title.ifBlank { _uiState.value.title },
                        uname = roomInfo.uname,
                        cover = roomInfo.cover.ifBlank { roomInfo.keyframe },
                        areaName = roomInfo.areaV2Name,
                    )
                }

                val qualities = liveRepository.getAvailableQualities(realRoomId)
                _uiState.update { it.copy(availableQualities = qualities) }

                val streamUrl = liveRepository.getLiveStreamUrl(realRoomId)
                if (streamUrl.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            playerState = LivePlayerState.Error,
                            errorMessage = "获取直播流地址失败",
                        )
                    }
                    return@launch
                }

                videoPlayer?.playUrl(streamUrl)
                videoPlayer?.prepare()
                videoPlayer?.start()
                danmakuPlayer?.start(null)

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
                LiveDataWebSocket.connectLiveEvent(realRoomId) { event ->
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

    private fun handleLiveEvent(event: LiveEvent) {
        when (event) {
            is DanmakuEvent -> {
                if (_uiState.value.danmakuEnabled) {
                    sendDanmaku(event.content, event.mid)
                }
            }
            is OnlineRankCountEvent -> {
                _uiState.update {
                    it.copy(onlineCount = formatOnlineCount(event.count))
                }
            }
            else -> {}
        }
    }

    private fun sendDanmaku(content: String, mid: Long) {
        val danmakuId = danmakuIdCounter++
        val data = DanmakuItemData(
            danmakuId = danmakuId,
            position = System.currentTimeMillis(),
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

    fun togglePlayPause() {
        val player = videoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            danmakuPlayer?.pause()
        } else {
            player.start()
            danmakuPlayer?.start(null)
        }
    }

    fun toggleDanmaku() {
        _uiState.update { it.copy(danmakuEnabled = !it.danmakuEnabled) }
    }

    fun toggleController() {
        _uiState.update { it.copy(showController = !it.showController) }
    }

    fun changeQuality(qn: Int) {
        _uiState.update { it.copy(currentQuality = qn) }
        val realRoomId = _uiState.value.realRoomId
        if (realRoomId == 0) return

        viewModelScope.launch {
            runCatching {
                val streamUrl = liveRepository.getLiveStreamUrl(realRoomId, qn)
                if (!streamUrl.isNullOrBlank()) {
                    videoPlayer?.playUrl(streamUrl)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to change quality" }
            }
        }
    }

    fun detachPlayer() {
        wsJob?.cancel()
        videoPlayer?.release()
        videoPlayer = null
        danmakuPlayer?.release()
        danmakuPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        detachPlayer()
    }
}
