package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.home.RcmdIndexData
import dev.frost819.newbv.biliapi.http.entity.home.RcmdTopData
import dev.frost819.newbv.biliapi.http.entity.video.Dimension
import dev.frost819.newbv.biliapi.http.entity.video.PopularVideoData
import dev.frost819.newbv.biliapi.http.entity.video.VideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoOwner
import dev.frost819.newbv.biliapi.http.entity.video.VideoRights
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [RecommendVideoRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证热门视频与推荐视频获取的
 * Web 路径参数传递、分页递增与数据转换。不依赖真实网络。
 */
class RecommendVideoRepositoryUnitTest {
    private lateinit var repository: RecommendVideoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var channelRepository: ChannelRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        channelRepository = ChannelRepository()
        repository = RecommendVideoRepository(authRepository, channelRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getPopularVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getPopularVideos Web returns mapped items with correct nextPage`() =
        runTest {
            val popularData =
                PopularVideoData(
                    list = listOf(fakeVideoInfo(aid = 1L, title = "v1"), fakeVideoInfo(aid = 2L, title = "v2")),
                    noMore = false,
                )
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = popularData)

            val result =
                repository.getPopularVideos(
                    page = PopularVideoPage(nextWebPageSize = 20, nextWebPageNumber = 1),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.list).hasSize(2)
            assertThat(result.list[0].aid).isEqualTo(1L)
            assertThat(result.list[0].title).isEqualTo("v1")
            assertThat(result.list[1].aid).isEqualTo(2L)
            assertThat(result.noMore).isFalse()
            assertThat(result.nextPage.nextWebPageNumber).isEqualTo(2)
            assertThat(result.nextPage.nextWebPageSize).isEqualTo(20)
        }

    @Test
    fun `getPopularVideos Web passes pageSize and pageNumber`() =
        runTest {
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = PopularVideoData(list = emptyList(), noMore = true))

            repository.getPopularVideos(
                page = PopularVideoPage(nextWebPageSize = 30, nextWebPageNumber = 5),
                preferApiType = ApiType.Web,
            )

            coVerify { BiliHttpApi.getPopularVideoData(eq(5), eq(30)) }
        }

    @Test
    fun `getPopularVideos Web returns noMore true when API says so`() =
        runTest {
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = PopularVideoData(list = emptyList(), noMore = true))

            val result =
                repository.getPopularVideos(
                    page = PopularVideoPage(),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.noMore).isTrue()
            assertThat(result.list).isEmpty()
        }

    @Test
    fun `getPopularVideos Web maps author and stat fields`() =
        runTest {
            val videoInfo =
                VideoInfo(
                    bvid = "BV1xx",
                    aid = 100L,
                    videos = 1,
                    tid = 0,
                    tname = "test",
                    copyright = 1,
                    pic = "http://pic.test",
                    title = "title",
                    pubdate = 1700000000,
                    desc = "desc",
                    state = 0,
                    duration = 300,
                    rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
                    owner = VideoOwner(mid = 999L, name = "test-up", face = "http://face.test"),
                    stat = VideoStat(aid = 100L, _view = 5000L, danmaku = 100, like = 200),
                    dynamic = "",
                    cid = 200L,
                    dimension = Dimension(1920, 1080, 0),
                )
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = PopularVideoData(list = listOf(videoInfo), noMore = false))

            val result = repository.getPopularVideos(page = PopularVideoPage(), preferApiType = ApiType.Web)

            val item = result.list[0]
            assertThat(item.author).isEqualTo("test-up")
            assertThat(item.authorMid).isEqualTo(999L)
            assertThat(item.play).isEqualTo(5000)
            assertThat(item.danmaku).isEqualTo(100)
            assertThat(item.duration).isEqualTo(300)
            assertThat(item.cover).isEqualTo("http://pic.test")
        }

    // ------------------------------------------------------------------
    // getRecommendVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getRecommendVideos Web returns mapped items with correct nextPage`() =
        runTest {
            val rcmdData =
                RcmdTopData(
                    item = listOf(fakeRcmdItem(id = 10L), fakeRcmdItem(id = 20L)),
                    mid = 1L,
                    preloadExposePct = 0.0,
                    preloadFloorExposePct = 0.0,
                )
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdData)

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextWebIdx = 3),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.items).hasSize(2)
            assertThat(result.items[0].aid).isEqualTo(10L)
            assertThat(result.items[1].aid).isEqualTo(20L)
            assertThat(result.nextPage.nextWebIdx).isEqualTo(4)
        }

    @Test
    fun `getRecommendVideos Web passes idx parameter`() =
        runTest {
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdDataEmpty())

            repository.getRecommendVideos(page = RecommendPage(nextWebIdx = 7), preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getFeedRcmd(any(), any(), eq(7)) }
        }

    @Test
    fun `getRecommendVideos Web with empty item list returns empty items`() =
        runTest {
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdDataEmpty())

            val result = repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.Web)

            assertThat(result.items).isEmpty()
            assertThat(result.nextPage.nextWebIdx).isEqualTo(2)
        }

    // ------------------------------------------------------------------
    // getPopularVideos (App)
    // ------------------------------------------------------------------

    @Test
    fun `getPopularVideos App throws when gRPC stub is null`() =
        runTest {
            // 通道未就绪时不回退 Web，也不静默返回空列表，而是抛明确异常供上层报错重试
            val error =
                runCatching {
                    repository.getPopularVideos(
                        page = PopularVideoPage(),
                        preferApiType = ApiType.App,
                    )
                }.exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalStateException::class.java)
        }

    // ------------------------------------------------------------------
    // getRecommendVideos (App)
    // ------------------------------------------------------------------

    @Test
    fun `getRecommendVideos App calls getFeedIndex with accessKey`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeRcmdIndexData(emptyList()))

            repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.App)

            coVerify { BiliHttpApi.getFeedIndex(any(), eq("test-access-token")) }
        }

    @Test
    fun `getRecommendVideos App maps items and advances nextPage`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            val rcmdData = fakeRcmdIndexData(listOf(fakeAppRcmdItem(idx = 1), fakeAppRcmdItem(idx = 2)))
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdData)

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextAppIdx = 1),
                    preferApiType = ApiType.App,
                )

            assertThat(result.items).hasSize(2)
            assertThat(result.items[0].title).isEqualTo("app-rcmd-1")
            assertThat(result.items[1].title).isEqualTo("app-rcmd-2")
            assertThat(result.nextPage.nextAppIdx).isEqualTo(3)
        }

    @Test
    fun `getRecommendVideos App advances cursor from raw items when all filtered out`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            // 两个卡片都不是 av，会被过滤掉；游标应基于原始列表的最后 idx(9) 推进到 10，
            // 而不是停留原地导致重复请求同一页
            val raw =
                listOf(
                    fakeAppRcmdItem(idx = 5).copy(cardGoto = "live"),
                    fakeAppRcmdItem(idx = 9).copy(cardGoto = "bangumi"),
                )
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeRcmdIndexData(raw))

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextAppIdx = 1),
                    preferApiType = ApiType.App,
                )

            assertThat(result.items).isEmpty()
            assertThat(result.nextPage.nextAppIdx).isEqualTo(10)
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeVideoInfo(
        aid: Long = 1L,
        title: String = "title",
    ) = VideoInfo(
        bvid = "BV$aid",
        aid = aid,
        videos = 1,
        tid = 0,
        tname = "test",
        copyright = 1,
        pic = "http://pic.test/$aid",
        title = title,
        pubdate = 1700000000,
        desc = "desc",
        state = 0,
        duration = 120,
        rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
        owner = VideoOwner(mid = 1L, name = "up", face = "http://face.test"),
        stat = VideoStat(aid = aid, _view = 1000L, danmaku = 50, like = 10),
        dynamic = "",
        cid = aid * 10,
        dimension = Dimension(1920, 1080, 0),
    )

    private fun fakeRcmdItem(id: Long = 1L) =
        RcmdTopData.RcmdItem(
            businessInfo = null,
            bvid = "BV$id",
            cid = id * 10,
            duration = 120,
            enableVt = 0,
            goto = "av",
            id = id,
            isFollowed = 0,
            isStock = 0,
            pic = "http://pic.test/$id",
            pos = 0,
            pubdate = 1700000000,
            showInfo = 0,
            title = "rcmd-$id",
            trackId = "t$id",
            uri = "uri$id",
            stat = RcmdTopData.RcmdItem.Stat(danmaku = 30, like = 10, view = 5000, vt = 0),
            owner = RcmdTopData.RcmdItem.Owner(face = "http://face.test", mid = 1L, name = "up"),
        )

    private fun rcmdDataEmpty() =
        RcmdTopData(
            item = emptyList(),
            mid = 1L,
            preloadExposePct = 0.0,
            preloadFloorExposePct = 0.0,
        )

    private fun fakeRcmdIndexData(items: List<RcmdIndexData.RcmdItem>) =
        RcmdIndexData(
            config =
                RcmdIndexData.Config(
                    autoRefreshTime = 0,
                    autoRefreshTimeByActive = 0,
                    autoRefreshTimeByAppear = 0,
                    autoplayCard = 0,
                    cardDensityExp = 0,
                    column = 0,
                    enableRcmdGuide = false,
                    feedCleanAbtest = 0,
                    homeTransferTest = 0,
                    inlineSound = 0,
                    isBackToHomepage = false,
                    showInlineDanmaku = 0,
                    storyModeV2GuideExp = 0,
                    toast = kotlinx.serialization.json.JsonNull,
                    visibleArea = 0,
                ),
            items = items,
        )

    private fun fakeAppRcmdItem(idx: Int = 1) =
        RcmdIndexData.RcmdItem(
            args =
                RcmdIndexData.RcmdItem.Args(
                    aid = (idx * 10).toLong(),
                    rid = 0,
                    rname = "",
                    tid = 0,
                    tname = "",
                    upId = 1L,
                    upName = "up",
                ),
            cardGoto = "av",
            cardType = "small_cover_v5",
            cover = "http://cover.test/$idx",
            coverLeftText1 = "5000",
            coverLeftText2 = "10",
            coverRightText = "5:00",
            idx = idx,
            param = "${idx * 10}",
            title = "app-rcmd-$idx",
            uri = "bilibili://video/${idx * 10}",
            threePointV2 = emptyList(),
        )
}
