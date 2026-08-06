package dev.frost819.newbv.app.ui.screen.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 收藏页面。
 *
 * 顶部收藏夹 Tab 栏 + 下方 4 列视频网格。
 * 切换收藏夹时自动加载该夹视频列表，无限滚动加载更多。
 *
 * @param viewModel 个人页 ViewModel。
 * @param navController 导航控制器。
 */
@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    if (state.favoriteFolders.isEmpty() && !state.favoriteLoading && !state.favoriteError) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Text(
                text = "没有收藏夹",
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.favoriteItems.size - 20
            }
            .collect {
                if (state.currentFolderId != -1L) {
                    viewModel.loadFavoriteItems(state.currentFolderId)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.favoriteFolders.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.favoriteFolders.size) { index ->
                    val folder = state.favoriteFolders[index]
                    androidx.tv.material3.Surface(
                        onClick = {
                            viewModel.loadFavoriteItems(folder.id, forceRefresh = true)
                        },
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        ),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = if (state.currentFolderId == folder.id) {
                                androidx.tv.material3.MaterialTheme.colorScheme.primary
                            } else {
                                androidx.tv.material3.MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        androidx.tv.material3.Text(
                            text = "${folder.title} (${folder.mediaCount})",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = androidx.tv.material3.MaterialTheme.typography.labelLarge,
                            color = if (state.currentFolderId == folder.id) {
                                androidx.tv.material3.MaterialTheme.colorScheme.onPrimary
                            } else {
                                androidx.tv.material3.MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }

        TvLazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = state.favoriteItems,
                key = { _, item -> item.id },
            ) { _, item ->
                val cardData = remember(item) {
                    VideoCardData(
                        avid = item.id,
                        title = item.title,
                        cover = item.cover,
                        playString = "",
                        danmakuString = "",
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        upName = item.upper.name,
                        upMid = item.upper.mid,
                    )
                }
                SmallVideoCard(
                    data = cardData,
                    onClick = {
                        navController.navigate(VideoDetailRoute(aid = item.id))
                    },
                    onGoToDetailPage = {
                        navController.navigate(VideoDetailRoute(aid = item.id))
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ListFooterTip(
                    isLoading = state.favoriteLoading,
                    isError = state.favoriteError,
                    hasMore = state.favoriteHasMore,
                    itemsIsEmpty = state.favoriteItems.isEmpty(),
                )
            }
        }
    }
}
