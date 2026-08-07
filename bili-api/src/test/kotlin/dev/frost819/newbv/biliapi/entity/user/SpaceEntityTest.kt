package dev.frost819.newbv.biliapi.entity.user

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SpaceVideoData]、[SpaceVideo]、[SpaceVideoPage] 实体 HTTP→Domain 转换方法的单元测试。
 */
class SpaceEntityTest {
    @Test
    fun `fromWebSpaceVideoData maps vlist items to SpaceVideo`() {
        val webData =
            dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData(
                list =
                    dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem(
                        vlist =
                            listOf(
                                fakeVListItem(aid = 100L, title = "视频1"),
                                fakeVListItem(aid = 200L, title = "视频2"),
                            ),
                    ),
                page =
                    dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.Page(
                        pageNumber = 1,
                        pageSize = 20,
                        count = 50,
                    ),
            )

        val result = SpaceVideoData.fromWebSpaceVideoData(webData)

        assertThat(result.videos).hasSize(2)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].title).isEqualTo("视频1")
        assertThat(result.videos[0].duration).isEqualTo(180)
        assertThat(result.videos[1].aid).isEqualTo(200L)
        assertThat(result.page.hasNext).isTrue()
        assertThat(result.page.nextWebPageNumber).isEqualTo(2)
        assertThat(result.page.nextWebPageSize).isEqualTo(20)
    }

    @Test
    fun `fromWebSpaceVideoData with null list returns empty videos`() {
        val webData = dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData()

        val result = SpaceVideoData.fromWebSpaceVideoData(webData)

        assertThat(result.videos).isEmpty()
        assertThat(result.page.hasNext).isFalse()
    }

    @Test
    fun `fromWebSpaceVideoData with count less than page size sets hasNext false`() {
        val webData =
            dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData(
                list =
                    dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem(
                        vlist = listOf(fakeVListItem()),
                    ),
                page =
                    dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.Page(
                        pageNumber = 3,
                        pageSize = 20,
                        count = 50,
                    ),
            )

        val result = SpaceVideoData.fromWebSpaceVideoData(webData)

        assertThat(result.page.hasNext).isFalse()
        assertThat(result.page.nextWebPageNumber).isEqualTo(4)
    }

    @Test
    fun `fromAppSpaceVideoData maps items and hasNext`() {
        val appData =
            dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData(
                count = 10,
                item = listOf(fakeAppSpaceItem(param = "100", title = "app-video")),
                lastWatchedLocator =
                    dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData.LastWatchedLocator(
                        displayThreshold = 0,
                        insertRanking = 0,
                        text = "",
                    ),
                hasNext = true,
            )

        val result = SpaceVideoData.fromAppSpaceVideoData(appData)

        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].title).isEqualTo("app-video")
        assertThat(result.page.hasNext).isTrue()
        assertThat(result.page.lastAvid).isEqualTo(100L)
    }

    @Test
    fun `fromAppSpaceVideoData with empty items sets lastAvid to 0`() {
        val appData =
            dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData(
                count = 0,
                item = emptyList(),
                lastWatchedLocator =
                    dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData.LastWatchedLocator(
                        displayThreshold = 0,
                        insertRanking = 0,
                        text = "",
                    ),
                hasNext = false,
            )

        val result = SpaceVideoData.fromAppSpaceVideoData(appData)

        assertThat(result.videos).isEmpty()
        assertThat(result.page.hasNext).isFalse()
        assertThat(result.page.lastAvid).isEqualTo(0L)
    }

    @Test
    fun `SpaceVideoOrder has PubDate and Click`() {
        assertThat(SpaceVideoOrder.PubDate.value).isEqualTo("pubdate")
        assertThat(SpaceVideoOrder.Click.value).isEqualTo("click")
    }

    @Test
    fun `SpaceVideoPage defaults are correct`() {
        val page = SpaceVideoPage()
        assertThat(page.hasNext).isTrue()
        assertThat(page.nextWebPageSize).isEqualTo(20)
        assertThat(page.nextWebPageNumber).isEqualTo(1)
        assertThat(page.lastAvid).isEqualTo(0L)
    }

    private fun fakeVListItem(
        aid: Long = 1L,
        title: String = "title",
    ) = dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem.VListItem(
        aid = aid,
        bvid = "BV$aid",
        author = "up",
        comment = 10,
        copyright = "1",
        created = 1700000000L,
        description = "desc",
        hideClick = false,
        isPay = 0,
        isUnionVideo = 0,
        length = "03:00",
        mid = 1L,
        pic = "http://pic.test/$aid",
        play = 1000,
        review = 5,
        subtitle = "",
        title = title,
        typeid = 1,
        videoReview = 50,
        isSteinsGate = 0,
        isLivePlayback = 0,
        _isAvoided = 0,
        attribute = 0,
    )

    private fun fakeAppSpaceItem(
        param: String = "1",
        title: String = "title",
    ) = dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData.SpaceVideoItem(
        title = title,
        subtitle = "",
        tname = "",
        cover = "http://cover.test",
        uri = "",
        param = param,
        goto = "av",
        length = "",
        duration = 120,
        isPopular = false,
        isSteins = false,
        isUgcpay = false,
        isCooperation = false,
        isPgc = false,
        isLivePlayback = false,
        play = 500,
        danmaku = 20,
        ctime = 1700000000,
        ugcPay = 0,
        author = "up",
        state = false,
        bvid = "BV$param",
        videos = 1,
        cursorAttr =
            dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData.SpaceVideoItem.CursorAttr(
                isLastWatchedArc = false,
                rank = 0,
            ),
        iconType = 0,
    )
}
