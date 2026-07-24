package dev.frost819.newbv.app.ui.screen.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.HomeRoute
import androidx.navigation.NavController

fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable<HomeRoute> {
        PlaceholderScreen(title = "Home")
    }
}
