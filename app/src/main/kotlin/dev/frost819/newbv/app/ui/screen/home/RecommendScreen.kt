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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
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
    focusSaver: FocusSaver,
) {
    val state by viewModel.uiState.collectAsState()
    // 推荐接口偶尔重复下发同一个 aid。列表要用 aid 作稳定 key，就必须先去重，
    // 否则 LazyGrid 会直接抛重复 key 异常。放在 UI 层，分页/刷新的语义不动。
    val displayItems =
        remember(state.recommendItems) { state.recommendItems.distinctBy { it.aid } }
    val itemCount by rememberUpdatedState(displayItems.size)
    val gridState = rememberLazyGridState()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                // 用 rememberUpdatedState 读最新条数，避免 LaunchedEffect 只捕获
                // 首次组合时的旧值，导致分页后阈值不更新、反复触发加载。
                index != null && index >= itemCount - 20
            }.collect {
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
            items = displayItems,
            // 用业务 id 而非下标：分页追加 / 下拉刷新时已有卡片能保持身份，
            // 不会整屏重新组合、也不会让焦点和图片请求全部重来。
            key = { _, item -> item.aid },
        ) { index, item ->
            val cardData =
                remember(item) {
                    VideoCardData(
                        avid = item.aid,
                        bvid = item.bvid,
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
                modifier = Modifier.focusSaverItem(focusSaver, "rcmd_$index"),
                data = cardData,
                onClick = {
                    navController.navigateFromVideoCard(cardData)
                },
                onGoToDetailPage = {
                    navController.navigateFromVideoCard(cardData, forceDetail = true)
                },
                onGoToUpPage =
                    item.authorMid?.let { mid ->
                        { navController.navigate(UserSpaceRoute(mid = mid, name = item.author)) }
                    },
                onAddWatchLater = { watchLaterViewModel.addToView(aid = item.aid) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.recommendLoading,
                isError = state.recommendError,
                hasMore = state.recommendHasMore,
                itemsIsEmpty = displayItems.isEmpty(),
            )
        }
    }
}
