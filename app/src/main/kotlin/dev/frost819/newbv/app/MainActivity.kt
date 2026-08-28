package dev.frost819.newbv.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.frost819.newbv.app.ui.navigation.AppNavHost
import dev.frost819.newbv.app.ui.navigation.HomeRoute
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import dev.frost819.newbv.data.datastore.Prefs
import javax.inject.Inject

/**
 * 主 Activity（单 Activity 架构）。
 *
 * 职责：
 * - SplashScreen 显示
 * - 交互模式追踪（触屏/遥控器）
 * - Navigation 宿主（[AppNavHost]）
 * - 主题模式 + density 从 Prefs 实时读取
 *
 * 所有页面通过 Navigation-Compose 导航，不启动新 Activity。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val logger = Loggers.get("MainActivity")

    @Inject
    lateinit var interactionTracker: InteractionTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by Prefs.themeModeFlow.collectAsState(initial = ThemeMode.Dark)
            val density by Prefs.densityFlow.collectAsState(initial = 2.0f)

            val coreThemeMode =
                when (themeMode) {
                    dev.frost819.newbv.data.datastore.ThemeMode.FollowSystem -> ThemeMode.FollowSystem
                    dev.frost819.newbv.data.datastore.ThemeMode.Dark -> ThemeMode.Dark
                    dev.frost819.newbv.data.datastore.ThemeMode.Light -> ThemeMode.Light
                    else -> ThemeMode.Dark
                }

            BVTheme(themeMode = coreThemeMode, density = density) {
                CompositionLocalProvider(
                    LocalInteractionTracker provides interactionTracker,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(startDestination = HomeRoute)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        interactionTracker.onTouch()
        return super.onTouchEvent(event)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            interactionTracker.onDpadKey()
            logger.info { "[INPUT] keyDown keyCode=${KeyEvent.keyCodeToString(event.keyCode)}" }
        }
        return super.dispatchKeyEvent(event)
    }
}
