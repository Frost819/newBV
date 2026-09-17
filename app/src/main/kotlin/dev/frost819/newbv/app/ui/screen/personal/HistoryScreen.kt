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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
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
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
    focusSaver: FocusSaver,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

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
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.historyItems.size - 20
            }.collect {
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
            val cardData =
                remember(item) {
                    val durationMs = item.duration * 1000L
                    val progressRatio =
                        if (item.progress == -1) {
                            1f
                        } else if (item.duration > 0) {
                            item.progress.toFloat() / item.duration.toFloat()
                        } else {
                            null
                        }
                    val timeString =
                        if (item.progress == -1) {
                            "已看完 / ${durationMs.formatHourMinSec()}"
                        } else if (item.progress > 0 && item.duration > 0) {
                            "${(item.progress * 1000L).formatHourMinSec()} / ${durationMs.formatHourMinSec()}"
                        } else {
                            durationMs.formatHourMinSec()
                        }
                    VideoCardData(
                        avid = item.oid,
                        bvid = item.bvid,
                        cid = item.cid,
                        epid = item.epid,
                        title = item.title,
                        cover = item.cover,
                        playString = "",
                        danmakuString = "",
                        timeString = timeString,
                        upName = item.author,
                        upMid = item.mid,
                        progress = progressRatio,
                    )
                }
            SmallVideoCard(
                modifier = Modifier.focusSaverItem(focusSaver, "history_$index"),
                data = cardData,
                onClick = {
                    navController.navigateFromVideoCard(cardData)
                },
                onGoToDetailPage = {
                    navController.navigateFromVideoCard(cardData, forceDetail = true)
                },
                onGoToUpPage =
                    item.mid?.let { mid ->
                        { navController.navigate(UserSpaceRoute(mid = mid, name = item.author)) }
                    },
                onAddWatchLater = { watchLaterViewModel.addToView(aid = item.oid) },
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
