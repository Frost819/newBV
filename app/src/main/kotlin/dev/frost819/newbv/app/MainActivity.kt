package dev.frost819.newbv.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
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
import dev.frost819.newbv.core.interaction.LocalInputMethod
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.SystemBarsEffect
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
            val inputMethod by interactionTracker.inputMethod.collectAsState()

            val coreThemeMode =
                when (themeMode) {
                    dev.frost819.newbv.data.datastore.ThemeMode.FollowSystem -> ThemeMode.FollowSystem
                    dev.frost819.newbv.data.datastore.ThemeMode.Dark -> ThemeMode.Dark
                    dev.frost819.newbv.data.datastore.ThemeMode.Light -> ThemeMode.Light
                    else -> ThemeMode.Dark
                }

            // 追踪器放在最外层：根主题里的 ripple、卡片边框都要按输入方式取值。
            // 整棵树只有这里订阅 StateFlow，组件读的是 LocalInputMethod 这个普通值。
            CompositionLocalProvider(LocalInputMethod provides inputMethod) {
                BVTheme(themeMode = coreThemeMode, density = density) {
                    SystemBarsEffect()
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(startDestination = HomeRoute)
                    }
                }
            }
        }
    }

    /**
     * 必须从 dispatchTouchEvent 记录触摸，不能用 onTouchEvent。
     *
     * Compose 的 pointerInput / clickable 会消费 DOWN 事件，窗口内一旦有人消费，
     * Activity.onTouchEvent 根本不会被调用，输入方式会一直停在 DPad，
     * 于是触屏点击后焦点边框不消失、列表按 TV 定轴滚动。
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            interactionTracker.onTouch()
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isNavigationKey) {
            interactionTracker.onDpadKey()
            // 遥控器长按会连发按键，每条日志都要做字符串拼接 + Log.println。
            // 只在 debug 构建保留，release 由 R8 常量折叠整块移除。
            if (dev.frost819.newbv.BuildConfig.DEBUG) {
                logger.info { "[INPUT] keyDown keyCode=${KeyEvent.keyCodeToString(event.keyCode)}" }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 只有导航/媒体类物理按键代表「用户拿起了遥控器」。
     *
     * 系统 IME、外接键盘敲字符都会走 dispatchKeyEvent，之前一律切回 DPad，
     * 触屏打字时焦点框会一按一个准地重新出现。
     */
    private val KeyEvent.isNavigationKey: Boolean
        get() {
            if (flags and KeyEvent.FLAG_SOFT_KEYBOARD != 0) return false
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_TAB,
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                -> true
                else -> false
            }
        }
}
