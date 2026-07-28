package dev.frost819.newbv.app.ui.screen.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.SettingsRoute

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsRoute> {
        PlaceholderScreen(title = "Settings")
    }
}
