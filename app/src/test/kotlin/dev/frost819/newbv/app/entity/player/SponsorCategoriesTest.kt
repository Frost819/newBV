package dev.frost819.newbv.app.entity.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SponsorCategories] 的单元测试。
 *
 * 验证分类颜色表、展示名映射、默认值与序列化往返语义。
 */
class SponsorCategoriesTest {
    @Test
    fun `every option has unique non-empty category and distinct opaque color`() {
        val categories = SponsorCategories.OPTIONS.map { it.category }
        val colors = SponsorCategories.OPTIONS.map { it.color }

        assertThat(categories).containsNoDuplicates()
        assertThat(colors).containsNoDuplicates()
        SponsorCategories.OPTIONS.forEach { option ->
            assertThat(option.category).isNotEmpty()
            assertThat(option.displayName).isNotEmpty()
            // Alpha 通道应全为不透明，确保设置弹窗色点与进度条渲染颜色基准一致
            assertThat(option.color ushr 24).isEqualTo(0xFFL)
        }
    }

    @Test
    fun `colorOf and displayNameOf resolve known categories and fallback safely`() {
        SponsorCategories.OPTIONS.forEach { option ->
            assertThat(SponsorCategories.colorOf(option.category)).isEqualTo(option.color)
            assertThat(SponsorCategories.displayNameOf(option.category)).isEqualTo(option.displayName)
        }
        assertThat(SponsorCategories.colorOf("unknown")).isEqualTo(0xFF00D400)
        assertThat(SponsorCategories.displayNameOf("unknown")).isEqualTo("unknown")
    }

    @Test
    fun `parse and serialize treat empty string as all disabled`() {
        assertThat(SponsorCategories.parse("")).isEmpty()
        assertThat(SponsorCategories.parse(null)).isEmpty()
        assertThat(SponsorCategories.parse("sponsor, intro, sponsor")).containsExactly("sponsor", "intro").inOrder()
        assertThat(SponsorCategories.serialize(emptyList())).isEmpty()
        assertThat(SponsorCategories.DEFAULT_ENABLED).containsExactly(SponsorCategories.SPONSOR)
    }
}
