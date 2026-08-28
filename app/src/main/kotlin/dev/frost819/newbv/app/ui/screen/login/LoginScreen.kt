package dev.frost819.newbv.app.ui.screen.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.frost819.newbv.app.ui.navigation.LoginRoute

/**
 * 登录页面导航注册。
 *
 * 显示扫码登录界面，登录成功后自动返回上一页。
 */
fun NavGraphBuilder.loginScreen(navController: NavController) {
    composable<LoginRoute> {
        QrLoginContent(
            onLoginSuccess = { navController.popBackStack() },
        )
    }
}
