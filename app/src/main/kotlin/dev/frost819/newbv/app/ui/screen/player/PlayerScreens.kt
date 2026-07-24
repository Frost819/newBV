package dev.frost819.newbv.app.ui.screen.player

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.SeasonPlayerRoute
import dev.frost819.newbv.app.ui.navigation.VideoPlayerRoute

fun NavGraphBuilder.videoPlayerScreen(navController: NavController) {
    composable<VideoPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VideoPlayerRoute>()
        PlaceholderScreen(title = "Video Player (aid=${route.aid}, cid=${route.cid})")
    }
}

fun NavGraphBuilder.seasonPlayerScreen(navController: NavController) {
    composable<SeasonPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SeasonPlayerRoute>()
        PlaceholderScreen(title = "Season Player (epid=${route.epid}, sid=${route.sid})")
    }
}

fun NavGraphBuilder.livePlayerScreen(navController: NavController) {
    composable<LivePlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LivePlayerRoute>()
        PlaceholderScreen(title = "Live Player (roomId=${route.roomId})")
    }
}
