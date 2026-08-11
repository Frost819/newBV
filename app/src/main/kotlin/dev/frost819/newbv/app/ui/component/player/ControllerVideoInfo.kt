package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVTheme
import kotlinx.coroutines.delay

/**
 * 播放器控制器信息层。
 *
 * 包含顶部标题+时钟和底部进度条+操作按钮两部分。
 * 通过 [AnimatedVisibility] 控制进出场动画。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param isSeeking 是否正在 seek
 * @param goTime seek 预览位置（毫秒）
 * @param seekerState 进度条状态
 * @param title 视频标题
 * @param clock 时钟（hour, minute）
 * @param videoShot 缩略图数据（为 null 时不显示预览）
 * @param videoShotCache 缩略图缓存
 * @param fromSeason 是否来自番剧（为 true 时隐藏详情/UP/相关视频按钮）
 * @param danmakuEnabled 弹幕是否开启
 * @param isLooping 是否循环播放
 * @param onDirectionLeft seek 左移回调
 * @param onDirectionRight seek 右移回调
 * @param onSeekGoTime 确认 seek 回调
 * @param onSeekToPosition 触屏拖拽/点击进度条时 seek 到指定位置（毫秒）
 * @param onPlayPause 播放/暂停回调
 * @param onDanmakuSwitchChange 弹幕开关回调
 * @param onShowSettings 打开设置回调
 * @param onShowRelatedVideos 打开相关视频回调
 * @param onGoToVideoInfo 跳转视频详情回调
 * @param onToggleLoop 切换循环回调
 * @param onGoToUpPage 跳转 UP 主页面回调
 */
@Composable
fun ControllerVideoInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    title: String,
    clock: Pair<Int, Int>,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerTopVideoInfo",
        ) {
            ControllerVideoInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                clock = clock,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerBottomVideoInfo",
        ) {
            ControllerVideoInfoBottom(
                modifier = Modifier.align(Alignment.BottomCenter),
                isSeeking = isSeeking,
                goTime = goTime,
                seekerState = seekerState,
                videoShot = videoShot,
                videoShotCache = videoShotCache,
                fromSeason = fromSeason,
                danmakuEnabled = danmakuEnabled,
                isLooping = isLooping,
                onDirectionLeft = onDirectionLeft,
                onDirectionRight = onDirectionRight,
                onSeekGoTime = onSeekGoTime,
                onSeekToPosition = onSeekToPosition,
                onPlayPause = onPlayPause,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onShowSettings = onShowSettings,
                onShowRelatedVideos = onShowRelatedVideos,
                onGoToVideoInfo = onGoToVideoInfo,
                onToggleLoop = onToggleLoop,
                onGoToUpPage = onGoToUpPage,
            )
        }
    }
}

/**
 * 控制器顶部信息（标题 + 时钟）。
 */
@Composable
fun ControllerVideoInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    clock: Pair<Int, Int>,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                MaterialTheme.shapes.large.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp),
                ),
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0f),
                    ),
                ),
            )
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    shadow = Shadow(color = Color.Black, blurRadius = 1f),
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Clock(hour = clock.first, minute = clock.second)
        }
    }
}

/**
 * 控制器底部信息（缩略图预览 + 时间 + 进度条 + 操作按钮）。
 */
@Composable
fun ControllerVideoInfoBottom(
    modifier: Modifier = Modifier,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,
) {
    val seekFocusRequester = remember { FocusRequester() }
    val buttonsFocusRequester = remember { FocusRequester() }

    var isSeekFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        runCatching { seekFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier.clip(
            MaterialTheme.shapes.large.copy(
                bottomStart = CornerSize(0.dp),
                bottomEnd = CornerSize(0.dp),
            ),
        ),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Seek 缩略图预览
        if (isSeeking && videoShot != null) {
            VideoShot(
                modifier = Modifier.padding(horizontal = 48.dp),
                videoShot = videoShot,
                imageCache = videoShotCache,
                position = goTime,
                duration = seekerState.totalDuration,
                coercedOffset = (-24).dp,
            )
        }

        // 时间显示
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(bottom = 2.dp, start = 24.dp),
                text = "${if (isSeeking) goTime.formatHourMinSec() else seekerState.currentTime.formatHourMinSec()} / ${seekerState.totalDuration.formatHourMinSec()}",
                color = Color.White,
                style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 1f)),
            )
        }

        // Seek bar（可聚焦，处理方向键 + 触屏拖拽）
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isSeekFocused) 1f else 0f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                )
                .focusable()
                .focusRequester(seekFocusRequester)
                .pointerInput(seekerState.totalDuration) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        firstDown.consume()

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            val w = this.size.width.toFloat()

                            if (!change.pressed) {
                                // 手指抬起：seek 到最终位置
                                if (w > 0 && seekerState.totalDuration > 0) {
                                    val ratio = (change.position.x / w).coerceIn(0f, 1f)
                                    val targetTime = (ratio * seekerState.totalDuration).toLong()
                                    onSeekToPosition(targetTime)
                                }
                                change.consume()
                                break
                            }

                            if (change.positionChanged()) {
                                // 拖拽中：持续 seek 到触摸位置
                                if (w > 0 && seekerState.totalDuration > 0) {
                                    val ratio = (change.position.x / w).coerceIn(0f, 1f)
                                    val targetTime = (ratio * seekerState.totalDuration).toLong()
                                    onSeekToPosition(targetTime)
                                }
                                change.consume()
                            }
                        }
                    }
                }
                .onKeyEvent {
                    when (it.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            if (isSeeking) onSeekGoTime() else onPlayPause()
                            true
                        }

                        Key.DirectionLeft, Key.MediaRewind -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionLeft()
                            true
                        }

                        Key.DirectionRight, Key.MediaFastForward -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionRight()
                            true
                        }

                        Key.DirectionDown -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            buttonsFocusRequester.requestFocus()
                            true
                        }

                        else -> false
                    }
                }
                .onFocusChanged { isSeekFocused = it.isFocused },
        ) {
            VideoProgressSeek(
                modifier = Modifier.focusable().fillMaxWidth(),
                duration = seekerState.totalDuration,
                position = if (isSeeking) goTime else seekerState.currentTime,
                bufferedPercentage = seekerState.bufferedPercentage,
                isPersistentSeek = false,
            )
        }

        // 操作按钮行
        val icons = buildList {
            add(ControllerIcon(R.drawable.play_pause_24px, "播放/暂停", onPlayPause))
            add(
                ControllerIcon(
                    if (danmakuEnabled) R.drawable.danmaku_on_24px else R.drawable.danmaku_off_24px,
                    "弹幕开关",
                    onDanmakuSwitchChange,
                ),
            )
            add(ControllerIcon(R.drawable.settings_24px, "打开设置", onShowSettings))
            if (!fromSeason) {
                add(ControllerIcon(R.drawable.info_24px, "视频信息", onGoToVideoInfo))
                add(ControllerIcon(R.drawable.contact_page_24px, "up主页", onGoToUpPage))
                add(ControllerIcon(R.drawable.related_videos_24px, "相关视频", onShowRelatedVideos))
            }
            add(
                ControllerIcon(
                    if (isLooping) R.drawable.repeat_one_on_24px else R.drawable.repeat_one_24px,
                    "循环播放",
                    onToggleLoop,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(buttonsFocusRequester)
                .onKeyEvent {
                    if (it.key == Key.DirectionUp) {
                        if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                        seekFocusRequester.requestFocus()
                        return@onKeyEvent true
                    }
                    false
                }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            icons.forEach { (icon, desc, action) ->
                key(icon) {
                    Surface(
                        modifier = Modifier.touchClickable(onClick = action),
                        onClick = action,
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                    ) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = desc,
                            modifier = Modifier.padding(5.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 操作按钮数据三元组。
 */
private data class ControllerIcon(
    val icon: Int,
    val description: String,
    val action: () -> Unit,
)

/**
 * 时钟组件。
 */
@Composable
private fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
) {
    Text(
        modifier = modifier,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 1f)),
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 32.sp)) {
                append("$hour".padStart(2, '0'))
                append(":")
                append("$minute".padStart(2, '0'))
            }
        },
    )
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ControllerVideoInfoPreview() {
    BVTheme {
        ControllerVideoInfo(
            show = true,
            isSeeking = false,
            goTime = 0L,
            seekerState = SeekerState(
                totalDuration = 600_000L,
                currentTime = 120_000L,
                bufferedPercentage = 50,
            ),
            title = "示例视频标题",
            clock = Pair(14, 30),
            videoShot = null,
            videoShotCache = VideoShotImageCache(),
            fromSeason = false,
            danmakuEnabled = true,
            isLooping = false,
            onDirectionLeft = {},
            onDirectionRight = {},
            onSeekGoTime = {},
            onSeekToPosition = {},
            onPlayPause = {},
            onDanmakuSwitchChange = {},
            onShowSettings = {},
            onShowRelatedVideos = {},
            onGoToVideoInfo = {},
            onToggleLoop = {},
            onGoToUpPage = {},
        )
    }
}

// endregion
