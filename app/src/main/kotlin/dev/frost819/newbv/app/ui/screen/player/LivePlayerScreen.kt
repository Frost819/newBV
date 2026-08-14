package dev.frost819.newbv.app.ui.screen.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.player.LivePlayerController
import dev.frost819.newbv.app.viewmodel.live.LivePlayerState
import dev.frost819.newbv.app.viewmodel.live.LivePlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.danmaku.component.DanmakuPlayerCompose
import dev.frost819.newbv.player.BvVideoPlayer

/**
 * 直播播放器页面。
 *
 * 使用 [LivePlayerController] 作为根控制器，整合 [LivePlayerViewModel]（播放控制）
 * 和 [DanmakuViewModel]（弹幕配置）。
 *
 * UI 层负责协调两者：
 * - 播放/暂停 → 同步弹幕播放/暂停
 * - 弹幕开关 → 调用 DanmakuViewModel.toggleDanmaku()
 * - 弹幕设置 → 调用 DanmakuViewModel.updateDanmakuState()
 */
@Composable
fun LivePlayerScreen(
    navController: NavController,
    viewModel: LivePlayerViewModel,
    danmakuViewModel: DanmakuViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val danmakuState by danmakuViewModel.danmakuState.collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()
    val videoPlayer = viewModel.videoPlayer
    val danmakuPlayer = danmakuViewModel.danmakuPlayer

    val danmakuEnabled = danmakuState.enabledTypes.isNotEmpty()

    LaunchedEffect(uiState.playerState) {
        when (uiState.playerState) {
            LivePlayerState.Playing -> danmakuViewModel.play()
            LivePlayerState.Paused, LivePlayerState.Error, LivePlayerState.Ended -> danmakuViewModel.pause()
            else -> {}
        }
    }

    LaunchedEffect(uiState.isBuffering) {
        if (uiState.isBuffering) {
            danmakuViewModel.pause()
        } else if (uiState.playerState == LivePlayerState.Playing) {
            danmakuViewModel.play()
        }
    }

    LivePlayerController(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        isPlaying = uiState.playerState == LivePlayerState.Playing,
        isBuffering = uiState.isBuffering,
        isError = uiState.playerState == LivePlayerState.Error,
        errorMessage = uiState.errorMessage,
        title = uiState.title,
        uname = uiState.uname,
        areaName = uiState.areaName,
        onlineCount = uiState.onlineCount,
        clock = getClock(),
        danmakuEnabled = danmakuEnabled,
        danmakuState = danmakuState,
        availableQualities = uiState.availableQualities,
        currentQuality = uiState.currentQuality,
        onBack = { navController.popBackStack() },
        onPlayPause = { viewModel.togglePlayPause() },
        onRefresh = { viewModel.refresh() },
        onToggleDanmaku = { danmakuViewModel.toggleDanmaku() },
        onQualityChange = { viewModel.changeQuality(it) },
        onDanmakuSettingChange = { danmakuViewModel.updateDanmakuState(it) },
        debugInfo = debugInfo,
    ) {
        if (videoPlayer != null) {
            BvVideoPlayer(
                modifier = Modifier.fillMaxSize(),
                videoPlayer = videoPlayer,
            )
        }

        if (danmakuPlayer != null && danmakuEnabled) {
            DanmakuPlayerCompose(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(danmakuState.alpha),
                danmakuPlayer = danmakuPlayer,
            )
        }
    }
}

/**
 * 获取当前时钟（小时，分钟）。
 */
private fun getClock(): Pair<Int, Int> {
    val calendar = java.util.Calendar.getInstance()
    return calendar.get(java.util.Calendar.HOUR_OF_DAY) to calendar.get(java.util.Calendar.MINUTE)
}
