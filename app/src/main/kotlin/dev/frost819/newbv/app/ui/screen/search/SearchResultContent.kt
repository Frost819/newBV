package dev.frost819.newbv.app.ui.screen.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.rememberScreenFocusSaver
import dev.frost819.newbv.app.ui.component.search.SearchResultFilter
import dev.frost819.newbv.app.ui.component.search.UpCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCardData
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.ui.state.search.SearchResultItem
import dev.frost819.newbv.app.ui.state.search.TypedSearchResult
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.removeHtmlTags
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchResultViewModel
import dev.frost819.newbv.biliapi.repositories.SearchType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val searchTypeLabels =
    mapOf(
        SearchType.Video to "视频",
        SearchType.MediaBangumi to "番剧",
        SearchType.MediaFt to "影视",
        SearchType.BiliUser to "用户",
        SearchType.LiveRoom to "直播间",
    )

private val searchTypeColumns =
    mapOf(
        SearchType.Video to 4,
        SearchType.MediaBangumi to 6,
        SearchType.MediaFt to 6,
        SearchType.BiliUser to 5,
        SearchType.LiveRoom to 4,
    )

/**
 * 搜索结果页内容。
 *
 * TopNav 切换 5 类结果，网格无限滚动加载，菜单键打开筛选弹窗。
 * 进入页面时自动根据 keyword 触发搜索。
 */
@Composable
fun SearchResultContent(
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel,
    keyword: String,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    val gridState = rememberLazyGridState()
    val tabRowFocusRequester = remember { FocusRequester() }
    val focusSaver = rememberScreenFocusSaver()
    var focusOnContent by remember { mutableStateOf(false) }

    focusSaver.RestoreFocus()

    val activeResult = uiState.results[uiState.activeType] ?: TypedSearchResult(uiState.activeType)
    val columnCount = searchTypeColumns[uiState.activeType] ?: 4

    val isVideoSearchViaWebApi =
        remember {
            derivedStateOf {
                uiState.activeType == SearchType.Video &&
                    dev.frost819.newbv.data.datastore.Prefs.apiType ==
                    dev.frost819.newbv.data.datastore.ApiType.Web
            }
        }

    BackHandler(focusOnContent) {
        runCatching { tabRowFocusRequester.requestFocus() }
    }

    LaunchedEffect(keyword) {
        if (keyword.isNotBlank()) {
            viewModel.search(keyword)
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastIndex =
                gridState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val current = viewModel.uiState.value
            val count = current.results[current.activeType]?.count ?: 0
            lastIndex to count
        }.distinctUntilChanged()
            .filter { (index, count) -> index >= 0 && index >= count - 20 }
            .collect {
                viewModel.loadMore(viewModel.uiState.value.activeType)
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onKeyEvent {
                    if (it.key == Key.Menu && it.type == KeyEventType.KeyDown) {
                        if (isVideoSearchViaWebApi.value) {
                            viewModel.toggleFilter(true)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 标题栏
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = keyword,
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // 5 类 Tab 导航
            TopNav(
                modifier = Modifier.focusRequester(tabRowFocusRequester),
                items = SearchType.entries.map { SearchTypeNavItem(it) },
                isLargePadding = !focusOnContent,
                accentColor = MaterialTheme.colorScheme.primary,
                onSelectedChanged = { item ->
                    viewModel.switchType((item as SearchTypeNavItem).type)
                },
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 结果网格
            LazyVerticalGrid(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .focusGroup()
                        .onKeyEvent {
                            if (it.key == Key.Menu && it.type == KeyEventType.KeyDown) {
                                if (isVideoSearchViaWebApi.value) {
                                    viewModel.toggleFilter(true)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }.onFocusChanged {
                            focusOnContent = it.hasFocus
                        },
                state = gridState,
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(activeResult.items, key = { item ->
                    when (item) {
                        is SearchResultItem.VideoItem -> "v_${item.video.aid}"
                        is SearchResultItem.PgcItem -> "p_${item.pgc.seasonId}"
                        is SearchResultItem.UserItem -> "u_${item.user.mid}"
                        is SearchResultItem.LiveRoomItem -> "l_${item.room.roomId}"
                    }
                }) { item ->
                    when (item) {
                        is SearchResultItem.VideoItem -> {
                            val v = item.video
                            val focusKey = "video_${v.aid}"
                            SmallVideoCard(
                                modifier = Modifier.focusSaverItem(focusSaver, focusKey),
                                data =
                                    VideoCardData(
                                        avid = v.aid,
                                        title = v.title.removeHtmlTags(),
                                        cover = v.cover,
                                        playString = v.play.toWanString(),
                                        danmakuString = v.danmaku.toWanString(),
                                        timeString = v.duration.formatHourMinSec(),
                                        upName = v.author,
                                        upMid = v.mid,
                                        pubTime = v.pubTime,
                                    ),
                                onClick = {
                                    navController.navigate(VideoDetailRoute(aid = v.aid))
                                },
                                onGoToDetailPage = {
                                    navController.navigate(VideoDetailRoute(aid = v.aid))
                                },
                                onGoToUpPage = {
                                    navController.navigate(UserSpaceRoute(mid = v.mid, name = v.author))
                                },
                                onAddWatchLater = { watchLaterViewModel.addToView(aid = v.aid) },
                            )
                        }
                        is SearchResultItem.PgcItem -> {
                            val p = item.pgc
                            val focusKey = "pgc_${p.seasonId}"
                            SeasonCard(
                                modifier = Modifier.focusSaverItem(focusSaver, focusKey),
                                data =
                                    SeasonCardData(
                                        seasonId = p.seasonId,
                                        title = p.title.removeHtmlTags(),
                                        cover = p.cover,
                                        rating = if (p.star > 0) String.format("%.1f", p.star) else null,
                                    ),
                                onClick = {
                                    navController.navigate(
                                        PgcFeatureRoute(
                                            seasonId = p.seasonId.toLong(),
                                        ),
                                    )
                                },
                            )
                        }
                        is SearchResultItem.UserItem -> {
                            val u = item.user
                            val focusKey = "user_${u.mid}"
                            UpCard(
                                modifier = Modifier.focusSaverItem(focusSaver, focusKey),
                                avatar = u.avatar,
                                username = u.name,
                                sign = u.sign,
                                onClick = {
                                    navController.navigate(UserSpaceRoute(mid = u.mid, name = u.name, face = u.avatar))
                                },
                            )
                        }
                        is SearchResultItem.LiveRoomItem -> {
                            val room = item.room
                            val focusKey = "live_${room.roomId}"
                            LiveRoomCard(
                                modifier = Modifier.focusSaverItem(focusSaver, focusKey),
                                data =
                                    LiveRoomCardData(
                                        roomId = room.roomId,
                                        title = room.title.removeHtmlTags(),
                                        uname = room.uname.removeHtmlTags(),
                                        uid = room.uid,
                                        cover = room.cover,
                                        face = room.face,
                                        areaV2Name = room.areaName.removeHtmlTags(),
                                        areaV2ParentName = "",
                                        onlineString = room.online.toWanString(),
                                        watchedString = "",
                                    ),
                                onClick = {
                                    navController.navigate(LivePlayerRoute(roomId = room.roomId))
                                },
                            )
                        }
                    }
                }

                // 底部加载/错误/没有更多
                item(span = {
                    androidx.compose.foundation.lazy.grid
                        .GridItemSpan(columnCount)
                }) {
                    ListFooterTip(
                        isLoading = activeResult.isLoading,
                        isError = activeResult.error,
                        hasMore = activeResult.hasMore,
                        itemsIsEmpty = activeResult.items.isEmpty(),
                    )
                }
            }
        }

        // 覆盖在标题栏上方，不参与标题栏测量，避免改变结果区域高度。
        if (isVideoSearchViaWebApi.value) {
            IconButton(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 40.dp),
                onClick = { viewModel.toggleFilter(true) },
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "筛选",
                )
            }
        }

        // 筛选弹窗
        if (uiState.showFilter) {
            SearchResultFilter(
                selectedOrder = uiState.selectedOrder,
                selectedDuration = uiState.selectedDuration,
                onConfirm = { order, duration ->
                    viewModel.updateFilter(order, duration)
                },
                onDismiss = { viewModel.toggleFilter(false) },
            )
        }
    }
}

private data class SearchTypeNavItem(
    val type: SearchType,
) : TopNavItem {
    override val displayName: String = searchTypeLabels[type] ?: type.name
}
