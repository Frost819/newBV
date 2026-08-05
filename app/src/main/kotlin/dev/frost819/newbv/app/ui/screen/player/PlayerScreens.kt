package dev.frost819.newbv.app.ui.screen.player

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.SeasonPlayerRoute
import dev.frost819.newbv.app.ui.navigation.VideoPlayerRoute
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.app.viewmodel.player.PlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.SubtitleViewModel
import dev.frost819.newbv.app.viewmodel.player.VideoListViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 视频播放器页面注册。
 *
 * 从 [VideoPlayerRoute] 提取参数，初始化 PlayerViewModel 和 DanmakuViewModel，
 * 配置全屏 + 隐藏系统栏，然后渲染 [VideoPlayerScreen]。
 */
fun NavGraphBuilder.videoPlayerScreen(navController: NavController) {
    composable<VideoPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VideoPlayerRoute>()
        val context = LocalContext.current
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val danmakuViewModel: DanmakuViewModel = hiltViewModel()
        val subtitleViewModel: SubtitleViewModel = hiltViewModel()
        val videoListViewModel: VideoListViewModel = hiltViewModel()

        // 初始化播放器状态 + 加载视频资源（顺序执行，避免竞态）
        LaunchedEffect(route.aid) {
            // 1. 设置播放状态（aid/cid/title 等）
            playerViewModel.init(
                aid = route.aid,
                cid = route.cid,
                epid = route.epid?.toInt(),
                title = route.title,
                lastPlayed = 0,
                fromSeason = false,
                subType = 0,
                seasonId = 0,
                authorName = "",
            )
            // 2. 设置当前视频 aid（过滤 repository 数据，防止叠加打开错位）
            videoListViewModel.setCurrentAid(route.aid)
            // 3. 初始化 ExoPlayer（同步，确保 videoPlayer 就绪）
            playerViewModel.initVideoPlayer(context)
            // 4. 初始化弹幕播放器并加载弹幕
            danmakuViewModel.init()
            danmakuViewModel.loadDanmaku(route.cid)
            danmakuViewModel.loadDanmakuMask(route.aid, route.cid)
            // 5. 加载字幕列表
            subtitleViewModel.loadSubtitleList(route.aid, route.cid)
            // 6. 加载视频详情（相关视频、历史进度）— 视频列表已由详情页填充
            //    loadVideoDetail 是 suspend，会等待详情加载完成后再继续
            //    仅当历史 cid 与当前 cid 一致时才应用断点续播
            playerViewModel.loadVideoDetail(route.aid)
            // 7. 获取播放地址并开始播放
            playerViewModel.loadVideoWithResources()
        }

        // 释放播放器资源
        DisposableEffect(Unit) {
            onDispose {
                playerViewModel.detachPlayer()
                danmakuViewModel.release()
            }
        }

        // 全屏 + 隐藏系统栏
        DisposableEffect(Unit) {
            val window = (context as? android.app.Activity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                WindowInsetsControllerCompat(it, it.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            onDispose {
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, true)
                    WindowInsetsControllerCompat(it, it.decorView).apply {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }

        VideoPlayerScreen(
            navController = navController,
            playerViewModel = playerViewModel,
            danmakuViewModel = danmakuViewModel,
            subtitleViewModel = subtitleViewModel,
            videoListViewModel = videoListViewModel,
        )
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
