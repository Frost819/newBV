package dev.frost819.newbv.core.theme

import androidx.compose.ui.graphics.Color

/**
 * new BV 品牌色板。
 *
 * 以低饱和蓝紫与青绿色为核心，延伸出辅助色与中性色。
 * 所有颜色以 `Color` 表示，供 [BVTheme] 构建 colorScheme。
 */
object BVColors {
    /** B 站粉，品牌主色。 */
    /** 低饱和蓝紫，作为 new BV 的主品牌色。 */
    val Primary = Color(0xFF7773AD)
    /** 深色背景下使用的主色变体。 */
    val PrimaryStrong = Color(0xFF5E5A8B)
    /** 浅色背景下使用的主色变体。 */
    val PrimaryLight = Color(0xFF9995CF)

    /** B 站蓝，辅助色。 */
    /** 低饱和青绿，用于辅助操作和信息状态。 */
    val Secondary = Color(0xFF5B9B94)
    val SecondaryLight = Color(0xFF79B8AE)

    /** 黄色，用于投币/警告。 */
    val Yellow = Color(0xFFC49B5C)

    /** 绿色，用于成功状态。 */
    val Green = Color(0xFF70A684)

    /** 红色，用于错误/直播。 */
    val Red = Color(0xFFC87878)

    /** 深色主题中性色。 */
    val DarkBackground = Color(0xFF171717)
    val DarkSurface = Color(0xFF222222)
    val DarkSurfaceVariant = Color(0xFF303030)
    val DarkOnBackground = Color(0xFFF2F2F2)
    val DarkOnSurface = Color(0xFFF2F2F2)
    val DarkOnSurfaceVariant = Color(0xFFB8B8B8)
    val DarkBorder = PrimaryLight

    /** 浅色主题中性色。 */
    val LightBackground = Color(0xFFF5F7FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE8ECF5)
    val LightOnBackground = Color(0xFF172033)
    val LightOnSurface = Color(0xFF172033)
    val LightOnSurfaceVariant = Color(0xFF59657C)
    val LightBorder = PrimaryStrong
}
