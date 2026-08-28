package dev.frost819.newbv.app.ui.screen.live

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.component.rememberScreenFocusSaver
import dev.frost819.newbv.app.ui.navigation.LiveAreaRoute
import dev.frost819.newbv.app.ui.navigation.LiveFollowRoute
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.viewmodel.live.LiveHomeViewModel
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.core.focus.touchClickable
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 直播浏览页（上→下：我的关注 → 推荐分区 → 推荐信息流）。
 *
 * 整体为单 [TvLazyVerticalGrid]（4 列），关注和分区为全宽 item（内含横向 [LazyRow]），
 * 推荐信息流为常规网格 item，支持无限滚动。
 *
 * @param navFocusRequester 内容区入口焦点请求器（由 MainScreen 传入）。
 * @param navController 导航控制器。
 */
@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    viewModel: LiveHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberScreenFocusSaver()

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .collect { index ->
                if (index != null &&
                    index >= state.recommendItems.size + 2 - 4 &&
                    state.recommendHasMore &&
                    !state.recommendLoading
                ) {
                    viewModel.loadMoreRecommend()
                }
            }
    }

    TvLazyVerticalGrid(
        modifier =
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                        viewModel.loadFollowLive()
                        viewModel.loadAreaList()
                        viewModel.loadRecommend()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Section 1: 我的关注（标题行） ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            FollowHeader(
                focusRequester = navFocusRequester,
                focusSaver = focusSaver,
                onMoreClick = { navController.navigate(LiveFollowRoute) },
            )
        }

        // ── Section 1: 我的关注（内容） ──
        if (state.followLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "加载中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (state.followError) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "加载失败",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else if (state.followItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "暂无关注的直播",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(
                items = state.followItems.take(4),
                key = { _, item -> "follow_${item.roomId}" },
            ) { _, item ->
                LiveRoomCard(
                    modifier = Modifier.focusSaverItem(focusSaver, "follow_${item.roomId}"),
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
        }

        // ── Section 2: 推荐分区 ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            AreaSection(
                areas = state.areaList,
                focusSaver = focusSaver,
                onAreaClick = { area ->
                    navController.navigate(
                        LiveAreaRoute(
                            parentAreaId = area.id,
                            areaId = 0,
                            title = area.name,
                        ),
                    )
                },
            )
        }

        // ── Section 3: 推荐直播标题 ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "推荐直播",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // ── Section 3: 推荐信息流 ──
        itemsIndexed(
            items = state.recommendItems,
            key = { _, item -> "rec_${item.roomId}" },
        ) { _, item ->
            LiveRoomCard(
                modifier = Modifier.focusSaverItem(focusSaver, "rec_${item.roomId}"),
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

        // ── Footer ──
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

// ── 我的关注 Section ──────────────────────────────────────────────────

/**
 * "我的关注"标题行：标题 + "更多>"按钮。
 * 卡片列表直接放在外层 4 列网格中，不在此组件内。
 */
@Composable
private fun FollowHeader(
    focusRequester: FocusRequester,
    focusSaver: dev.frost819.newbv.app.ui.component.ScreenFocusSaver,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "我的关注",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .focusSaverItem(focusSaver, "follow_more"),
            onClick = onMoreClick,
            shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
            colors =
                ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "更多",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.height(12.dp),
                )
            }
        }
    }
}

// ── 推荐分区 Section ──────────────────────────────────────────────────

/**
 * "推荐分区"区域：标题 + 横向分区卡片列表（小卡片）。
 */
@Composable
private fun AreaSection(
    areas: List<LiveAreaParent>,
    focusSaver: dev.frost819.newbv.app.ui.component.ScreenFocusSaver,
    onAreaClick: (LiveAreaParent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "推荐分区",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp), // 标题稍微往右缩进对齐
        )
        Spacer(Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            // contentPadding 是关键：它在滚动区域内部添加 padding，
            // 既能给首尾卡片的放大留出空间，又不会在滚动时遮挡内容
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(areas) { area ->
                AreaMiniCard(
                    modifier = Modifier.focusSaverItem(focusSaver, "area_${area.id}"),
                    area = area,
                    onClick = { onAreaClick(area) },
                )
            }
        }
    }
}

/**
 * 分区迷你卡片（横向滚动用）。
 *
 * 固定宽度 100dp，高度 100dp，上方居中显示分区图标，下方显示名称。
 */
@Composable
private fun AreaMiniCard(
    modifier: Modifier = Modifier,
    area: LiveAreaParent,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier =
            modifier
                .size(100.dp)
                .touchClickable(onClick = onClick),
        shape = CardDefaults.shape(MaterialTheme.shapes.medium),
        border =
            CardDefaults.border(
                focusedBorder =
                    Border(
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.border),
                        shape = MaterialTheme.shapes.medium,
                    ),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (area.list.isNotEmpty()) {
                AsyncImage(
                    modifier = Modifier.size(48.dp),
                    model = area.list.first().pic,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.shapes.small,
                            ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = area.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
