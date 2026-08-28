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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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
import dev.frost819.newbv.app.ui.navigation.LiveFollowRoute
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveRoom
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 关注直播列表页 ViewModel。
 *
 * 加载用户关注的主播中正在直播的全部房间列表。
 */
@HiltViewModel
class LiveFollowViewModel
    @Inject
    constructor(
        private val liveRepository: LiveRepository,
    ) : ViewModel() {
        companion object {
            private const val LOAD_TIMEOUT_MS = 10_000L
        }

        private val logger = Loggers.get("LiveFollowScreen")

        private val _uiState = MutableStateFlow(LiveFollowUiState())
        val uiState: StateFlow<LiveFollowUiState> = _uiState.asStateFlow()

        init {
            loadFollowLive()
        }

        /**
         * 加载关注主播正在直播的房间列表。
         */
        fun loadFollowLive() {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    isLoading = true,
                    isError = false,
                )
            }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val response = liveRepository.getFollowLive()
                        response.rooms.filter { it.liveStatus == 1 }.map { it.toCardData() }
                    }
                }.onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            items = items,
                            isLoading = false,
                            isError = false,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.warn(error) { "Failed to load follow live" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                        )
                    }
                }
            }
        }
    }

private fun FollowLiveRoom.toCardData(): LiveRoomCardData {
    val coverUrl = coverFromUser.ifBlank { keyframe }
    return LiveRoomCardData(
        roomId = roomId,
        title = title,
        uname = uname.ifBlank { nickname },
        uid = uid,
        cover = coverUrl,
        face = face,
        areaV2Name = areaV2Name,
        areaV2ParentName = areaV2ParentName,
        onlineString = formatOnlineCount(online),
        watchedString = "",
    )
}

data class LiveFollowUiState(
    val items: List<LiveRoomCardData> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

/**
 * 注册关注直播列表页到 NavGraph。
 */
fun NavGraphBuilder.liveFollowScreen(navController: NavController) {
    composable<LiveFollowRoute> {
        val viewModel: LiveFollowViewModel = hiltViewModel()
        LiveFollowScreen(
            viewModel = viewModel,
            navController = navController,
        )
    }
}

/**
 * 关注直播列表页。
 *
 * 全屏 4 列网格展示正在直播的关注房间。
 */
@Composable
private fun LiveFollowScreen(
    viewModel: LiveFollowViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()

    focusSaver.RestoreFocus()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.padding(24.dp, 16.dp),
            text = "我的关注",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        TvLazyVerticalGrid(
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
                    hasMore = false,
                    itemsIsEmpty = state.items.isEmpty(),
                )
            }
        }
    }
}
