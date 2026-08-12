package dev.frost819.newbv.app.ui.screen.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.viewmodel.live.LiveHomeViewModel

/**
 * 直播推荐列表（4 列网格）。
 */
@Composable
fun LiveRecommendContent(
    modifier: Modifier = Modifier,
    viewModel: LiveHomeViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = dev.frost819.newbv.app.ui.component.rememberFocusSaver()

    focusSaver.RestoreFocus()

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
            key = { _, item -> item.roomId },
        ) { index, item ->
            LiveRoomCard(
                modifier = Modifier.focusSaverItem(focusSaver, index),
                data = item,
                onClick = {
                    navController.navigate(
                        LivePlayerRoute(
                            roomId = item.roomId,
                            title = item.title,
                            cover = item.cover,
                        ),
                    )
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.recommendLoading,
                isError = state.recommendError,
                hasMore = false,
                itemsIsEmpty = state.recommendItems.isEmpty(),
            )
        }
    }
}
