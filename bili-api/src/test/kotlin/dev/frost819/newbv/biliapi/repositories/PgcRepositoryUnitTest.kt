package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.SeasonIndexType
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedData
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedV3Data
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcWebInitialStateData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PgcRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证轮播图获取和 PGC Feed 获取逻辑。
 * 不依赖真实网络。
 */
class PgcRepositoryUnitTest {
    private lateinit var repository: PgcRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        repository = PgcRepository()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getCarousel
    // ------------------------------------------------------------------

    @Test
    fun `getCarousel maps PgcWebInitialStateData to CarouselData`() =
        runTest {
            coEvery { BiliHttpApi.getPgcWebInitialStateData(any()) } returns fakeInitialStateData()

            val result = repository.getCarousel(PgcType.Anime)

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].title).isEqualTo("番剧轮播")
            assertThat(result.items[0].seasonId).isEqualTo(40000)
            assertThat(result.items[0].episodeId).isEqualTo(800001)
        }

    @Test
    fun `getCarousel passes pgcType to API`() =
        runTest {
            coEvery { BiliHttpApi.getPgcWebInitialStateData(any()) } returns fakeInitialStateData()

            repository.getCarousel(PgcType.Movie)

            coVerify { BiliHttpApi.getPgcWebInitialStateData(PgcType.Movie) }
        }

    // ------------------------------------------------------------------
    // getFeed (Anime/GuoChuang → getPgcFeedV3)
    // ------------------------------------------------------------------

    @Test
    fun `getFeed Anime uses getPgcFeedV3 and maps items`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeedV3(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        PgcFeedV3Data(
                            coursor = 5,
                            hasNext = true,
                            items =
                                listOf(
                                    PgcFeedV3Data.FeedItem(
                                        rankId = 1,
                                        subItems =
                                            listOf(
                                                fakeV3SubItem(title = "番剧A", cardStyle = "v_card"),
                                            ),
                                    ),
                                ),
                        ),
                )

            val result = repository.getFeed(PgcType.Anime, cursor = 0)

            assertThat(result.hasNext).isTrue()
            assertThat(result.cursor).isEqualTo(5)
            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].title).isEqualTo("番剧A")
        }

    @Test
    fun `getFeed GuoChuang uses getPgcFeedV3 with lowercase name`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeedV3(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        PgcFeedV3Data(
                            coursor = 0,
                            hasNext = false,
                            items = emptyList(),
                        ),
                )

            repository.getFeed(PgcType.GuoChuang, cursor = 0)

            coVerify { BiliHttpApi.getPgcFeedV3(name = "guochuang", cursor = 0) }
        }

    // ------------------------------------------------------------------
    // getFeed (Movie/Tv/Documentary/Variety → getPgcFeed)
    // ------------------------------------------------------------------

    @Test
    fun `getFeed Movie uses getPgcFeed and maps items`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeed(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        PgcFeedData(
                            coursor = 3,
                            hasNext = true,
                            items =
                                listOf(
                                    PgcFeedData.FeedSubItem(
                                        cover = "cover.jpg",
                                        episodeId = 1,
                                        rankId = 1,
                                        rating = "8.5",
                                        seasonId = 100,
                                        seasonType = 2,
                                        subTitle = "副标题",
                                        title = "电影A",
                                    ),
                                ),
                        ),
                )

            val result = repository.getFeed(PgcType.Movie, cursor = 0)

            assertThat(result.hasNext).isTrue()
            assertThat(result.cursor).isEqualTo(3)
            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].title).isEqualTo("电影A")
            assertThat(result.items[0].seasonType).isEqualTo(SeasonIndexType.Movie)
        }

    @Test
    fun `getFeed Documentary uses getPgcFeed with lowercase name`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeed(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = PgcFeedData(coursor = 0, hasNext = false, items = emptyList()),
                )

            repository.getFeed(PgcType.Documentary, cursor = 5)

            coVerify { BiliHttpApi.getPgcFeed(name = "documentary", cursor = 5) }
        }

    @Test
    fun `getFeed Tv uses getPgcFeed with lowercase name`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeed(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = PgcFeedData(coursor = 0, hasNext = false, items = emptyList()),
                )

            repository.getFeed(PgcType.Tv, cursor = 3)

            coVerify { BiliHttpApi.getPgcFeed(name = "tv", cursor = 3) }
        }

    @Test
    fun `getFeed Variety uses getPgcFeed with lowercase name`() =
        runTest {
            coEvery { BiliHttpApi.getPgcFeed(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = PgcFeedData(coursor = 0, hasNext = false, items = emptyList()),
                )

            repository.getFeed(PgcType.Variety, cursor = 7)

            coVerify { BiliHttpApi.getPgcFeed(name = "variety", cursor = 7) }
        }

    // ------------------------------------------------------------------
    // getPgcIndex
    // ------------------------------------------------------------------

    @Test
    fun `getPgcIndex Anime calls seasonIndexAnimeResult and maps data`() =
        runTest {
            coEvery {
                BiliHttpApi.seasonIndexAnimeResult(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Anime,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
            assertThat(result.list[0].seasonId).isEqualTo(400)
            assertThat(result.nextPage.hasNext).isTrue()
            assertThat(result.nextPage.nextPage).isEqualTo(2)
        }

    @Test
    fun `getPgcIndex GuoChuang calls seasonIndexGuochuangResult`() =
        runTest {
            coEvery {
                BiliHttpApi.seasonIndexGuochuangResult(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.GuoChuang,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
        }

    @Test
    fun `getPgcIndex Movie calls seasonIndexMovieResult`() =
        runTest {
            coEvery {
                BiliHttpApi.seasonIndexMovieResult(any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Movie,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
        }

    @Test
    fun `getPgcIndex Documentary calls seasonIndexDocumentaryResult`() =
        runTest {
            coEvery {
                BiliHttpApi.seasonIndexDocumentaryResult(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Documentary,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
        }

    @Test
    fun `getPgcIndex Tv calls seasonIndexTvResult`() =
        runTest {
            coEvery { BiliHttpApi.seasonIndexTvResult(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Tv,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
        }

    @Test
    fun `getPgcIndex Variety calls seasonIndexVarietyResult`() =
        runTest {
            coEvery { BiliHttpApi.seasonIndexVarietyResult(any(), any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData())

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Variety,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.list).hasSize(1)
        }

    @Test
    fun `getPgcIndex maps hasNext false when API returns hasNext 0`() =
        runTest {
            coEvery {
                BiliHttpApi.seasonIndexAnimeResult(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns
                BiliResponse(code = 0, message = "", data = fakeIndexResultData(hasNext = 0))

            val result =
                repository.getPgcIndex(
                    pgcType = PgcType.Anime,
                    indexOrder = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder.values().first(),
                    indexOrderType = dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType.values().first(),
                    seasonVersion = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion.values().first(),
                    spokenLanguage = dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage.values().first(),
                    area = dev.frost819.newbv.biliapi.entity.pgc.index.Area.values().first(),
                    isFinish = dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish.values().first(),
                    copyright = dev.frost819.newbv.biliapi.entity.pgc.index.Copyright.values().first(),
                    seasonStatus = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus.values().first(),
                    seasonMonth = dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth.values().first(),
                    producer = dev.frost819.newbv.biliapi.entity.pgc.index.Producer.values().first(),
                    year = dev.frost819.newbv.biliapi.entity.pgc.index.Year.values().first(),
                    releaseDate = dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate.values().first(),
                    style = dev.frost819.newbv.biliapi.entity.pgc.index.Style.values().first(),
                    page = dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData.PgcIndexPage(),
                )

            assertThat(result.nextPage.hasNext).isFalse()
        }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun fakeIndexResultData(hasNext: Int = 1) =
        dev.frost819.newbv.biliapi.http.entity.index.IndexResultData(
            hasNext = hasNext,
            list =
                listOf(
                    dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem(
                        badge = "",
                        badgeInfo =
                            dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem.BadgeInfo(
                                bgColor = "",
                                bgColorNight = "",
                                text = "",
                            ),
                        badgeType = 0,
                        cover = "http://cover.test",
                        firstEp =
                            dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem.FirstEp(
                                cover = "",
                                epId = 0,
                            ),
                        indexShow = "全12话",
                        isFinish = 1,
                        link = "https://www.bilibili.com/bangumi/play/ep1",
                        mediaId = 100,
                        order = "",
                        orderType = "",
                        score = "9.0",
                        seasonId = 400,
                        seasonStatus = 0,
                        seasonType = 1,
                        subTitle = "副标题",
                        title = "测试番剧",
                        titleIcon = "",
                    ),
                ),
            num = 1,
            size = 20,
            total = 1,
        )

    private fun fakeInitialStateData() =
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
                                    PgcWebInitialStateData.Modules.Banner.BannerItem(
                                        title = "番剧轮播",
                                        cover = "https://example.com/cover.jpg",
                                        link = "https://www.bilibili.com/bangumi/play/ep800001",
                                        rankId = 0,
                                        id = "1",
                                        showReportData =
                                            PgcWebInitialStateData.Modules.Banner.BannerItem.ShowReportData(
                                                moduleType = "banner",
                                                moduleId = 0,
                                            ),
                                        seasonId = 40000,
                                        episodeId = 800001,
                                        bigCover = "https://example.com/big.jpg",
                                    ),
                                ),
                            wids = JsonArray(emptyList()),
                            moduleId = 1668,
                        ),
                ),
        )

    private fun fakeV3SubItem(
        title: String,
        cardStyle: String = "v_card",
    ) = PgcFeedV3Data.FeedItem.FeedSubItem(
        cardStyle = cardStyle,
        cover = "https://example.com/cover.jpg",
        episodeId = 800001,
        evaluate = null,
        hover = null,
        inline = null,
        link = null,
        rankId = 1,
        rating = "9.0",
        ratingCount = null,
        report = PgcFeedV3Data.FeedItem.FeedSubItem.Report(firstEp = null, scene = null),
        seasonId = 40000,
        seasonType = 1,
        stat = null,
        subItems = null,
        subTitle = "副标题",
        text = null,
        title = title,
        userStatus = null,
    )
}
