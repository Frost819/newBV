package dev.frost819.newbv.biliapi.repositories

import bilibili.pagination.paginationReply
import bilibili.polymer.app.search.v1.SearchByTypeRequest
import bilibili.polymer.app.search.v1.item
import bilibili.polymer.app.search.v1.searchBangumiCard
import bilibili.polymer.app.search.v1.searchByTypeResponse
import bilibili.polymer.app.search.v1.searchUpperCard
import bilibili.polymer.app.search.v1.searchVideoCard
import bilibili.polymer.app.search.v1.share
import bilibili.polymer.app.search.v1.video
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.search.SearchBiliUserResult
import dev.frost819.newbv.biliapi.http.entity.search.SearchMediaResult
import dev.frost819.newbv.biliapi.http.entity.search.SearchResultData
import dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult
import dev.frost819.newbv.biliapi.http.entity.user.OfficialVerify
import org.junit.jupiter.api.Test

/**
 * [SearchRepository.kt] 中定义的辅助类与枚举的单元测试。
 *
 * 覆盖 [SearchType]、[SearchTypePage]、[SearchFilterOrderType]、[SearchFilterDuration]、
 * [SearchTypeResult] 及其内部 [SearchTypeResult.Video]、[SearchTypeResult.Pgc]、
 * [SearchTypeResult.User] 子类型的 from 方法。
 */
class SearchRepositoryHelpersTest {
    // ------------------------------------------------------------------
    // SearchType enum
    // ------------------------------------------------------------------

    @Test
    fun `SearchType Video has correct params`() {
        assertThat(SearchType.Video.httpTypeParam).isEqualTo("video")
        assertThat(SearchType.Video.grpcTypeParam).isEqualTo(10)
    }

    @Test
    fun `SearchType MediaBangumi has correct params`() {
        assertThat(SearchType.MediaBangumi.httpTypeParam).isEqualTo("media_bangumi")
        assertThat(SearchType.MediaBangumi.grpcTypeParam).isEqualTo(7)
    }

    @Test
    fun `SearchType MediaFt has correct params`() {
        assertThat(SearchType.MediaFt.httpTypeParam).isEqualTo("media_ft")
        assertThat(SearchType.MediaFt.grpcTypeParam).isEqualTo(8)
    }

    @Test
    fun `SearchType BiliUser has correct params`() {
        assertThat(SearchType.BiliUser.httpTypeParam).isEqualTo("bili_user")
        assertThat(SearchType.BiliUser.grpcTypeParam).isEqualTo(2)
    }

    @Test
    fun `SearchType LiveRoom uses web search params`() {
        assertThat(SearchType.LiveRoom.httpTypeParam).isEqualTo("live_room")
        assertThat(SearchType.LiveRoom.grpcTypeParam).isEqualTo(4)
    }

    @Test
    fun `SearchType has exactly 5 values`() {
        assertThat(SearchType.entries).hasSize(5)
    }

    // ------------------------------------------------------------------
    // SearchTypePage
    // ------------------------------------------------------------------

    @Test
    fun `SearchTypePage defaults are correct`() {
        val page = SearchTypePage()
        assertThat(page.nextPageForWeb).isEqualTo(1)
        assertThat(page.nextPageForApp).isEqualTo("")
    }

    @Test
    fun `SearchTypePage can be created with custom values`() {
        val page = SearchTypePage(nextPageForWeb = 5, nextPageForApp = "cursor-abc")
        assertThat(page.nextPageForWeb).isEqualTo(5)
        assertThat(page.nextPageForApp).isEqualTo("cursor-abc")
    }

    // ------------------------------------------------------------------
    // SearchFilterOrderType enum
    // ------------------------------------------------------------------

    @Test
    fun `SearchFilterOrderType ComprehensiveSort has correct params`() {
        assertThat(SearchFilterOrderType.ComprehensiveSort.httpOrderParam).isNull()
        assertThat(SearchFilterOrderType.ComprehensiveSort.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.CATEGORY_SORT_DEFAULT)
    }

    @Test
    fun `SearchFilterOrderType MostClicks has correct params`() {
        assertThat(SearchFilterOrderType.MostClicks.httpOrderParam).isEqualTo("click")
        assertThat(SearchFilterOrderType.MostClicks.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.CATEGORY_SORT_CLICK_COUNT)
    }

    @Test
    fun `SearchFilterOrderType LatestPublish has correct params`() {
        assertThat(SearchFilterOrderType.LatestPublish.httpOrderParam).isEqualTo("pubdate")
        assertThat(SearchFilterOrderType.LatestPublish.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.CATEGORY_SORT_PUBLISH_TIME)
    }

    @Test
    fun `SearchFilterOrderType MostDanmaku has correct params`() {
        assertThat(SearchFilterOrderType.MostDanmaku.httpOrderParam).isEqualTo("dm")
        assertThat(SearchFilterOrderType.MostDanmaku.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.UNRECOGNIZED)
    }

    @Test
    fun `SearchFilterOrderType MostFavorites has correct params`() {
        assertThat(SearchFilterOrderType.MostFavorites.httpOrderParam).isEqualTo("stow")
        assertThat(SearchFilterOrderType.MostFavorites.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.UNRECOGNIZED)
    }

    @Test
    fun `SearchFilterOrderType MostComment has correct params`() {
        assertThat(SearchFilterOrderType.MostComment.httpOrderParam).isNull()
        assertThat(SearchFilterOrderType.MostComment.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.CATEGORY_SORT_COMMENT_COUNT)
    }

    @Test
    fun `SearchFilterOrderType MostLikes has correct params`() {
        assertThat(SearchFilterOrderType.MostLikes.httpOrderParam).isNull()
        assertThat(SearchFilterOrderType.MostLikes.grpcOrderParam)
            .isEqualTo(SearchByTypeRequest.CategorySort.CATEGORY_SORT_LIKE_COUNT)
    }

    @Test
    fun `SearchFilterOrderType webFilters contains exactly 5 entries`() {
        assertThat(SearchFilterOrderType.webFilters).hasSize(5)
        assertThat(SearchFilterOrderType.webFilters).containsExactly(
            SearchFilterOrderType.ComprehensiveSort,
            SearchFilterOrderType.MostClicks,
            SearchFilterOrderType.LatestPublish,
            SearchFilterOrderType.MostDanmaku,
            SearchFilterOrderType.MostFavorites,
        )
    }

    @Test
    fun `SearchFilterOrderType allFilters contains exactly 5 entries`() {
        assertThat(SearchFilterOrderType.allFilters).hasSize(5)
        assertThat(SearchFilterOrderType.allFilters).containsExactly(
            SearchFilterOrderType.ComprehensiveSort,
            SearchFilterOrderType.MostClicks,
            SearchFilterOrderType.LatestPublish,
            SearchFilterOrderType.MostComment,
            SearchFilterOrderType.MostLikes,
        )
    }

    // ------------------------------------------------------------------
    // SearchFilterDuration enum
    // ------------------------------------------------------------------

    @Test
    fun `SearchFilterDuration All has null param`() {
        assertThat(SearchFilterDuration.All.httpDurationParam).isNull()
    }

    @Test
    fun `SearchFilterDuration LessThan10Minutes has param 1`() {
        assertThat(SearchFilterDuration.LessThan10Minutes.httpDurationParam).isEqualTo(1)
    }

    @Test
    fun `SearchFilterDuration Between10And30Minutes has param 2`() {
        assertThat(SearchFilterDuration.Between10And30Minutes.httpDurationParam).isEqualTo(2)
    }

    @Test
    fun `SearchFilterDuration Between30And60Minutes has param 3`() {
        assertThat(SearchFilterDuration.Between30And60Minutes.httpDurationParam).isEqualTo(3)
    }

    @Test
    fun `SearchFilterDuration MoreThan60Minutes has param 4`() {
        assertThat(SearchFilterDuration.MoreThan60Minutes.httpDurationParam).isEqualTo(4)
    }

    @Test
    fun `SearchFilterDuration has exactly 5 values`() {
        assertThat(SearchFilterDuration.entries).hasSize(5)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult defaults
    // ------------------------------------------------------------------

    @Test
    fun `SearchTypeResult defaults are correct`() {
        val result = SearchTypeResult(page = SearchTypePage())
        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
        assertThat(result.hasMore).isTrue()
        assertThat(result.page.nextPageForWeb).isEqualTo(1)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (HTTP) - Video
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult HTTP with video results maps to Video list`() {
        val searchResultData = fakeSearchResultDataWithVideos()

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.videos).hasSize(2)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].bvid).isEqualTo("BV1xx")
        assertThat(result.videos[0].title).isEqualTo("test video 1")
        assertThat(result.videos[0].cover).isEqualTo("https://pic.example.com/1.jpg")
        assertThat(result.videos[0].author).isEqualTo("UP主1")
        assertThat(result.videos[0].mid).isEqualTo(1L)
        assertThat(result.videos[0].duration).isEqualTo(300)
        assertThat(result.videos[0].play).isEqualTo(1000)
        assertThat(result.videos[0].danmaku).isEqualTo(50)
        assertThat(result.page.nextPageForWeb).isEqualTo(2)
        assertThat(result.hasMore).isTrue()
    }

    @Test
    fun `fromSearchTypeResult HTTP with video results hasMore false when page equals numPages`() {
        val searchResultData = fakeSearchResultDataWithVideos().copy(page = 3, numPages = 3)

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromSearchTypeResult HTTP with video maps play to 0 when null`() {
        val searchResultData = fakeSearchResultDataWithVideos(play = null)

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.videos[0].play).isEqualTo(0)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (HTTP) - Media/Pgc
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult HTTP with media results maps to Pgc list`() {
        val searchResultData = fakeSearchResultDataWithMedia()

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.pgcs).hasSize(1)
        assertThat(result.pgcs[0].title).isEqualTo("test bangumi")
        assertThat(result.pgcs[0].cover).isEqualTo("http://cover.example.com/1.jpg")
        assertThat(result.pgcs[0].star).isEqualTo(9.5f)
        assertThat(result.pgcs[0].seasonId).isEqualTo(39707)
    }

    @Test
    fun `fromSearchTypeResult HTTP with no results returns empty lists`() {
        val result =
            SearchTypeResult.fromSearchTypeResult(
                SearchResultData(
                    seid = "test-seid",
                    page = 1,
                    pageSize = 20,
                    numResults = 0,
                    numPages = 0,
                    suggestKeyword = "",
                    rqtType = "",
                    eggHit = 0,
                ),
            )

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
        assertThat(result.liveRooms).isEmpty()
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `SearchResultData maps result_type media wrapper to typed results`() {
        val mediaJson =
            kotlinx.serialization.json.Json.encodeToString(
                SearchMediaResult.serializer(),
                fakeSearchMediaResult(),
            )
        val wrappedJson =
            kotlinx.serialization.json.Json.parseToJsonElement(
                """
                {
                  "result_type": "media_bangumi",
                  "data": [$mediaJson]
                }
                """.trimIndent(),
            )

        val searchResultData =
            SearchResultData(
                seid = "test-seid",
                page = 1,
                pageSize = 20,
                numResults = 1,
                numPages = 2,
                suggestKeyword = "",
                rqtType = "",
                eggHit = 0,
                result = listOf(wrappedJson),
            )

        assertThat(SearchTypeResult.fromSearchTypeResult(searchResultData).pgcs).hasSize(1)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (HTTP) - User
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult HTTP with bili_user results maps to User list`() {
        val searchResultData = fakeSearchResultDataWithUsers()

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.users).hasSize(1)
        assertThat(result.users[0].mid).isEqualTo(555L)
        assertThat(result.users[0].name).isEqualTo("用户1")
        assertThat(result.users[0].avatar).isEqualTo("https://upic.example.com/1.jpg")
        assertThat(result.users[0].sign).isEqualTo("签名1")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (HTTP) - Unknown type
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult HTTP with activity result type returns empty lists via else branch`() {
        val activityJson =
            kotlinx.serialization.json.Json.encodeToString(
                dev.frost819.newbv.biliapi.http.entity.search.SearchActivityResult
                    .serializer(),
                dev.frost819.newbv.biliapi.http.entity.search.SearchActivityResult(
                    status = 0,
                    author = "",
                    url = "",
                    title = "activity",
                    cover = "",
                    pos = 0,
                    cardType = 0,
                    state = 0,
                    position = 0,
                    corner = "",
                    cardValue = "",
                    type = "activity",
                    id = 0,
                    desc = "",
                ),
            )
        val searchResultData =
            SearchResultData(
                seid = "test-seid",
                page = 1,
                pageSize = 20,
                numResults = 1,
                numPages = 2,
                suggestKeyword = "",
                rqtType = "",
                eggHit = 0,
                result =
                    listOf(
                        kotlinx.serialization.json.Json
                            .parseToJsonElement(activityJson),
                    ),
            )

        val result = SearchTypeResult.fromSearchTypeResult(searchResultData)

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (gRPC) - Empty items
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult gRPC with empty items returns empty result`() {
        val response =
            searchByTypeResponse {
                pagination = paginationReply { next = "cursor-next" }
            }

        val result = SearchTypeResult.fromSearchTypeResult(response)

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
        assertThat(result.page.nextPageForApp).isEqualTo("cursor-next")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.Video.fromSearchVideoResult - duration parsing
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchVideoResult parses MM SS duration correctly`() {
        val videoResult = fakeSearchVideoResult(duration = "5:30")

        val video = SearchTypeResult.Video.fromSearchVideoResult(videoResult)

        assertThat(video.duration).isEqualTo(330)
    }

    @Test
    fun `fromSearchVideoResult parses HH MM SS duration correctly`() {
        val videoResult = fakeSearchVideoResult(duration = "1:30:45")

        val video = SearchTypeResult.Video.fromSearchVideoResult(videoResult)

        assertThat(video.duration).isEqualTo(5445)
    }

    @Test
    fun `fromSearchVideoResult parses single digit seconds`() {
        val videoResult = fakeSearchVideoResult(duration = "0:05")

        val video = SearchTypeResult.Video.fromSearchVideoResult(videoResult)

        assertThat(video.duration).isEqualTo(5)
    }

    @Test
    fun `fromSearchVideoResult prepends https to pic url`() {
        val videoResult = fakeSearchVideoResult(pic = "//pic.example.com/1.jpg")

        val video = SearchTypeResult.Video.fromSearchVideoResult(videoResult)

        assertThat(video.cover).isEqualTo("https://pic.example.com/1.jpg")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.Pgc.fromSearchPgcResult
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchPgcResult maps all fields correctly`() {
        val mediaResult = fakeSearchMediaResult()

        val pgc = SearchTypeResult.Pgc.fromSearchPgcResult(mediaResult)

        assertThat(pgc.title).isEqualTo("test bangumi")
        assertThat(pgc.cover).isEqualTo("http://cover.example.com/1.jpg")
        assertThat(pgc.star).isEqualTo(9.5f)
        assertThat(pgc.seasonId).isEqualTo(39707)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.User.fromSearchUserResult
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchUserResult prepends https to upic`() {
        val userResult = fakeSearchBiliUserResult(upic = "//upic.example.com/1.jpg")

        val user = SearchTypeResult.User.fromSearchUserResult(userResult)

        assertThat(user.avatar).isEqualTo("https://upic.example.com/1.jpg")
    }

    @Test
    fun `fromSearchUserResult maps all fields correctly`() {
        val userResult = fakeSearchBiliUserResult()

        val user = SearchTypeResult.User.fromSearchUserResult(userResult)

        assertThat(user.mid).isEqualTo(555L)
        assertThat(user.name).isEqualTo("用户1")
        assertThat(user.sign).isEqualTo("签名1")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (gRPC) - AV items
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult gRPC with AV items maps to Video list`() {
        val response =
            searchByTypeResponse {
                pagination = paginationReply { next = "cursor-av" }
                items +=
                    item {
                        param = "100"
                        av =
                            searchVideoCard {
                                title = "gRPC视频"
                                cover = "http://cover.grpc/1.jpg"
                                author = "gRPC UP"
                                mid = 1L
                                duration = "5:30"
                                play = 500
                                danmaku = 20
                                share =
                                    share {
                                        video =
                                            video {
                                                bvid = "BVgRPC1"
                                            }
                                    }
                            }
                    }
            }

        val result = SearchTypeResult.fromSearchTypeResult(response)

        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].bvid).isEqualTo("BVgRPC1")
        assertThat(result.videos[0].title).isEqualTo("gRPC视频")
        assertThat(result.videos[0].cover).isEqualTo("http://cover.grpc/1.jpg")
        assertThat(result.videos[0].author).isEqualTo("gRPC UP")
        assertThat(result.videos[0].mid).isEqualTo(1L)
        assertThat(result.videos[0].duration).isEqualTo(330)
        assertThat(result.videos[0].play).isEqualTo(500)
        assertThat(result.videos[0].danmaku).isEqualTo(20)
        assertThat(result.page.nextPageForApp).isEqualTo("cursor-av")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (gRPC) - BANGUMI items
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult gRPC with BANGUMI items maps to Pgc list`() {
        val response =
            searchByTypeResponse {
                pagination = paginationReply { next = "cursor-bg" }
                items +=
                    item {
                        bangumi =
                            searchBangumiCard {
                                title = "gRPC番剧"
                                cover = "http://cover.bg/1.jpg"
                                rating = 9.0
                                seasonId = 40000L
                            }
                    }
            }

        val result = SearchTypeResult.fromSearchTypeResult(response)

        assertThat(result.pgcs).hasSize(1)
        assertThat(result.pgcs[0].title).isEqualTo("gRPC番剧")
        assertThat(result.pgcs[0].cover).isEqualTo("http://cover.bg/1.jpg")
        assertThat(result.pgcs[0].star).isEqualTo(9.0f)
        assertThat(result.pgcs[0].seasonId).isEqualTo(40000)
        assertThat(result.page.nextPageForApp).isEqualTo("cursor-bg")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (gRPC) - AUTHOR items
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult gRPC with AUTHOR items maps to User list`() {
        val response =
            searchByTypeResponse {
                pagination = paginationReply { next = "cursor-user" }
                items +=
                    item {
                        param = "555"
                        author =
                            searchUpperCard {
                                title = "gRPC用户"
                                cover = "http://avatar.grpc/1.jpg"
                                sign = "gRPC签名"
                            }
                    }
            }

        val result = SearchTypeResult.fromSearchTypeResult(response)

        assertThat(result.users).hasSize(1)
        assertThat(result.users[0].mid).isEqualTo(555L)
        assertThat(result.users[0].name).isEqualTo("gRPC用户")
        assertThat(result.users[0].avatar).isEqualTo("http://avatar.grpc/1.jpg")
        assertThat(result.users[0].sign).isEqualTo("gRPC签名")
        assertThat(result.page.nextPageForApp).isEqualTo("cursor-user")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.fromSearchTypeResult (gRPC) - Unknown card type
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchTypeResult gRPC with unknown card type returns empty lists`() {
        val response =
            searchByTypeResponse {
                pagination = paginationReply { next = "cursor-unknown" }
                items +=
                    item {
                        uri = "some-uri"
                    }
            }

        val result = SearchTypeResult.fromSearchTypeResult(response)

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
        assertThat(result.page.nextPageForApp).isEqualTo("cursor-unknown")
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.Video.fromSearchVideoCard
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchVideoCard maps all fields from gRPC Item`() {
        val grpcItem =
            item {
                param = "200"
                av =
                    searchVideoCard {
                        title = "card视频"
                        cover = "http://cover.card/1.jpg"
                        author = "card UP"
                        mid = 2L
                        duration = "10:00"
                        play = 1000
                        danmaku = 50
                        share =
                            share {
                                video =
                                    video {
                                        bvid = "BVcard1"
                                    }
                            }
                    }
            }

        val video = SearchTypeResult.Video.fromSearchVideoCard(grpcItem)

        assertThat(video.aid).isEqualTo(200L)
        assertThat(video.bvid).isEqualTo("BVcard1")
        assertThat(video.title).isEqualTo("card视频")
        assertThat(video.cover).isEqualTo("http://cover.card/1.jpg")
        assertThat(video.author).isEqualTo("card UP")
        assertThat(video.mid).isEqualTo(2L)
        assertThat(video.duration).isEqualTo(600)
        assertThat(video.play).isEqualTo(1000)
        assertThat(video.danmaku).isEqualTo(50)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.Pgc.fromSearchPgcCard
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchPgcCard maps all fields from gRPC Item`() {
        val grpcItem =
            item {
                bangumi =
                    searchBangumiCard {
                        title = "card番剧"
                        cover = "http://cover.pgc/1.jpg"
                        rating = 8.5
                        seasonId = 30000L
                    }
            }

        val pgc = SearchTypeResult.Pgc.fromSearchPgcCard(grpcItem)

        assertThat(pgc.title).isEqualTo("card番剧")
        assertThat(pgc.cover).isEqualTo("http://cover.pgc/1.jpg")
        assertThat(pgc.star).isEqualTo(8.5f)
        assertThat(pgc.seasonId).isEqualTo(30000)
    }

    // ------------------------------------------------------------------
    // SearchTypeResult.User.fromSearchUserCard
    // ------------------------------------------------------------------

    @Test
    fun `fromSearchUserCard maps all fields from gRPC Item`() {
        val grpcItem =
            item {
                param = "999"
                author =
                    searchUpperCard {
                        title = "card用户"
                        cover = "http://avatar.card/1.jpg"
                        sign = "card签名"
                    }
            }

        val user = SearchTypeResult.User.fromSearchUserCard(grpcItem)

        assertThat(user.mid).isEqualTo(999L)
        assertThat(user.name).isEqualTo("card用户")
        assertThat(user.avatar).isEqualTo("http://avatar.card/1.jpg")
        assertThat(user.sign).isEqualTo("card签名")
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeSearchVideoResult(
        duration: String = "5:00",
        pic: String = "//pic.example.com/1.jpg",
        play: Int? = 1000,
    ) = SearchVideoResult(
        type = "video",
        id = 100L,
        author = "UP主1",
        mid = 1L,
        typeId = "1",
        typeName = "综合",
        arcUrl = "https://b23.tv/BV1xx",
        aid = 100L,
        bvid = "BV1xx",
        title = "test video 1",
        description = "description",
        pic = pic,
        play = play,
        videoReview = 50,
        favorites = 100,
        tag = "tag1,tag2",
        review = 10,
        pubDate = 1700000000,
        sendDate = 1700000000,
        duration = duration,
        badgePay = false,
        hitColumns = listOf("title"),
        viewType = "",
        isPay = 0,
        isUnionVideo = 0,
        newRecTags = emptyList(),
        like = 200,
        upic = "",
        corner = "",
        cover = "",
        desc = "",
        url = "",
        recReason = "",
        danmaku = 50,
        vtDisplay = "",
        subtitle = "",
        episodeCountText = "",
        releaseStatus = 0,
        isIntervene = 0,
    )

    private fun fakeSearchResultDataWithVideos(play: Int? = 1000): SearchResultData {
        val videoJson =
            kotlinx.serialization.json.Json.encodeToString(
                SearchVideoResult.serializer(),
                fakeSearchVideoResult(play = play),
            )
        return SearchResultData(
            seid = "test-seid",
            page = 1,
            pageSize = 20,
            numResults = 2,
            numPages = 2,
            suggestKeyword = "",
            rqtType = "",
            eggHit = 0,
            result =
                listOf(
                    kotlinx.serialization.json.Json
                        .parseToJsonElement(videoJson),
                    kotlinx.serialization.json.Json
                        .parseToJsonElement(videoJson),
                ),
        )
    }

    private fun fakeSearchMediaResult() =
        SearchMediaResult(
            type = "media_bangumi",
            mediaId = 100,
            title = "test bangumi",
            orgTitle = "",
            mediaType = 1,
            cv = "",
            staff = "",
            seasonId = 39707,
            isAvid = true,
            hitEpids = "",
            seasonType = 1,
            seasonTypeName = "番剧",
            selectionStyle = "horizontal",
            epSize = 12,
            url = "",
            buttonText = "立即观看",
            isFollow = 0,
            isSelection = 0,
            cover = "http://cover.example.com/1.jpg",
            areas = "日本",
            styles = "热血",
            gotoUrl = "",
            desc = "test description",
            pubTime = 1700000000,
            mediaMode = 0,
            fixPubTimeStr = "",
            mediaScore = SearchMediaResult.MediaScore(score = 9.5f, userCount = 10000),
            pgcSeasonId = 39707,
            corner = 2,
            indexShow = "全12话",
        )

    private fun fakeSearchResultDataWithMedia(): SearchResultData {
        val mediaJson =
            kotlinx.serialization.json.Json.encodeToString(
                SearchMediaResult.serializer(),
                fakeSearchMediaResult(),
            )
        return SearchResultData(
            seid = "test-seid",
            page = 1,
            pageSize = 20,
            numResults = 1,
            numPages = 2,
            suggestKeyword = "",
            rqtType = "",
            eggHit = 0,
            result =
                listOf(
                    kotlinx.serialization.json.Json
                        .parseToJsonElement(mediaJson),
                ),
        )
    }

    private fun fakeSearchBiliUserResult(upic: String = "//upic.example.com/1.jpg") =
        SearchBiliUserResult(
            type = "bili_user",
            mid = 555L,
            uname = "用户1",
            usign = "签名1",
            fans = 1000,
            videos = 50,
            upic = upic,
            faceNft = 0,
            faceNftType = 0,
            verifyInfo = "",
            level = 5,
            gender = 0,
            isUpUser = 1,
            isLive = 0,
            roomId = 0,
            res = emptyList(),
            officialVerify = OfficialVerify(type = -1, desc = ""),
            hitColumns = listOf("uname"),
            isSeniorMember = 0,
        )

    private fun fakeSearchResultDataWithUsers(): SearchResultData {
        val userJson =
            kotlinx.serialization.json.Json.encodeToString(
                SearchBiliUserResult.serializer(),
                fakeSearchBiliUserResult(),
            )
        return SearchResultData(
            seid = "test-seid",
            page = 1,
            pageSize = 20,
            numResults = 1,
            numPages = 2,
            suggestKeyword = "",
            rqtType = "",
            eggHit = 0,
            result =
                listOf(
                    kotlinx.serialization.json.Json
                        .parseToJsonElement(userJson),
                ),
        )
    }
}
