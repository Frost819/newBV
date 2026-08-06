package dev.frost819.newbv.app.ui.screen.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 历史记录页面。
 *
 * 4 列网格 + 无限滚动，距离底部 20 条时触发加载更多。
 * 卡片底部显示播放进度条（progress/duration）。
 *
 * @param viewModel 个人页 ViewModel。
 * @param navController 导航控制器。
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()

    focusSaver.RestoreFocus()

    if (state.historyItems.isEmpty() && !state.historyLoading && !state.historyError) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Text(
                text = "没有观看记录",
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.historyItems.size - 20
            }
            .collect {
                viewModel.loadHistory()
            }
    }

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = state.historyItems,
            key = { index, _ -> index },
        ) { index, item ->
            val cardData = remember(item) {
                VideoCardData(
                    avid = item.oid,
                    cid = item.cid,
                    title = item.title,
                    cover = item.cover,
                    playString = "",
                    danmakuString = "",
                    timeString = (item.duration * 1000L).formatHourMinSec(),
                    upName = item.author,
                    upMid = item.mid,
                    progress = if (item.progress == -1) {
                        1f
                    } else if (item.duration > 0) {
                        item.progress.toFloat() / item.duration.toFloat()
                    } else {
                        null
                    },
                )
            }
            SmallVideoCard(
                modifier = Modifier.focusSaverItem(focusSaver, index),
                data = cardData,
                onClick = {
                    navController.navigate(VideoDetailRoute(aid = item.oid))
                },
                onGoToDetailPage = {
                    navController.navigate(VideoDetailRoute(aid = item.oid))
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.historyLoading,
                isError = state.historyError,
                hasMore = state.historyHasMore,
                itemsIsEmpty = state.historyItems.isEmpty(),
            )
        }
    }
}
