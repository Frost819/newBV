package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * 手势提示覆盖层。
 *
 * 在手势执行期间显示反馈（亮度/音量百分比、倍速值、seek 状态）。
 * 与 [PlayStateTips]/[SkipTips] 平级，放在 [VideoPlayerController] 覆盖层中。
 *
 * @param state 手势提示状态。
 */
@Composable
fun GestureTip(
    state: GestureTipState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.isActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val iconVector =
                    when (state.type) {
                        GestureTipType.Brightness -> Icons.Rounded.BrightnessHigh
                        GestureTipType.Volume -> Icons.AutoMirrored.Rounded.VolumeUp
                        GestureTipType.Speed -> Icons.Rounded.Speed
                        GestureTipType.Seek -> Icons.Rounded.FastForward
                        GestureTipType.None -> null
                    }
                val displayText =
                    when (state.type) {
                        GestureTipType.Brightness -> "${(state.value * 100).toInt()}%"
                        GestureTipType.Volume -> "${state.value.toInt()}%"
                        GestureTipType.Speed -> "${state.value}x"
                        GestureTipType.Seek -> "快进/快退"
                        GestureTipType.None -> ""
                    }
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                if (displayText.isNotEmpty()) {
                    Text(
                        text = displayText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
