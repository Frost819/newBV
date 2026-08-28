package dev.frost819.newbv.app.ui.screen.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.navigation.MediaCodecRoute
import dev.frost819.newbv.app.ui.navigation.SettingsRoute
import dev.frost819.newbv.app.ui.navigation.LogViewerRoute

/** 设置页路由注册。 */
fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateToMediaCodec = { navController.navigate(MediaCodecRoute) },
            onNavigateToLogViewer = { navController.navigate(LogViewerRoute) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<MediaCodecRoute> {
        MediaCodecScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable<LogViewerRoute> {
        LogViewerScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
