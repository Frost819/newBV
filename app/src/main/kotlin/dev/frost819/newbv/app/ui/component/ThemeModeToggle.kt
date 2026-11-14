package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.ThemeMode

/**
 * 主题相关的公共读写工具。
 *
 * 应用里有多处需要「当前是不是深色」这个信息：根主题装配、设置页、左侧导航栏快捷按钮。
 * 统一放在这里，避免各处各自拼 `ThemeMode.isDark(isSystemInDarkTheme())` 导致口径不一致。
 */

/**
 * 观察当前是否处于深色外观。
 *
 * 「跟随系统」档要结合系统 uiMode 才能得出结果，因此不能直接读 [Prefs.themeMode]。
 *
 * @return `true` 表示当前应渲染深色主题。
 */
@Composable
fun rememberIsDarkTheme(): Boolean {
    val mode by Prefs.themeModeFlow.collectAsState(initial = Prefs.themeMode)
    val systemIsDark = isSystemInDarkTheme()
    return when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.FollowSystem -> systemIsDark
    }
}

/**
 * 在当前外观的反面之间切换，并落成明确的 [ThemeMode.Dark] / [ThemeMode.Light]。
 *
 * 直接用「当前实际外观」取反，而不是用「持久化的档位」取反：
 * 处于「跟随系统」时若系统恰好是深色，按档位取反会切到 `Dark`，
 * 界面看起来毫无变化（因为本来就是深色）。按实际外观取反才符合直觉。
 *
 * 执行后档位会停留在显式深色/浅色，不再跟随系统——用户手动点了一次，
 * 意图就是要一个确定的外观。
 *
 * @param currentlyDark 当前实际是否深色，由调用方从 [rememberIsDarkTheme] 取得。
 */
fun toggleThemeMode(currentlyDark: Boolean) {
    Prefs.themeMode = if (currentlyDark) ThemeMode.Light else ThemeMode.Dark
}