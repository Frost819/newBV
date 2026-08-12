package dev.frost819.newbv.app.ui.screen.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.R
import dev.frost819.newbv.app.viewmodel.live.LivePlayerState
import dev.frost819.newbv.app.viewmodel.live.LivePlayerViewModel
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.danmaku.component.DanmakuPlayerCompose
import dev.frost819.newbv.player.BvVideoPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LivePlayerScreen(
    navController: NavController,
    viewModel: LivePlayerViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val videoPlayer = viewModel.videoPlayer
    val danmakuPlayer = viewModel.danmakuPlayer

    var controllerVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(uiState.playerState) {
        if (uiState.playerState == LivePlayerState.Playing) {
            while (isActive) {
                delay(1000)
                if (System.currentTimeMillis() - lastInteractionTime > 5000 && controllerVisible) {
                    controllerVisible = false
                }
            }
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (System.currentTimeMillis() - lastBackPressTime < 3000) {
            navController.popBackStack()
        } else {
            lastBackPressTime = System.currentTimeMillis()
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (videoPlayer != null) {
            BvVideoPlayer(
                modifier = Modifier.fillMaxSize(),
                videoPlayer = videoPlayer,
            )
        }

        if (danmakuPlayer != null && uiState.danmakuEnabled) {
            DanmakuPlayerCompose(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f),
                danmakuPlayer = danmakuPlayer,
            )
        }

        AnimatedVisibility(
            visible = controllerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            LiveControllerOverlay(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onTogglePlayPause = {
                    lastInteractionTime = System.currentTimeMillis()
                    viewModel.togglePlayPause()
                },
                onToggleDanmaku = {
                    lastInteractionTime = System.currentTimeMillis()
                    viewModel.toggleDanmaku()
                },
                onToggleController = {
                    lastInteractionTime = System.currentTimeMillis()
                    controllerVisible = !controllerVisible
                },
            )
        }

        if (uiState.playerState == LivePlayerState.Loading || uiState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White,
                )
            }
        }

        if (uiState.playerState == LivePlayerState.Error) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.errorMessage ?: "播放失败",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun LiveControllerOverlay(
    uiState: dev.frost819.newbv.app.viewmodel.live.LivePlayerUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onToggleController: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControllerButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "人气 ${uiState.onlineCount}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControllerButton(
                icon = if (uiState.playerState == LivePlayerState.Playing) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = "播放/暂停",
                onClick = onTogglePlayPause,
            )
            Spacer(Modifier.width(24.dp))
            ControllerButton(
                iconRes = if (uiState.danmakuEnabled) {
                    R.drawable.danmaku_on_24px
                } else {
                    R.drawable.danmaku_off_24px
                },
                contentDescription = "弹幕开关",
                onClick = onToggleDanmaku,
            )
        }
    }
}

@Composable
private fun ControllerButton(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .touchClickable(onClick = onClick),
        shape = CircleShape,
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.15f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                )
            } else if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = contentDescription,
                    tint = Color.White,
                )
            }
        }
    }
}
