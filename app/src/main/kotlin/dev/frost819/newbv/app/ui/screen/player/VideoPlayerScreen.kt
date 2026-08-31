package dev.frost819.newbv.app.ui.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.component.comment.CommentDialogMode
import dev.frost819.newbv.app.ui.component.comment.CommentsDialog
import dev.frost819.newbv.app.ui.component.player.VideoInteractionDialog
import dev.frost819.newbv.app.ui.component.player.VideoPlayerController
import dev.frost819.newbv.app.ui.component.rememberDoublePressExit
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.util.ToastUtils
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.comment.CommentViewModel
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.app.viewmodel.player.PlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.SubtitleViewModel
import dev.frost819.newbv.app.viewmodel.player.VideoListViewModel
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.danmaku.component.DanmakuPlayerCompose
import dev.frost819.newbv.danmaku.util.DanmakuMaskFinder
import dev.frost819.newbv.danmaku.util.calculateMaskDelay
import dev.frost819.newbv.danmaku.util.danmakuMask
import dev.frost819.newbv.player.BvVideoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.math.absoluteValue

/**
 * 视频播放器页面。
 *
 * 收集 5 个 ViewModel 的状态，组装 UI 层并传递给 [VideoPlayerController]。
 * 管理播放器生命周期、心跳、弹幕蒙版更新循环。
 */
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    danmakuViewModel: DanmakuViewModel = hiltViewModel(),
    subtitleViewModel: SubtitleViewModel = hiltViewModel(),
    videoListViewModel: VideoListViewModel = hiltViewModel(),
    commentViewModel: CommentViewModel = hiltViewModel(),
) {
    val logger = Loggers.get("VideoPlayerScreen")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val videoPlayer = playerViewModel.videoPlayer
    val danmakuPlayer = danmakuViewModel.danmakuPlayer

    val uiState by playerViewModel.uiState.collectAsState()
    val seekerState = playerViewModel.seekerState.collectAsState()
    val danmakuState by danmakuViewModel.danmakuState.collectAsState()
    val danmakuMask by danmakuViewModel.danmakuMask.collectAsState()
    val videoListState by videoListViewModel.videoListState.collectAsState()
    val subtitleState by subtitleViewModel.subtitleState.collectAsState()
    val subtitleId by subtitleViewModel.subtitleId.collectAsState()
    val subtitleData by subtitleViewModel.subtitleData.collectAsState()
    val subtitleList by subtitleViewModel.subtitleList.collectAsState()
    val sharedState by playerViewModel.videoSharedState.collectAsState()

    val maskFinder = remember { DanmakuMaskFinder() }
    var currentDanmakuMaskFrame by remember { mutableStateOf<DanmakuMaskFrame?>(null) }
    var showInteractionDialog by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var resumeAfterComments by remember { mutableStateOf(false) }
    var resumeAfterInteraction by remember { mutableStateOf(false) }

    val videoShotCache by remember(uiState.videoShot) { mutableStateOf(VideoShotImageCache()) }

    // 合并 UI 状态（包含 videoList 和 relatedVideos）
    val mergedUiState =
        remember(
            uiState,
            danmakuState,
            danmakuMask,
            subtitleState,
            subtitleId,
            subtitleData,
            subtitleList,
            videoListState.videoList,
            videoListState.relatedVideos,
        ) {
            uiState.copy(
                danmakuState = danmakuState,
                danmakuMask = danmakuMask,
                subtitleState = subtitleState,
                subtitleId = subtitleId,
                subtitleData = subtitleData,
                subtitleList = subtitleList,
                videoList = videoListState.videoList,
                relatedVideos =
                    videoListState.relatedVideos.map { related ->
                        VideoCardData(
                            avid = related.aid,
                            cid = related.cid,
                            title = related.title,
                            cover = related.cover,
                            upName = related.author?.name ?: "",
                            upMid = related.author?.mid,
                            playString = related.view.toWanString(),
                            danmakuString = related.danmaku.toWanString(),
                            timeString = (related.duration * 1000L).formatHourMinSec(),
                        )
                    },
            )
        }

    // UI Effect 收集
    LaunchedEffect(Unit) {
        playerViewModel.uiEffect.collect { effect ->
            when (effect) {
                dev.frost819.newbv.app.ui.state.player.PlayerUiEffect.FinishActivity -> {
                    navController.popBackStack()
                }
                is dev.frost819.newbv.app.ui.state.player.PlayerUiEffect.ShowToast -> {
                    ToastUtils.show(context, effect.message)
                }
                dev.frost819.newbv.app.ui.state.player.PlayerUiEffect.PlayEnded -> {
                    playerViewModel.onPlaybackEnded()
                }
            }
        }
    }

    // 切换视频事件：协调弹幕/字幕/蒙版重载
    LaunchedEffect(Unit) {
        playerViewModel.videoSwitchEvent.collect { event ->
            danmakuViewModel.clearDanmaku()
            // 切集瞬间历史进度尚未异步加载，通常从第 1 段开始；
            // 断点续播 seek 后由下方进度同步驱动目标段加载
            danmakuViewModel.loadDanmaku(
                aid = event.aid,
                cid = event.cid,
                initialPositionMs = uiState.lastPlayed.toLong() * 1000,
            )
            danmakuViewModel.loadDanmakuMask(event.aid, event.cid)
            subtitleViewModel.clearSubtitle()
            subtitleViewModel.loadSubtitleList(event.aid, event.cid)
        }
    }

    // 弹幕分段加载：进度驱动（高频喂入，VM 内部按段号去重）
    LaunchedEffect(Unit) {
        playerViewModel.seekerState
            .map { it.currentTime }
            .collect { danmakuViewModel.onProgressChanged(it) }
    }

    // 弹幕播放/暂停同步：跟随播放器状态
    LaunchedEffect(uiState.playerState) {
        when (val state = uiState.playerState) {
            PlayerState.Playing -> danmakuViewModel.play()
            PlayerState.Paused, is PlayerState.Error, PlayerState.Ended -> danmakuViewModel.pause()
            else -> {}
        }
    }

    // 弹幕缓冲同步：缓冲时暂停弹幕
    LaunchedEffect(uiState.isBuffering) {
        if (uiState.isBuffering) {
            danmakuViewModel.pause()
        } else if (uiState.playerState == PlayerState.Playing) {
            danmakuViewModel.play()
        }
    }

    // 心跳循环（5s 延迟后，每 15s 发送）
    LaunchedEffect(Unit) {
        delay(5000)
        while (isActive) {
            if (uiState.playerState == PlayerState.Playing) {
                playerViewModel.trySendHeartbeat()
            }
            delay(15000)
        }
    }

    // 弹幕蒙版更新循环
    LaunchedEffect(danmakuState.maskEnabled, danmakuMask) {
        if (!danmakuState.maskEnabled || danmakuMask == null) {
            currentDanmakuMaskFrame = null
            return@LaunchedEffect
        }
        maskFinder.reset()
        val mask = danmakuMask ?: return@LaunchedEffect
        var lastCheckTime = -1L
        while (isActive) {
            val currentTime = seekerState.value.currentTime
            val isPlaying = uiState.playerState == PlayerState.Playing
            val isTimeJumping = (currentTime - lastCheckTime).absoluteValue > 200
            if (isPlaying || isTimeJumping) {
                val foundFrame = maskFinder.findFrame(mask, currentTime)
                if (currentDanmakuMaskFrame != foundFrame) {
                    currentDanmakuMaskFrame = foundFrame
                }
                lastCheckTime = currentTime
                val delayTime = calculateMaskDelay(foundFrame, currentTime, isPlaying)
                delay(delayTime)
            } else {
                delay(500L)
            }
        }
    }

    // 生命周期管理：onResume 恢复播放，onPause 暂停
    LaunchedEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (uiState.playerState == PlayerState.Paused) {
                            playerViewModel.togglePlayPause()
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        if (uiState.playerState == PlayerState.Playing) {
                            playerViewModel.togglePlayPause()
                        }
                    }
                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // 双击退出：TV 遥控器（Controller onExit）和非 TV（BackHandler）共用同一计时器
    val handleBack =
        rememberDoublePressExit(
            onExit = { navController.popBackStack() },
            message = "再按一次退出播放",
        )
    BackHandler { handleBack() }

    VideoPlayerController(
        modifier = Modifier.fillMaxSize(),
        fromSeason = uiState.fromSeason,
        isLooping = uiState.isLooping,
        videoShotCache = videoShotCache,
        uiState = mergedUiState,
        seekerState = seekerState,
        onPlay = {
            logger.info { "[PLAYBACK] play aid=${uiState.aid}, cid=${uiState.cid}" }
            playerViewModel.togglePlayPause()
        },
        onPause = {
            logger.info { "[PLAYBACK] pause aid=${uiState.aid}, cid=${uiState.cid}" }
            playerViewModel.togglePlayPause()
        },
        onExit = {
            logger.info { "[PLAYBACK] exit aid=${uiState.aid}, cid=${uiState.cid}" }
            handleBack()
        },
        onGoTime = { time ->
            logger.info { "[PLAYBACK] seek aid=${uiState.aid}, cid=${uiState.cid}, positionMs=$time" }
            playerViewModel.seekToTime(time)
            danmakuViewModel.seekTo(time)
        },
        onBackToStart = { playerViewModel.backToStart() },
        onCancelSkipToNextEp = { playerViewModel.cancelPlayNext() },
        onPlayNewVideo = { item: VideoListItem ->
            logger.info { "[PLAYBACK] switch aid=${item.aid}, cid=${item.cid}" }
            playerViewModel.playNewVideo(item)
        },
        onPlayPrevious = { playerViewModel.playPreviousNow() },
        onPlayNext = { playerViewModel.playNextNow() },
        onToggleLoop = {
            logger.info { "[PLAYBACK] loopToggle enabled=${!uiState.isLooping}" }
            playerViewModel.toggleLoop()
        },
        onToggleSubtitle = { subtitleViewModel.toggleSubtitle() },
        onGoToUpPage = {
            logger.info { "[NAV] playerToUp mid=${uiState.authorMid}" }
            navController.navigate(UserSpaceRoute(mid = uiState.authorMid, name = uiState.authorName))
        },
        onGoToVideoDetail = {
            navController.popBackStack()
        },
        onShowInteraction = {
            logger.info { "[CARD] openVideoInteraction aid=${uiState.aid}" }
            resumeAfterInteraction = uiState.playerState == PlayerState.Playing
            playerViewModel.pausePlayback()
            danmakuViewModel.pause()
            showInteractionDialog = true
        },
        onShowComments = {
            logger.info { "[CARD] openComments aid=${uiState.aid}" }
            resumeAfterComments = uiState.playerState == PlayerState.Playing
            playerViewModel.pausePlayback()
            danmakuViewModel.pause()
            showCommentsDialog = true
        },
        onMediaProfileSettingChange = { action -> playerViewModel.updateMediaProfile(action) },
        onAspectRatioChange = { ratio -> playerViewModel.updateVideoAspectRatio(ratio) },
        onPlaySpeedChange = { speed ->
            logger.info { "[PLAYBACK] speedChange aid=${uiState.aid}, speed=$speed" }
            playerViewModel.updatePlaySpeed(speed)
            danmakuViewModel.updateSpeed(speed)
        },
        onDanmakuSettingChange = { action -> danmakuViewModel.updateDanmakuState(action) },
        onSubtitleChange = { subtitle -> subtitleViewModel.selectSubtitle(subtitle.id) },
        onSubtitleSettingChange = { action -> subtitleViewModel.updateSubtitleState(action) },
        onRelatedVideoClicked = { video: VideoCardData ->
            logger.info { "[CARD] relatedVideo aid=${video.avid}, cid=${video.cid ?: 0L}" }
            playerViewModel.playNewVideo(
                VideoListItem(
                    aid = video.avid,
                    cid = video.cid ?: 0,
                    title = video.title,
                ),
            )
        },
        onToggleDanmaku = { danmakuViewModel.toggleDanmaku() },
        onShowShortcutTip = { text -> playerViewModel.showShortcutTip(text) },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 视频画面
            val aspectRatio =
                uiState.aspectRatio.ratio ?: run {
                    if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                        uiState.videoWidth.toFloat() / uiState.videoHeight.toFloat()
                    } else {
                        16f / 9f
                    }
                }

            if (videoPlayer != null) {
                BvVideoPlayer(
                    modifier = Modifier.fillMaxSize(),
                    videoPlayer = videoPlayer,
                )
            }

            // 弹幕层
            if (danmakuPlayer != null) {
                DanmakuPlayerCompose(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(if (danmakuState.enabledTypes.isNotEmpty()) 1f else 0f)
                            .danmakuMask(
                                frame = currentDanmakuMaskFrame,
                                aspectRatio = aspectRatio,
                            ),
                    danmakuPlayer = danmakuPlayer,
                )
            }
        }
    }

    if (showInteractionDialog) {
        VideoInteractionDialog(
            actionState = sharedState?.takeIf { it.aid == uiState.aid },
            onLike = { playerViewModel.toggleVideoLike() },
            onCoin = { playerViewModel.sendVideoCoin() },
            onFavorite = { playerViewModel.toggleVideoFavorite() },
            onOneClickTriple = { playerViewModel.oneClickTripleAction() },
            onDismiss = {
                showInteractionDialog = false
                if (resumeAfterInteraction) {
                    playerViewModel.resumePlayback()
                    danmakuViewModel.play()
                }
                resumeAfterInteraction = false
            },
        )
    }

    if (showCommentsDialog) {
        CommentsDialog(
            aid = uiState.aid,
            mode = CommentDialogMode.Player,
            viewModel = commentViewModel,
            onDismiss = {
                showCommentsDialog = false
                if (resumeAfterComments) {
                    playerViewModel.resumePlayback()
                    danmakuViewModel.play()
                }
                resumeAfterComments = false
            },
        )
    }
}
