package dev.frost819.newbv.app.ui.screen.search

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.navigation.SearchRoute

/**
 * 搜索页导航注册（全屏入口，从详情页 Tag 点击进入）。
 *
 * [SearchRoute] 对应全屏 [SearchContent]。
 * MainScreen 左侧导航栏的搜索入口直接嵌入 [SearchContent]，不走此路由。
 */
fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<SearchRoute> {
        val focusRequester = remember { FocusRequester() }
        SearchContent(
            navFocusRequester = focusRequester,
            navController = navController,
        )
    }
}
