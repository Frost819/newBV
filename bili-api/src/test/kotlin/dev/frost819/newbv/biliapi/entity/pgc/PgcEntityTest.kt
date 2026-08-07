package dev.frost819.newbv.biliapi.entity.pgc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.SeasonIndexType
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedV3Data
import org.junit.jupiter.api.Test
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedData as HttpPgcFeedData

/**
 * [PgcItem]、[PgcFeedData] 实体 HTTP→Domain 转换方法的单元测试。
 */
class PgcEntityTest {
    // ------------------------------------------------------------------
    // PgcItem.fromFeedSubItem(PgcFeedData.FeedSubItem)
    // ------------------------------------------------------------------

    @Test
    fun `PgcItem fromFeedSubItem PgcFeed maps all fields with rating`() {
        val httpSubItem =
            HttpPgcFeedData.FeedSubItem(
                cover = "https://example.com/cover.jpg",
                episodeId = 800001,
                rankId = 1,
                rating = "9.5",
                seasonId = 40000,
                seasonType = 1,
                subTitle = " subtitle",
                title = "番剧标题",
            )

        val item = PgcItem.fromFeedSubItem(httpSubItem)

        assertThat(item.cover).isEqualTo("https://example.com/cover.jpg")
        assertThat(item.title).isEqualTo("番剧标题")
        assertThat(item.subTitle).isEqualTo(" subtitle")
        assertThat(item.seasonId).isEqualTo(40000)
        assertThat(item.episodeId).isEqualTo(800001)
        assertThat(item.seasonType).isEqualTo(SeasonIndexType.Anime)
        assertThat(item.rating).isEqualTo("9.5")
    }

    @Test
    fun `PgcItem fromFeedSubItem PgcFeed uses default rating when null`() {
        val httpSubItem =
            HttpPgcFeedData.FeedSubItem(
                cover = "cover.jpg",
                episodeId = 1,
                rankId = 1,
                rating = null,
                seasonId = 100,
                seasonType = 2,
                subTitle = "",
                title = "电影",
            )

        val item = PgcItem.fromFeedSubItem(httpSubItem)

        assertThat(item.rating).isEqualTo("0")
        assertThat(item.seasonType).isEqualTo(SeasonIndexType.Movie)
    }

    // ------------------------------------------------------------------
    // PgcItem.fromFeedSubItem(PgcFeedV3Data.FeedItem.FeedSubItem)
    // ------------------------------------------------------------------

    @Test
    fun `PgcItem fromFeedSubItem PgcFeedV3 maps all fields with episodeId`() {
        val httpSubItem =
            fakeV3SubItem(
                episodeId = 800001,
                seasonId = 40000,
                seasonType = 1,
                rating = "9.0",
                cardStyle = "v_card",
            )

        val item = PgcItem.fromFeedSubItem(httpSubItem)

        assertThat(item.cover).isEqualTo("https://example.com/v3_cover.jpg")
        assertThat(item.title).isEqualTo("V3番剧")
        assertThat(item.seasonId).isEqualTo(40000)
        assertThat(item.episodeId).isEqualTo(800001)
        assertThat(item.seasonType).isEqualTo(SeasonIndexType.Anime)
        assertThat(item.rating).isEqualTo("9.0")
    }

    @Test
    fun `PgcItem fromFeedSubItem PgcFeedV3 falls back to inline epId when episodeId is null`() {
        val httpSubItem =
            fakeV3SubItem(
                episodeId = null,
                seasonId = 40000,
                seasonType = 1,
                rating = null,
                cardStyle = "v_card",
                inline =
                    PgcFeedV3Data.FeedItem.FeedSubItem.Inline(
                        endTime = null,
                        epId = 800002,
                        firstEp = 1,
                        materialNo = null,
                        scene = 1,
                        startTime = null,
                    ),
            )

        val item = PgcItem.fromFeedSubItem(httpSubItem)

        assertThat(item.episodeId).isEqualTo(800002)
        assertThat(item.rating).isEqualTo("0")
    }

    // ------------------------------------------------------------------
    // PgcFeedData.fromPgcFeedData(PgcFeedData)
    // ------------------------------------------------------------------

    @Test
    fun `PgcFeedData fromPgcFeedData maps hasNext cursor and items`() {
        val httpFeed =
            HttpPgcFeedData(
                coursor = 5,
                hasNext = true,
                items =
                    listOf(
                        HttpPgcFeedData.FeedSubItem(
                            cover = "cover1.jpg",
                            episodeId = 1,
                            rankId = 1,
                            rating = "8.0",
                            seasonId = 100,
                            seasonType = 1,
                            subTitle = "sub1",
                            title = "title1",
                        ),
                        HttpPgcFeedData.FeedSubItem(
                            cover = "cover2.jpg",
                            episodeId = 2,
                            rankId = 2,
                            rating = null,
                            seasonId = 200,
                            seasonType = 2,
                            subTitle = "sub2",
                            title = "title2",
                        ),
                    ),
            )

        val feed = PgcFeedData.fromPgcFeedData(httpFeed)

        assertThat(feed.hasNext).isTrue()
        assertThat(feed.cursor).isEqualTo(5)
        assertThat(feed.items).hasSize(2)
        assertThat(feed.items[0].title).isEqualTo("title1")
        assertThat(feed.items[1].rating).isEqualTo("0")
        assertThat(feed.ranks).isEmpty()
    }

    // ------------------------------------------------------------------
    // PgcFeedData.fromPgcFeedData(PgcFeedV3Data)
    // ------------------------------------------------------------------

    @Test
    fun `PgcFeedData fromPgcFeedV3Data separates v_card items and rank items`() {
        val httpFeed =
            PgcFeedV3Data(
                coursor = 10,
                hasNext = false,
                items =
                    listOf(
                        PgcFeedV3Data.FeedItem(
                            rankId = 1,
                            subItems =
                                listOf(
                                    fakeV3SubItem(cardStyle = "v_card", title = "卡片1"),
                                    fakeV3SubItem(cardStyle = "v_card", title = "卡片2"),
                                ),
                        ),
                        PgcFeedV3Data.FeedItem(
                            rankId = 2,
                            subItems =
                                listOf(
                                    fakeV3SubItem(cardStyle = "rank", title = "排行1"),
                                ),
                        ),
                    ),
            )

        val feed = PgcFeedData.fromPgcFeedData(httpFeed)

        assertThat(feed.hasNext).isFalse()
        assertThat(feed.cursor).isEqualTo(10)
        assertThat(feed.items).hasSize(2)
        assertThat(feed.items[0].title).isEqualTo("卡片1")
        assertThat(feed.items[1].title).isEqualTo("卡片2")
        assertThat(feed.ranks).hasSize(1)
        assertThat(feed.ranks[0].title).isEqualTo("排行1")
    }

    @Test
    fun `PgcFeedData fromPgcFeedV3Data with empty items returns empty lists`() {
        val httpFeed =
            PgcFeedV3Data(
                coursor = 0,
                hasNext = false,
                items = emptyList(),
            )

        val feed = PgcFeedData.fromPgcFeedData(httpFeed)

        assertThat(feed.items).isEmpty()
        assertThat(feed.ranks).isEmpty()
    }

    // ------------------------------------------------------------------
    // PgcItem.fromIndexResultItem
    // ------------------------------------------------------------------

    @Test
    fun `PgcItem fromIndexResultItem maps all fields`() {
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
                score = "9.5",
                seasonId = 400,
                seasonStatus = 1,
                seasonType = 1,
                subTitle = "测试副标题",
                title = "测试番剧",
                titleIcon = "",
            )

        val item = PgcItem.fromIndexResultItem(httpItem)

        assertThat(item.cover).isEqualTo("http://cover.test")
        assertThat(item.title).isEqualTo("测试番剧")
        assertThat(item.subTitle).isEqualTo("测试副标题")
        assertThat(item.seasonId).isEqualTo(400)
        assertThat(item.episodeId).isEqualTo(500)
        assertThat(item.seasonType).isEqualTo(SeasonIndexType.Anime)
        assertThat(item.rating).isEqualTo("9.5")
    }

    // ------------------------------------------------------------------
    // PgcFeedData.FeedRank.fromFeedSubItem
    // ------------------------------------------------------------------

    @Test
    fun `FeedRank fromFeedSubItem with null subItems returns empty items list`() {
        val httpSubItem =
            fakeV3SubItem(
                cardStyle = "rank",
                title = "排行榜",
                cover = "http://rank-cover.test",
            )

        val rank = PgcFeedData.FeedRank.fromFeedSubItem(httpSubItem)

        assertThat(rank.cover).isEqualTo("http://rank-cover.test")
        assertThat(rank.title).isEqualTo("排行榜")
        assertThat(rank.items).isEmpty()
    }

    @Test
    fun `FeedRank fromFeedSubItem with subItems maps to PgcItems`() {
        val subItem1 =
            fakeV3SubItem(
                cardStyle = "v_card",
                title = "子项1",
                seasonId = 100,
                seasonType = 1,
            )
        val rankSubItem =
            PgcFeedV3Data.FeedItem.FeedSubItem(
                cardStyle = "rank",
                cover = "http://rank.test",
                episodeId = null,
                evaluate = null,
                hover = null,
                inline = null,
                link = null,
                rankId = 1,
                rating = "8.0",
                ratingCount = null,
                report = PgcFeedV3Data.FeedItem.FeedSubItem.Report(firstEp = null, scene = null),
                seasonId = null,
                seasonType = null,
                stat = null,
                subItems =
                    listOf(subItem1),
                subTitle = "排行副标题",
                text = null,
                title = "排行榜",
                userStatus = null,
            )

        val rank = PgcFeedData.FeedRank.fromFeedSubItem(rankSubItem)

        assertThat(rank.items).hasSize(1)
        assertThat(rank.items[0].title).isEqualTo("子项1")
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun fakeV3SubItem(
        cardStyle: String = "v_card",
        cover: String = "https://example.com/v3_cover.jpg",
        episodeId: Int? = 800001,
        seasonId: Int? = 40000,
        seasonType: Int? = 1,
        rating: String? = "9.0",
        title: String = "V3番剧",
        subTitle: String = "V3副标题",
        inline: PgcFeedV3Data.FeedItem.FeedSubItem.Inline? = null,
    ) = PgcFeedV3Data.FeedItem.FeedSubItem(
        cardStyle = cardStyle,
        cover = cover,
        episodeId = episodeId,
        evaluate = null,
        hover = null,
        inline = inline,
        link = null,
        rankId = 1,
        rating = rating,
        ratingCount = null,
        report = PgcFeedV3Data.FeedItem.FeedSubItem.Report(firstEp = null, scene = null),
        seasonId = seasonId,
        seasonType = seasonType,
        stat = null,
        subItems = null,
        subTitle = subTitle,
        text = null,
        title = title,
        userStatus = null,
    )
}
