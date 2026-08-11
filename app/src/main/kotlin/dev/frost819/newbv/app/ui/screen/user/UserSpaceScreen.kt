package dev.frost819.newbv.app.ui.screen.user

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
import dev.frost819.newbv.app.viewmodel.user.UserSpaceUiEffect
import dev.frost819.newbv.app.viewmodel.user.UserSpaceViewModel
import dev.frost819.newbv.core.focus.touchClickable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 用户空间页。
 *
 * 展示用户信息（头像、昵称、签名、关注按钮）和投稿视频网格。
 */
fun NavGraphBuilder.userSpaceScreen(navController: NavController) {
    composable<UserSpaceRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<UserSpaceRoute>()
        val viewModel: UserSpaceViewModel = hiltViewModel()
        UserSpaceScreen(
            mid = route.mid,
            viewModel = viewModel,
            navController = navController,
        )
    }
}

@Composable
private fun UserSpaceScreen(
    mid: Long,
    viewModel: UserSpaceViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    LaunchedEffect(mid) {
        viewModel.init(mid)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserSpaceUiEffect.ShowToast -> {
                    // Toast handled by UI
                }
            }
        }
    }

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState, state.videos.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.videos.size - 20
            }
            .collect {
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
            UserSpaceHeader(
                state = state,
                onFollowClick = { viewModel.toggleFollow() },
            )
        }

        itemsIndexed(
            items = state.videos,
            key = { _, item -> item.aid },
        ) { index, video ->
            val cardData = remember(video) {
                VideoCardData(
                    avid = video.aid,
                    bvid = video.bvid,
                    title = video.title,
                    cover = video.cover,
                    playString = video.play.takeIf { it != -1 }.toWanString(),
                    danmakuString = video.danmaku.takeIf { it != -1 }.toWanString(),
                    timeString = (video.duration * 1000L).formatHourMinSec(),
                    upName = video.author,
                    upMid = mid,
                    pubTime = video.pubTime,
                )
            }
            SmallVideoCard(
                modifier = Modifier.focusSaverItem(focusSaver, index),
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
private fun UserSpaceHeader(
    state: dev.frost819.newbv.app.viewmodel.user.UserSpaceUiState,
    onFollowClick: () -> Unit,
) {
    val info = state.userInfo

    if (state.userInfoLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.userInfoError || info == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "加载用户信息失败",
                color = Color.Gray,
                fontSize = 14.sp,
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = info.face,
            contentDescription = info.name,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LV${info.level}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.border,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = info.sign.ifEmpty { "这个人很神秘" },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        FollowButton(
            isFollowing = state.isFollowing,
            isLoading = state.followLoading,
            onClick = onFollowClick,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val text = when {
        isLoading -> "处理中..."
        isFollowing -> "已关注"
        else -> "关注"
    }
    val color = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.border

    androidx.tv.material3.Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.touchClickable(onClick = onClick),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
