package dev.frost819.newbv.core.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.pow

/**
 * new BV 品牌色板测试。
 *
 * 验证主题使用低饱和品牌色，并检查主要文字组合满足可读性要求。
 */
class ColorPaletteTest {
    @Test
    fun `brand palette uses the approved muted primary and secondary colors`() {
        assertThat(BVColors.Primary).isEqualTo(Color(0xFF7773AD))
        assertThat(BVColors.PrimaryStrong).isEqualTo(Color(0xFF5E5A8B))
        assertThat(BVColors.Secondary).isEqualTo(Color(0xFF5B9B94))
    }

    @Test
    fun `dark theme primary text has readable contrast`() {
        assertThat(contrastRatio(BVColors.DarkBackground, BVColors.DarkOnBackground))
            .isAtLeast(MIN_TEXT_CONTRAST)
        assertThat(contrastRatio(BVColors.DarkSurface, BVColors.DarkOnSurface))
            .isAtLeast(MIN_TEXT_CONTRAST)
    }

    @Test
    fun `light theme primary text has readable contrast`() {
        assertThat(contrastRatio(BVColors.LightBackground, BVColors.LightOnBackground))
            .isAtLeast(MIN_TEXT_CONTRAST)
        assertThat(contrastRatio(BVColors.LightSurface, BVColors.LightOnSurface))
            .isAtLeast(MIN_TEXT_CONTRAST)
    }

    @Test
    fun `brand button text has readable contrast`() {
        assertThat(contrastRatio(BVColors.PrimaryStrong, Color.White))
            .isAtLeast(MIN_TEXT_CONTRAST)
        assertThat(contrastRatio(BVColors.Secondary, BVColors.LightOnBackground))
            .isAtLeast(MIN_TEXT_CONTRAST)
    }

    @Test
    fun `light theme secondary accent is readable as foreground text`() {
        // secondary 在浅色主题中用作选中态文字/图标等前景色，必须满足文字对比度
        assertThat(contrastRatio(BVColors.LightSurface, BVColors.SecondaryStrong))
            .isAtLeast(MIN_TEXT_CONTRAST)
        assertThat(contrastRatio(BVColors.LightBackground, BVColors.SecondaryStrong))
            .isAtLeast(MIN_TEXT_CONTRAST)
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return linearize(color.red) * 0.2126 +
            linearize(color.green) * 0.7152 +
            linearize(color.blue) * 0.0722
    }

    private companion object {
        private const val MIN_TEXT_CONTRAST = 4.5
    }
}
