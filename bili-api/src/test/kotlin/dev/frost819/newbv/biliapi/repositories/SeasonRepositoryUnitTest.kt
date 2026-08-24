package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonStatus
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.biliapi.entity.season.TimelineFilter
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.season.AppFollowingSeason
import dev.frost819.newbv.biliapi.http.entity.season.FollowingSeasonAppData
import dev.frost819.newbv.biliapi.http.entity.season.FollowingSeasonWebData
import dev.frost819.newbv.biliapi.http.entity.season.WebFollowingSeason
import dev.frost819.newbv.biliapi.http.entity.video.Timeline
import dev.frost819.newbv.biliapi.http.entity.video.TimelineAppData
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * [SeasonRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证放送时间表获取的
 * Web / App 路径参数传递与数据转换。不依赖真实网络。
 */
class SeasonRepositoryUnitTest {
    private lateinit var repository: SeasonRepository
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.mid = 12345L
        authRepository.accessToken = "test-access-token"
        repository = SeasonRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getTimeline
    // ------------------------------------------------------------------

    @Test
    fun `getTimeline Web maps timelines correctly`() =
        runTest {
            val httpTimelines =
                listOf(
                    httpTimeline(
                        date = "2024-01-01",
                        dateTs = 1704067200,
                        dayOfWeek = 1,
                        isToday = 1,
                        episodes = listOf(httpEpisode()),
                    ),
                    httpTimeline(
                        date = "2024-01-02",
                        dateTs = 1704153600,
                        dayOfWeek = 2,
                        isToday = 0,
                        episodes = emptyList(),
                    ),
                )
            coEvery { BiliHttpApi.getTimeline(any<Int>(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = httpTimelines)

            val result = repository.getTimeline(filter = TimelineFilter.All, preferApiType = ApiType.Web)

            assertThat(result).hasSize(2)
            assertThat(result[0].dateString).isEqualTo("2024-01-01")
            assertThat(result[0].isToday).isTrue()
            assertThat(result[0].episodes).hasSize(1)
            assertThat(result[0].episodes[0].title).isEqualTo("番剧名")
            assertThat(result[0].episodes[0].seasonId).isEqualTo(400)
            assertThat(result[1].isToday).isFalse()
            assertThat(result[1].episodes).isEmpty()
        }

    @Test
    fun `getTimeline Web passes filter webFilterId with before=7 and after=7`() =
        runTest {
            coEvery { BiliHttpApi.getTimeline(any<Int>(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = emptyList())

            repository.getTimeline(filter = TimelineFilter.Anime, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getTimeline(eq(TimelineFilter.Anime.webFilterId), eq(7), eq(7)) }
        }

    @Test
    fun `getTimeline Web with All filter passes -1`() =
        runTest {
            coEvery { BiliHttpApi.getTimeline(any<Int>(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = emptyList())

            repository.getTimeline(filter = TimelineFilter.All, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getTimeline(eq(-1), any(), any()) }
        }

    @Test
    fun `getTimeline Web returns empty list when no data`() =
        runTest {
            coEvery { BiliHttpApi.getTimeline(any<Int>(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = emptyList())

            val result = repository.getTimeline(filter = TimelineFilter.GuoChuang, preferApiType = ApiType.Web)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getTimeline Web converts dateTs to Date correctly`() =
        runTest {
            val httpTimelines =
                listOf(
                    httpTimeline(
                        date = "2024-06-15",
                        dateTs = 1718400000,
                        dayOfWeek = 6,
                        isToday = 0,
                        episodes = emptyList(),
                    ),
                )
            coEvery { BiliHttpApi.getTimeline(any<Int>(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = httpTimelines)

            val result = repository.getTimeline(filter = TimelineFilter.All, preferApiType = ApiType.Web)

            assertThat(result[0].date.time).isEqualTo(1718400000L * 1000L)
            assertThat(result[0].dayOfWeek).isEqualTo(6)
        }

    // ------------------------------------------------------------------
    // getTimeline (App)
    // ------------------------------------------------------------------

    @Test
    @Disabled("Season timeline is Web-only")
    fun `getTimeline App maps timelines correctly`() =
        runTest {
            val timelineAppData =
                TimelineAppData(
                    currentTimeText = "会一直在你身边的",
                    data =
                        listOf(
                            httpTimeline(
                                date = "2024-01-01",
                                dateTs = 1704067200,
                                dayOfWeek = 1,
                                isToday = 1,
                                episodes = listOf(httpEpisode()),
                            ),
                        ),
                    filter = emptyList(),
                    isNightMode = 0,
                    navigationTitle = "放送时间表",
                )
            coEvery { BiliHttpApi.getTimeline(any<Int>()) } returns
                BiliResponse(code = 0, message = "", data = timelineAppData)

            val result = repository.getTimeline(filter = TimelineFilter.All, preferApiType = ApiType.App)

            assertThat(result).hasSize(1)
            assertThat(result[0].dateString).isEqualTo("2024-01-01")
            assertThat(result[0].isToday).isTrue()
            assertThat(result[0].episodes).hasSize(1)
            assertThat(result[0].episodes[0].title).isEqualTo("番剧名")
        }

    @Test
    @Disabled("Season timeline is Web-only")
    fun `getTimeline App passes appFilterId`() =
        runTest {
            coEvery { BiliHttpApi.getTimeline(any<Int>()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        TimelineAppData(
                            currentTimeText = "",
                            data = emptyList(),
                            filter = emptyList(),
                            isNightMode = 0,
                            navigationTitle = "",
                        ),
                )

            repository.getTimeline(filter = TimelineFilter.Anime, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.getTimeline(eq(TimelineFilter.Anime.appFilterId)) }
        }

    @Test
    @Disabled("Season timeline is Web-only")
    fun `getTimeline App returns empty list when no data`() =
        runTest {
            coEvery { BiliHttpApi.getTimeline(any<Int>()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        TimelineAppData(
                            currentTimeText = "",
                            data = emptyList(),
                            filter = emptyList(),
                            isNightMode = 0,
                            navigationTitle = "",
                        ),
                )

            val result = repository.getTimeline(filter = TimelineFilter.All, preferApiType = ApiType.App)

            assertThat(result).isEmpty()
        }

    // ------------------------------------------------------------------
    // getFollowingSeasons (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getFollowingSeasons Web returns mapped seasons with total`() =
        runTest {
            val webData =
                FollowingSeasonWebData(
                    list = listOf(fakeWebFollowingSeason(seasonId = 400, title = "番剧1")),
                    pageNumber = 1,
                    pageSize = 30,
                    total = 1,
                )
            coEvery { BiliHttpApi.getFollowingSeasons(any<Int>(), any(), any(), any(), any<Long>()) } returns
                BiliResponse(code = 0, message = "", data = webData)

            val result =
                repository.getFollowingSeasons(
                    type = FollowingSeasonType.Bangumi,
                    status = FollowingSeasonStatus.All,
                    pageNumber = 1,
                    pageSize = 30,
                    preferApiType = ApiType.Web,
                )

            assertThat(result.list).hasSize(1)
            assertThat(result.list[0].seasonId).isEqualTo(400)
            assertThat(result.list[0].title).isEqualTo("番剧1")
            assertThat(result.total).isEqualTo(1)
        }

    @Test
    fun `getFollowingSeasons Web passes mid and type id`() =
        runTest {
            coEvery { BiliHttpApi.getFollowingSeasons(any<Int>(), any(), any(), any(), any<Long>()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        FollowingSeasonWebData(
                            list = emptyList(),
                            pageNumber = 1,
                            pageSize = 30,
                            total = 0,
                        ),
                )

            repository.getFollowingSeasons(
                type = FollowingSeasonType.Cinema,
                status = FollowingSeasonStatus.Watching,
                pageNumber = 2,
                pageSize = 15,
                preferApiType = ApiType.Web,
            )

            coVerify {
                BiliHttpApi.getFollowingSeasons(
                    type = eq(FollowingSeasonType.Cinema.id),
                    status = eq(FollowingSeasonStatus.Watching.id),
                    pageNumber = eq(2),
                    pageSize = eq(15),
                    mid = eq(12345L),
                )
            }
        }

    // ------------------------------------------------------------------
    // getFollowingSeasons (App)
    // ------------------------------------------------------------------

    @Test
    @Disabled("Following seasons are Web-only")
    fun `getFollowingSeasons App returns mapped seasons with total`() =
        runTest {
            val appData =
                FollowingSeasonAppData(
                    followList = listOf(fakeAppFollowingSeason(seasonId = 400, title = "番剧1")),
                    _hasNext = 0,
                    total = 1,
                )
            coEvery {
                BiliHttpApi.getFollowingSeasons(any<String>(), any(), any(), any(), any(), any())
            } returns
                BiliResponse(code = 0, message = "", data = appData)

            val result =
                repository.getFollowingSeasons(
                    type = FollowingSeasonType.Bangumi,
                    status = FollowingSeasonStatus.All,
                    pageNumber = 1,
                    pageSize = 30,
                    preferApiType = ApiType.App,
                )

            assertThat(result.list).hasSize(1)
            assertThat(result.list[0].seasonId).isEqualTo(400)
            assertThat(result.list[0].title).isEqualTo("番剧1")
            assertThat(result.total).isEqualTo(1)
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeWebFollowingSeason(
        seasonId: Int = 400,
        title: String = "测试番剧",
    ) = WebFollowingSeason(
        badge = "",
        badgeEp = "",
        badgeInfo =
            WebFollowingSeason.BadgeInfo(
                bgColor = "",
                bgColorNight = "",
                multiImg =
                    WebFollowingSeason.BadgeInfo.MultiImg(
                        color = "",
                        mediumRemind = "",
                    ),
            ),
        badgeType = 0,
        bothFollow = false,
        canWatch = 0,
        cover = "http://cover.test",
        evaluate = "",
        firstEp = 0,
        firstEpInfo = WebFollowingSeason.EpInfo(),
        followStatus = 0,
        isFinish = 0,
        isNew = 0,
        isPlay = 0,
        isStarted = 0,
        mediaAttr = 0,
        mediaId = 0,
        mode = 0,
        newEp = WebFollowingSeason.EpInfo(),
        progress = "",
        publish =
            WebFollowingSeason.Publish(
                pubTime = "",
                pubTimeShow = "",
                releaseDate = "",
                releaseDateShow = "",
            ),
        rights =
            WebFollowingSeason.Rights(
                isSelection = 0,
                selectionStyle = 0,
            ),
        seasonAttr = 0,
        seasonId = seasonId,
        seasonStatus = 0,
        seasonTitle = title,
        seasonType = 1,
        seasonTypeName = "番剧",
        section = emptyList(),
        shortUrl = "",
        squareCover = "",
        stat = VideoStat(aid = seasonId.toLong()),
        subtitle = "",
        summary = "",
        title = title,
        totalCount = 12,
        url = "",
    )

    private fun fakeAppFollowingSeason(
        seasonId: Int = 400,
        title: String = "测试番剧",
    ) = AppFollowingSeason(
        badge = "",
        badgeInfo =
            AppFollowingSeason.BadgeInfo(
                bgColor = "",
                bgColorNight = "",
                text = "",
            ),
        badgeType = 0,
        canWatch = 0,
        cover = "http://cover.test",
        follow = 0,
        _isFinish = 0,
        movable = 0,
        mtime = 0,
        newEp =
            AppFollowingSeason.NewEp(
                cover = "",
                duration = 0,
                id = 0,
                indexShow = "",
                _isNew = 0,
            ),
        seasonId = seasonId,
        seasonType = 1,
        seasonTypeName = "番剧",
        series =
            AppFollowingSeason.Series(
                count = 1,
                id = 0,
                title = title,
            ),
        squareCover = "",
        title = title,
        url = "",
    )

    private fun httpTimeline(
        date: String,
        dateTs: Int,
        dayOfWeek: Int,
        isToday: Int,
        episodes: List<Timeline.Episode>,
    ) = Timeline(
        date = date,
        dateTs = dateTs,
        dayOfWeek = dayOfWeek,
        episodes = episodes,
        _isToday = isToday,
    )

    private fun httpEpisode() =
        Timeline.Episode(
            cover = "http://cover.test", delay = 0, delayId = 0,
            delayIndex = "", delayReason = "",
            episodeId = 100, pubIndex = "第1话", pubTime = "2024-01-01",
            pubTs = 1704067200, _published = 1,
            seasonId = 400, squareCover = "http://square.test",
            title = "番剧名",
        )
}
