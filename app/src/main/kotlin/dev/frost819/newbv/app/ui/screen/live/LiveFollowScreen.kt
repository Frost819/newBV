package dev.frost819.newbv.app.ui.screen.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 关注直播列表页 ViewModel。
 *
 * 分页加载关注主播中正在直播的房间：首次进入拉第 1 页，滚动接近底部时追加下一页。
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

        /** 已加载到的页码，0 表示尚未加载。 */
        private var page = 0

        init {
            loadFollowLive()
        }

        /**
         * 首次加载或重新加载关注直播列表（重置分页）。
         */
        fun loadFollowLive() {
            if (_uiState.value.isLoading) return
            page = 0
            _uiState.update {
                it.copy(items = emptyList(), isLoading = true, isError = false, hasMore = true, total = 0)
            }
            loadPage()
        }

        /**
         * 加载下一页，滚动接近底部时调用。
         *
         * 与其它信息流一致：错误态不阻止翻页，用户继续滚动时重试同一页
         * （[loadPage] 开头会清除错误态）。`isLoading` 与 `hasMore` 由状态保证不重复请求。
         */
        fun loadMore() {
            val state = _uiState.value
            if (state.isLoading || !state.hasMore) return
            loadPage()
        }

        /** 拉取下一页并追加到列表；失败时保留已加载内容并置错误态。 */
        private fun loadPage() {
            val nextPage = page + 1
            _uiState.update { it.copy(isLoading = true, isError = false) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        liveRepository.getFollowLive(page = nextPage)
                    }
                }.onSuccess { result ->
                    page = nextPage
                    _uiState.update { state ->
                        val existingRoomIds = state.items.mapTo(mutableSetOf()) { it.roomId }
                        val newItems =
                            result.rooms
                                .filter { it.liveStatus == 1 && existingRoomIds.add(it.roomId) }
                                .map { it.toCardData() }
                        state.copy(
                            items = state.items + newItems,
                            isLoading = false,
                            isError = false,
                            // 服务端重复下发同一页时不会新增内容，必须停止继续翻页
                            hasMore = result.hasMore && newItems.isNotEmpty(),
                            total = if (result.total > 0) result.total else state.total,
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
    val hasMore: Boolean = true,
    /** 正在直播的关注人数（服务端返回，0 表示未知）。 */
    val total: Int = 0,
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

    // 滚动到接近底部时加载下一页（列表不足一屏时也会立即补齐）。
    // 阈值按每页 10 条设置；其它信息流页更大（20~30 条）所以用 20。
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.items.size - 6
            }.collect {
                viewModel.loadMore()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(24.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "我的关注",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.total > 0) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${state.total} 人正在直播",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                items = state.items,
                key = { _, item -> item.roomId },
            ) { _, item ->
                LiveRoomCard(
                    modifier = Modifier.focusSaverItem(focusSaver, "live_follow_${item.roomId}"),
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

            if (!state.isLoading && !state.isError && state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无正在直播的关注",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
