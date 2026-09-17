package dev.frost819.newbv.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme as CommonMaterialTheme
import androidx.compose.material3.Surface as CommonSurface
import androidx.compose.material3.darkColorScheme as commonDark
import androidx.compose.material3.lightColorScheme as commonLight
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Surface as TvSurface
import androidx.tv.material3.darkColorScheme as tvDark
import androidx.tv.material3.lightColorScheme as tvLight

/**
 * new BV 根主题。
 *
 * 根据 [ThemeMode] 与系统状态选择深/浅色板，并提供自定义密度与字体缩放。
 * 可嵌套调用：播放器需要在浅色应用下固定使用深色主题时，用
 * `BVTheme(themeMode = ThemeMode.Dark, density = LocalDensity.current.density)` 包裹即可。
 *
 * 系统状态栏的同步由 [SystemBarsEffect] 负责，仅需在根主题处调用一次。
 *
 * @param themeMode 主题模式（跟随系统 / 深色 / 浅色）。
 * @param density 屏幕密度（TV 场景通常为 2.0）。
 * @param content 主题包裹的内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BVTheme(
    themeMode: ThemeMode = ThemeMode.FollowSystem,
    density: Float = 1f,
    content: @Composable () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val systemIsDark = isSystemInDarkTheme()
    val isDark = themeMode.isDark(systemIsDark)

    val tvColorScheme =
        if (isDark) {
            tvDark(
                primary = BVColors.Primary,
                onPrimary = Color.White,
                primaryContainer = BVColors.PrimaryStrong,
                onPrimaryContainer = Color.White,
                secondary = BVColors.Secondary,
                onSecondary = BVColors.DarkOnBackground,
                secondaryContainer = BVColors.DarkSurfaceVariant,
                onSecondaryContainer = BVColors.DarkOnSurface,
                background = BVColors.DarkBackground,
                onBackground = BVColors.DarkOnBackground,
                surface = BVColors.DarkSurface,
                onSurface = BVColors.DarkOnSurface,
                surfaceVariant = BVColors.DarkSurfaceVariant,
                onSurfaceVariant = BVColors.DarkOnSurfaceVariant,
                border = BVColors.DarkBorder,
            )
        } else {
            tvLight(
                primary = BVColors.PrimaryStrong,
                onPrimary = Color.White,
                primaryContainer = BVColors.PrimaryLight,
                onPrimaryContainer = BVColors.LightOnBackground,
                // secondary 作为强调色（选中态文字/图标）使用时需要足够对比度，
                // 浅色背景下使用加深变体，避免低对比导致文字发虚
                secondary = BVColors.SecondaryStrong,
                onSecondary = Color.White,
                secondaryContainer = BVColors.LightSurfaceVariant,
                onSecondaryContainer = BVColors.LightOnBackground,
                background = BVColors.LightBackground,
                onBackground = BVColors.LightOnBackground,
                surface = BVColors.LightSurface,
                onSurface = BVColors.LightOnSurface,
                surfaceVariant = BVColors.LightSurfaceVariant,
                onSurfaceVariant = BVColors.LightOnSurfaceVariant,
                border = BVColors.LightBorder,
            )
        }

    val commonColorScheme =
        if (isDark) {
            commonDark(
                primary = BVColors.Primary,
                onPrimary = Color.White,
                primaryContainer = BVColors.PrimaryStrong,
                onPrimaryContainer = Color.White,
                secondary = BVColors.Secondary,
                onSecondary = BVColors.DarkOnBackground,
                secondaryContainer = BVColors.DarkSurfaceVariant,
                onSecondaryContainer = BVColors.DarkOnSurface,
                background = BVColors.DarkBackground,
                onBackground = BVColors.DarkOnBackground,
                surface = BVColors.DarkSurface,
                onSurface = BVColors.DarkOnSurface,
                surfaceVariant = BVColors.DarkSurfaceVariant,
                onSurfaceVariant = BVColors.DarkOnSurfaceVariant,
            )
        } else {
            commonLight(
                primary = BVColors.PrimaryStrong,
                onPrimary = Color.White,
                primaryContainer = BVColors.PrimaryLight,
                onPrimaryContainer = BVColors.LightOnBackground,
                secondary = BVColors.SecondaryStrong,
                onSecondary = Color.White,
                secondaryContainer = BVColors.LightSurfaceVariant,
                onSecondaryContainer = BVColors.LightOnBackground,
                background = BVColors.LightBackground,
                onBackground = BVColors.LightOnBackground,
                surface = BVColors.LightSurface,
                onSurface = BVColors.LightOnSurface,
                surfaceVariant = BVColors.LightSurfaceVariant,
                onSurfaceVariant = BVColors.LightOnSurfaceVariant,
            )
        }

    TvMaterialTheme(
        colorScheme = tvColorScheme,
        typography = BVTypography.tv,
    ) {
        CommonMaterialTheme(
            colorScheme = commonColorScheme,
            typography = BVTypography.common,
        ) {
            CompositionLocalProvider(
                LocalRippleConfiguration provides null,
                LocalDensity provides Density(density = density, fontScale = fontScale),
            ) {
                CommonSurface(color = Color.Transparent) {
                    TvSurface(shape = RoundedCornerShape(0.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * 将当前主题色同步到系统状态栏（背景色 + 图标明暗）。
 *
 * 必须在根主题 [BVTheme] 内调用一次。嵌套主题（如播放器强制深色）不需要、
 * 也不应重复调用，否则退出后状态栏会停留在内层主题的颜色上。
 */
@Composable
fun SystemBarsEffect() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val statusBarColor = TvMaterialTheme.colorScheme.primary
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = statusBarColor.toArgb()
        // 状态栏图标外观由背景亮度决定：暗色状态栏配浅色图标，反之配深色图标
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            statusBarColor.luminance() > 0.5f
    }
}
