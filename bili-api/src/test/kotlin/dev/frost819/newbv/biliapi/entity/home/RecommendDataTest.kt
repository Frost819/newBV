package dev.frost819.newbv.biliapi.entity.home

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import org.junit.jupiter.api.Test

/**
 * [RecommendData] 与 [RecommendPage] 实体的单元测试。
 */
class RecommendDataTest {
    @Test
    fun `RecommendData holds items and page`() {
        val items = listOf(fakeUgcItem(1L), fakeUgcItem(2L))
        val page = RecommendPage()
        val data = RecommendData(items = items, nextPage = page)

        assertThat(data.items).hasSize(2)
        assertThat(data.nextPage).isEqualTo(page)
    }

    @Test
    fun `RecommendPage defaults are correct`() {
        val page = RecommendPage()

        assertThat(page.nextWebIdx).isEqualTo(1)
        assertThat(page.nextAppIdx).isEqualTo(0)
    }

    @Test
    fun `RecommendPage with custom values`() {
        val page = RecommendPage(nextWebIdx = 10, nextAppIdx = 99)

        assertThat(page.nextWebIdx).isEqualTo(10)
        assertThat(page.nextAppIdx).isEqualTo(99)
    }

    @Test
    fun `RecommendData with empty items`() {
        val data = RecommendData(items = emptyList(), nextPage = RecommendPage())

        assertThat(data.items).isEmpty()
    }

    private fun fakeUgcItem(aid: Long) =
        UgcItem(
            aid = aid,
            bvid = "BV$aid",
            title = "title-$aid",
            cover = "http://cover.test/$aid",
            author = "up",
            authorMid = 1L,
            play = 100,
            danmaku = 10,
            duration = 120,
        )
}
