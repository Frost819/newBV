package dev.frost819.newbv.biliapi.entity

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcWebInitialStateData
import dev.frost819.newbv.biliapi.http.entity.region.RegionDynamic
import dev.frost819.newbv.biliapi.http.entity.region.RegionLocs
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Test

/**
 * [CarouselData] 实体转换方法的单元测试。
 *
 * 覆盖 `fromPgcWebInitialStateData`（PGC 轮播图）、
 * `fromUgcRegionDynamicBanner`（UGC 动态 Banner）以及
 * `fromUgcRegionLocs`（UGC Locs 广告位）三条转换路径。
 */
class CarouselDataTest {
    @Test
    fun `fromPgcWebInitialStateData maps anime banner with direct seasonId and episodeId`() {
        val initialState =
            PgcWebInitialStateData(
                modules =
                    PgcWebInitialStateData.Modules(
                        banner =
                            PgcWebInitialStateData.Modules.Banner(
                                title = "番剧",
                                spmid = "",
                                size = 5,
                                style = "v_card",
                                headers = JsonArray(emptyList()),
                                items =
                                    listOf(
                                        fakeBannerItem(
                                            title = "番剧1",
                                            cover = "https://example.com/cover1.jpg",
                                            seasonId = 40000,
                                            episodeId = 800001,
                                            bigCover = "https://example.com/big1.jpg",
                                            id = "1",
                                        ),
                                        fakeBannerItem(
                                            title = "番剧2",
                                            cover = "https://example.com/cover2.jpg",
                                            seasonId = 40001,
                                            episodeId = 800002,
                                            bigCover = null,
                                            id = "2",
                                        ),
                                    ),
                                wids = JsonArray(emptyList()),
                                moduleId = 1668,
                            ),
                    ),
            )

        val carousel = CarouselData.fromPgcWebInitialStateData(initialState)

        assertThat(carousel.items).hasSize(2)
        assertThat(carousel.items[0].title).isEqualTo("番剧1")
        assertThat(carousel.items[0].seasonId).isEqualTo(40000)
        assertThat(carousel.items[0].episodeId).isEqualTo(800001)
        assertThat(carousel.items[0].cover).isEqualTo("https://example.com/big1.jpg")
    }

    @Test
    fun `fromPgcWebInitialStateData parses seasonId and episodeId from URL for movie type`() {
        val initialState =
            PgcWebInitialStateData(
                modules =
                    PgcWebInitialStateData.Modules(
                        banner =
                            PgcWebInitialStateData.Modules.Banner(
                                title = "电影",
                                spmid = "",
                                size = 3,
                                style = "v_card",
                                headers = JsonArray(emptyList()),
                                items =
                                    listOf(
                                        fakeBannerItem(
                                            title = "电影1",
                                            cover = "https://example.com/movie1.jpg",
                                            seasonId = null,
                                            episodeId = null,
                                            bigCover = null,
                                            link = "https://www.bilibili.com/bangumi/play/ep800003",
                                            id = "3",
                                        ),
                                        fakeBannerItem(
                                            title = "剧集2",
                                            cover = "https://example.com/movie2.jpg",
                                            seasonId = null,
                                            episodeId = null,
                                            bigCover = null,
                                            link = "https://www.bilibili.com/bangumi/play/ss40002",
                                            id = "4",
                                        ),
                                    ),
                                wids = JsonArray(emptyList()),
                                moduleId = 1675,
                            ),
                    ),
            )

        val carousel = CarouselData.fromPgcWebInitialStateData(initialState)

        assertThat(carousel.items).hasSize(2)
        assertThat(carousel.items[0].episodeId).isEqualTo(800003)
        assertThat(carousel.items[0].seasonId).isEqualTo(-1)
        assertThat(carousel.items[1].seasonId).isEqualTo(40002)
        assertThat(carousel.items[1].episodeId).isEqualTo(-1)
    }

    @Test
    fun `fromPgcWebInitialStateData filters items without episodeId or seasonId for non-movie types`() {
        val initialState =
            PgcWebInitialStateData(
                modules =
                    PgcWebInitialStateData.Modules(
                        banner =
                            PgcWebInitialStateData.Modules.Banner(
                                title = "番剧",
                                spmid = "",
                                size = 3,
                                style = "v_card",
                                headers = JsonArray(emptyList()),
                                items =
                                    listOf(
                                        fakeBannerItem(
                                            title = "有ep",
                                            cover = "cover1.jpg",
                                            seasonId = 100,
                                            episodeId = 200,
                                            id = "1",
                                        ),
                                        fakeBannerItem(
                                            title = "无ep无ss",
                                            cover = "cover2.jpg",
                                            seasonId = null,
                                            episodeId = null,
                                            link = "https://www.bilibili.com/some/other",
                                            id = "2",
                                        ),
                                    ),
                                wids = JsonArray(emptyList()),
                                moduleId = 1668,
                            ),
                    ),
            )

        val carousel = CarouselData.fromPgcWebInitialStateData(initialState)

        assertThat(carousel.items).hasSize(1)
        assertThat(carousel.items[0].title).isEqualTo("有ep")
    }

    @Test
    fun `fromPgcWebInitialStateData prepends https to protocol-relative cover URL`() {
        val initialState =
            PgcWebInitialStateData(
                modules =
                    PgcWebInitialStateData.Modules(
                        banner =
                            PgcWebInitialStateData.Modules.Banner(
                                title = "番剧",
                                spmid = "",
                                size = 1,
                                style = "v_card",
                                headers = JsonArray(emptyList()),
                                items =
                                    listOf(
                                        fakeBannerItem(
                                            title = "测试",
                                            cover = "//example.com/cover.jpg",
                                            seasonId = 100,
                                            episodeId = 200,
                                            bigCover = "//example.com/big.jpg",
                                            id = "1",
                                        ),
                                    ),
                                wids = JsonArray(emptyList()),
                                moduleId = 1668,
                            ),
                    ),
            )

        val carousel = CarouselData.fromPgcWebInitialStateData(initialState)

        assertThat(carousel.items[0].cover).isEqualTo("https://example.com/big.jpg")
    }

    private fun fakeBannerItem(
        title: String,
        cover: String,
        seasonId: Int? = null,
        episodeId: Int? = null,
        bigCover: String? = null,
        link: String = "https://www.bilibili.com/bangumi/play/ep1",
        id: String,
    ) = PgcWebInitialStateData.Modules.Banner.BannerItem(
        title = title,
        cover = cover,
        link = link,
        rankId = 0,
        id = id,
        showReportData =
            PgcWebInitialStateData.Modules.Banner.BannerItem.ShowReportData(
                moduleType = "banner",
                moduleId = 0,
            ),
        seasonId = seasonId,
        episodeId = episodeId,
        bigCover = bigCover,
    )

    // region ---- fromUgcRegionDynamicBanner ----

    @Test
    fun `fromUgcRegionDynamicBanner maps video URLs to CarouselItems with avid and bvid`() {
        val banner =
            RegionDynamic.Banner(
                top =
                    listOf(
                        RegionDynamic.Banner.Top(
                            clientIp = null,
                            cmMark = 0,
                            hash = "h1",
                            id = 1,
                            image = "http://img.test/1.jpg",
                            index = 0,
                            isAd = null,
                            isAdLoc = null,
                            requestId = "r1",
                            resourceId = 0,
                            serverType = 0,
                            srcId = null,
                            title = "视频1",
                            uri = "https://www.bilibili.com/video/av100",
                        ),
                        RegionDynamic.Banner.Top(
                            clientIp = null,
                            cmMark = 0,
                            hash = "h2",
                            id = 2,
                            image = "http://img.test/2.jpg",
                            index = 1,
                            isAd = null,
                            isAdLoc = null,
                            requestId = "r2",
                            resourceId = 0,
                            serverType = 0,
                            srcId = null,
                            title = "非视频链接",
                            uri = "https://www.bilibili.com/live/123",
                        ),
                    ),
            )

        val result = CarouselData.fromUgcRegionDynamicBanner(banner)

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].title).isEqualTo("视频1")
        assertThat(result.items[0].cover).isEqualTo("http://img.test/1.jpg")
        assertThat(result.items[0].avid).isEqualTo(100L)
        assertThat(result.items[0].bvid).isNotNull()
    }

    @Test
    fun `fromUgcRegionDynamicBanner with no video URLs returns empty list`() {
        val banner =
            RegionDynamic.Banner(
                top =
                    listOf(
                        RegionDynamic.Banner.Top(
                            clientIp = null,
                            cmMark = 0,
                            hash = "h1",
                            id = 1,
                            image = "",
                            index = 0,
                            isAd = null,
                            isAdLoc = null,
                            requestId = "r1",
                            resourceId = 0,
                            serverType = 0,
                            srcId = null,
                            title = "非视频",
                            uri = "https://www.bilibili.com/live/123",
                        ),
                    ),
            )

        val result = CarouselData.fromUgcRegionDynamicBanner(banner)

        assertThat(result.items).isEmpty()
    }

    // endregion

    // region ---- fromUgcRegionLocs ----

    @Test
    fun `fromUgcRegionLocs filters items containing video path and extracts bvid`() {
        val locs =
            RegionLocs(
                adsControl =
                    RegionLocs.AdsControl(
                        hasDanmu = 0,
                        hasLiveBookingAd = false,
                        underPlayerScrollerSeconds = 0,
                    ),
                code = 0,
                count = 2,
                data =
                    mapOf(
                        "slot1" to
                            listOf(
                                fakeLocData(
                                    title = "视频广告1",
                                    url = "https://www.bilibili.com/video/BV1xx",
                                    pic = "http://pic.test/1.jpg",
                                ),
                                fakeLocData(
                                    title = "非视频",
                                    url = "https://www.bilibili.com/live/123",
                                    pic = "http://pic.test/2.jpg",
                                ),
                            ),
                        "slot2" to null,
                    ),
                live = null,
                message = "",
            )

        val result = CarouselData.fromUgcRegionLocs(locs)

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].title).isEqualTo("视频广告1")
        assertThat(result.items[0].cover).isEqualTo("http://pic.test/1.jpg")
        assertThat(result.items[0].bvid).isEqualTo("BV1xx")
    }

    @Test
    fun `fromUgcRegionLocs with empty data returns empty list`() {
        val locs =
            RegionLocs(
                adsControl =
                    RegionLocs.AdsControl(
                        hasDanmu = 0,
                        hasLiveBookingAd = false,
                        underPlayerScrollerSeconds = 0,
                    ),
                code = 0,
                count = 0,
                data = emptyMap(),
                live = null,
                message = "",
            )

        val result = CarouselData.fromUgcRegionLocs(locs)

        assertThat(result.items).isEmpty()
    }

    private fun fakeLocData(
        title: String = "title",
        url: String = "https://www.bilibili.com/video/BV1xx",
        pic: String = "http://pic.test.jpg",
    ) = RegionLocs.LocData(
        activityType = 0,
        adCb = "",
        adDesc = "",
        adverName = "",
        agency = "",
        area = 0,
        asgId = 0,
        businessMark = null,
        cardType = 0,
        clickUrls = null,
        cmMark = 0,
        contractId = "",
        creativeType = 0,
        epId = 0,
        feedbackPanel = null,
        id = 0,
        inline =
            RegionLocs.LocData.Inline(
                inlineBarrageSwitch = 0,
                inlineType = 0,
                inlineUrl = "",
                inlineUseSame = 0,
            ),
        intro = "",
        isAdLoc = false,
        jumpTarget = 0,
        label = "",
        litPic = "",
        mid = "",
        name = "",
        nullFrame = false,
        operater = "",
        pic = pic,
        picMainColor = "",
        posNum = 0,
        requestId = "",
        resId = 0,
        room = null,
        salesType = 0,
        season = null,
        serverType = 0,
        showUrls = null,
        srcId = 0,
        sTime = 0,
        style = 0,
        subTitle = "",
        title = title,
        trackId = "",
        url = url,
    )

    // endregion
}
