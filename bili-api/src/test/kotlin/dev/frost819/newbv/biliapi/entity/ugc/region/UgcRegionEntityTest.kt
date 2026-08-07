package dev.frost819.newbv.biliapi.entity.ugc.region

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UgcFeedData]、[UgcFeedPage]、[UgcRegionPage] 实体的单元测试。
 */
class UgcRegionEntityTest {
    @Test
    fun `UgcFeedPage default nextPage is 1`() {
        val page = UgcFeedPage()
        assertThat(page.nextPage).isEqualTo(1)
    }

    @Test
    fun `UgcFeedPage with custom nextPage`() {
        val page = UgcFeedPage(nextPage = 5)
        assertThat(page.nextPage).isEqualTo(5)
    }

    @Test
    fun `UgcRegionPage default nextPage is 0`() {
        val page = UgcRegionPage()
        assertThat(page.nextPage).isEqualTo(0L)
    }

    @Test
    fun `UgcRegionPage with custom nextPage`() {
        @Suppress("DEPRECATION")
        val page = UgcRegionPage(nextPage = 100L)
        assertThat(page.nextPage).isEqualTo(100L)
    }

    @Test
    fun `UgcFeedData defaults are correct`() {
        val data = UgcFeedData(hasNext = true, nextPage = UgcFeedPage())
        assertThat(data.hasNext).isTrue()
        assertThat(data.items).isEmpty()
        assertThat(data.nextPage.nextPage).isEqualTo(1)
    }

    @Test
    fun `UgcFeedData with items and custom page`() {
        val data =
            UgcFeedData(
                hasNext = false,
                nextPage = UgcFeedPage(nextPage = 3),
                items = listOf(),
            )
        assertThat(data.hasNext).isFalse()
        assertThat(data.nextPage.nextPage).isEqualTo(3)
    }
}
