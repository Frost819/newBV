package dev.frost819.newbv.core.theme

import androidx.compose.ui.graphics.Color

/**
 * new BV 品牌色板。
 *
 * 以低饱和蓝紫与青绿色为核心，延伸出辅助色与中性色。
 * 所有颜色以 `Color` 表示，供 [BVTheme] 构建 colorScheme。
 */
object BVColors {
    /** 低饱和蓝紫，作为 new BV 的主品牌色。 */
    val Primary = Color(0xFF7773AD)

    /** 深色背景下使用的主色变体。 */
    val PrimaryStrong = Color(0xFF5E5A8B)

    /** 浅色背景下使用的主色变体。 */
    val PrimaryLight = Color(0xFF9995CF)

    /** 低饱和青绿，用于辅助操作和信息状态。 */
    val Secondary = Color(0xFF5B9B94)
    val SecondaryLight = Color(0xFF79B8AE)

    /** 浅色背景下使用的中性绿色变体，作为强调色文字/图标时保证对比度。 */
    val SecondaryStrong = Color(0xFF3F7A73)

    /** 黄色，用于投币/警告。 */
    val Yellow = Color(0xFFC49B5C)

    /** 绿色，用于成功状态。 */
    val Green = Color(0xFF70A684)

    /** 红色，用于错误/直播。 */
    val Red = Color(0xFFC87878)

    /*
     * 播放器进度条配色。
     *
     * 进度条直接压在视频画面上，且播放器固定深色主题，故用固定色而非半透明白：
     * 半透明白会随画面亮度变化，亮画面上「未缓冲」的视觉亮度会反超「已播放」段。
     * 三色相对亮度约 0.41 / 0.25 / 0.08，靠明度阶梯 + 色相双重区分。
     */

    /** 已播放段：在品牌紫同色相上提亮，保证黑底与亮画面上都清晰。 */
    val PlayerSeekActive = Color(0xFFA9A4E8)

    /** 已缓冲段：中性中灰，明显亮于未缓冲轨道。 */
    val PlayerSeekBuffered = Color(0xFF8A8A8A)

    /** 未缓冲轨道：中性暗灰，暗画面下仍有轮廓、亮画面下不会喧宾夺主。 */
    val PlayerSeekInactive = Color(0xFF4F4F4F)

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
