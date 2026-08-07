package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.search.Hotword
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.search.KeywordSuggest
import dev.frost819.newbv.biliapi.http.entity.search.SearchTendingData
import dev.frost819.newbv.biliapi.http.entity.search.WebSearchSquareData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [SearchRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证热搜词获取与搜索建议的
 * Web / App 路径参数传递与数据转换。不依赖真实网络。
 */
class SearchRepositoryUnitTest {
    private lateinit var repository: SearchRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var channelRepository: ChannelRepository

    companion object {
        private const val BUVID = "test-buvid"
        private const val KEYWORD = "test-keyword"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.buvid = BUVID
        channelRepository = ChannelRepository()
        repository = SearchRepository(authRepository, channelRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getSearchHotwords
    // ------------------------------------------------------------------

    @Test
    fun `getSearchHotwords Web maps trending list to Hotword`() =
        runTest {
            val squareData =
                WebSearchSquareData(
                    trending =
                        WebSearchSquareData.Trending(
                            title = "热搜榜",
                            trackId = "track-1",
                            list =
                                listOf(
                                    dev.frost819.newbv.biliapi.http.entity.search.Hotword(
                                        keyword = "kw1",
                                        showName = "关键词1",
                                        icon = "icon1",
                                        uri = "uri1",
                                        goto = "search",
                                    ),
                                    dev.frost819.newbv.biliapi.http.entity.search.Hotword(
                                        keyword = "kw2",
                                        showName = "关键词2",
                                        icon = "icon2",
                                        uri = "uri2",
                                        goto = "search",
                                    ),
                                ),
                        ),
                )
            coEvery { BiliHttpApi.getWebSearchSquare(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = squareData)

            val result = repository.getSearchHotwords(limit = 30, preferApiType = ApiType.Web)

            assertThat(result).hasSize(2)
            assertThat(result[0].keyword).isEqualTo("kw1")
            assertThat(result[0].showName).isEqualTo("关键词1")
            assertThat(result[1].keyword).isEqualTo("kw2")
        }

    @Test
    fun `getSearchHotwords Web passes limit parameter`() =
        runTest {
            coEvery { BiliHttpApi.getWebSearchSquare(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = webSquareDataEmpty())

            repository.getSearchHotwords(limit = 50, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getWebSearchSquare(eq(50), any()) }
        }

    @Test
    fun `getSearchHotwords Web returns empty list when trending list is empty`() =
        runTest {
            coEvery { BiliHttpApi.getWebSearchSquare(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = webSquareDataEmpty())

            val result = repository.getSearchHotwords(limit = 10, preferApiType = ApiType.Web)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getSearchHotwords App maps trending list to Hotword`() =
        runTest {
            val tendingData =
                SearchTendingData(
                    trackId = "track-2",
                    list =
                        listOf(
                            SearchTendingData.Hotword(
                                position = 1,
                                keyword = "app-kw1",
                                showName = "App词1",
                                icon = "icon1",
                                hotId = 100,
                                isCommercial = 0,
                            ),
                        ),
                    hotwordEggInfo = 0,
                )
            coEvery { BiliHttpApi.getSearchTrendRank(any()) } returns
                BiliResponse(code = 0, message = "", data = tendingData)

            val result = repository.getSearchHotwords(limit = 30, preferApiType = ApiType.App)

            assertThat(result).hasSize(1)
            assertThat(result[0].keyword).isEqualTo("app-kw1")
            assertThat(result[0].showName).isEqualTo("App词1")
            assertThat(result[0].icon).isEqualTo("icon1")
        }

    @Test
    fun `getSearchHotwords App always requests limit of 50`() =
        runTest {
            coEvery { BiliHttpApi.getSearchTrendRank(any()) } returns
                BiliResponse(code = 0, message = "", data = tendingDataEmpty())

            repository.getSearchHotwords(limit = 10, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.getSearchTrendRank(eq(50)) }
        }

    // ------------------------------------------------------------------
    // getSearchSuggest
    // ------------------------------------------------------------------

    @Test
    fun `getSearchSuggest Web maps suggests to keyword values`() =
        runTest {
            val keywordSuggest =
                KeywordSuggest(
                    expStr = "",
                    code = 0,
                    msg = "",
                    stoken = "token",
                )
            keywordSuggest.suggests.addAll(
                listOf(
                    KeywordSuggest.Result.Tag(value = "suggest1", term = "t1", ref = 0, name = "s1", spid = 0),
                    KeywordSuggest.Result.Tag(value = "suggest2", term = "t2", ref = 0, name = "s2", spid = 0),
                    KeywordSuggest.Result.Tag(value = "suggest3", term = "t3", ref = 0, name = "s3", spid = 0),
                ),
            )
            coEvery { BiliHttpApi.getKeywordSuggest(any(), any(), any(), any()) } returns keywordSuggest

            val result = repository.getSearchSuggest(keyword = KEYWORD, preferApiType = ApiType.Web)

            assertThat(result).containsExactly("suggest1", "suggest2", "suggest3").inOrder()
        }

    @Test
    fun `getSearchSuggest Web passes keyword and buvid`() =
        runTest {
            val keywordSuggest = KeywordSuggest(expStr = "", code = 0, msg = "", stoken = "t")
            coEvery { BiliHttpApi.getKeywordSuggest(any(), any(), any(), any()) } returns keywordSuggest

            repository.getSearchSuggest(keyword = KEYWORD, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getKeywordSuggest(eq(KEYWORD), any(), any(), eq(BUVID)) }
        }

    @Test
    fun `getSearchSuggest Web uses empty string when buvid is null`() =
        runTest {
            authRepository.buvid = null
            val keywordSuggest = KeywordSuggest(expStr = "", code = 0, msg = "", stoken = "t")
            coEvery { BiliHttpApi.getKeywordSuggest(any(), any(), any(), any()) } returns keywordSuggest

            repository.getSearchSuggest(keyword = KEYWORD, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getKeywordSuggest(eq(KEYWORD), any(), any(), eq("")) }
        }

    @Test
    fun `getSearchSuggest Web returns empty list when suggests is empty`() =
        runTest {
            val keywordSuggest = KeywordSuggest(expStr = "", code = 0, msg = "", stoken = "t")
            coEvery { BiliHttpApi.getKeywordSuggest(any(), any(), any(), any()) } returns keywordSuggest

            val result = repository.getSearchSuggest(keyword = KEYWORD, preferApiType = ApiType.Web)

            assertThat(result).isEmpty()
        }

    // ------------------------------------------------------------------
    // searchType (Web)
    // ------------------------------------------------------------------

    @Test
    fun `searchType Web calls API with correct params and returns mapped result`() =
        runTest {
            val videoJson =
                kotlinx.serialization.json.Json.encodeToString(
                    dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult.serializer(),
                    dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult(
                        type = "video",
                        id = 100L,
                        author = "UP",
                        mid = 1L,
                        typeId = "1",
                        typeName = "综合",
                        arcUrl = "",
                        aid = 100L,
                        bvid = "BV100",
                        title = "搜索结果",
                        description = "",
                        pic = "//pic.test/1.jpg",
                        play = 500,
                        videoReview = 10,
                        favorites = 20,
                        tag = "",
                        review = 0,
                        pubDate = 1700000000,
                        sendDate = 1700000000,
                        duration = "5:00",
                        badgePay = false,
                        hitColumns = emptyList(),
                        viewType = "",
                        isPay = 0,
                        isUnionVideo = 0,
                        newRecTags = emptyList(),
                        like = 50,
                        upic = "",
                        corner = "",
                        cover = "",
                        desc = "",
                        url = "",
                        recReason = "",
                        danmaku = 5,
                        vtDisplay = "",
                        subtitle = "",
                        episodeCountText = "",
                        releaseStatus = 0,
                        isIntervene = 0,
                    ),
                )
            val searchResultData =
                dev.frost819.newbv.biliapi.http.entity.search.SearchResultData(
                    seid = "seid",
                    page = 1,
                    pageSize = 20,
                    numResults = 1,
                    numPages = 2,
                    suggestKeyword = "",
                    rqtType = "",
                    eggHit = 0,
                    result = listOf(kotlinx.serialization.json.Json.parseToJsonElement(videoJson)),
                )
            coEvery { BiliHttpApi.searchType(any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = searchResultData)

            val result =
                repository.searchType(
                    keyword = KEYWORD,
                    type = SearchType.Video,
                    tid = null,
                    order = SearchFilterOrderType.ComprehensiveSort,
                    duration = SearchFilterDuration.All,
                    page = SearchTypePage(nextPageForWeb = 1),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.videos).hasSize(1)
            assertThat(result.videos[0].aid).isEqualTo(100L)
            assertThat(result.videos[0].title).isEqualTo("搜索结果")
            assertThat(result.hasMore).isTrue()
            assertThat(result.page.nextPageForWeb).isEqualTo(2)
        }

    @Test
    fun `searchType Web passes correct parameters to API`() =
        runTest {
            val videoJson =
                kotlinx.serialization.json.Json.encodeToString(
                    dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult.serializer(),
                    dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult(
                        type = "video",
                        id = 1L,
                        author = "UP",
                        mid = 1L,
                        typeId = "1",
                        typeName = "综合",
                        arcUrl = "",
                        aid = 1L,
                        bvid = "BV1",
                        title = "v",
                        description = "",
                        pic = "",
                        play = 0,
                        videoReview = 0,
                        favorites = 0,
                        tag = "",
                        review = 0,
                        pubDate = 0,
                        sendDate = 0,
                        duration = "0:01",
                        badgePay = false,
                        hitColumns = emptyList(),
                        viewType = "",
                        isPay = 0,
                        isUnionVideo = 0,
                        newRecTags = emptyList(),
                        like = 0,
                        upic = "",
                        corner = "",
                        cover = "",
                        desc = "",
                        url = "",
                        recReason = "",
                        danmaku = 0,
                        vtDisplay = "",
                        subtitle = "",
                        episodeCountText = "",
                        releaseStatus = 0,
                        isIntervene = 0,
                    ),
                )
            coEvery { BiliHttpApi.searchType(any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.search.SearchResultData(
                            seid = "",
                            page = 1,
                            pageSize = 20,
                            numResults = 1,
                            numPages = 1,
                            suggestKeyword = "",
                            rqtType = "",
                            eggHit = 0,
                            result = listOf(kotlinx.serialization.json.Json.parseToJsonElement(videoJson)),
                        ),
                )

            repository.searchType(
                keyword = KEYWORD,
                type = SearchType.Video,
                tid = 10,
                order = SearchFilterOrderType.LatestPublish,
                duration = SearchFilterDuration.LessThan10Minutes,
                page = SearchTypePage(nextPageForWeb = 3),
                preferApiType = ApiType.Web,
            )

            coVerify {
                BiliHttpApi.searchType(
                    keyword = KEYWORD,
                    type = "video",
                    page = 3,
                    tid = 10,
                    order = "pubdate",
                    duration = 1,
                )
            }
        }

    // ------------------------------------------------------------------
    // getSearchSuggest (App)
    // ------------------------------------------------------------------

    @Test
    fun `getSearchSuggest App returns empty list when gRPC stub is null`() =
        runTest {
            val result = repository.getSearchSuggest(keyword = KEYWORD, preferApiType = ApiType.App)

            assertThat(result).isEmpty()
        }

    // ------------------------------------------------------------------
    // searchType (App)
    // ------------------------------------------------------------------

    @Test
    fun `searchType App throws IllegalStateException when gRPC stub is null`() =
        runTest {
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                repository.searchType(
                    keyword = KEYWORD,
                    type = SearchType.Video,
                    tid = null,
                    order = SearchFilterOrderType.ComprehensiveSort,
                    duration = SearchFilterDuration.All,
                    page = SearchTypePage(),
                    preferApiType = ApiType.App,
                )
            }
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun webSquareDataEmpty() =
        WebSearchSquareData(
            trending =
                WebSearchSquareData.Trending(
                    title = "",
                    trackId = "",
                    list = emptyList(),
                ),
        )

    private fun tendingDataEmpty() =
        SearchTendingData(
            trackId = "",
            list = emptyList(),
            hotwordEggInfo = 0,
        )
}
