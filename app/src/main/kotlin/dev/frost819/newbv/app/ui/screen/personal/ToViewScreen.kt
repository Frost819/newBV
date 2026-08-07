package dev.frost819.newbv.app.ui.screen.personal

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.viewmodel.personal.PersonalUiEffect
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel

/**
 * 稍后再看页面。
 *
 * 按观看状态分为"未看完"和"已看完"两组，4 列网格展示。
 * 使用 [mutableStateListOf] 原地增删，删除项后 Compose 自动保持焦点到同位置下一项。
 *
 * @param viewModel 个人页 ViewModel。
 * @param navController 导航控制器。
 */
@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    val (unwatched, watched) = remember(viewModel.toViewItems.toList()) {
        viewModel.toViewItems.partition { it.progress != -1 }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PersonalUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (viewModel.toViewItems.isEmpty() && !state.toViewLoading && !state.toViewError) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Text(
                text = "没有稍后再看的视频",
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (unwatched.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "unwatched_header") {
                SectionHeader(title = "未看完 (${unwatched.size})")
            }
            itemsIndexed(items = unwatched) { _, item ->
                val cardData = remember(item) {
                    VideoCardData(
                        avid = item.oid,
                        cid = item.cid,
                        title = item.title,
                        cover = item.cover,
                        playString = "",
                        danmakuString = "",
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        upName = item.author,
                        upMid = item.mid,
                        progress = if (item.duration > 0) {
                            item.progress.toFloat() / item.duration.toFloat()
                        } else null,
                    )
                }
                SmallVideoCard(
                    data = cardData,
                    onClick = {
                        navController.navigateFromVideoCard(cardData)
                    },
                    onGoToDetailPage = {
                        navController.navigate(VideoDetailRoute(aid = item.oid))
                    },
                    onRemoveWatchLater = {
                        viewModel.delToView(aid = item.oid)
                    },
                    onGoToUpPage = item.mid?.let { mid ->
                        { navController.navigate(UserSpaceRoute(mid = mid)) }
                    },
                )
            }
        }

        if (watched.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "watched_header") {
                SectionHeader(title = "已看完 (${watched.size})")
            }
            itemsIndexed(items = watched) { _, item ->
                val cardData = remember(item) {
                    VideoCardData(
                        avid = item.oid,
                        cid = item.cid,
                        title = item.title,
                        cover = item.cover,
                        playString = "",
                        danmakuString = "",
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        upName = item.author,
                        upMid = item.mid,
                        progress = 1f,
                    )
                }
                SmallVideoCard(
                    data = cardData,
                    onClick = {
                        navController.navigateFromVideoCard(cardData)
                    },
                    onGoToDetailPage = {
                        navController.navigate(VideoDetailRoute(aid = item.oid))
                    },
                    onRemoveWatchLater = {
                        viewModel.delToView(aid = item.oid)
                    },
                    onGoToUpPage = item.mid?.let { mid ->
                        { navController.navigate(UserSpaceRoute(mid = mid)) }
                    },
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.toViewLoading,
                isError = state.toViewError,
                hasMore = false,
                itemsIsEmpty = viewModel.toViewItems.isEmpty(),
            )
        }
    }
}

/**
 * 分区标题。
 */
@Composable
private fun SectionHeader(title: String) {
    androidx.tv.material3.Text(
        text = title,
        style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
        color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}
