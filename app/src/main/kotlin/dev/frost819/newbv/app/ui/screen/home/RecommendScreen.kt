package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 推荐视频列表页。
 *
 * 4 列网格 + 无限滚动，距离底部 20 条时触发加载更多。
 * 支持从详情页返回后恢复焦点到之前点击的卡片。
 */
@Composable
fun RecommendScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.recommendItems.size - 20
            }
            .collect {
                viewModel.loadRecommend()
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
            items = state.recommendItems,
            key = { index, _ -> index },
        ) { index, item ->
            val cardData = remember(item) {
                VideoCardData(
                    avid = item.aid,
                    title = item.title,
                    cover = item.cover,
                    playString = item.play.takeIf { it != -1 }.toWanString(),
                    danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                    timeString = (item.duration * 1000L).formatHourMinSec(),
                    upName = item.author,
                    upMid = item.authorMid,
                    pubTime = item.pubTime,
                )
            }
            SmallVideoCard(
                modifier = Modifier
                    .focusRequester(focusSaver.focusRequesterFor(index))
                    .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedIndex(index) },
                data = cardData,
                onClick = {
                    navController.navigate(VideoDetailRoute(aid = item.aid))
                },
                onGoToDetailPage = {
                    navController.navigate(VideoDetailRoute(aid = item.aid))
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.recommendLoading,
                isError = state.recommendError,
                hasMore = state.recommendHasMore,
                itemsIsEmpty = state.recommendItems.isEmpty(),
            )
        }
    }
}
