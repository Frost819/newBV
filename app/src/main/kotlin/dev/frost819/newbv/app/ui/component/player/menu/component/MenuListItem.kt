package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 菜单列表项。
 *
 * 可展开/折叠的菜单项，展开时显示文本（200dp 宽），折叠时仅显示图标（66dp 宽）。
 * 支持选中状态、焦点变化回调和点击事件。
 *
 * @param modifier 修饰符
 * @param text 文本内容
 * @param icon 图标（为 null 时折叠状态显示空白占位）
 * @param expanded 是否展开
 * @param selected 是否选中
 * @param textAlign 文本对齐方式
 * @param onFocus 获得焦点回调
 * @param onClick 点击回调
 */
@Composable
fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    expanded: Boolean = true,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val itemWidth by animateDpAsState(
        targetValue = if (expanded) 200.dp else 66.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "MenuListItem width [$text]",
    )

    Surface(
        modifier = modifier
            .width(itemWidth)
            .onFocusChanged { if (it.hasFocus) onFocus() }
            .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.4f)
            } else {
                Color.Transparent
            },
        ),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(vertical = 0.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    label = "MenuListItem text [$text]",
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        text = text,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    label = "MenuListItem icon [$text]",
                ) {
                    if (icon == null) {
                        Box(modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = icon,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
