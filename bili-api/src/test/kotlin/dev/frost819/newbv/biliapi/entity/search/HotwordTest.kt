package dev.frost819.newbv.biliapi.entity.search

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Hotword] 实体的单元测试。
 *
 * 覆盖 `fromHttpWebHotword`、`fromHttpAppSquareDataItem`、
 * `fromHttpAppSearchTrendingHotword` 三个转换方法的字段映射与 null 处理。
 */
class HotwordTest {
    // ------------------------------------------------------------------
    // fromHttpWebHotword
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpWebHotword maps all fields correctly`() {
        val httpHotword =
            dev.frost819.newbv.biliapi.http.entity.search.Hotword(
                keyword = "test-kw",
                showName = "测试关键词",
                icon = "http://icon.test",
                uri = "http://uri.test",
                goto = "search",
            )

        val result = Hotword.fromHttpWebHotword(httpHotword)

        assertThat(result.keyword).isEqualTo("test-kw")
        assertThat(result.showName).isEqualTo("测试关键词")
        assertThat(result.icon).isEqualTo("http://icon.test")
    }

    @Test
    fun `fromHttpWebHotword preserves icon field`() {
        val httpHotword =
            dev.frost819.newbv.biliapi.http.entity.search.Hotword(
                keyword = "kw",
                showName = "sn",
                icon = "icon-url",
                uri = "",
                goto = "",
            )

        assertThat(Hotword.fromHttpWebHotword(httpHotword).icon).isEqualTo("icon-url")
    }

    // ------------------------------------------------------------------
    // fromHttpAppSquareDataItem
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpAppSquareDataItem maps non-null fields correctly`() {
        val item =
            dev.frost819.newbv.biliapi.http.entity.search.AppSearchSquareData.SquareData.SquareDataItem(
                keyword = "app-kw",
                showName = "App关键词",
                icon = "http://app-icon.test",
                position = 1,
            )

        val result = Hotword.fromHttpAppSquareDataItem(item)

        assertThat(result.keyword).isEqualTo("app-kw")
        assertThat(result.showName).isEqualTo("App关键词")
        assertThat(result.icon).isEqualTo("http://app-icon.test")
    }

    @Test
    fun `fromHttpAppSquareDataItem converts null keyword and showName to empty strings`() {
        val item =
            dev.frost819.newbv.biliapi.http.entity.search.AppSearchSquareData.SquareData.SquareDataItem(
                keyword = null,
                showName = null,
                icon = null,
                position = 5,
            )

        val result = Hotword.fromHttpAppSquareDataItem(item)

        assertThat(result.keyword).isEmpty()
        assertThat(result.showName).isEmpty()
        assertThat(result.icon).isNull()
    }

    // ------------------------------------------------------------------
    // fromHttpAppSearchTrendingHotword
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpAppSearchTrendingHotword maps all fields correctly`() {
        val httpHotword =
            dev.frost819.newbv.biliapi.http.entity.search.SearchTendingData.Hotword(
                position = 3,
                keyword = "trending-kw",
                showName = "热搜词",
                icon = "http://trending-icon.test",
                hotId = 100,
                isCommercial = 0,
            )

        val result = Hotword.fromHttpAppSearchTrendingHotword(httpHotword)

        assertThat(result.keyword).isEqualTo("trending-kw")
        assertThat(result.showName).isEqualTo("热搜词")
        assertThat(result.icon).isEqualTo("http://trending-icon.test")
    }

    @Test
    fun `fromHttpAppSearchTrendingHotword maps null icon correctly`() {
        val httpHotword =
            dev.frost819.newbv.biliapi.http.entity.search.SearchTendingData.Hotword(
                position = 1,
                keyword = "kw",
                showName = "sn",
                icon = null,
                hotId = 1,
                isCommercial = 0,
            )

        val result = Hotword.fromHttpAppSearchTrendingHotword(httpHotword)

        assertThat(result.icon).isNull()
        assertThat(result.keyword).isEqualTo("kw")
    }
}
