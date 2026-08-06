package dev.frost819.newbv.app.ui.screen.ugc

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Scaffold as Material3Scaffold
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.ugc.UgcViewModel
import dev.frost819.newbv.biliapi.entity.ugc.UgcTypeV2
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * UGC 分区顶部导航项。
 *
 * 仅包含一级分区（有 channelId 的 [UgcTypeV2]）。
 *
 * @property ugcTypeV2 对应的 UGC 分区类型。
 */
enum class UgcTabItem(val ugcTypeV2: UgcTypeV2, override val displayName: String) : TopNavItem {
    Douga(UgcTypeV2.Douga, "动画"),
    Game(UgcTypeV2.Game, "游戏"),
    Kichiku(UgcTypeV2.Kichiku, "鬼畜"),
    Music(UgcTypeV2.Music, "音乐"),
    Dance(UgcTypeV2.Dance, "舞蹈"),
    Cinephile(UgcTypeV2.Cinephile, "影视"),
    Ent(UgcTypeV2.Ent, "娱乐"),
    Knowledge(UgcTypeV2.Knowledge, "知识"),
    Tech(UgcTypeV2.Tech, "科技数码"),
    Information(UgcTypeV2.Information, "资讯"),
    Food(UgcTypeV2.Food, "美食"),
    Car(UgcTypeV2.Car, "汽车"),
    Fashion(UgcTypeV2.Fashion, "时尚美妆"),
    Sports(UgcTypeV2.Sports, "体育运动"),
    Animal(UgcTypeV2.Animal, "动物"),
    ;
}

/**
 * UGC 分区内容（TopNav + 4 列网格）。
 *
 * 顶部 Tab 切换分区，内容区为视频网格 + 无限滚动。
 * 菜单键刷新当前分区数据。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器。
 * @param navController 导航控制器。
 * @param viewModel UGC ViewModel。
 */
@Composable
fun UgcContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    viewModel: UgcViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(UgcTabItem.Douga) }
    var focusOnContent by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Material3Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = UgcTabItem.entries.toList(),
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    val tab = nav as UgcTabItem
                    if (tab != selectedTab) {
                        selectedTab = tab
                        viewModel.switchType(tab.ugcTypeV2)
                    }
                },
                onClick = { nav ->
                    val tab = nav as UgcTabItem
                    viewModel.refresh()
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                        viewModel.refresh()
                        navFocusRequester.requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
        ) {
            UgcGrid(
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}

/**
 * UGC 视频网格。
 *
 * 4 列网格 + 无限滚动，距离底部 20 条时触发加载更多。
 */
@Composable
private fun UgcGrid(
    viewModel: UgcViewModel,
    navController: NavController,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val focusSaver = rememberFocusSaver()

    focusSaver.RestoreFocus()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.items.size - 20
            }
            .collect {
                viewModel.loadMore()
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
            key = { index, _ -> index },
        ) { index, item ->
            val cardData = remember(item) {
                VideoCardData(
                    avid = item.aid,
                    title = item.title,
                    cover = item.cover,
                    playString = item.play.takeIf { it != -1 }.toWanString(),
                    danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                    timeString = (item.duration * 1000L).formatHourMinSec(),
                    upName = item.author,
                    upMid = item.authorMid,
                    pubTime = item.pubTime,
                )
            }
            SmallVideoCard(
                modifier = Modifier.focusSaverItem(focusSaver, index),
                data = cardData,
                onClick = {
                    navController.navigate(VideoDetailRoute(aid = item.aid))
                },
                onGoToDetailPage = {
                    navController.navigate(VideoDetailRoute(aid = item.aid))
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ListFooterTip(
                isLoading = state.loading,
                isError = state.error,
                hasMore = state.hasMore,
                itemsIsEmpty = state.items.isEmpty(),
            )
        }
    }
}
