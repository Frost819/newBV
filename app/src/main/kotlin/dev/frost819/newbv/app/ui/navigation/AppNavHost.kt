package dev.frost819.newbv.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.frost819.newbv.app.ui.screen.detail.videoDetailScreen
import dev.frost819.newbv.app.ui.screen.home.homeScreen
import dev.frost819.newbv.app.ui.screen.login.loginScreen
import dev.frost819.newbv.app.ui.screen.pgc.pgcFeatureScreen
import dev.frost819.newbv.app.ui.screen.player.livePlayerScreen
import dev.frost819.newbv.app.ui.screen.player.seasonPlayerScreen
import dev.frost819.newbv.app.ui.screen.player.videoPlayerScreen
import dev.frost819.newbv.app.ui.screen.search.searchScreen
import dev.frost819.newbv.app.ui.screen.settings.settingsScreen
import dev.frost819.newbv.app.ui.screen.user.userSwitchScreen
import dev.frost819.newbv.app.ui.screen.user.userSpaceScreen

/**
 * 应用 Navigation 宿主。
 *
 * 单 Activity 架构：所有页面通过 Navigation-Compose 导航，无新 Activity 启动。
 * 路由定义见 [Routes]。
 *
 * @param navController 导航控制器，默认使用 [rememberNavController]
 * @param startDestination 起始路由，默认 [HomeRoute]
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = HomeRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── 主流程 ────────────────────────────────────────────────────
        homeScreen(navController)
        searchScreen(navController)

        // ── 视频详情 ─────────────────────────────────────────────────
        videoDetailScreen(navController)

        // ── 播放器 ───────────────────────────────────────────────────
        videoPlayerScreen(navController)
        seasonPlayerScreen(navController)
        livePlayerScreen(navController)

        // ── 用户 ─────────────────────────────────────────────────────
        userSpaceScreen(navController)

        // ── PGC ──────────────────────────────────────────────────────
        pgcFeatureScreen(navController)

        // ── 设置 ─────────────────────────────────────────────────────
        settingsScreen(navController)
        userSwitchScreen(navController)

        // ── 登录 ─────────────────────────────────────────────────────
        loginScreen(navController)
    }
}
