package dev.frost819.newbv.app.ui.screen.live

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
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.viewmodel.live.LiveHomeViewModel

/**
 * 直播内容（TopNav + 2 个子 Tab：推荐/分区）。
 *
 * 替换 MainScreen 中的 `PlaceholderContent("直播")`。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器（由 MainScreen 传入）。
 * @param navController 导航控制器。
 */
@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    viewModel: LiveHomeViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(LiveTabItem.Recommend) }
    var focusOnContent by remember { mutableStateOf(false) }

    val items = LiveTabItem.entries.toList()

    Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = items,
                selectedIndex = items.indexOf(selectedTab),
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    selectedTab = nav as LiveTabItem
                },
                onClick = { nav ->
                    val tab = nav as LiveTabItem
                    when (tab) {
                        LiveTabItem.Recommend -> viewModel.loadRecommend()
                        LiveTabItem.Area -> viewModel.loadAreaList()
                    }
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
                        when (selectedTab) {
                            LiveTabItem.Recommend -> viewModel.loadRecommend()
                            LiveTabItem.Area -> viewModel.loadAreaList()
                        }
                        navFocusRequester.requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "live-animated-content",
                transitionSpec = {
                    val coefficient = 10
                    if (items.indexOf(targetState) < items.indexOf(initialState)) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                },
            ) { screen ->
                when (screen) {
                    LiveTabItem.Recommend -> LiveRecommendContent(
                        viewModel = viewModel,
                        navController = navController,
                    )
                    LiveTabItem.Area -> LiveAreaContent(
                        viewModel = viewModel,
                        navController = navController,
                    )
                }
            }
        }
    }
}
