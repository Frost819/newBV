package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material3.Scaffold as Material3Scaffold
import dev.frost819.newbv.app.ui.component.HomeTabItem
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 首页内容（TopNav + 3 个子 Tab）。
 *
 * 顶部 Tab 按首选项排序，Tab 切换使用 [AnimatedContent] 横向滑动。
 * 菜单键刷新当前 Tab 数据。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器（由 MainScreen 传入）。
 * @param navController 导航控制器（跳转详情页等）。
 */
@Composable
fun HomeContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val firstTab = remember { Prefs.firstHomeTopNavItem }
    var selectedTab by rememberSaveable { mutableStateOf(firstTab) }
    var focusOnContent by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val reorderedItems = remember {
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
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    val tab = (nav as HomeTabItem).item
                    selectedTab = tab
                    if (tab == HomeTopNavItem.Dynamics && uiState.dynamicItems.isEmpty()) {
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
            modifier = Modifier
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
            AnimatedContent(
                targetState = selectedTab,
                label = "home-animated-content",
                transitionSpec = {
                    val coefficient = 10
                    if (reorderedItems.indexOf(HomeTabItem(targetState)) <
                        reorderedItems.indexOf(HomeTabItem(initialState))
                    ) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                },
            ) { screen ->
                when (screen) {
                    HomeTopNavItem.Recommend -> RecommendScreen(viewModel = viewModel, navController = navController)
                    HomeTopNavItem.Popular -> PopularScreen(viewModel = viewModel, navController = navController)
                    HomeTopNavItem.Dynamics -> DynamicsScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }
}
