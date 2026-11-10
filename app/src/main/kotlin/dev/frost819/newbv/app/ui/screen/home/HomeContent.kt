package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.HomeTabItem
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.Prefs
import androidx.compose.material3.Scaffold as Material3Scaffold

/**
 * 首页内容（TopNav + 3 个子 Tab）。
 *
 * 顶部 Tab 按首选项排序，Tab 切换使用 [AnimatedContent] 横向滑动。
 * 菜单键刷新当前 Tab 数据。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器（由 MainScreen 传入）。
 * @param navController 导航控制器（跳转详情页等）。
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 */
@Composable
fun HomeContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    focusSaver: FocusSaver,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val firstTab = remember { Prefs.firstHomeTopNavItem }
    var selectedTab by rememberSaveable { mutableStateOf(firstTab) }
    var focusOnContent by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val reorderedItems =
        remember {
            val allItems = HomeTopNavItem.entries
            val startIndex = allItems.indexOf(firstTab)
            if (startIndex == -1) {
                allItems.map { HomeTabItem(it) }
            } else {
                (allItems.drop(startIndex) + allItems.take(startIndex)).map { HomeTabItem(it) }
            }
        }

    Material3Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = reorderedItems,
                selectedIndex = reorderedItems.indexOf(HomeTabItem(selectedTab)),
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    val tab = (nav as HomeTabItem).item
                    selectedTab = tab
                    // 首次切到某个 Tab 时再拉数据，冷启动不再三个 Tab 一起请求。
                    when {
                        tab == HomeTopNavItem.Recommend && uiState.recommendItems.isEmpty() ->
                            viewModel.loadRecommend()
                        tab == HomeTopNavItem.Popular && uiState.popularItems.isEmpty() ->
                            viewModel.loadPopular()
                        tab == HomeTopNavItem.Dynamics && uiState.dynamicItems.isEmpty() ->
                            viewModel.loadDynamic()
                    }
                },
                onClick = { nav ->
                    val tab = (nav as HomeTabItem).item
                    viewModel.refresh(tab)
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .onFocusChanged { focusOnContent = it.hasFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                            viewModel.refresh(selectedTab)
                            navFocusRequester.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
        ) {
            // 切 Tab 不再做 fade/slide 转场：转场期间新旧两个网格（各几十张卡片）
            // 会同时留在组合树里并逐帧重绘，左右切换时明显掉帧。
            when (selectedTab) {
                HomeTopNavItem.Recommend ->
                    RecommendScreen(
                        viewModel = viewModel,
                        navController = navController,
                        focusSaver = focusSaver,
                    )
                HomeTopNavItem.Popular ->
                    PopularScreen(
                        viewModel = viewModel,
                        navController = navController,
                        focusSaver = focusSaver,
                    )
                HomeTopNavItem.Dynamics ->
                    DynamicsScreen(
                        viewModel = viewModel,
                        navController = navController,
                        focusSaver = focusSaver,
                    )
            }
        }
    }
}
