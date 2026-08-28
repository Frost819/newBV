package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.core.focus.touchClickable
import kotlinx.coroutines.delay

/**
 * 直播播放器控制器信息层。
 *
 * 包含顶部标题+人气 meta 行+时钟和底部操作按钮两部分。
 * 通过 [AnimatedVisibility] 控制进出场动画。
 *
 * @param show 是否显示
 * @param title 直播间标题
 * @param areaName 分区名
 * @param onlineCount 人气值
 * @param clock 时钟（hour, minute）
 * @param isPlaying 是否正在播放
 * @param danmakuEnabled 弹幕是否开启
 * @param onPlayPause 播放/暂停回调
 * @param onRefresh 刷新回调
 * @param onDanmakuSwitchChange 弹幕开关回调
 * @param onShowSettings 打开设置回调
 */
@Composable
fun LiveControllerInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    areaName: String,
    onlineCount: String,
    clock: Pair<Int, Int>,
    isPlaying: Boolean,
    danmakuEnabled: Boolean,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "LiveControllerTop",
        ) {
            LiveControllerInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                areaName = areaName,
                onlineCount = onlineCount,
                clock = clock,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "LiveControllerBottom",
        ) {
            LiveControllerInfoBottom(
                modifier = Modifier.align(Alignment.BottomCenter),
                isPlaying = isPlaying,
                danmakuEnabled = danmakuEnabled,
                onPlayPause = onPlayPause,
                onRefresh = onRefresh,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onShowSettings = onShowSettings,
            )
        }
    }
}

/**
 * 直播控制器顶部信息（标题 + 人气 meta 行 + 时钟）。
 *
 * 人气显示与点播 [ControllerVideoInfoTop] 的同时观看人数保持一致：
 * 标题正下方 meta 行，双人形图标 + 小字文案。
 */
@Composable
private fun LiveControllerInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    areaName: String,
    onlineCount: String,
    clock: Pair<Int, Int>,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp),
                    ),
                ).background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0f),
                                ),
                        ),
                ).padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                text = title,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(color = Color.Black, blurRadius = 1f),
                    ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (areaName.isNotBlank()) {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
            Clock(hour = clock.first, minute = clock.second)
        }
        // 人气 meta 行：与点播的同时观看人数样式一致（标题下方小字）
        if (onlineCount.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_player_watching),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "人气 $onlineCount",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            shadow = Shadow(color = Color.Black, blurRadius = 1f),
                        ),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * 直播控制器底部信息（操作按钮行）。
 */
@Composable
private fun LiveControllerInfoBottom(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    danmakuEnabled: Boolean,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val buttonsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(50)
        runCatching { buttonsFocusRequester.requestFocus() }
    }

    Column(
        modifier =
            modifier.clip(
                MaterialTheme.shapes.large.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                ),
            ),
        verticalArrangement = Arrangement.Bottom,
    ) {
        val icons =
            buildList {
                add(LiveControllerIcon(R.drawable.play_pause_24px, "播放/暂停", onPlayPause))
                add(LiveControllerIcon(R.drawable.baseline_refresh_24, "刷新", onRefresh))
                add(
                    LiveControllerIcon(
                        if (danmakuEnabled) R.drawable.danmaku_on_24px else R.drawable.danmaku_off_24px,
                        "弹幕开关",
                        onDanmakuSwitchChange,
                    ),
                )
                add(LiveControllerIcon(R.drawable.settings_24px, "打开设置", onShowSettings))
            }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(buttonsFocusRequester)
                    .onKeyEvent {
                        if (it.key == Key.DirectionUp) {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            return@onKeyEvent false
                        }
                        false
                    }.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            icons.forEachIndexed { index, (icon, desc, action) ->
                key(index) {
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
private data class LiveControllerIcon(
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
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 32.sp)) {
                    append("$hour".padStart(2, '0'))
                    append(":")
                    append("$minute".padStart(2, '0'))
                }
            },
    )
}
