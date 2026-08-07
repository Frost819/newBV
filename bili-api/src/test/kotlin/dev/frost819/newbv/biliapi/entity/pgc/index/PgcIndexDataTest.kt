package dev.frost819.newbv.biliapi.entity.pgc.index

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [PgcIndexData] 实体 HTTP→Domain 转换方法的单元测试。
 */
class PgcIndexDataTest {
    @Test
    fun `fromIndexResultData maps list and page fields`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem(
                badge = "会员",
                badgeInfo =
                    dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem.BadgeInfo(
                        bgColor = "#FB7299",
                        bgColorNight = "#BB5C7B",
                        text = "会员",
                    ),
                badgeType = 1,
                cover = "http://cover.test",
                firstEp =
                    dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem.FirstEp(
                        cover = "http://ep.test",
                        epId = 500,
                    ),
                indexShow = "全24话",
                isFinish = 1,
                link = "http://link.test",
                mediaId = 100,
                order = "1",
                orderType = "click",
                score = "9.0",
                seasonId = 400,
                seasonStatus = 1,
                seasonType = 1,
                subTitle = "测试副标题",
                title = "测试番剧",
                titleIcon = "",
            )
        val httpData =
            dev.frost819.newbv.biliapi.http.entity.index.IndexResultData(
                hasNext = 1,
                list = listOf(httpItem),
                num = 1,
                size = 20,
                total = 200,
            )

        val result = PgcIndexData.fromIndexResultData(httpData)

        assertThat(result.list).hasSize(1)
        assertThat(result.nextPage.currentPage).isEqualTo(1)
        assertThat(result.nextPage.pageSize).isEqualTo(20)
        assertThat(result.nextPage.totalSize).isEqualTo(200)
        assertThat(result.nextPage.nextPage).isEqualTo(2)
        assertThat(result.nextPage.hasNext).isTrue()
    }

    @Test
    fun `fromIndexResultData with hasNext 0 sets hasNext false`() {
        val httpData =
            dev.frost819.newbv.biliapi.http.entity.index.IndexResultData(
                hasNext = 0,
                list = emptyList(),
                num = 3,
                size = 30,
                total = 50,
            )

        val result = PgcIndexData.fromIndexResultData(httpData)

        assertThat(result.nextPage.hasNext).isFalse()
        assertThat(result.nextPage.currentPage).isEqualTo(3)
        assertThat(result.nextPage.pageSize).isEqualTo(30)
        assertThat(result.nextPage.totalSize).isEqualTo(50)
        assertThat(result.nextPage.nextPage).isEqualTo(4)
    }

    @Test
    fun `PgcIndexPage defaults are correct`() {
        val page = PgcIndexData.PgcIndexPage()
        assertThat(page.currentPage).isEqualTo(1)
        assertThat(page.pageSize).isEqualTo(20)
        assertThat(page.totalSize).isEqualTo(0)
        assertThat(page.nextPage).isEqualTo(1)
        assertThat(page.hasNext).isTrue()
    }
}
