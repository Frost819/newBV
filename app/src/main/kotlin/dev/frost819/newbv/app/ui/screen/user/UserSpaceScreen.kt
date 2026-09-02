package dev.frost819.newbv.app.ui.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.user.UserSpaceViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 用户空间页。
 *
 * 展示用户信息（头像、昵称）和投稿视频网格。
 * 用户名和头像由路由参数传入，不单独调用 API 获取（与原版 BV 行为一致）。
 */
fun NavGraphBuilder.userSpaceScreen(navController: NavController) {
    composable<UserSpaceRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<UserSpaceRoute>()
        val viewModel: UserSpaceViewModel = hiltViewModel()
        UserSpaceScreen(
            mid = route.mid,
            name = route.name ?: "",
            face = route.face,
            viewModel = viewModel,
            navController = navController,
        )
    }
}

@Composable
private fun UserSpaceScreen(
    mid: Long,
    name: String,
    face: String?,
    viewModel: UserSpaceViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    LaunchedEffect(mid) {
        viewModel.init(mid, name, face)
    }

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState, state.videos.size) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.videos.size - 20
            }.collect {
                viewModel.loadVideos(mid)
            }
    }

    TvLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            UserSpaceHeader(state = state)
        }

        itemsIndexed(
            items = state.videos,
            key = { _, item -> item.aid },
        ) { index, video ->
            val cardData =
                remember(video) {
                    val durationMs = video.duration * 1000L
                    val progressRatio =
                        if (video.playbackPosition > 0) {
                            video.playbackPosition / 100f
                        } else {
                            null
                        }
                    VideoCardData(
                        avid = video.aid,
                        bvid = video.bvid,
                        title = video.title,
                        cover = video.cover,
                        playString = video.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = video.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = durationMs.formatHourMinSec(),
                        upName = video.author,
                        upMid = mid,
                        pubTime = video.pubTime,
                        progress = progressRatio,
                    )
                }
            SmallVideoCard(
                modifier = Modifier.focusSaverItem(focusSaver, "user_space_$index"),
                data = cardData,
                onClick = { navController.navigateFromVideoCard(cardData) },
                onGoToDetailPage = {
                    navController.navigate(VideoDetailRoute(aid = video.aid, bvid = video.bvid))
                },
                onGoToUpPage = {},
                onAddWatchLater = { watchLaterViewModel.addToView(aid = video.aid) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.loading,
                isError = state.error,
                hasMore = state.hasMore,
                itemsIsEmpty = state.videos.isEmpty(),
            )
        }
    }
}

@Composable
private fun UserSpaceHeader(state: dev.frost819.newbv.app.viewmodel.user.UserSpaceUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.face.isNotEmpty()) {
            AsyncImage(
                model = state.face,
                contentDescription = state.name,
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
