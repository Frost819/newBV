package dev.frost819.newbv.app.ui.screen.player

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.app.ui.action.player.MediaProfileSettingAction
import dev.frost819.newbv.app.ui.action.player.SubtitleSettingAction
import dev.frost819.newbv.app.ui.component.player.VideoPlayerController
import dev.frost819.newbv.app.ui.component.player.VideoProgressSeek
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.app.viewmodel.player.PlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.SubtitleViewModel
import dev.frost819.newbv.app.viewmodel.player.VideoListViewModel
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.frost819.newbv.danmaku.component.DanmakuPlayerCompose
import dev.frost819.newbv.danmaku.util.DanmakuMaskFinder
import dev.frost819.newbv.danmaku.util.calculateMaskDelay
import dev.frost819.newbv.danmaku.util.danmakuMask
import dev.frost819.newbv.player.BvVideoPlayer
import kotlinx.coroutines.delay
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val videoPlayer = playerViewModel.videoPlayer
    val danmakuPlayer = danmakuViewModel.danmakuPlayer

    val uiState by playerViewModel.uiState.collectAsState()
    val seekerState = playerViewModel.seekerState.collectAsState()
    val danmakuState by danmakuViewModel.danmakuState.collectAsState()
    val videoListState by videoListViewModel.videoListState.collectAsState()
    val subtitleState by subtitleViewModel.subtitleState.collectAsState()
    val subtitleId by subtitleViewModel.subtitleId.collectAsState()
    val subtitleData by subtitleViewModel.subtitleData.collectAsState()
    val subtitleList by subtitleViewModel.subtitleList.collectAsState()

    val maskFinder = remember { DanmakuMaskFinder() }
    var currentDanmakuMaskFrame by remember { mutableStateOf<DanmakuMaskFrame?>(null) }
    var isLooping by remember { mutableStateOf(false) }

    val videoShotCache by remember(uiState.videoShot) { mutableStateOf(VideoShotImageCache()) }

    // 合并 UI 状态（包含 videoList 和 relatedVideos）
    val mergedUiState = remember(
        uiState, danmakuState, subtitleState, subtitleId, subtitleData, subtitleList,
        videoListState.videoList, videoListState.relatedVideos,
    ) {
        uiState.copy(
            danmakuState = danmakuState,
            subtitleState = subtitleState,
            subtitleId = subtitleId,
            subtitleData = subtitleData,
            subtitleList = subtitleList,
            videoList = videoListState.videoList,
            relatedVideos = videoListState.relatedVideos.map { related ->
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
                    (context as? Activity)?.finish()
                }
                dev.frost819.newbv.app.ui.state.player.PlayerUiEffect.PlayEnded -> {
                    if (isLooping) {
                        playerViewModel.backToStart()
                    } else {
                        playerViewModel.checkAndPlayNext()
                    }
                }
            }
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
    LaunchedEffect(danmakuState.maskEnabled, uiState.danmakuMask) {
        if (!danmakuState.maskEnabled || uiState.danmakuMask == null) {
            currentDanmakuMaskFrame = null
            return@LaunchedEffect
        }
        maskFinder.reset()
        val mask = uiState.danmakuMask ?: return@LaunchedEffect
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
        val observer = LifecycleEventObserver { _, event ->
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

    // 返回键由 VideoPlayerController 的 onPreviewKeyEvent 处理（TV 遥控器）
    // BackHandler 仅作为非 TV 设备的系统返回兜底
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (System.currentTimeMillis() - lastBackPressTime < 3000) {
            navController.popBackStack()
        } else {
            lastBackPressTime = System.currentTimeMillis()
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    VideoPlayerController(
        modifier = Modifier.fillMaxSize(),
        fromSeason = uiState.fromSeason,
        isLooping = isLooping,
        videoShotCache = videoShotCache,
        uiState = mergedUiState,
        seekerState = seekerState,
        onPlay = { playerViewModel.togglePlayPause() },
        onPause = { playerViewModel.togglePlayPause() },
        onExit = {
            navController.popBackStack()
        },
        onGoTime = { time -> playerViewModel.seekToTime(time) },
        onBackToStart = { playerViewModel.backToStart() },
        onCancelSkipToNextEp = { playerViewModel.cancelPlayNext() },
        onPlayNewVideo = { item: VideoListItem ->
            playerViewModel.playNewVideo(item)
        },
        onPlayPrevious = { playerViewModel.playPreviousNow() },
        onPlayNext = { playerViewModel.playNextNow() },
        onToggleLoop = {
            isLooping = !isLooping
            playerViewModel.toggleLoop()
        },
        onToggleSubtitle = { subtitleViewModel.toggleSubtitle() },
        onGoToUpPage = {
            navController.navigate(UserSpaceRoute(mid = uiState.authorMid))
        },
        onGoToVideoDetail = {
            navController.navigate(VideoDetailRoute(aid = uiState.aid))
        },
        onMediaProfileSettingChange = { action -> playerViewModel.updateMediaProfile(action) },
        onAspectRatioChange = { ratio -> playerViewModel.updateVideoAspectRatio(ratio) },
        onPlaySpeedChange = { speed -> playerViewModel.updatePlaySpeed(speed) },
        onDanmakuSettingChange = { action -> danmakuViewModel.updateDanmakuState(action) },
        onSubtitleChange = { subtitle -> subtitleViewModel.selectSubtitle(subtitle.id) },
        onSubtitleSettingChange = { action -> subtitleViewModel.updateSubtitleState(action) },
        onRelatedVideoClicked = { video: VideoCardData ->
            playerViewModel.playNewVideo(
                VideoListItem(
                    aid = video.avid,
                    cid = video.cid ?: 0,
                    title = video.title,
                ),
            )
        },
        onToggleDanmaku = { danmakuViewModel.toggleDanmaku() },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 视频画面
            val aspectRatio = uiState.aspectRatio.ratio ?: run {
                if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                    uiState.videoWidth.toFloat() / uiState.videoHeight.toFloat()
                } else {
                    16f / 9f
                }
            }

            if (videoPlayer != null) {
                BvVideoPlayer(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(aspectRatio),
                    videoPlayer = videoPlayer,
                )
            }

            // 弹幕层
            if (danmakuPlayer != null) {
                DanmakuPlayerCompose(
                    modifier = Modifier
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
}
