package dev.frost819.newbv.app.ui.screen.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute

fun NavGraphBuilder.videoDetailScreen(navController: NavController) {
    composable<VideoDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VideoDetailRoute>()
        PlaceholderScreen(title = "Video Detail (aid=${route.aid})")
    }
}
