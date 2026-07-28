package dev.frost819.newbv.app.ui.screen.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.navigation.HomeRoute
import dev.frost819.newbv.app.ui.screen.main.MainScreen

/**
 * 主页导航注册。
 *
 * [HomeRoute] 对应 [MainScreen]（NavigationDrawer + HomeContent）。
 */
fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable<HomeRoute> {
        MainScreen(navController = navController)
    }
}
