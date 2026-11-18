package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.player.menu.component.MenuListItem
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.data.datastore.Resolution
import kotlinx.coroutines.delay

/**
 * 播放器清晰度快切弹窗。
 *
 * 从播放器右下角弹出，列出服务端返回的全部可用画质档位（按 qn 降序），
 * 当前播放档位高亮选中并自动聚焦，D-pad 上下选择、确认切换、Back 关闭。
 *
 * 使用 [Dialog] 独立窗口实现（AGENTS §11.2.1）：焦点被限制在弹窗列表内，
 * Back 键由 [onDismiss] 自动处理，无需播放器控制器额外按键路由；
 * 弹窗打开期间底层控制器的自动隐藏计时不影响本窗口。
 *
 * @param modifier 修饰符
 * @param availableQualityIds 可用画质 ID 列表（B 站 qn 参数，顺序内部按降序重排）
 * @param currentQualityId 当前播放画质 ID（在可用列表中时高亮并聚焦到该项）
 * @param onSelect 选择画质回调，参数为选中的 qn
 * @param onDismiss 关闭回调（Back 键 / 点击遮罩）
 */
@Composable
fun QualitySelectDialog(
    modifier: Modifier = Modifier,
    availableQualityIds: List<Int>,
    currentQualityId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val qualityIds = remember(availableQualityIds) { availableQualityIds.sortedByDescending { it } }
    // 当前档位不在可用列表时（如解码回退瞬间）回退聚焦到第一项
    val focusIndex = qualityIds.indexOf(currentQualityId).coerceAtLeast(0)
    val selectedFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // 等待列表项完成组合后再请求焦点，否则 FocusRequester 未绑定节点会静默失败
        delay(50)
        runCatching { selectedFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Surface(
                modifier = Modifier.padding(end = 48.dp, bottom = 80.dp),
                colors =
                    SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "清晰度",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    qualityIds.forEachIndexed { index, qn ->
                        MenuListItem(
                            modifier =
                                Modifier.then(
                                    if (index == focusIndex) {
                                        Modifier.focusRequester(selectedFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                ),
                            text = Resolution.fromCode(qn).displayName,
                            selected = qn == currentQualityId,
                            onClick = { onSelect(qn) },
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun QualitySelectDialogPreview() {
    BVTheme {
        QualitySelectDialog(
            availableQualityIds = listOf(16, 32, 64, 80, 116),
            currentQualityId = 80,
            onSelect = {},
            onDismiss = {},
        )
    }
}

// endregion
