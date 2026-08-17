package dev.frost819.newbv.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme as CommonMaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface as CommonSurface
import androidx.compose.material3.darkColorScheme as commonDark
import androidx.compose.material3.lightColorScheme as commonLight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Surface as TvSurface
import androidx.tv.material3.darkColorScheme as tvDark
import androidx.tv.material3.lightColorScheme as tvLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BVTheme(
    themeMode: ThemeMode = ThemeMode.FollowSystem,
    density: Float = 1f,
    fpsMonitor: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val fontScale = LocalDensity.current.fontScale
    val systemIsDark = isSystemInDarkTheme()
    val isDark = themeMode.isDark(systemIsDark)

    val tvColorScheme = if (isDark) tvDark(
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
        border = BVColors.DarkBorder
    ) else tvLight(
        primary = BVColors.PrimaryStrong,
        onPrimary = Color.White,
        primaryContainer = BVColors.PrimaryLight,
        onPrimaryContainer = BVColors.LightOnBackground,
        secondary = BVColors.Secondary,
        onSecondary = BVColors.LightOnBackground,
        secondaryContainer = BVColors.LightSurfaceVariant,
        onSecondaryContainer = BVColors.LightOnBackground,
        background = BVColors.LightBackground,
        onBackground = BVColors.LightOnBackground,
        surface = BVColors.LightSurface,
        onSurface = BVColors.LightOnSurface,
        surfaceVariant = BVColors.LightSurfaceVariant,
        onSurfaceVariant = BVColors.LightOnSurfaceVariant,
        border = BVColors.LightBorder
    )

    val commonColorScheme = if (isDark) commonDark(
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
        onSurfaceVariant = BVColors.DarkOnSurfaceVariant
    ) else commonLight(
        primary = BVColors.PrimaryStrong,
        onPrimary = Color.White,
        primaryContainer = BVColors.PrimaryLight,
        onPrimaryContainer = BVColors.LightOnBackground,
        secondary = BVColors.Secondary,
        onSecondary = BVColors.LightOnBackground,
        secondaryContainer = BVColors.LightSurfaceVariant,
        onSecondaryContainer = BVColors.LightOnBackground,
        background = BVColors.LightBackground,
        onBackground = BVColors.LightOnBackground,
        surface = BVColors.LightSurface,
        onSurface = BVColors.LightOnSurface,
        surfaceVariant = BVColors.LightSurfaceVariant,
        onSurfaceVariant = BVColors.LightOnSurfaceVariant
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = tvColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    TvMaterialTheme(
        colorScheme = tvColorScheme,
        typography = BVTypography.tv
    ) {
        CommonMaterialTheme(
            colorScheme = commonColorScheme,
            typography = BVTypography.common
        ) {
            CompositionLocalProvider(
                LocalRippleConfiguration provides null,
                LocalDensity provides Density(density = density, fontScale = fontScale)
            ) {
                CommonSurface(color = Color.Transparent) {
                    TvSurface(shape = RoundedCornerShape(0.dp)) {
                        if (fpsMonitor != null) {
                            fpsMonitor()
                        } else {
                            content()
                        }
                    }
                }
            }
        }
    }
}
