package dev.frost819.newbv.core.theme

import androidx.compose.ui.graphics.Color

/**
 * new BV 品牌色板。
 *
 * 以 B 站粉 `#FB7299` 为核心，延伸出辅助色与中性色。
 * 所有颜色以 `Color` 表示，供 [BVTheme] 构建 colorScheme。
 */
object BVColors {
    /** B 站粉，品牌主色。 */
    val Pink = Color(0xFFFB7299)
    val PinkDark = Color(0xFFE8698F)
    val PinkLight = Color(0xFFFF9BBC)

    /** B 站蓝，辅助色。 */
    val Blue = Color(0xFF00AEEC)
    val BlueDark = Color(0xFF0090C9)
    val BlueLight = Color(0xFF33C5FF)

    /** 黄色，用于投币/警告。 */
    val Yellow = Color(0xFFFFB027)

    /** 绿色，用于成功状态。 */
    val Green = Color(0xFF4CAF50)

    /** 红色，用于错误/直播。 */
    val Red = Color(0xFFEF4444)

    /** 深色主题中性色。 */
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkSurfaceVariant = Color(0xFF2A2A2A)
    val DarkOnBackground = Color(0xFFE8E8E8)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkBorder = Color.White

    /** 浅色主题中性色。 */
    val LightBackground = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0F0F0)
    val LightOnBackground = Color(0xFF1A1A1A)
    val LightOnSurface = Color(0xFF222222)
    val LightBorder = Color.Black
}
