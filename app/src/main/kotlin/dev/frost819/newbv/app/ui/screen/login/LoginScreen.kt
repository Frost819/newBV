package dev.frost819.newbv.app.ui.screen.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.LoginRoute

fun NavGraphBuilder.loginScreen(navController: NavController) {
    composable<LoginRoute> {
        PlaceholderScreen(title = "Login")
    }
}
