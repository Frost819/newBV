package dev.frost819.newbv.biliapi.entity.rank

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import org.junit.jupiter.api.Test

/**
 * [PopularVideoData] 与 [PopularVideoPage] 实体的单元测试。
 */
class PopularTest {
    @Test
    fun `PopularVideoData holds list and page`() {
        val items = listOf(fakeUgcItem(1L), fakeUgcItem(2L))
        val page = PopularVideoPage()
        val data = PopularVideoData(list = items, nextPage = page, noMore = false)

        assertThat(data.list).hasSize(2)
        assertThat(data.noMore).isFalse()
        assertThat(data.nextPage).isEqualTo(page)
    }

    @Test
    fun `PopularVideoPage defaults are correct`() {
        val page = PopularVideoPage()

        assertThat(page.nextWebPageSize).isEqualTo(20)
        assertThat(page.nextWebPageNumber).isEqualTo(1)
        assertThat(page.nextAppIndex).isEqualTo(0)
    }

    @Test
    fun `PopularVideoPage with custom values`() {
        val page = PopularVideoPage(nextWebPageSize = 30, nextWebPageNumber = 5, nextAppIndex = 100)

        assertThat(page.nextWebPageSize).isEqualTo(30)
        assertThat(page.nextWebPageNumber).isEqualTo(5)
        assertThat(page.nextAppIndex).isEqualTo(100)
    }

    @Test
    fun `PopularVideoData with empty list and noMore true`() {
        val data = PopularVideoData(list = emptyList(), nextPage = PopularVideoPage(), noMore = true)

        assertThat(data.list).isEmpty()
        assertThat(data.noMore).isTrue()
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
