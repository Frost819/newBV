package dev.frost819.newbv.app.ui.screen.pgc

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute

fun NavGraphBuilder.pgcFeatureScreen(navController: NavController) {
    composable<PgcFeatureRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PgcFeatureRoute>()
        PlaceholderScreen(title = "PGC Feature (seasonId=${route.seasonId})")
    }
}
