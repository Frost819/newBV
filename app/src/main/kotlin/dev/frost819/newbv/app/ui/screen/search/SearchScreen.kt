package dev.frost819.newbv.app.ui.screen.search

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.SearchRoute

fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<SearchRoute> {
        PlaceholderScreen(title = "Search")
    }
}
