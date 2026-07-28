package dev.frost819.newbv.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.frost819.newbv.app.ui.navigation.AppNavHost
import dev.frost819.newbv.app.ui.navigation.HomeRoute
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import javax.inject.Inject

/**
 * 主 Activity（单 Activity 架构）。
 *
 * 职责：
 * - SplashScreen 显示
 * - 交互模式追踪（触屏/遥控器）
 * - Navigation 宿主（[AppNavHost]）
 *
 * 所有页面通过 Navigation-Compose 导航，不启动新 Activity。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var interactionTracker: InteractionTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            BVTheme(themeMode = ThemeMode.Dark, density = 2.0f) {
                CompositionLocalProvider(
                    LocalInteractionTracker provides interactionTracker
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode in DPAD_KEY_CODES) {
            interactionTracker.onDpadKey()
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        private val DPAD_KEY_CODES = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER
        )
    }
}
