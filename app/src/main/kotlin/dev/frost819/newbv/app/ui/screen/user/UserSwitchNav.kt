package dev.frost819.newbv.app.ui.screen.user

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.navigation.LoginRoute
import dev.frost819.newbv.app.ui.navigation.UserSwitchRoute

/**
 * 账号管理页面导航注册。
 *
 * "添加账号"跳转到 [LoginRoute]。
 */
fun NavGraphBuilder.userSwitchScreen(navController: NavController) {
    composable<UserSwitchRoute> {
        UserSwitchScreen(
            onNavigateLogin = {
                navController.navigate(LoginRoute)
            },
        )
    }
}
