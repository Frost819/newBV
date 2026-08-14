package dev.frost819.newbv.app.ui.screen.search

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.navigation.SearchResultRoute
import dev.frost819.newbv.app.ui.navigation.SearchRoute
import dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchResultViewModel

/**
 * 搜索输入页导航注册（全屏入口）。
 *
 * [SearchRoute] 对应全屏 [SearchInputContent]。
 * 从详情页 Tag 点击进入；MainScreen 左侧导航栏的搜索入口直接嵌入 [SearchInputContent]。
 */
fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<SearchRoute> {
        val focusRequester = remember { FocusRequester() }
        val viewModel: SearchInputViewModel = hiltViewModel()
        SearchInputContent(
            viewModel = viewModel,
            focusRequester = focusRequester,
            onSearch = { keyword ->
                viewModel.commitSearch(keyword) {
                    navController.navigate(SearchResultRoute(keyword = keyword))
                }
            },
        )
    }
}

/**
 * 搜索结果页导航注册。
 *
 * [SearchResultRoute] 对应全屏 [SearchResultContent]，keyword 通过路由参数传递。
 */
fun NavGraphBuilder.searchResultScreen(navController: NavController) {
    composable<SearchResultRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SearchResultRoute>()
        val viewModel: SearchResultViewModel = hiltViewModel()
        SearchResultContent(
            viewModel = viewModel,
            keyword = route.keyword,
            navController = navController,
        )
    }
}
