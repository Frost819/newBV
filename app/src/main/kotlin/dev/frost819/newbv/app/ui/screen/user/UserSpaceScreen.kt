package dev.frost819.newbv.app.ui.screen.user

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute

fun NavGraphBuilder.userSpaceScreen(navController: NavController) {
    composable<UserSpaceRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<UserSpaceRoute>()
        PlaceholderScreen(title = "User Space (mid=${route.mid})")
    }
}
