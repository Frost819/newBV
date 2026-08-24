package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData
import dev.frost819.newbv.biliapi.http.entity.season.Episode
import dev.frost819.newbv.biliapi.http.entity.season.NewEP
import dev.frost819.newbv.biliapi.http.entity.season.Publish
import dev.frost819.newbv.biliapi.http.entity.season.SeasonRights
import dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData
import dev.frost819.newbv.biliapi.http.entity.video.Dimension
import dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoOwner
import dev.frost819.newbv.biliapi.http.entity.video.VideoRights
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * [VideoDetailRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例与子 Repository，
 * 验证 Web 路径下视频详情获取的参数传递与数据转换。
 */
class VideoDetailRepositoryUnitTest {
    private lateinit var repository: VideoDetailRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var channelRepository: ChannelRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var coinRepository: CoinRepository

    companion object {
        private const val AID = 993403941L
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = mockk()
        channelRepository = mockk()
        favoriteRepository = mockk()
        likeRepository = mockk()
        coinRepository = mockk()
        repository =
            VideoDetailRepository(
                authRepository,
                channelRepository,
                favoriteRepository,
                likeRepository,
                coinRepository,
            )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    @Test
    fun `getVideoDetail Web returns VideoDetail with mapped fields`() =
        runTest {
            val httpVideoDetail = fakeHttpVideoDetail()
            coEvery { BiliHttpApi.getVideoDetail(any()) } returns
                BiliResponse(code = 0, message = "", data = httpVideoDetail)
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeVideoMoreInfo())
            coEvery { favoriteRepository.checkVideoFavoured(any(), any()) } returns true
            coEvery { likeRepository.checkVideoLiked(any(), any()) } returns false
            coEvery { coinRepository.checkVideoCoined(any(), any()) } returns true

            val result = repository.getVideoDetail(aid = AID, preferApiType = ApiType.Web)

            assertThat(result.aid).isEqualTo(AID)
            assertThat(result.bvid).isEqualTo("BV1xx")
            assertThat(result.title).isEqualTo("测试视频")
            assertThat(result.userActions.favorite).isTrue()
            assertThat(result.userActions.like).isFalse()
            assertThat(result.userActions.coin).isTrue()
            assertThat(result.history.progress).isEqualTo(60)
            assertThat(result.history.lastPlayedCid).isEqualTo(1051761130L)
            assertThat(result.tags).hasSize(1)
            assertThat(result.tags[0].id).isEqualTo(42)
            assertThat(result.tags[0].name).isEqualTo("标签")
        }

    @Test
    fun `getVideoDetail Web passes aid to getVideoDetail`() =
        runTest {
            coEvery { BiliHttpApi.getVideoDetail(any()) } returns
                BiliResponse(code = 0, message = "", data = fakeHttpVideoDetail())
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeVideoMoreInfo())
            coEvery { favoriteRepository.checkVideoFavoured(any(), any()) } returns false
            coEvery { likeRepository.checkVideoLiked(any(), any()) } returns false
            coEvery { coinRepository.checkVideoCoined(any(), any()) } returns false

            repository.getVideoDetail(aid = AID, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getVideoDetail(eq(AID)) }
        }

    @Test
    fun `getVideoDetail Web defaults history when getVideoMoreInfo fails`() =
        runTest {
            coEvery { BiliHttpApi.getVideoDetail(any()) } returns
                BiliResponse(code = 0, message = "", data = fakeHttpVideoDetail())
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } throws RuntimeException("network error")
            coEvery { favoriteRepository.checkVideoFavoured(any(), any()) } returns false
            coEvery { likeRepository.checkVideoLiked(any(), any()) } returns false
            coEvery { coinRepository.checkVideoCoined(any(), any()) } returns false

            val result = repository.getVideoDetail(aid = AID, preferApiType = ApiType.Web)

            assertThat(result.history.progress).isEqualTo(0)
            assertThat(result.history.lastPlayedCid).isEqualTo(0L)
        }

    @Test
    fun `getVideoDetail Web defaults userActions when check methods fail`() =
        runTest {
            coEvery { BiliHttpApi.getVideoDetail(any()) } returns
                BiliResponse(code = 0, message = "", data = fakeHttpVideoDetail())
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeVideoMoreInfo())
            coEvery { favoriteRepository.checkVideoFavoured(any(), any()) } throws RuntimeException("err")
            coEvery { likeRepository.checkVideoLiked(any(), any()) } throws RuntimeException("err")
            coEvery { coinRepository.checkVideoCoined(any(), any()) } throws RuntimeException("err")

            val result = repository.getVideoDetail(aid = AID, preferApiType = ApiType.Web)

            assertThat(result.userActions.favorite).isFalse()
            assertThat(result.userActions.like).isFalse()
            assertThat(result.userActions.coin).isFalse()
        }

    private fun fakeHttpVideoDetail(): VideoDetail {
        val videoInfo =
            dev.frost819.newbv.biliapi.http.entity.video.VideoInfo(
                bvid = "BV1xx",
                aid = AID,
                videos = 1,
                tid = 0,
                tname = "test",
                copyright = 1,
                pic = "http://pic.test",
                title = "测试视频",
                pubdate = 1700000000,
                desc = "描述",
                state = 0,
                duration = 300,
                rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
                owner = VideoOwner(mid = 1L, name = "UP主", face = "http://face.test"),
                stat = VideoStat(aid = AID, danmaku = 100, like = 200),
                dynamic = "",
                cid = 1051761130L,
                dimension = Dimension(1920, 1080, 0),
            )
        return VideoDetail(
            view = videoInfo,
            card = fakeUserCardData(),
            tags =
                listOf(
                    VideoDetail.Tag(
                        tagId = 42,
                        tagName = "标签",
                        musicId = "",
                        tagType = "",
                        jumpUrl = "",
                    ),
                ),
            related =
                listOf(
                    RelatedVideoInfo(
                        bvid = "BV2xx",
                        aid = 200L,
                        videos = 1,
                        tid = 1,
                        tname = "综合",
                        copyright = 1,
                        pic = "http://pic2.test",
                        title = "相关视频",
                        pubdate = 1700000000,
                        ctime = 1700000000,
                        desc = "",
                        state = 0,
                        duration = 600,
                        rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
                        owner = VideoOwner(mid = 2L, name = "UP2", face = ""),
                        stat = VideoStat(aid = 200L, danmaku = 50),
                        dynamic = "",
                        cid = 200L,
                        dimension = Dimension(1920, 1080, 0),
                        rcmdReason = "",
                    ),
                ),
            spec = null,
            hotShare = VideoDetail.HotShare(show = false, list = kotlinx.serialization.json.JsonArray(emptyList())),
            elec = null,
            recommend = null,
            viewAddit = kotlinx.serialization.json.JsonObject(emptyMap()),
            guide = null,
            queryTags = null,
        )
    }

    private fun fakeUserCardData() =
        dev.frost819.newbv.biliapi.http.entity.user.UserCardData(
            card =
                dev.frost819.newbv.biliapi.http.entity.user.UserCardData.UserCardInfo(
                    mid = "1",
                    name = "UP主",
                    approve = false,
                    sex = "男",
                    rank = 10000,
                    face = "http://face.test",
                    faceNft = 0,
                    faceNftType = 0,
                    displayRank = "",
                    regtime = 0,
                    spacesta = 0,
                    birthday = "",
                    place = "",
                    description = "签名",
                    article = 0,
                    fans = 100,
                    friend = 50,
                    attention = 50,
                    sign = "签名",
                    levelInfo =
                        dev.frost819.newbv.biliapi.http.entity.user.LevelInfo(
                            currentLevel = 5,
                            currentMin = 0,
                            currentExp = 0,
                            nextExp = 0,
                        ),
                    pendant =
                        dev.frost819.newbv.biliapi.http.entity.user.Pendant(
                            pid = 0,
                            name = "",
                            image = "",
                            expire = 0,
                            imageEnhance = "",
                            imageEnhanceFrame = "",
                        ),
                    nameplate =
                        dev.frost819.newbv.biliapi.http.entity.user.Nameplate(
                            nid = 0,
                            name = "",
                            image = "",
                            imageSmall = "",
                            level = "",
                            condition = "",
                        ),
                    official =
                        dev.frost819.newbv.biliapi.http.entity.user.Official(
                            role = 0,
                            title = "",
                            desc = "",
                            type = -1,
                        ),
                    officialVerify =
                        dev.frost819.newbv.biliapi.http.entity.user.OfficialVerify(
                            type = -1,
                            desc = "",
                        ),
                    vip =
                        dev.frost819.newbv.biliapi.http.entity.user.Vip(
                            type = 0,
                            status = 0,
                            dueDate = 0L,
                            vipPayType = 0,
                            themeType = 0,
                            label =
                                dev.frost819.newbv.biliapi.http.entity.user.Vip.Label(
                                    path = "",
                                    text = "",
                                    labelTheme = "",
                                    textColor = "",
                                    bgStyle = 0,
                                    bgColor = "",
                                    borderColor = "",
                                ),
                            avatarSubscript = 0,
                            nicknameColor = "",
                            role = 0,
                            avatarSubscriptUrl = "",
                            tvVipStatus = 0,
                            tvVipPayType = 0,
                        ),
                    isSeniorMember = 0,
                ),
            following = false,
            archiveCount = 10,
            articleCount = 0,
            follower = 100,
            likeNum = 200,
        )

    // ------------------------------------------------------------------
    // getUgcPages
    // ------------------------------------------------------------------

    @Test
    fun `getUgcPages Web returns mapped VideoPage list`() =
        runTest {
            val videoInfo =
                dev.frost819.newbv.biliapi.http.entity.video.VideoInfo(
                    bvid = "BV1xx",
                    aid = AID,
                    videos = 2,
                    tid = 0,
                    tname = "test",
                    copyright = 1,
                    pic = "http://pic.test",
                    title = "测试视频",
                    pubdate = 1700000000,
                    desc = "描述",
                    state = 0,
                    duration = 300,
                    rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
                    owner = VideoOwner(mid = 1L, name = "UP主", face = "http://face.test"),
                    stat = VideoStat(aid = AID, danmaku = 100, like = 200),
                    dynamic = "",
                    cid = 1051761130L,
                    dimension = Dimension(1920, 1080, 0),
                    pages =
                        listOf(
                            dev.frost819.newbv.biliapi.http.entity.video.VideoPage(
                                cid = 1051761130L,
                                page = 1,
                                from = "vupload",
                                part = "第一P",
                                duration = 150,
                                vid = "",
                                weblink = "",
                                dimension = Dimension(1920, 1080, 0),
                            ),
                            dev.frost819.newbv.biliapi.http.entity.video.VideoPage(
                                cid = 1051761131L,
                                page = 2,
                                from = "vupload",
                                part = "第二P",
                                duration = 150,
                                vid = "",
                                weblink = "",
                                dimension = Dimension(1920, 1080, 0),
                            ),
                        ),
                )
            coEvery { BiliHttpApi.getVideoInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = videoInfo)

            val result = repository.getUgcPages(aid = AID, preferApiType = ApiType.Web)

            assertThat(result).hasSize(2)
            assertThat(result[0].cid).isEqualTo(1051761130L)
            assertThat(result[0].index).isEqualTo(1)
            assertThat(result[0].title).isEqualTo("第一P")
            assertThat(result[0].duration).isEqualTo(150)
            assertThat(result[1].cid).isEqualTo(1051761131L)
            assertThat(result[1].title).isEqualTo("第二P")
        }

    @Test
    fun `getUgcPages Web returns empty list when pages is empty`() =
        runTest {
            coEvery { BiliHttpApi.getVideoInfo(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.video.VideoInfo(
                            bvid = "BV1xx",
                            aid = AID,
                            videos = 1,
                            tid = 0,
                            tname = "test",
                            copyright = 1,
                            pic = "http://pic.test",
                            title = "测试视频",
                            pubdate = 1700000000,
                            desc = "描述",
                            state = 0,
                            duration = 300,
                            rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
                            owner = VideoOwner(mid = 1L, name = "UP主", face = "http://face.test"),
                            stat = VideoStat(aid = AID, danmaku = 100, like = 200),
                            dynamic = "",
                            cid = 1051761130L,
                            dimension = Dimension(1920, 1080, 0),
                        ),
                )

            val result = repository.getUgcPages(aid = AID, preferApiType = ApiType.Web)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getUgcPages Web returns empty list when API throws`() =
        runTest {
            coEvery { BiliHttpApi.getVideoInfo(any(), any()) } throws RuntimeException("network error")

            val result = repository.getUgcPages(aid = AID, preferApiType = ApiType.Web)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getUgcPages App returns empty list when channel is null`() =
        runTest {
            val result = repository.getUgcPages(aid = AID, preferApiType = ApiType.App)

            assertThat(result).isEmpty()
        }

    // ------------------------------------------------------------------
    // getPgcVideoDetail
    // ------------------------------------------------------------------

    @Test
    fun `getPgcVideoDetail Web returns SeasonDetail with playerIcon when episodes exist`() =
        runTest {
            coEvery { BiliHttpApi.getWebSeasonInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeWebSeasonData(episodes = listOf(fakeEpisode())))
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeVideoMoreInfo().copy(playerIcon = fakePlayerIcon()))

            val result = repository.getPgcVideoDetail(epid = 1, preferApiType = ApiType.Web)

            assertThat(result.title).isEqualTo("测试番剧")
            assertThat(result.seasonId).isEqualTo(40000)
            assertThat(result.playerIcon).isNotNull()
            assertThat(result.playerIcon!!.idle).isEqualTo("http://idle.test")
            assertThat(result.playerIcon!!.moving).isEqualTo("http://moving.test")
        }

    @Test
    fun `getPgcVideoDetail Web returns SeasonDetail without calling getVideoMoreInfo when episodes is empty`() =
        runTest {
            coEvery { BiliHttpApi.getWebSeasonInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeWebSeasonData(episodes = emptyList()))

            val result = repository.getPgcVideoDetail(epid = 1, preferApiType = ApiType.Web)

            assertThat(result.playerIcon).isNull()
            coVerify(exactly = 0) { BiliHttpApi.getVideoMoreInfo(any(), any()) }
        }

    @Test
    fun `getPgcVideoDetail Web defaults playerIcon to null when getVideoMoreInfo fails`() =
        runTest {
            coEvery { BiliHttpApi.getWebSeasonInfo(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeWebSeasonData(episodes = listOf(fakeEpisode())))
            coEvery { BiliHttpApi.getVideoMoreInfo(any(), any()) } throws RuntimeException("network error")

            val result = repository.getPgcVideoDetail(epid = 1, preferApiType = ApiType.Web)

            assertThat(result.title).isEqualTo("测试番剧")
            assertThat(result.playerIcon).isNull()
        }

    @Test
    @Disabled("PGC season detail is Web-only")
    fun `getPgcVideoDetail App returns SeasonDetail from AppSeasonData`() =
        runTest {
            every { authRepository.accessToken } returns "test-token"
            coEvery {
                BiliHttpApi.getAppSeasonInfo(
                    seasonId = any(),
                    epId = any(),
                    mobiApp = any(),
                    adExtra = any(),
                    autoPlay = any(),
                    build = any(),
                    cLocale = any(),
                    channel = any(),
                    disableRcmd = any(),
                    fromAv = any(),
                    fromSpmid = any(),
                    isShowAllSeries = any(),
                    platform = any(),
                    sLocale = any(),
                    spmid = any(),
                    statistics = any(),
                    trackPath = any(),
                    trackid = any(),
                    ts = any(),
                    accessKey = any(),
                )
            } returns BiliResponse(code = 0, message = "", data = fakeAppSeasonData())

            val result = repository.getPgcVideoDetail(epid = 1, preferApiType = ApiType.App)

            assertThat(result.title).isEqualTo("测试番剧")
            assertThat(result.seasonId).isEqualTo(40000)
        }

    @Test
    @Disabled("PGC season detail is Web-only")
    fun `getPgcVideoDetail App uses empty string when accessToken is null`() =
        runTest {
            every { authRepository.accessToken } returns null
            coEvery {
                BiliHttpApi.getAppSeasonInfo(
                    seasonId = any(),
                    epId = any(),
                    mobiApp = any(),
                    adExtra = any(),
                    autoPlay = any(),
                    build = any(),
                    cLocale = any(),
                    channel = any(),
                    disableRcmd = any(),
                    fromAv = any(),
                    fromSpmid = any(),
                    isShowAllSeries = any(),
                    platform = any(),
                    sLocale = any(),
                    spmid = any(),
                    statistics = any(),
                    trackPath = any(),
                    trackid = any(),
                    ts = any(),
                    accessKey = any(),
                )
            } returns BiliResponse(code = 0, message = "", data = fakeAppSeasonData())

            val result = repository.getPgcVideoDetail(seasonId = 40000, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.getAppSeasonInfo(
                    seasonId = 40000,
                    epId = null,
                    mobiApp = "android_hd",
                    accessKey = "",
                )
            }
        }

    // ------------------------------------------------------------------
    // getPgcVideoDetail helpers
    // ------------------------------------------------------------------

    private fun fakeWebSeasonData(episodes: List<Episode> = emptyList()): WebSeasonData =
        WebSeasonData(
            activity = WebSeasonData.Activity(headBgUrl = "", id = 0, title = ""),
            alias = "",
            bkgCover = "",
            cover = "http://cover.test",
            episodes = episodes,
            evaluate = "简介",
            jpTitle = "",
            link = "http://link.test",
            mediaId = 100,
            mode = 2,
            newEp = NewEP(id = 1, desc = "更新"),
            positive = WebSeasonData.Positive(id = 0, title = ""),
            publish =
                Publish(
                    _isFinish = 1,
                    _isStarted = 1,
                    pubTime = "2024-01-01",
                    pubTimeShow = "2024-01-01",
                    unknowPubDate = 0,
                    weekday = 0,
                ),
            rating = null,
            record = "",
            rights =
                SeasonRights(
                    allowBp = 0,
                    allowBpRank = 0,
                    allowDownload = 0,
                    allowReview = 0,
                    areaLimit = 0,
                    banAreaShow = 0,
                    canWatch = 1,
                    copyright = "bilibili",
                    forbidPre = 0,
                    isCoverShow = 1,
                    isPreview = 0,
                    onlyVipDownload = 0,
                    resource = "",
                    watchPlatform = 0,
                ),
            seasonId = 40000,
            seasonTitle = "番剧标题",
            seasons = emptyList(),
            section = emptyList(),
            series = WebSeasonData.Series(displayType = 0, seriesId = 0, seriesTitle = ""),
            shareCopy = "",
            shareSubTitle = "",
            shareUrl = "",
            show = WebSeasonData.Show(wideScreen = 1),
            squareCover = "",
            stat =
                WebSeasonData.SeasonStat(
                    coins = 0,
                    danmakus = 0,
                    favorites = 0,
                    likes = 0,
                    reply = 0,
                    share = 0,
                    views = 0L,
                ),
            status = 0,
            styles = listOf("热血"),
            subtitle = "",
            title = "测试番剧",
            total = 12,
            type = 1,
            upInfo = null,
            userStatus =
                WebSeasonData.UserStatus(
                    areaLimit = 0,
                    banAreaShow = 0,
                    follow = 0,
                    followStatus = 0,
                    login = 1,
                    pay = 0,
                    payPackPaid = 0,
                    sponsor = 0,
                ),
        )

    private fun fakeEpisode(
        aid: Long = 100L,
        cid: Long = 200L,
    ): Episode =
        Episode(
            aid = aid,
            badge = "",
            badgeInfo = Episode.BadgeInfo(bgColor = "", bgColorNight = "", text = ""),
            cid = cid,
            cover = "http://cover.test",
            enableVt = false,
            id = 1,
            isViewHide = false,
            link = "http://link.test",
            pubTime = 1700000000L,
            pv = 0,
            status = 0,
            title = "第一集",
        )

    private fun fakeAppSeasonData(): AppSeasonData =
        AppSeasonData(
            actor = AppSeasonData.Actor(info = "", title = ""),
            alias = "",
            allButtons = AppSeasonData.AllButtons(watchFormal = ""),
            badge = "",
            cover = "http://cover.test",
            detail = "",
            dynamicSubtitle = "",
            earphoneConf = AppSeasonData.EarphoneConf(spPhones = JsonArray(emptyList())),
            enableVt = false,
            evaluate = "简介",
            iconFont = AppSeasonData.IconFont(name = "", text = ""),
            link = "",
            mediaBadgeInfo = Episode.BadgeInfo(bgColor = "", bgColorNight = "", text = ""),
            mediaId = 100,
            mode = 2,
            newEp = NewEP(id = 1, desc = "更新"),
            payment =
                AppSeasonData.Payment(
                    dialog = JsonNull,
                    payType = AppSeasonData.Payment.PayType(allowTicket = 0),
                    price = "0",
                    reportType = 0,
                    tvPrice = "0",
                    vipDiscountPrice = "0",
                    vipPromotion = "",
                ),
            publish =
                Publish(
                    _isFinish = 1,
                    _isStarted = 1,
                    pubTime = "2024-01-01",
                    pubTimeShow = "2024-01-01",
                    unknowPubDate = 0,
                    weekday = 0,
                ),
            rating = null,
            record = "",
            refineCover = "",
            reserve = AppSeasonData.Reserve(episodes = JsonArray(emptyList()), tip = ""),
            seasonId = 40000,
            seasonTitle = "番剧标题",
            series = AppSeasonData.Series(displayType = 0, seriesId = 0, seriesTitle = ""),
            shareCopy = "",
            shareUrl = "",
            shortLink = "",
            showSeasonType = 1,
            squareCover = "",
            staff = AppSeasonData.Staff(info = "", title = ""),
            stat =
                AppSeasonData.Stat(
                    coin = 0,
                    danmaku = 0,
                    favorite = 0,
                    favorites = 0,
                    followers = "0",
                    likes = 0,
                    play = "0",
                    reply = 0,
                    share = 0,
                    views = 0L,
                    vt = 0,
                ),
            status = 0,
            subtitle = "",
            title = "测试番剧",
            total = 12,
            type = 1,
            typeDesc = "番剧",
            typeName = "番剧",
            userStatus =
                AppSeasonData.UserStatus(
                    follow = 0,
                    followBubble = 0,
                    followStatus = 0,
                    pay = 0,
                    payFor = 0,
                    sponsor = 0,
                    vip = 0,
                    vipFrozen = 0,
                ),
        )

    private fun fakePlayerIcon() =
        VideoMoreInfo.PlayerIcon(
            url1 = "http://moving.test",
            hash1 = "hash1",
            url2 = "http://idle.test",
            hash2 = "hash2",
            ctime = 0,
        )

    private fun fakeVideoMoreInfo() =
        VideoMoreInfo(
            aid = AID,
            bvid = "BV1xx",
            allowBp = false,
            noShare = false,
            cid = 1051761130L,
            maxLimit = 0,
            pageNo = 1,
            hasNext = false,
            ipInfo =
                VideoMoreInfo.IpInfo(
                    ip = "",
                    zoneIp = "",
                    zoneId = 0,
                    country = "",
                    province = "",
                    city = "",
                ),
            loginMid = 0L,
            loginMidHash = "",
            isOwner = false,
            name = "UP主",
            permission = "",
            levelInfo =
                dev.frost819.newbv.biliapi.http.entity.user.LevelInfo(
                    currentLevel = 0,
                    currentMin = 0,
                    currentExp = 0,
                    nextExp = 0,
                ),
            vip =
                dev.frost819.newbv.biliapi.http.entity.user.Vip(
                    type = 0, status = 0, dueDate = 0L, vipPayType = 0,
                    themeType = 0,
                    label =
                        dev.frost819.newbv.biliapi.http.entity.user.Vip.Label(
                            path = "",
                            text = "",
                            labelTheme = "",
                            textColor = "",
                            bgStyle = 0,
                            bgColor = "",
                            borderColor = "",
                        ),
                    avatarSubscript = 0, nicknameColor = "", role = 0,
                    avatarSubscriptUrl = "", tvVipStatus = 0, tvVipPayType = 0,
                ),
            answerStatue = 0,
            blockTime = 0,
            role = "",
            lastPlayTime = 60000,
            lastPlayCid = 1051761130L,
            nowTime = 0,
            onlineCount = 0,
            dmMask = null,
            subtitle = null,
            playerIcon = null,
            viewPoints = kotlinx.serialization.json.JsonArray(emptyList()),
            isUgcPayPreview = false,
            previewToast = "",
            pcdnLoader = null,
            options = VideoMoreInfo.Options(is360 = false, withoutVip = false),
            guideAttention = kotlinx.serialization.json.JsonArray(emptyList()),
            jumpCard = kotlinx.serialization.json.JsonArray(emptyList()),
            operationCard = kotlinx.serialization.json.JsonArray(emptyList()),
            onlineSwitch =
                VideoMoreInfo.OnlineSwitch(
                    enableGrayDashPlayback = "",
                    newBroadcast = "",
                    realtimeDm = "",
                    subtitleSubmitSwitch = "",
                ),
            fawkes = VideoMoreInfo.Fawkes(configVersion = 0, ffVersion = 0),
            showSwitch = VideoMoreInfo.ShowSwitch(longProgress = false),
            toastBlock = false,
        )
}
