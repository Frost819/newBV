package dev.frost819.newbv.app.ui.screen.pgc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Scaffold as Material3Scaffold
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.PgcCarousel
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.component.videocard.SeasonCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCardData
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.viewmodel.pgc.PgcViewModel
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * PGC 影视顶部导航项。
 *
 * 对应 6 个 PGC 分区。
 *
 * @property pgcType 对应的 PGC 分区类型。
 */
enum class PgcTabItem(val pgcType: PgcType, override val displayName: String) : TopNavItem {
    Anime(PgcType.Anime, "番剧"),
    GuoChuang(PgcType.GuoChuang, "国创"),
    Movie(PgcType.Movie, "电影"),
    Documentary(PgcType.Documentary, "纪录片"),
    Tv(PgcType.Tv, "电视剧"),
    Variety(PgcType.Variety, "综艺"),
    ;
}

/**
 * PGC 影视内容（TopNav + 轮播图 + 4 列网格）。
 *
 * 顶部 Tab 切换分区，内容区上方为轮播图，下方为番剧/影视卡片网格 + 无限滚动。
 * 菜单键刷新当前分区数据。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器。
 * @param navController 导航控制器。
 * @param viewModel PGC ViewModel。
 */
@Composable
fun PgcContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    viewModel: PgcViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(PgcTabItem.Anime) }
    var focusOnContent by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Material3Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = PgcTabItem.entries.toList(),
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    val tab = nav as PgcTabItem
                    if (tab != selectedTab) {
                        selectedTab = tab
                        viewModel.switchType(tab.pgcType)
                    }
                },
                onClick = { nav ->
                    val tab = nav as PgcTabItem
                    viewModel.refresh()
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                        viewModel.refresh()
                        navFocusRequester.requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
        ) {
            PgcGrid(
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}

/**
 * PGC 番剧网格（含轮播图）。
 *
 * 第一行为全宽轮播图，后续为 4 列番剧卡片网格 + 无限滚动。
 * 距离底部 10 条时触发加载更多。
 */
@Composable
private fun PgcGrid(
    viewModel: PgcViewModel,
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
                index != null && index >= state.items.size - 10
            }
            .collect {
                viewModel.loadMore()
            }
    }

    TvLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 轮播图：全宽，始终占位避免异步加载后内容下移
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (state.carouselItems.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PgcCarousel(
                        data = state.carouselItems,
                        onClick = { item ->
                            val seasonId = item.seasonId?.toLong()
                            if (seasonId != null) {
                                navController.navigate(PgcFeatureRoute(seasonId = seasonId))
                            }
                        },
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
            }
        }

        itemsIndexed(
            items = state.items,
            key = { index, _ -> index },
        ) { index, item ->
            val cardData = remember(item) {
                SeasonCardData(
                    seasonId = item.seasonId,
                    title = item.title,
                    subTitle = item.subTitle,
                    cover = item.cover,
                    rating = item.rating,
                )
            }
            SeasonCard(
                data = cardData,
                onClick = {
                    navController.navigate(PgcFeatureRoute(seasonId = item.seasonId.toLong()))
                },
                onGoToDetailPage = {
                    navController.navigate(PgcFeatureRoute(seasonId = item.seasonId.toLong()))
                },
                modifier = Modifier.focusSaverItem(focusSaver, index),
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.loading,
                isError = state.error,
                hasMore = state.hasMore,
                itemsIsEmpty = state.items.isEmpty(),
            )
        }
    }
}
