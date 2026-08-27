package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 跳转提示覆盖层。
 *
 * 在播放器左下角显示三种提示（可同时显示多个）：
 * - **试看提示**：视频需付费，当前为试看片段
 * - **跳下集提示**：播放结束，即将播放下一集
 * - **回到开头提示**：从上次播放位置继续，按确认键从头播放
 * - **快捷键提示**：显示最近一次触发的快捷键或分集边界提示
 *
 * 每个提示通过 [PlayerTip] 独立做进出场动画，Column 自动堆叠。
 *
 * @param modifier 修饰符
 * @param showBackToStart 是否显示"回到开头"提示
 * @param showSkipToNextEp 是否显示"跳下集"提示
 * @param showPreviewTip 是否显示"试看"提示
 * @param shortcutTipText 最近一次快捷键提示文本，为 null 时不显示
 */
@Composable
fun SkipTips(
    modifier: Modifier = Modifier,
    showBackToStart: Boolean,
    showSkipToNextEp: Boolean,
    showPreviewTip: Boolean,
    shortcutTipText: String? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerTip(
                show = showPreviewTip,
                text = "视频需付费，当前为试看片段",
                icon = Icons.Outlined.Info,
            )
            PlayerTip(
                show = showSkipToNextEp,
                text = "播放结束，即将播放下一集",
                icon = Icons.Outlined.SkipNext,
            )
            PlayerTip(
                show = showBackToStart,
                text = "从上次播放位置继续，按确认键从头播放",
                icon = Icons.Outlined.Replay,
            )
            PlayerTip(
                show = shortcutTipText != null,
                text = shortcutTipText.orEmpty(),
                icon = Icons.Outlined.SettingsRemote,
            )
        }
    }
}

/**
 * 单个提示条。
 *
 * 使用 [AnimatedVisibility] 做进出场动画：
 * - 进场：从底部展开 + 淡入（弹性弹簧 + 400ms 淡入）
 * - 退场：向底部收缩 + 淡出（350ms 收缩 + 280ms 淡出）
 *
 * 视觉为左下角圆角矩形（右上右下圆角），60% 黑色半透明背景，
 * 内含图标 + 文本。
 *
 * @param show 是否可见
 * @param text 提示文本
 * @param icon 提示图标
 */
@Composable
fun PlayerTip(
    show: Boolean,
    text: String,
    icon: ImageVector,
) {
    AnimatedVisibility(
        visible = show,
        enter = expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ) + fadeIn(tween(400)),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Bottom,
            animationSpec = tween(350),
        ) + fadeOut(tween(280)),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                .background(Color.Black.copy(alpha = 0.6f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SkipTipsAllVisiblePreview() {
    BVTheme {
        SkipTips(
            showBackToStart = true,
            showSkipToNextEp = true,
            showPreviewTip = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SkipTipsBackToStartPreview() {
    BVTheme {
        SkipTips(
            showBackToStart = true,
            showSkipToNextEp = false,
            showPreviewTip = false,
        )
    }
}

// endregion
