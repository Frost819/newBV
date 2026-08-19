package dev.frost819.newbv.app.ui.screen.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.livecard.formatOnlineCount
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.navigation.LiveAreaRoute
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class LiveAreaListViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val LOAD_TIMEOUT_MS = 10_000L
    }

    private val route = savedStateHandle.toRoute<LiveAreaRoute>()

    private val logger = Loggers.get("LiveAreaListScreen")

    private val _uiState = MutableStateFlow(LiveAreaListUiState())
    val uiState: StateFlow<LiveAreaListUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        currentPage = 1
        _uiState.update {
            it.copy(items = emptyList(), hasMore = true, isLoading = false, isError = false)
        }
        loadMore()
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return

        _uiState.update { it.copy(isLoading = true, isError = false) }

        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    val result = liveRepository.getAreaLiveList(
                        parentAreaId = route.parentAreaId,
                        areaId = route.areaId,
                        page = currentPage,
                    )
                    val newItems = result.list.map { it.toCardData() }
                    currentPage to (newItems to result.hasMore)
                }
            }.onSuccess { (page, pair) ->
                val (newItems, hasMore) = pair
                _uiState.update {
                    it.copy(
                        items = it.items + newItems,
                        hasMore = hasMore,
                        isLoading = false,
                        isError = false,
                    )
                }
                currentPage = page + 1
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.warn(error) { "Failed to load area live list" }
                _uiState.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }
}

private fun LiveRoomItem.toCardData(): LiveRoomCardData {
    val coverUrl = cover.ifBlank { keyframe.ifBlank { userCover } }
    return LiveRoomCardData(
        roomId = roomId.toLong(),
        title = title,
        uname = uname,
        uid = uid,
        cover = coverUrl,
        face = face,
        areaV2Name = areaV2Name,
        areaV2ParentName = areaV2ParentName,
        onlineString = formatOnlineCount(online),
        watchedString = watchedShow?.textSmall ?: "",
    )
}

data class LiveAreaListUiState(
    val items: List<LiveRoomCardData> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val hasMore: Boolean = true,
)

fun NavGraphBuilder.liveAreaScreen(navController: NavController) {
    composable<LiveAreaRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LiveAreaRoute>()
        val viewModel: LiveAreaListViewModel = hiltViewModel()
        LiveAreaListScreen(
            title = route.title,
            viewModel = viewModel,
            navController = navController,
        )
    }
}

@Composable
private fun LiveAreaListScreen(
    title: String,
    viewModel: LiveAreaListViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null && index >= state.items.size - 5 && state.hasMore && !state.isLoading) {
                    viewModel.loadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.padding(24.dp, 16.dp),
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        TvLazyVerticalGrid(
            modifier = Modifier.weight(1f),
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = state.items,
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
                    isLoading = state.isLoading,
                    isError = state.isError,
                    hasMore = state.hasMore,
                    itemsIsEmpty = state.items.isEmpty(),
                )
            }
        }
    }
}
