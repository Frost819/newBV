package dev.frost819.newbv.biliapi.entity.video

import bilibili.app.view.v1.history
import bilibili.app.view.v1.playerIcon
import bilibili.app.view.v1.reqUser
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UserActions]、[VideoDetail.History]、[VideoDetail.PlayerIcon]、[VideoDetail.fromVideoDetail]
 * 实体转换方法的单元测试。
 */
class VideoDetailEntityTest {
    @Test
    fun `UserActions defaults are all false`() {
        val actions = UserActions()
        assertThat(actions.like).isFalse()
        assertThat(actions.favorite).isFalse()
        assertThat(actions.coin).isFalse()
        assertThat(actions.dislike).isFalse()
    }

    @Test
    fun `PlayerIcon fromPlayerIcon HTTP maps url2 to idle and url1 to moving`() {
        val httpPlayerIcon =
            dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.PlayerIcon(
                url1 = "http://moving.test",
                hash1 = "",
                url2 = "http://idle.test",
                hash2 = "",
                ctime = 0,
            )

        val icon = VideoDetail.PlayerIcon.fromPlayerIcon(httpPlayerIcon)

        assertThat(icon?.idle).isEqualTo("http://idle.test")
        assertThat(icon?.moving).isEqualTo("http://moving.test")
    }

    @Test
    fun `PlayerIcon fromPlayerIcon HTTP null returns null`() {
        val icon =
            VideoDetail.PlayerIcon.fromPlayerIcon(
                null as dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.PlayerIcon?,
            )
        assertThat(icon).isNull()
    }

    @Test
    fun `History holds progress and lastPlayedCid`() {
        val history = VideoDetail.History(progress = 120, lastPlayedCid = 456L)
        assertThat(history.progress).isEqualTo(120)
        assertThat(history.lastPlayedCid).isEqualTo(456L)
    }

    @Test
    fun `Stat holds all stat fields`() {
        val stat =
            VideoDetail.Stat(
                view = 10000,
                danmaku = 500,
                reply = 200,
                favorite = 100,
                coin = 50,
                share = 10,
                like = 1000,
                historyRank = 5,
            )
        assertThat(stat.view).isEqualTo(10000)
        assertThat(stat.danmaku).isEqualTo(500)
        assertThat(stat.reply).isEqualTo(200)
        assertThat(stat.favorite).isEqualTo(100)
        assertThat(stat.coin).isEqualTo(50)
        assertThat(stat.share).isEqualTo(10)
        assertThat(stat.like).isEqualTo(1000)
        assertThat(stat.historyRank).isEqualTo(5)
    }

    @Test
    fun `PlayerIcon fromPlayerIcon AppSeasonData null returns null`() {
        val icon =
            VideoDetail.PlayerIcon.fromPlayerIcon(
                null as dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData.PlayerIcon?,
            )
        assertThat(icon).isNull()
    }

    @Test
    fun `PlayerIcon fromPlayerIcon AppSeasonData with null urls returns null`() {
        val httpPlayerIcon =
            dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData.PlayerIcon(
                ctime = 0,
                dragData = null,
                hash1 = null,
                hash2 = null,
                noDragData = null,
                url1 = null,
                url2 = null,
            )

        val icon =
            VideoDetail.PlayerIcon.fromPlayerIcon(
                httpPlayerIcon,
            )

        assertThat(icon).isNull()
    }

    @Test
    fun `PlayerIcon fromPlayerIcon AppSeasonData with valid urls maps correctly`() {
        val httpPlayerIcon =
            dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData.PlayerIcon(
                ctime = 0,
                dragData = null,
                hash1 = null,
                hash2 = null,
                noDragData = null,
                url1 = "http://moving.test",
                url2 = "http://idle.test",
            )

        val icon = VideoDetail.PlayerIcon.fromPlayerIcon(httpPlayerIcon)

        assertThat(icon).isNotNull()
        assertThat(icon!!.idle).isEqualTo("http://idle.test")
        assertThat(icon.moving).isEqualTo("http://moving.test")
    }

    @Test
    fun `PlayerIcon fromPlayerIcon AppSeasonData with only url1 null returns null`() {
        val httpPlayerIcon =
            dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData.PlayerIcon(
                ctime = 0,
                dragData = null,
                hash1 = null,
                hash2 = null,
                noDragData = null,
                url1 = null,
                url2 = "http://idle.test",
            )

        val icon = VideoDetail.PlayerIcon.fromPlayerIcon(httpPlayerIcon)

        assertThat(icon).isNull()
    }

    @Test
    fun `fromVideoDetail maps all fields from HTTP VideoDetail`() {
        val httpVideoDetail = fakeHttpVideoDetail()

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.bvid).isEqualTo("BV1xx")
        assertThat(result.aid).isEqualTo(993403941L)
        assertThat(result.cid).isEqualTo(1051761130L)
        assertThat(result.cover).isEqualTo("http://pic.test")
        assertThat(result.title).isEqualTo("测试视频")
        assertThat(result.description).isEqualTo("描述")
        assertThat(result.stat.view).isEqualTo(100)
        assertThat(result.author.mid).isEqualTo(1L)
        assertThat(result.pages).hasSize(1)
        assertThat(result.pages[0].cid).isEqualTo(1051761130L)
        assertThat(result.relatedVideos).hasSize(1)
        assertThat(result.relatedVideos[0].aid).isEqualTo(200L)
        assertThat(result.tags).hasSize(1)
        assertThat(result.tags[0].id).isEqualTo(42)
        assertThat(result.userActions.like).isFalse()
        assertThat(result.history.progress).isEqualTo(0)
        assertThat(result.history.lastPlayedCid).isEqualTo(0L)
        assertThat(result.playerIcon).isNull()
        assertThat(result.isUpowerExclusive).isFalse()
    }

    @Test
    fun `fromVideoDetail with null redirectUrl sets redirectToEp false`() {
        val httpVideoDetail = fakeHttpVideoDetail(redirectUrl = null, isUpowerExclusive = null)

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.redirectToEp).isFalse()
        assertThat(result.epid).isNull()
        assertThat(result.isUpowerExclusive).isFalse()
    }

    @Test
    fun `fromVideoDetail with ep redirectUrl sets redirectToEp true and extracts epid`() {
        val httpVideoDetail =
            fakeHttpVideoDetail(redirectUrl = "https://www.bilibili.com/bangumi/play/ep12345?from=xxx")

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.redirectToEp).isTrue()
        assertThat(result.epid).isEqualTo(12345)
    }

    @Test
    fun `fromVideoDetail with null related returns empty list`() {
        val httpVideoDetail = fakeHttpVideoDetail(related = null)

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.relatedVideos).isEmpty()
    }

    @Test
    fun `fromVideoDetail with empty argueMsg returns null argueTip`() {
        val httpVideoDetail = fakeHttpVideoDetail()

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.argueTip).isNull()
    }

    @Test
    fun `fromVideoDetail with non-empty argueMsg returns argueTip`() {
        val httpVideoDetail =
            fakeHttpVideoDetail(
                stat =
                    dev.frost819.newbv.biliapi.http.entity.video.VideoStat(
                        aid = 993403941L,
                        _view = 100L,
                        danmaku = 10,
                        argueMsg = "争议提示",
                    ),
            )

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.argueTip).isEqualTo("争议提示")
    }

    @Test
    fun `fromVideoDetail with isUpowerExclusive true maps correctly`() {
        val httpVideoDetail = fakeHttpVideoDetail(isUpowerExclusive = true)

        val result = VideoDetail.fromVideoDetail(httpVideoDetail)

        assertThat(result.isUpowerExclusive).isTrue()
    }

    // ------------------------------------------------------------------
    // gRPC conversion methods
    // ------------------------------------------------------------------

    @Test
    fun `History fromHistory gRPC maps progress and cid`() {
        val grpcHistory =
            history {
                cid = 456L
                progress = 120L
            }

        val result = VideoDetail.History.fromHistory(grpcHistory)

        assertThat(result.progress).isEqualTo(120)
        assertThat(result.lastPlayedCid).isEqualTo(456L)
    }

    @Test
    fun `History fromHistory gRPC with zero progress`() {
        val grpcHistory =
            history {
                cid = 0L
                progress = 0L
            }

        val result = VideoDetail.History.fromHistory(grpcHistory)

        assertThat(result.progress).isEqualTo(0)
        assertThat(result.lastPlayedCid).isEqualTo(0L)
    }

    @Test
    fun `History fromHistory gRPC with negative progress`() {
        val grpcHistory =
            history {
                cid = 999L
                progress = -1L
            }

        val result = VideoDetail.History.fromHistory(grpcHistory)

        assertThat(result.progress).isEqualTo(-1)
        assertThat(result.lastPlayedCid).isEqualTo(999L)
    }

    @Test
    fun `UserActions fromReqUser maps all actions as true`() {
        val grpcReqUser =
            reqUser {
                attention = 1
                guestAttention = 0
                favorite = 1
                like = 1
                dislike = 1
                coin = 1
            }

        val actions = UserActions.fromReqUser(grpcReqUser)

        assertThat(actions.like).isTrue()
        assertThat(actions.favorite).isTrue()
        assertThat(actions.coin).isTrue()
        assertThat(actions.dislike).isTrue()
    }

    @Test
    fun `UserActions fromReqUser maps all actions as false when zero`() {
        val grpcReqUser = reqUser { }

        val actions = UserActions.fromReqUser(grpcReqUser)

        assertThat(actions.like).isFalse()
        assertThat(actions.favorite).isFalse()
        assertThat(actions.coin).isFalse()
        assertThat(actions.dislike).isFalse()
    }

    @Test
    fun `UserActions fromReqUser maps mixed actions`() {
        val grpcReqUser =
            reqUser {
                like = 1
                favorite = 0
                coin = 1
                dislike = 0
            }

        val actions = UserActions.fromReqUser(grpcReqUser)

        assertThat(actions.like).isTrue()
        assertThat(actions.favorite).isFalse()
        assertThat(actions.coin).isTrue()
        assertThat(actions.dislike).isFalse()
    }

    @Test
    fun `PlayerIcon fromPlayerIcon gRPC maps url2 to idle and url1 to moving`() {
        val grpcPlayerIcon =
            playerIcon {
                url1 = "http://moving.test"
                url2 = "http://idle.test"
            }

        val icon = VideoDetail.PlayerIcon.fromPlayerIcon(grpcPlayerIcon)

        assertThat(icon.idle).isEqualTo("http://idle.test")
        assertThat(icon.moving).isEqualTo("http://moving.test")
    }

    @Test
    fun `PlayerIcon fromPlayerIcon gRPC with empty urls`() {
        val grpcPlayerIcon = playerIcon { }

        val icon = VideoDetail.PlayerIcon.fromPlayerIcon(grpcPlayerIcon)

        assertThat(icon.idle).isEqualTo("")
        assertThat(icon.moving).isEqualTo("")
    }

    private fun fakeHttpVideoDetail(
        redirectUrl: String? = null,
        related: List<dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo>? =
            listOf(
                dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo(
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
                    rights =
                        dev.frost819.newbv.biliapi.http.entity.video.VideoRights(
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            arcPay = 0,
                        ),
                    owner =
                        dev.frost819.newbv.biliapi.http.entity.video.VideoOwner(
                            mid = 2L,
                            name = "UP2",
                            face = "",
                        ),
                    stat =
                        dev.frost819.newbv.biliapi.http.entity.video.VideoStat(
                            aid = 200L,
                            _view = 50L,
                            danmaku = 10,
                        ),
                    dynamic = "",
                    cid = 200L,
                    dimension =
                        dev.frost819.newbv.biliapi.http.entity.video
                            .Dimension(1920, 1080, 0),
                    rcmdReason = "",
                ),
            ),
        isUpowerExclusive: Boolean? = false,
        stat: dev.frost819.newbv.biliapi.http.entity.video.VideoStat =
            dev.frost819.newbv.biliapi.http.entity.video.VideoStat(
                aid = 993403941L,
                _view = 100L,
                danmaku = 10,
            ),
    ): dev.frost819.newbv.biliapi.http.entity.video.VideoDetail {
        val videoInfo =
            dev.frost819.newbv.biliapi.http.entity.video.VideoInfo(
                bvid = "BV1xx",
                aid = 993403941L,
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
                rights =
                    dev.frost819.newbv.biliapi.http.entity.video.VideoRights(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        arcPay = 0,
                    ),
                owner =
                    dev.frost819.newbv.biliapi.http.entity.video.VideoOwner(
                        mid = 1L,
                        name = "UP主",
                        face = "http://face.test",
                    ),
                stat = stat,
                dynamic = "",
                cid = 1051761130L,
                dimension =
                    dev.frost819.newbv.biliapi.http.entity.video
                        .Dimension(1920, 1080, 0),
                redirectUrl = redirectUrl,
                isUpowerExclusive = isUpowerExclusive,
                pages =
                    listOf(
                        dev.frost819.newbv.biliapi.http.entity.video.VideoPage(
                            cid = 1051761130L,
                            page = 1,
                            from = "vupload",
                            part = "第一P",
                            duration = 300,
                            vid = "",
                            weblink = "",
                            dimension =
                                dev.frost819.newbv.biliapi.http.entity.video
                                    .Dimension(1920, 1080, 0),
                        ),
                    ),
            )
        return dev.frost819.newbv.biliapi.http.entity.video.VideoDetail(
            view = videoInfo,
            card =
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
                ),
            tags =
                listOf(
                    dev.frost819.newbv.biliapi.http.entity.video.VideoDetail.Tag(
                        tagId = 42,
                        tagName = "标签",
                        musicId = "",
                        tagType = "",
                        jumpUrl = "",
                    ),
                ),
            related = related,
            spec = null,
            hotShare =
                dev.frost819.newbv.biliapi.http.entity.video.VideoDetail.HotShare(
                    show = false,
                    list = kotlinx.serialization.json.JsonArray(emptyList()),
                ),
            elec = null,
            recommend = null,
            viewAddit = kotlinx.serialization.json.JsonObject(emptyMap()),
            guide = null,
            queryTags = null,
        )
    }
}
