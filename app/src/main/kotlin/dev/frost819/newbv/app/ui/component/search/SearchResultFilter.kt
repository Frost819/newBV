package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.core.focus.touchClickable

private val orderLabels =
    mapOf(
        SearchFilterOrderType.ComprehensiveSort to "综合排序",
        SearchFilterOrderType.MostClicks to "最多点击",
        SearchFilterOrderType.LatestPublish to "最新发布",
        SearchFilterOrderType.MostDanmaku to "最多弹幕",
        SearchFilterOrderType.MostFavorites to "最多收藏",
    )

private val durationLabels =
    mapOf(
        SearchFilterDuration.All to "全部时长",
        SearchFilterDuration.LessThan10Minutes to "10分钟以下",
        SearchFilterDuration.Between10And30Minutes to "10-30分钟",
        SearchFilterDuration.Between30And60Minutes to "30-60分钟",
        SearchFilterDuration.MoreThan60Minutes to "60分钟以上",
    )

/**
 * 搜索结果筛选弹窗（仅视频 Tab 支持）。
 *
 * 提供排序方式和时长筛选。
 *
 * @param selectedOrder 当前选中的排序方式
 * @param selectedDuration 当前选中的时长筛选
 * @param onConfirm 确认筛选回调
 * @param onDismiss 关闭弹窗回调
 */
@OptIn(ExperimentalLayoutApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun SearchResultFilter(
    selectedOrder: SearchFilterOrderType,
    selectedDuration: SearchFilterDuration,
    onConfirm: (SearchFilterOrderType, SearchFilterDuration) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentOrder by remember { mutableStateOf(selectedOrder) }
    var currentDuration by remember { mutableStateOf(selectedDuration) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleLarge,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "排序方式",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        orderLabels.forEach { (order, label) ->
                            FilterChip(
                                selected = currentOrder == order,
                                onClick = { currentOrder = order },
                                modifier = Modifier.touchClickable(onClick = { currentOrder = order }),
                                colors =
                                    FilterChipDefaults.colors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                            ) {
                                Text(
                                    text = label,
                                    color =
                                        if (currentOrder == order) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "时长",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        durationLabels.forEach { (duration, label) ->
                            FilterChip(
                                selected = currentDuration == duration,
                                onClick = { currentDuration = duration },
                                modifier = Modifier.touchClickable(onClick = { currentDuration = duration }),
                                colors =
                                    FilterChipDefaults.colors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                            ) {
                                Text(
                                    text = label,
                                    color =
                                        if (currentDuration == duration) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.touchClickable(onClick = onDismiss),
                    ) {
                        Text("取消")
                    }
                    Button(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .touchClickable(onClick = { onConfirm(currentOrder, currentDuration) }),
                        onClick = { onConfirm(currentOrder, currentDuration) },
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
