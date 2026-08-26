package dev.frost819.newbv.app.viewmodel.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.action.player.MediaProfileSettingAction
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.ui.state.player.PlayerUiEffect
import dev.frost819.newbv.app.ui.state.player.PlayerUiState
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.PlayerConstants
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.PlayData
import dev.frost819.newbv.biliapi.entity.video.HeartbeatVideoType
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.biliapi.repositories.CoinRepository
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.LikeRepository
import dev.frost819.newbv.biliapi.repositories.OneClickTripleActionRepository
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerListener
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.Calendar
import javax.inject.Inject

private const val PLAYER_ACTION_TIMEOUT_MS = 10_000L

/** 同时观看人数刷新间隔（cid 就绪后周期轮询）。 */
private const val ONLINE_WATCH_REFRESH_MS = 60_000L

/**
 * 播放器主 ViewModel。
 *
 * 管理视频播放器生命周期、URL 解析与播放、进度同步、心跳上报、
 * 播放结束动作、上下集切换。
 *
 * 不管理弹幕播放器（由 [DanmakuViewModel] 管理）和字幕（由 [SubtitleViewModel] 管理），
 * 但会通过 UI 层协调与它们同步。
 *
 * @param videoPlayRepository 播放数据仓库（URL、弹幕、字幕、蒙版、心跳、缩略图）
 * @param videoInfoRepository 视频信息共享仓库（分集列表、详情）
 * @param authRepository 鉴权仓库（会话凭证）
 * @param likeRepository 视频点赞仓库
 * @param coinRepository 视频投币仓库
 * @param favoriteRepository 视频收藏仓库
 * @param oneClickTripleActionRepository 一键三连仓库
 * @param exoPlayerFactory ExoPlayer 工厂
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val videoPlayRepository: VideoPlayRepository,
    private val videoInfoRepository: VideoInfoRepository,
    private val authRepository: AuthRepository,
    private val exoPlayerFactory: ExoPlayerFactory,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository,
    private val favoriteRepository: FavoriteRepository,
    private val oneClickTripleActionRepository: OneClickTripleActionRepository,
) : ViewModel() {

    private val logger = Loggers.get("PlayerViewModel")

    /** 视频播放器实例，供 Compose `AndroidView` 绑定。 */
    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
        private set

    private var playData: PlayData? = null

    private val detachedWorkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _seekerState = MutableStateFlow(SeekerState())
    val seekerState = _seekerState.asStateFlow()

    /** 当前视频共享状态（交互 + 历史）。 */
    val videoSharedState = videoInfoRepository.videoSharedState

    private val _uiEffect = MutableSharedFlow<PlayerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    /**
     * 切换视频时发出的事件流。
     *
     * UI 层（[VideoPlayerScreen]）收集此流，在切换视频时协调
     * [DanmakuViewModel] 和 [SubtitleViewModel] 的重载。
     */
    private val _videoSwitchEvent = MutableSharedFlow<VideoSwitchEvent>(extraBufferCapacity = 1)
    val videoSwitchEvent = _videoSwitchEvent.asSharedFlow()

    private var seekerUpdateJob: Job? = null
    private var debugInfoUpdateJob: Job? = null
    private var clockUpdateJob: Job? = null
    private var heartbeatJob: Job? = null
    private var loadVideoJob: Job? = null
    private var backToStartCountdownJob: Job? = null
    private var playNextCountdownJob: Job? = null
    private var previewTipCountdownJob: Job? = null
    private var onlineWatchJob: Job? = null

    /**
     * 切换视频事件。
     *
     * @param aid 新视频 AV 号
     * @param cid 新视频 CID
     */
    data class VideoSwitchEvent(val aid: Long, val cid: Long)

    private val videoPlayerListener = object : VideoPlayerListener {
        override fun onError(error: Exception) {
            logger.info { "onError: $error" }
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(error.message ?: "Unknown error"),
                    isBuffering = false,
                )
            }
        }

        override fun onReady() {
            logger.info { "onReady" }
            _uiState.update { it.copy(playerState = PlayerState.Ready) }
            updatePlaySpeed(forceUpdate = true)
            startSeekerUpdater()
            startDebugInfoUpdater()
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            _uiState.update { it.copy(playerState = PlayerState.Playing, isBuffering = false) }
            if (_uiState.value.lastPlayed > 0) {
                seekToLastPlayed()
                _uiState.update { it.copy(lastPlayed = 0) }
            }
        }

        override fun onPause() {
            logger.info { "onPause" }
            _uiState.update { it.copy(playerState = PlayerState.Paused) }
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }
            _uiState.update { it.copy(isBuffering = true) }
        }

        override fun onEnd() {
            logger.info { "onEnd" }
            stopSeekerUpdater()
            stopDebugInfoUpdater()
            _uiState.update { it.copy(playerState = PlayerState.Ended) }
            viewModelScope.launch { _uiEffect.emit(PlayerUiEffect.PlayEnded) }
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {}
        override fun onSeekForward(seekForwardIncrementMs: Long) {}
    }

    /**
     * 初始化播放器状态。
     *
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     * @param epid 番剧分集 ID（UGC 为 null）
     * @param title 视频标题
     * @param lastPlayed 上次播放位置（秒）
     * @param fromSeason 是否为番剧播放
     * @param subType 番剧子类型
     * @param seasonId 番剧 season ID
     * @param authorMid UP 主 mid
     * @param authorName UP 主名称
     */
    fun init(
        aid: Long,
        cid: Long,
        epid: Int?,
        title: String,
        lastPlayed: Int,
        fromSeason: Boolean,
        subType: Int,
        seasonId: Int,
        authorMid: Long = 0,
        authorName: String,
    ) {
        _uiState.update {
            it.copy(
                aid = aid,
                cid = cid,
                epid = epid.takeIf { it != null && it != 0 },
                seasonId = seasonId,
                title = title,
                lastPlayed = lastPlayed,
                fromSeason = fromSeason,
                subType = subType,
                authorMid = authorMid,
                authorName = authorName,
            )
        }

        startClockUpdater()
        // 同时观看人数观察者：监听 cid 变化即时拉取，就绪后周期刷新
        startOnlineWatchingObserver()
    }

    /**
     * 加载视频详情（相关视频、历史进度等）。
     *
     * 视频列表由详情页在导航前通过 [VideoInfoRepository] 填充。
     * 若播放器直接打开（无详情页上下文），列表为空，仅加载详情用于相关视频和历史。
     *
     * 加载完成后，仅当历史 cid 与当前播放 cid 一致时才应用断点续播，
     * 避免多 P 视频中把 P2 的进度应用到 P1。
     *
     * @param aid 视频 AV 号
     */
    suspend fun loadVideoDetail(aid: Long, bvid: String = "") {
        videoInfoRepository.loadVideoDetail(aid, getApiType(), bvid)
        videoInfoRepository.videoDetail.value?.let { detail ->
            _uiState.update {
                it.copy(
                    cid = detail.cid,
                    authorMid = detail.author.mid,
                    authorName = detail.author.name,
                )
            }
        }
        val sharedState = videoInfoRepository.videoSharedState.value
        val historyCid = sharedState?.lastPlayedCid ?: 0L
        val historyTime = sharedState?.lastPlayedTime ?: 0
        if (historyCid == _uiState.value.cid && historyTime > 0) {
            _uiState.update { it.copy(lastPlayed = historyTime) }
        }
    }

    /**
     * 启动同时观看人数观察者（[init] 时启动，随播放器会话存续）。
     *
     * 响应式监听 uiState 的 cid 变化：cid 就绪（直进时详情返回、
     * init 携带有效 cid、播放器内切集）时立即拉取，之后按
     * [ONLINE_WATCH_REFRESH_MS] 周期刷新。
     * [collectLatest] 保证切集瞬间取消旧 cid 的在途请求与等待。
     * 每轮循环读取实时 uiState，防御 cid 中途失效的边界情况。
     * 请求失败静默忽略（保留现有文案），绝不影响播放（参考 blbl 降级策略）。
     */
    private fun startOnlineWatchingObserver() {
        onlineWatchJob?.cancel()
        onlineWatchJob = viewModelScope.launch {
            _uiState
                .map { it.cid }
                .filter { it > 0L }
                .distinctUntilChanged()
                .collectLatest {
                    while (isActive) {
                        val state = _uiState.value
                        if (state.cid > 0L && state.aid > 0L) {
                            fetchOnlineWatching(state)
                        }
                        delay(ONLINE_WATCH_REFRESH_MS)
                    }
                }
        }
    }

    /**
     * 拉取一次同时观看人数并更新 uiState。
     *
     * @param state 当前 UI 状态（提供 aid/cid）
     */
    private suspend fun fetchOnlineWatching(state: PlayerUiState) {
        runCatching {
            withTimeout(PLAYER_ACTION_TIMEOUT_MS) {
                videoInfoRepository.getOnlineWatchingText(
                    aid = state.aid,
                    cid = state.cid,
                    preferApiType = getApiType(),
                )
            }
        }.onSuccess { text ->
            if (text != null) _uiState.update { it.copy(onlineWatching = text) }
        }.onFailure { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            logger.error(error) { "Failed to fetch online watching count" }
        }
    }

    /** 停止同时观看人数观察者。 */
    private fun stopOnlineWatchingPolling() {
        onlineWatchJob?.cancel()
        onlineWatchJob = null
    }

    /**
     * 初始化视频播放器实例。
     *
     * 根据 Prefs 中的 API 类型设置 User-Agent 和 Referer，
     * 创建 ExoPlayer 并绑定事件监听器。
     */
    fun initVideoPlayer(context: Context) {
        val apiType = Prefs.apiType
        val options = VideoPlayerOptions(
            userAgent = PlayerConstants.getUserAgent(
                if (apiType == DataApiType.App) ApiType.App else ApiType.Web,
            ),
            referer = PlayerConstants.getReferer(
                if (apiType == DataApiType.App) ApiType.App else ApiType.Web,
            ),
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder,
        )

        val newPlayer = exoPlayerFactory.create(context.applicationContext, options)
        newPlayer.setPlayerEventListener(videoPlayerListener)
        videoPlayer = newPlayer

        val initialSpeed = Prefs.defaultPlaySpeed.speed
        _uiState.update { it.copy(playSpeed = initialSpeed) }
        newPlayer.speed = initialSpeed
    }

    /** 释放播放器资源，同步进度到 B 站。 */
    fun detachPlayer() {
        syncProgress(scope = detachedWorkScope, isDetaching = true)
        videoPlayer?.release()
        videoPlayer = null
        stopSeekerUpdater()
        stopDebugInfoUpdater()
        stopOnlineWatchingPolling()
        clockUpdateJob?.cancel()
    }

    /** 播放/暂停切换。 */
    fun togglePlayPause() {
        val player = videoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.start()
        }
    }

    /** 暂停视频，不改变其它播放器设置。 */
    fun pausePlayback() {
        if (videoPlayer?.isPlaying == true) videoPlayer?.pause()
    }

    /** 恢复视频播放。 */
    fun resumePlayback() {
        if (videoPlayer?.isPlaying != true) videoPlayer?.start()
    }

    /** 切换当前视频点赞状态，并同步详情页。 */
    fun toggleVideoLike() {
        val aid = _uiState.value.aid.takeIf { it > 0L } ?: return
        val current = videoInfoRepository.videoSharedState.value?.takeIf { it.aid == aid }?.liked ?: false
        viewModelScope.launch {
            runCatching {
                withTimeout(PLAYER_ACTION_TIMEOUT_MS) {
                    likeRepository.updateVideoLiked(aid = aid, like = !current, preferApiType = getApiType())
                }
            }.onSuccess {
                videoInfoRepository.updateVideoActionState(aid = aid, liked = !current)
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                logger.error(error) { "Failed to toggle video like: aid=$aid" }
                _uiEffect.emit(PlayerUiEffect.ShowToast("点赞失败: ${error.message ?: "未知错误"}"))
            }
        }
    }

    /** 为当前视频投一枚硬币，并同步详情页。 */
    fun sendVideoCoin() {
        val aid = _uiState.value.aid.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            runCatching {
                withTimeout(PLAYER_ACTION_TIMEOUT_MS) {
                    coinRepository.sendVideoCoin(aid = aid, preferApiType = getApiType())
                }
            }.onSuccess {
                videoInfoRepository.updateVideoActionState(aid = aid, coined = true)
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                logger.error(error) { "Failed to send video coin: aid=$aid" }
                _uiEffect.emit(PlayerUiEffect.ShowToast("投币失败: ${error.message ?: "未知错误"}"))
            }
        }
    }

    /** 收藏当前视频到默认收藏夹，并同步详情页。 */
    fun toggleVideoFavorite() {
        val aid = _uiState.value.aid.takeIf { it > 0L } ?: return
        val current = videoInfoRepository.videoSharedState.value?.takeIf { it.aid == aid }?.favorited ?: false
        viewModelScope.launch {
            runCatching {
                withTimeout(PLAYER_ACTION_TIMEOUT_MS) {
                    val folders = favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = authRepository.mid ?: error("未登录"),
                        rid = aid,
                        preferApiType = getApiType(),
                    )
                    val selected = folders.filter { it.videoInThisFav }.map { it.id }
                    val defaultFolder = folders.firstOrNull { it.title == "默认收藏夹" }?.id
                    if (current) {
                        favoriteRepository.updateVideoToFavoriteFolder(
                            aid = aid,
                            addMediaIds = emptyList(),
                            delMediaIds = selected,
                            preferApiType = getApiType(),
                        )
                    } else {
                        favoriteRepository.updateVideoToFavoriteFolder(
                            aid = aid,
                            addMediaIds = listOfNotNull(defaultFolder),
                            delMediaIds = emptyList(),
                            preferApiType = getApiType(),
                        )
                    }
                }
            }.onSuccess {
                videoInfoRepository.updateVideoActionState(aid = aid, favorited = !current)
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                logger.error(error) { "Failed to toggle video favorite: aid=$aid" }
                _uiEffect.emit(PlayerUiEffect.ShowToast("收藏失败: ${error.message ?: "未知错误"}"))
            }
        }
    }

    /** 执行当前视频一键三连，并同步详情页。 */
    fun oneClickTripleAction() {
        val aid = _uiState.value.aid.takeIf { it > 0L } ?: return
        val bvid = videoInfoRepository.videoDetail.value?.bvid
        viewModelScope.launch {
            runCatching {
                withTimeout(PLAYER_ACTION_TIMEOUT_MS) {
                    oneClickTripleActionRepository.sendVideoOneClickTripleAction(
                        aid = aid,
                        bvid = bvid,
                        preferApiType = getApiType(),
                    )
                }
            }.onSuccess { result ->
                if (result != null) {
                    videoInfoRepository.updateVideoActionState(
                        aid = aid,
                        liked = result.like,
                        coined = result.coin,
                        favorited = result.fav,
                    )
                }
                _uiEffect.emit(PlayerUiEffect.ShowToast("一键三连"))
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                logger.error(error) { "Failed to send one-click triple action: aid=$aid" }
                _uiEffect.emit(PlayerUiEffect.ShowToast("一键三连失败: ${error.message ?: "未知错误"}"))
            }
        }
    }

    /** 跳转到指定位置（毫秒）。 */
    fun seekToTime(time: Long) {
        videoPlayer?.seekTo(time)
        _seekerState.update { it.copy(currentTime = time) }
    }

    /** 回到开头。 */
    fun backToStart() {
        backToStartCountdownJob?.cancel()
        _uiState.update { it.copy(showBackToStart = false) }
        videoPlayer?.seekTo(0)
    }

    /** 立即播放下一集。 */
    fun playNextNow() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
        findNextPlayTarget()?.let { playNextTarget(it) }
    }

    /** 立即播放上一集。 */
    fun playPreviousNow() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
        findPreviousPlayTarget()?.let { playNextTarget(it) }
    }

    /** 取消自动播放下一集。 */
    fun cancelPlayNext() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
    }

    /**
     * 播放结束后的检查逻辑。
     *
     * 根据 Prefs.actionAfterPlay 决定：暂停 / 播放下一集 / 退出 / 播放相关视频。
     */
    fun checkAndPlayNext() {
        when (Prefs.actionAfterPlay) {
            dev.frost819.newbv.data.datastore.ActionAfterPlay.Pause -> return
            dev.frost819.newbv.data.datastore.ActionAfterPlay.Exit -> {
                viewModelScope.launch { _uiEffect.emit(PlayerUiEffect.FinishActivity) }
                return
            }
            dev.frost819.newbv.data.datastore.ActionAfterPlay.PlayRelated -> {
                val firstRelated = videoInfoRepository.relatedVideos.value.firstOrNull()
                if (firstRelated != null) {
                    playNewVideo(
                        VideoListItem(
                            aid = firstRelated.aid,
                            cid = firstRelated.cid,
                            title = firstRelated.title,
                        ),
                    )
                    return
                }
            }
            dev.frost819.newbv.data.datastore.ActionAfterPlay.PlayNext -> { /* 继续执行 */ }
        }

        val nextTarget = findNextPlayTarget()
        if (nextTarget != null) {
            startNextEpisodeCountdown(nextTarget)
        } else {
            viewModelScope.launch { _uiEffect.emit(PlayerUiEffect.FinishActivity) }
        }
    }

    /**
     * 切换播放视频。
     *
     * 同步旧视频进度，更新 UI 状态，重新加载资源。
     * 通过 [videoSwitchEvent] 通知 UI 层协调弹幕/字幕重载。
     */
    fun playNewVideo(newVideo: VideoListItem) {
        videoPlayer?.pause()

        val state = _uiState.value
        val shouldUpdateDetail = state.aid != newVideo.aid
        val shouldUpdateList = videoInfoRepository.videoList.value.none { it.aid == newVideo.aid }

        syncProgress(viewModelScope)

        // 清空旧视频状态，防止新视频加载失败时残留旧数据
        playData = null
        stopSeekerUpdater()
        stopDebugInfoUpdater()

        _uiState.update {
            it.copy(
                aid = newVideo.aid,
                cid = newVideo.cid,
                epid = newVideo.epid,
                seasonId = newVideo.seasonId ?: 0,
                title = newVideo.title,
                lastPlayed = 0,
                isBuffering = true,
                playerState = PlayerState.Ready,
                videoShot = null,
                danmakuMask = null,
                subtitleList = emptyList(),
                subtitleData = emptyList(),
                subtitleId = -1L,
                availableQuality = emptyMap(),
                availableVideoCodec = emptyList(),
                availableAudio = emptyList(),
                onlineWatching = "",
                showPreviewTip = false,
                showSkipToNextEp = false,
                showBackToStart = false,
            )
        }

        // 通知 UI 层重载弹幕/字幕（非阻塞，避免卡住 playNewVideo）
        viewModelScope.launch { _videoSwitchEvent.emit(VideoSwitchEvent(newVideo.aid, newVideo.cid)) }

        // 异步加载详情 + 历史进度（不阻塞 playVideoWithResources）
        if (shouldUpdateDetail) {
            viewModelScope.launch(Dispatchers.IO) {
                videoInfoRepository.loadVideoDetail(newVideo.aid, getApiType())
                // 仅当历史 cid 与当前播放 cid 一致时才应用断点续播
                val sharedState = videoInfoRepository.videoSharedState.value
                val historyCid = sharedState?.lastPlayedCid ?: 0L
                val historyTime = sharedState?.lastPlayedTime ?: 0
                if (historyCid == newVideo.cid && historyTime > 0) {
                    _uiState.update { it.copy(lastPlayed = historyTime) }
                }
            }
        }
        if (shouldUpdateList) {
            videoInfoRepository.updateVideoList(listOf(newVideo))
        }

        loadVideoWithResources()
    }

    /** 发送心跳（进度上报）。 */
    fun trySendHeartbeat() {
        syncProgress(scope = viewModelScope, updateLocal = false)
    }

    /**
     * 加载视频资源并开始播放。
     *
     * 并行加载：播放地址 + 弹幕 + 字幕 + 蒙版 + 缩略图 + 分 P。
     */
    fun loadVideoWithResources() {
        val state = _uiState.value
        val aid = state.aid
        val cid = state.cid
        val epid = state.epid

        loadVideoJob?.cancel()
        loadVideoJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                resolveUrlsAndPlay(aid, cid, epid ?: 0)
                launch { updateVideoShot() }
                launch { updateVideoPages() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Loading video data error: $e" }
                _uiState.update {
                    it.copy(
                        playerState = PlayerState.Error(e.message ?: "未知错误"),
                        isBuffering = false,
                    )
                }
            }
        }
    }

    /** 更新播放速度。 */
    fun updatePlaySpeed(speed: Float? = null, forceUpdate: Boolean = false) {
        val currentSpeed = _uiState.value.playSpeed
        val targetSpeed = speed ?: currentSpeed
        if (!forceUpdate && currentSpeed == targetSpeed) return
        _uiState.update { it.copy(playSpeed = targetSpeed) }
        videoPlayer?.speed = targetSpeed
    }

    /** 更新宽高比。 */
    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    /** 切换循环播放。 */
    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    /**
     * 更新媒体格式（画质/编码/音轨）。
     *
     * 如果当前正在播放，会暂停、重新解析 URL、恢复进度后继续播放。
     */
    fun updateMediaProfile(action: MediaProfileSettingAction) {
        val old = _uiState.value.mediaProfileState
        val new = when (action) {
            is MediaProfileSettingAction.SetQuality -> old.copy(qualityId = action.qualityId)
            is MediaProfileSettingAction.SetVideoCodec -> old.copy(videoCodec = action.codec)
            is MediaProfileSettingAction.SetAudio -> old.copy(audio = action.audio)
        }
        if (old == new) return

        _uiState.update { it.copy(mediaProfileState = new) }

        videoPlayer?.let { player ->
            player.pause()
            val currentPosition = player.currentPosition
            val mediaUrls = resolveMediaUrls(new.qualityId, new.videoCodec, new.audio)
            if (mediaUrls != null) {
                player.playUrl(mediaUrls.videoUrl, mediaUrls.audioUrl)
                player.prepare()
                if (currentPosition > 0) player.seekTo(currentPosition)
                player.start()
            }
        }
    }

    // ── 私有方法 ──────────────────────────────────────────────

    private suspend fun resolveUrlsAndPlay(aid: Long, cid: Long, epid: Int = 0) {
        try {
            val mediaUrls = fetchMediaUrls(aid, cid, epid)
            withContext(Dispatchers.Main) { executePlayback(mediaUrls) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to load media: ${e.message}" }
            throw IllegalStateException(e.message, e)
        }
    }

    private suspend fun fetchMediaUrls(aid: Long, cid: Long, epid: Int): MediaUrls {
        val config = loadPlaybackConfig(aid, cid, epid)
        return resolveMediaUrls(config.qn, config.codec, config.audio)
            ?: throw IllegalStateException("视频源解析失败")
    }

    private suspend fun loadPlaybackConfig(
        aid: Long,
        cid: Long,
        epid: Int = 0,
    ): PlaybackConfig {
        val apiType = getApiType()
        val playData = fetchPlayData(aid, cid, epid, apiType)
        this.playData = playData

        val resolutionMap = playData.dashVideos.associate { video ->
            video.quality to Resolution.fromCode(video.quality).name
        }

        val availableAudioList = buildList {
            addAll(playData.dashAudios.map { Audio.fromCode(it.codecId) })
            playData.dolby?.let { add(Audio.fromCode(it.codecId)) }
            playData.flac?.let { add(Audio.fromCode(it.codecId)) }
        }.distinct()

        val targetQualityId = calculateTargetQuality(resolutionMap.keys, Prefs.defaultQuality.code)
        val targetAudio = calculateTargetAudio(availableAudioList, Prefs.defaultAudio)
        val targetCodec = getTargetVideoCodec()

        _uiState.update {
            it.copy(
                availableQuality = resolutionMap,
                availableAudio = availableAudioList,
                mediaProfileState = it.mediaProfileState.copy(
                    qualityId = targetQualityId,
                    audio = targetAudio,
                ),
            )
        }

        if (playData.needPay) startShowPreviewTipCountdown()

        return PlaybackConfig(qn = targetQualityId, codec = targetCodec, audio = targetAudio)
    }

    private suspend fun fetchPlayData(aid: Long, cid: Long, epid: Int, apiType: ApiType): PlayData {
        return if (_uiState.value.fromSeason) {
            videoPlayRepository.getPgcPlayData(
                aid = aid,
                cid = cid,
                epid = epid,
                preferCodec = Prefs.defaultVideoCodec.let {
                    when (it) {
                        VideoCodec.AVC -> dev.frost819.newbv.biliapi.entity.CodeType.Code264
                        VideoCodec.HEVC -> dev.frost819.newbv.biliapi.entity.CodeType.Code265
                        VideoCodec.AV1 -> dev.frost819.newbv.biliapi.entity.CodeType.CodeAv1
                        else -> dev.frost819.newbv.biliapi.entity.CodeType.NoCode
                    }
                },
                preferApiType = apiType,
            )
        } else {
            videoPlayRepository.getPlayData(aid = aid, cid = cid, preferApiType = apiType)
        }
    }

    private fun calculateTargetQuality(available: Set<Int>, default: Int): Int {
        if (available.contains(default)) return default
        val sorted = available.sorted()
        return sorted.findLast { it <= default } ?: sorted.firstOrNull() ?: 0
    }

    private fun calculateTargetAudio(available: List<Audio>, default: Audio): Audio {
        if (available.contains(default)) return default
        return when {
            default == Audio.ADolbyAtoms && available.contains(Audio.AHiRes) -> Audio.AHiRes
            default == Audio.AHiRes && available.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
            available.contains(Audio.A192K) -> Audio.A192K
            available.contains(Audio.A132K) -> Audio.A132K
            available.contains(Audio.A64K) -> Audio.A64K
            else -> available.firstOrNull() ?: Audio.A132K
        }
    }

    private fun getTargetVideoCodec(): VideoCodec? {
        val state = _uiState.value
        val data = playData ?: return null
        val apiType = getApiType()

        if (apiType == ApiType.App && data.codec.isEmpty()) {
            val videoItem = data.dashVideos.find { it.quality == state.mediaProfileState.qualityId }
                ?: data.dashVideos.firstOrNull() ?: return null
            val codec = VideoCodec.fromCodecId(videoItem.codecId)
            _uiState.update {
                it.copy(
                    availableVideoCodec = listOf(codec),
                    mediaProfileState = it.mediaProfileState.copy(videoCodec = codec),
                )
            }
            return codec
        }

        val codecList = data.codec[state.mediaProfileState.qualityId]
            ?.mapNotNull { VideoCodec.fromCodecString(it) }
            ?.takeIf { it.isNotEmpty() } ?: return null

        val targetCodec = if (codecList.contains(Prefs.defaultVideoCodec)) {
            Prefs.defaultVideoCodec
        } else {
            codecList.minByOrNull { it.ordinal } ?: return null
        }

        _uiState.update {
            it.copy(
                availableVideoCodec = codecList,
                mediaProfileState = it.mediaProfileState.copy(videoCodec = targetCodec),
            )
        }
        return targetCodec
    }

    private fun resolveMediaUrls(
        qn: Int? = null,
        codec: VideoCodec? = null,
        audio: Audio? = null,
    ): MediaUrls? {
        val data = playData ?: return null
        val state = _uiState.value
        val targetQn = qn ?: state.mediaProfileState.qualityId
        val targetCodec = codec ?: state.mediaProfileState.videoCodec
        val targetAudio = audio ?: state.mediaProfileState.audio
        val apiType = getApiType()

        val foundVideo = data.dashVideos.find {
            val codecStr = it.codecs
            when (apiType) {
                ApiType.Web -> it.quality == targetQn && codecStr != null && targetCodec.prefixes.any { p -> codecStr.startsWith(p) }
                ApiType.App -> if (data.codec.isEmpty()) it.quality == targetQn
                    else it.quality == targetQn && codecStr != null && targetCodec.prefixes.any { p -> codecStr.startsWith(p) }
            }
        } ?: data.dashVideos.firstOrNull() ?: return null

        val videoUrls = mutableListOf<String?>()
        videoUrls.add(foundVideo.baseUrl)
        videoUrls.addAll(foundVideo.backUrl)

        val audioItem = data.dashAudios.find { it.codecId == targetAudio.code }
            ?: data.dolby.takeIf { it?.codecId == targetAudio.code }
            ?: data.flac.takeIf { it?.codecId == targetAudio.code }
            ?: data.dashAudios.minByOrNull { it.codecId }

        val audioUrls = mutableListOf<String>()
        audioItem?.baseUrl?.let { audioUrls.add(it) }
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        val videoUrl = selectOfficialCdnUrl(videoUrls.filterNotNull())
        val audioUrl = if (audioUrls.isNotEmpty()) selectOfficialCdnUrl(audioUrls) else null

        _uiState.update { it.copy(videoHeight = foundVideo.height, videoWidth = foundVideo.width) }
        return MediaUrls(videoUrl, audioUrl)
    }

    private fun executePlayback(mediaUrls: MediaUrls) {
        val player = videoPlayer ?: run {
            logger.error { "VideoPlayer is not initialized!" }
            return
        }
        player.playUrl(mediaUrls.videoUrl, mediaUrls.audioUrl)
        player.prepare()
        player.start()
    }

    private suspend fun updateVideoShot() {
        val state = _uiState.value
        runCatching {
            val shot = videoPlayRepository.getVideoShot(
                aid = state.aid,
                cid = state.cid,
                preferApiType = getApiType(),
            )
            _uiState.update { it.copy(videoShot = shot) }
        }.onFailure { logger.warn { "Load video shot failed: $it" } }
    }

    private suspend fun updateVideoPages() {
        videoInfoRepository.updateUgcPages(getApiType())
    }

    private fun syncProgress(
        scope: CoroutineScope,
        updateLocal: Boolean = true,
        isDetaching: Boolean = false,
    ) {
        val player = videoPlayer ?: return
        val state = _uiState.value
        val currentTime = (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
        val totalTime = (player.duration.coerceAtLeast(0) / 1000).toInt()
        val reportTime = if (currentTime >= totalTime) -1 else currentTime

        if (updateLocal) {
            videoInfoRepository.updateHistory(reportTime, state.cid)
        }

        if (!Prefs.incognitoMode) {
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch(Dispatchers.IO) {
                try {
                    if (isDetaching) {
                        withTimeout(PlayerConstants.HEARTBEAT_DETACH_TIMEOUT_MS) {
                            uploadHistory(state, reportTime)
                        }
                    } else {
                        uploadHistory(state, reportTime)
                    }
                } catch (e: Exception) {
                    logger.warn { "Failed to upload history: $e" }
                }
            }
        }
    }

    private suspend fun uploadHistory(state: PlayerUiState, time: Int) {
        try {
            val apiType = getApiType()
            if (!state.fromSeason) {
                videoPlayRepository.sendHeartbeat(
                    aid = state.aid,
                    cid = state.cid,
                    time = time,
                    preferApiType = apiType,
                )
            } else {
                videoPlayRepository.sendHeartbeat(
                    aid = state.aid,
                    cid = state.cid,
                    time = time,
                    type = HeartbeatVideoType.Season,
                    subType = state.subType,
                    epid = state.epid,
                    seasonId = state.seasonId,
                    preferApiType = apiType,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn { "Send heartbeat failed: $e" }
        }
    }

    private fun seekToLastPlayed() {
        val time = _uiState.value.lastPlayed.toLong()
        videoPlayer?.seekTo(time)
        _uiState.update { it.copy(showBackToStart = true) }
        backToStartCountdownJob?.cancel()
        backToStartCountdownJob = viewModelScope.launch {
            delay(PlayerConstants.COUNTDOWN_DURATION_MS)
            _uiState.update { it.copy(showBackToStart = false) }
        }
    }

    private fun startSeekerUpdater() {
        if (seekerUpdateJob?.isActive == true) return
        seekerUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateSeekerState()
                delay(PlayerConstants.SEEKER_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopSeekerUpdater() {
        seekerUpdateJob?.cancel()
        seekerUpdateJob = null
    }

    /**
     * 启动调试信息轮询（仅 [Prefs.showPlayerDebugInfo] 开启时）。
     *
     * 独立于 seekerUpdater，以 500ms 间隔运行，避免无谓开销。
     */
    private fun startDebugInfoUpdater() {
        if (!Prefs.showPlayerDebugInfo) return
        if (debugInfoUpdateJob?.isActive == true) return
        debugInfoUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = videoPlayer ?: break
                _seekerState.update { it.copy(debugInfo = player.debugInfo) }
                delay(500)
            }
        }
    }

    private fun stopDebugInfoUpdater() {
        debugInfoUpdateJob?.cancel()
        debugInfoUpdateJob = null
    }

    private fun updateSeekerState() {
        val player = videoPlayer ?: return
        _seekerState.update {
            it.copy(
                totalDuration = player.duration.coerceAtLeast(0L),
                currentTime = player.currentPosition.coerceAtLeast(0L),
                bufferedPercentage = player.bufferedPercentage,
            )
        }
    }

    private fun startClockUpdater() {
        clockUpdateJob?.cancel()
        clockUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val cal = Calendar.getInstance()
                _uiState.update { it.copy(clock = Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) }
                delay(PlayerConstants.CLOCK_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * 选择官方 CDN URL。
     *
     * 过滤掉 mcdn/szbdyd/IP 地址的 URL，优先使用官方 CDN。
     */
    private fun selectOfficialCdnUrl(urls: List<String>): String {
        val filtered = urls
            .filter { !it.contains(".mcdn.bilivideo.") }
            .filter { !it.contains(".szbdyd.com") }
            .filter { !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/.*)?(\\?.*)?$").matches(it) }
        return filtered.firstOrNull() ?: urls.first()
    }

    private fun findNextPlayTarget(): NextPlayTarget? {
        val state = _uiState.value
        val videoList = videoInfoRepository.videoList.value
        val index = videoList.indexOfFirst { it.aid == state.aid }
        if (index == -1) return null

        val current = videoList.getOrNull(index)
        if (current?.ugcPages?.isNotEmpty() == true) {
            val innerIndex = current.ugcPages.indexOfFirst { it.cid == state.cid }
            if (innerIndex != -1 && innerIndex + 1 < current.ugcPages.size) {
                return NextPlayTarget.UgcPage(current, current.ugcPages[innerIndex + 1])
            }
        }
        if (index + 1 < videoList.size) return NextPlayTarget.VideoItem(videoList[index + 1])
        return null
    }

    private fun findPreviousPlayTarget(): NextPlayTarget? {
        val state = _uiState.value
        val videoList = videoInfoRepository.videoList.value
        val index = videoList.indexOfFirst { it.aid == state.aid }
        if (index == -1) return null

        val current = videoList.getOrNull(index)
        if (current?.ugcPages?.isNotEmpty() == true) {
            val innerIndex = current.ugcPages.indexOfFirst { it.cid == state.cid }
            if (innerIndex > 0) return NextPlayTarget.UgcPage(current, current.ugcPages[innerIndex - 1])
        }
        if (index > 0) {
            val prev = videoList[index - 1]
            val prevLastPage = prev.ugcPages?.lastOrNull()
            return if (prevLastPage != null) NextPlayTarget.UgcPage(prev, prevLastPage)
            else NextPlayTarget.VideoItem(prev)
        }
        return null
    }

    private fun startNextEpisodeCountdown(target: NextPlayTarget) {
        playNextCountdownJob?.cancel()
        playNextCountdownJob = viewModelScope.launch {
            _uiState.update { it.copy(showSkipToNextEp = true) }
            delay(PlayerConstants.COUNTDOWN_DURATION_MS)
            playNextTarget(target)
            _uiState.update { it.copy(showSkipToNextEp = false) }
        }
    }

    private fun startShowPreviewTipCountdown() {
        previewTipCountdownJob?.cancel()
        previewTipCountdownJob = viewModelScope.launch {
            _uiState.update { it.copy(showPreviewTip = true) }
            delay(PlayerConstants.COUNTDOWN_DURATION_MS)
            _uiState.update { it.copy(showPreviewTip = false) }
        }
    }

    private fun playNextTarget(target: NextPlayTarget) {
        when (target) {
            is NextPlayTarget.UgcPage -> playNewVideo(
                VideoListItem(aid = target.parentVideo.aid, cid = target.page.cid, title = target.title),
            )
            is NextPlayTarget.VideoItem -> playNewVideo(
                VideoListItem(
                    aid = target.video.aid,
                    cid = target.video.cid,
                    title = target.title,
                    epid = target.video.epid,
                    seasonId = target.video.seasonId,
                ),
            )
        }
    }

    /** 将 DataApiType 映射为 bili-api 的 ApiType。 */
    private fun getApiType(): ApiType =
        if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web

    private sealed interface NextPlayTarget {
        val title: String
        data class UgcPage(val parentVideo: VideoListItem, val page: VideoPage) : NextPlayTarget {
            override val title: String = page.title
        }
        data class VideoItem(val video: VideoListItem) : NextPlayTarget {
            override val title: String = video.title
        }
    }

    private data class PlaybackConfig(val qn: Int, val codec: VideoCodec?, val audio: Audio)
    private data class MediaUrls(val videoUrl: String, val audioUrl: String?)

    override fun onCleared() {
        super.onCleared()
        detachedWorkScope.cancel()
    }
}
