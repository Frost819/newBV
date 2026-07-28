package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.LoadingTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * 推荐视频列表页。
 *
 * 4 列网格 + 无限滚动，距离底部 20 条时触发加载更多。
 */
@Composable
fun RecommendScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

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
        ) { _, item ->
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
            if (state.recommendLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingTip()
                }
            } else if (!state.recommendHasMore && state.recommendItems.isNotEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "没有更多了捏",
                    color = Color.White,
                )
            }
        }
    }
}
