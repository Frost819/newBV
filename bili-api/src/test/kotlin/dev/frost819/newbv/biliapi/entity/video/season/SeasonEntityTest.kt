package dev.frost819.newbv.biliapi.entity.video.season

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Episode]、[Section]、[PgcSeason]、[UgcSeason] 实体 HTTP→Domain 转换方法的单元测试。
 */
class SeasonEntityTest {
    @Test
    fun `Episode fromEpisode HTTP season Episode maps all fields`() {
        val httpEpisode =
            dev.frost819.newbv.biliapi.http.entity.season.Episode(
                aid = 100L,
                badge = "",
                badgeInfo =
                    dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                        bgColor = "",
                        bgColorNight = "",
                        text = "",
                    ),
                badgeType = 0,
                bvid = "BV100",
                cid = 200L,
                cover = "http://cover.test",
                dimension = dev.frost819.newbv.biliapi.http.entity.video.Dimension(1920, 1080, 0),
                duration = 1440,
                enableVt = false,
                epId = 500,
                from = "",
                id = 1,
                isViewHide = false,
                link = "http://link.test",
                longTitle = "第一话 完整标题",
                pubTime = 1700000000L,
                pv = 0,
                status = 1,
                title = "第一话",
            )

        val episode = Episode.fromEpisode(httpEpisode)

        assertThat(episode.id).isEqualTo(1)
        assertThat(episode.aid).isEqualTo(100L)
        assertThat(episode.bvid).isEqualTo("BV100")
        assertThat(episode.cid).isEqualTo(200L)
        assertThat(episode.epid).isEqualTo(500)
        assertThat(episode.title).isEqualTo("第一话")
        assertThat(episode.longTitle).isEqualTo("第一话 完整标题")
        assertThat(episode.cover).isEqualTo("http://cover.test")
        assertThat(episode.duration).isEqualTo(1440)
        assertThat(episode.dimension).isNotNull()
        assertThat(episode.dimension!!.width).isEqualTo(1920)
        assertThat(episode.dimension!!.height).isEqualTo(1080)
    }

    @Test
    fun `Episode fromEpisode HTTP season Episode with null dimension`() {
        val httpEpisode =
            dev.frost819.newbv.biliapi.http.entity.season.Episode(
                aid = 100L,
                badge = "",
                badgeInfo =
                    dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                        bgColor = "",
                        bgColorNight = "",
                        text = "",
                    ),
                bvid = "BV100",
                cid = 200L,
                cover = "http://cover.test",
                dimension = null,
                duration = 300,
                enableVt = false,
                id = 1,
                isViewHide = false,
                link = "",
                pubTime = 0L,
                pv = 0,
                status = 0,
                title = "第1话",
            )

        val episode = Episode.fromEpisode(httpEpisode)

        assertThat(episode.dimension).isNull()
    }

    @Test
    fun `Episode fromEpisode UGC Section Episode maps all fields`() {
        val ugcEpisode =
            dev.frost819.newbv.biliapi.http.entity.video.UgcSeason.Section.Episode(
                seasonId = 1,
                sectionId = 1,
                id = 10,
                aid = 100L,
                cid = 200L,
                title = "UGC分P标题",
                attribute = 0,
                arc =
                    dev.frost819.newbv.biliapi.http.entity.video.UgcSeason.Section.Episode.Arc(
                        aid = 100L,
                        videos = 1,
                        typeId = 1,
                        typeName = "类型",
                        copyright = 1,
                        pic = "http://pic.test",
                        title = "视频标题",
                        pubDate = 1700000000,
                        ctime = 1700000000,
                        desc = "描述",
                        state = 0,
                        duration = 300,
                        rights =
                            dev.frost819.newbv.biliapi.http.entity.video.VideoRights(
                                bp = 0, elec = 0, download = 0, movie = 0, pay = 0,
                                hd5 = 0, noReprint = 0, autoplay = 0, ugcPay = 0,
                                isCooperation = 0, ugcPayPreview = 0, arcPay = 0,
                            ),
                        stat = dev.frost819.newbv.biliapi.http.entity.video.VideoStat(),
                        dynamic = "",
                        isChargeableSeason = false,
                        isBlooper = false,
                    ),
                page =
                    dev.frost819.newbv.biliapi.http.entity.video.VideoPage(
                        cid = 200L,
                        page = 1,
                        from = "vupload",
                        part = "第一P",
                        duration = 300,
                        vid = "",
                        weblink = "",
                        dimension =
                            dev.frost819.newbv.biliapi.http.entity.video.Dimension(
                                width = 1920,
                                height = 1080,
                                rotate = 0,
                            ),
                    ),
                bvid = "BV100",
                pages = emptyList(),
            )

        val episode = Episode.fromEpisode(ugcEpisode)

        assertThat(episode.id).isEqualTo(10)
        assertThat(episode.aid).isEqualTo(100L)
        assertThat(episode.bvid).isEqualTo("BV100")
        assertThat(episode.cid).isEqualTo(200L)
        assertThat(episode.title).isEqualTo("UGC分P标题")
        assertThat(episode.longTitle).isEqualTo("UGC分P标题")
        assertThat(episode.cover).isEqualTo("http://pic.test")
        assertThat(episode.duration).isEqualTo(300)
        assertThat(episode.dimension).isNotNull()
        assertThat(episode.dimension?.width).isEqualTo(1920)
        assertThat(episode.dimension?.height).isEqualTo(1080)
        assertThat(episode.epid).isNull()
    }

    @Test
    fun `PgcSeason fromSeason OtherSeason maps seasonId title and cover`() {
        val otherSeason =
            dev.frost819.newbv.biliapi.http.entity.season.OtherSeason(
                badge = "",
                badgeInfo =
                    dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                        bgColor = "",
                        bgColorNight = "",
                        text = "",
                    ),
                badgeType = 0,
                cover = "http://cover.test",
                horizontalCover = "http://h-cover.test",
                mediaId = 100,
                newEp = fakeNewEP(),
                seasonId = 400,
                seasonTitle = "第二季",
                seasonType = 1,
                title = "测试番剧",
            )

        val pgcSeason = PgcSeason.fromSeason(otherSeason)

        assertThat(pgcSeason.seasonId).isEqualTo(400)
        assertThat(pgcSeason.title).isEqualTo("测试番剧")
        assertThat(pgcSeason.shortTitle).isEqualTo("第二季")
        assertThat(pgcSeason.cover).isEqualTo("http://cover.test")
        assertThat(pgcSeason.horizontalCover).isEqualTo("http://h-cover.test")
    }

    @Test
    fun `PgcSeason fromSeason with null horizontalCover falls back to newEp cover`() {
        val otherSeason =
            dev.frost819.newbv.biliapi.http.entity.season.OtherSeason(
                badge = "",
                badgeInfo =
                    dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                        bgColor = "",
                        bgColorNight = "",
                        text = "",
                    ),
                badgeType = 0,
                cover = "http://cover.test",
                horizontalCover = null,
                mediaId = 100,
                newEp = fakeNewEP(cover = "http://ep-cover.test"),
                seasonId = 400,
                seasonTitle = "第二季",
                seasonType = 1,
                title = "测试番剧",
            )

        val pgcSeason = PgcSeason.fromSeason(otherSeason)

        assertThat(pgcSeason.horizontalCover).isEqualTo("http://ep-cover.test")
    }

    @Test
    fun `Section fromSection SeasonSection filters episodes with aid zero`() {
        val httpSection =
            dev.frost819.newbv.biliapi.http.entity.season.SeasonSection(
                attr = 0,
                episodeId = 0,
                episodes =
                    listOf(
                        fakeSeasonEpisode(aid = 100L, id = 1),
                        fakeSeasonEpisode(aid = 0L, id = 2),
                        fakeSeasonEpisode(aid = 200L, id = 3),
                    ),
                id = 100L,
                title = "花絮",
                type = 1,
            )

        val section = Section.fromSection(httpSection)

        assertThat(section.id).isEqualTo(100L)
        assertThat(section.title).isEqualTo("花絮")
        assertThat(section.episodes).hasSize(2)
        assertThat(section.episodes[0].aid).isEqualTo(100L)
        assertThat(section.episodes[1].aid).isEqualTo(200L)
    }

    @Test
    fun `UgcSeason fromUgcSeason HTTP maps id title cover and sections`() {
        val httpUgcSeason =
            dev.frost819.newbv.biliapi.http.entity.video.UgcSeason(
                id = 100,
                title = "UGC合集",
                cover = "http://cover.test",
                mid = 12345L,
                intro = "简介",
                signState = 0,
                attribute = 0,
                sections =
                    listOf(
                        dev.frost819.newbv.biliapi.http.entity.video.UgcSeason.Section(
                            seasonId = 1,
                            id = 10L,
                            title = "正片",
                            type = 1,
                            episodes = emptyList(),
                        ),
                    ),
            )

        val result = UgcSeason.fromUgcSeason(httpUgcSeason)

        assertThat(result.id).isEqualTo(100)
        assertThat(result.title).isEqualTo("UGC合集")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.sections).hasSize(1)
        assertThat(result.sections[0].id).isEqualTo(10L)
        assertThat(result.sections[0].title).isEqualTo("正片")
    }

    @Test
    fun `UgcSeason fromUgcSeason HTTP with empty sections returns empty list`() {
        val httpUgcSeason =
            dev.frost819.newbv.biliapi.http.entity.video.UgcSeason(
                id = 1,
                title = "空合集",
                cover = "",
                mid = 0L,
                intro = "",
                signState = 0,
                attribute = 0,
                sections = emptyList(),
            )

        val result = UgcSeason.fromUgcSeason(httpUgcSeason)

        assertThat(result.sections).isEmpty()
    }

    @Test
    fun `Section fromSection HTTP UgcSeason Section maps id title and episodes`() {
        val httpSection =
            dev.frost819.newbv.biliapi.http.entity.video.UgcSeason.Section(
                seasonId = 1,
                id = 10L,
                title = "正片",
                type = 1,
                episodes = emptyList(),
            )

        val result = Section.fromSection(httpSection)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.title).isEqualTo("正片")
        assertThat(result.episodes).isEmpty()
    }

    @Test
    fun `UgcSeason holds id title cover and sections`() {
        val ugcSeason =
            UgcSeason(
                id = 1,
                title = "合集标题",
                cover = "http://cover.test",
                sections = emptyList(),
            )
        assertThat(ugcSeason.id).isEqualTo(1)
        assertThat(ugcSeason.title).isEqualTo("合集标题")
        assertThat(ugcSeason.cover).isEqualTo("http://cover.test")
        assertThat(ugcSeason.sections).isEmpty()
    }

    @Test
    fun `SeasonDetail holds all fields with defaults`() {
        val detail =
            SeasonDetail(
                title = "番剧",
                styles = listOf("热血", "搞笑"),
                cover = "http://cover.test",
                description = "简介",
                subType = 1,
                seasonId = 100,
                userStatus =
                    SeasonDetail.UserStatus(
                        follow = false,
                        pay = false,
                    ),
                publish =
                    SeasonDetail.Publish(
                        isPublished = true,
                        publishDate = "2024-01-01",
                    ),
                newEpDesc = "第1话",
            )
        assertThat(detail.title).isEqualTo("番剧")
        assertThat(detail.originTitle).isNull()
        assertThat(detail.styles).containsExactly("热血", "搞笑")
        assertThat(detail.seasons).isEmpty()
        assertThat(detail.episodes).isEmpty()
        assertThat(detail.sections).isEmpty()
        assertThat(detail.playerIcon).isNull()
    }

    @Test
    fun `SeasonDetail UserStatus defaults progress to null`() {
        val userStatus = SeasonDetail.UserStatus(follow = true, pay = false)
        assertThat(userStatus.progress).isNull()
    }

    @Test
    fun `SeasonDetail UserStatus with progress maps fields`() {
        val userStatus =
            SeasonDetail.UserStatus(
                follow = true,
                pay = true,
                progress =
                    SeasonDetail.UserStatus.Progress(
                        lastEpId = 500,
                        lastEpIndex = "第5话",
                        lastTime = 120,
                    ),
            )
        assertThat(userStatus.progress!!.lastEpId).isEqualTo(500)
        assertThat(userStatus.progress!!.lastEpIndex).isEqualTo("第5话")
        assertThat(userStatus.progress!!.lastTime).isEqualTo(120)
    }

    private fun fakeNewEP(cover: String = "http://newep.test") =
        dev.frost819.newbv.biliapi.http.entity.season.NewEP(
            cover = cover,
            id = 1,
            desc = "第1话",
            indexShow = "第1话",
        )

    @Test
    fun `SeasonDetail fromSeasonData Web maps all fields correctly`() {
        val webSeasonData =
            dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData(
                activity = fakeActivity(),
                alias = "",
                bkgCover = "",
                cover = "http://cover.test",
                episodes = listOf(fakeSeasonEpisode(aid = 100L, id = 1)),
                evaluate = "简介",
                jpTitle = "",
                link = "http://link.test",
                mediaId = 100,
                mode = 2,
                newEp = fakeNewEP(),
                positive = fakePositive(),
                publish = fakePublish(),
                rating = null,
                record = "",
                rights = fakeSeasonRights(),
                seasonId = 400,
                seasonTitle = "番剧标题",
                seasons = listOf(fakeOtherSeason()),
                section = listOf(fakeSeasonSection()),
                series = fakeSeries(),
                shareCopy = "",
                shareSubTitle = "",
                shareUrl = "http://share.test",
                show = fakeShow(),
                squareCover = "",
                stat = fakeSeasonStat(),
                status = 0,
                styles = listOf("热血", "搞笑"),
                subtitle = "",
                title = "测试番剧",
                total = 12,
                type = 1,
                userStatus = fakeWebUserStatus(follow = 1, pay = 0),
            )

        val result = SeasonDetail.fromSeasonData(webSeasonData)

        assertThat(result.title).isEqualTo("测试番剧")
        assertThat(result.originTitle).isNull()
        assertThat(result.styles).containsExactly("热血", "搞笑")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.description).isEqualTo("简介")
        assertThat(result.subType).isEqualTo(1)
        assertThat(result.seasonId).isEqualTo(400)
        assertThat(result.newEpDesc).isEqualTo("第1话")
        assertThat(result.episodes).hasSize(1)
        assertThat(result.seasons).hasSize(1)
        assertThat(result.seasons[0].seasonId).isEqualTo(400)
        assertThat(result.userStatus.follow).isTrue()
        assertThat(result.userStatus.pay).isFalse()
        assertThat(result.userStatus.progress).isNull()
        assertThat(result.publish.isPublished).isTrue()
        assertThat(result.publish.publishDate).isEqualTo("2024-01-01")
    }

    @Test
    fun `SeasonDetail fromSeasonData Web filters empty sections`() {
        val webSeasonData =
            dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData(
                activity = fakeActivity(),
                alias = "",
                bkgCover = "",
                cover = "",
                episodes = emptyList(),
                evaluate = "",
                jpTitle = "",
                link = "",
                mediaId = 0,
                mode = 0,
                newEp = fakeNewEP(),
                positive = fakePositive(),
                publish = fakePublish(),
                rating = null,
                record = "",
                rights = fakeSeasonRights(),
                seasonId = 1,
                seasonTitle = "",
                seasons = emptyList(),
                section =
                    listOf(
                        fakeSeasonSection(),
                        dev.frost819.newbv.biliapi.http.entity.season.SeasonSection(
                            attr = 0,
                            episodeId = 0,
                            episodes = emptyList(),
                            id = 2L,
                            title = "空花絮",
                            type = 1,
                        ),
                    ),
                series = fakeSeries(),
                shareCopy = "",
                shareSubTitle = "",
                shareUrl = "",
                show = fakeShow(),
                squareCover = "",
                stat = fakeSeasonStat(),
                status = 0,
                styles = emptyList(),
                subtitle = "",
                title = "测试",
                total = 0,
                type = 1,
                userStatus = fakeWebUserStatus(follow = 0, pay = 0),
            )

        val result = SeasonDetail.fromSeasonData(webSeasonData)

        assertThat(result.sections).hasSize(1)
        assertThat(result.sections[0].title).isEqualTo("花絮")
    }

    @Test
    fun `SeasonDetail fromSeasonData Web with progress maps userStatus progress`() {
        val webSeasonData =
            dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData(
                activity = fakeActivity(),
                alias = "",
                bkgCover = "",
                cover = "",
                episodes = emptyList(),
                evaluate = "",
                jpTitle = "",
                link = "",
                mediaId = 0,
                mode = 0,
                newEp = fakeNewEP(),
                positive = fakePositive(),
                publish = fakePublish(),
                rating = null,
                record = "",
                rights = fakeSeasonRights(),
                seasonId = 1,
                seasonTitle = "",
                seasons = emptyList(),
                section = emptyList(),
                series = fakeSeries(),
                shareCopy = "",
                shareSubTitle = "",
                shareUrl = "",
                show = fakeShow(),
                squareCover = "",
                stat = fakeSeasonStat(),
                status = 0,
                styles = emptyList(),
                subtitle = "",
                title = "测试",
                total = 0,
                type = 1,
                userStatus =
                    fakeWebUserStatus(
                        follow = 1,
                        pay = 1,
                        progress =
                            dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.UserStatus.Progress(
                                lastEpId = 500,
                                lastEpIndex = "第5话",
                                lastTime = 120,
                            ),
                    ),
            )

        val result = SeasonDetail.fromSeasonData(webSeasonData)

        assertThat(result.userStatus.follow).isTrue()
        assertThat(result.userStatus.pay).isTrue()
        assertThat(result.userStatus.progress).isNotNull()
        assertThat(result.userStatus.progress!!.lastEpId).isEqualTo(500)
        assertThat(result.userStatus.progress!!.lastEpIndex).isEqualTo("第5话")
        assertThat(result.userStatus.progress!!.lastTime).isEqualTo(120)
    }

    @Test
    fun `SeasonDetail Publish fromPublish maps isStarted to isPublished`() {
        val publish =
            dev.frost819.newbv.biliapi.http.entity.season.Publish(
                _isFinish = 1,
                _isStarted = 1,
                pubTime = "2024-01-01 00:00:00",
                pubTimeShow = "2024-01-01",
                unknowPubDate = 0,
                weekday = 0,
            )

        val result = SeasonDetail.Publish.fromPublish(publish)

        assertThat(result.isPublished).isTrue()
        assertThat(result.publishDate).isEqualTo("2024-01-01")
    }

    @Test
    fun `SeasonDetail Publish fromPublish with not started maps isPublished false`() {
        val publish =
            dev.frost819.newbv.biliapi.http.entity.season.Publish(
                _isFinish = 0,
                _isStarted = 0,
                pubTime = "",
                pubTimeShow = "未发布",
                unknowPubDate = 0,
                weekday = 0,
            )

        val result = SeasonDetail.Publish.fromPublish(publish)

        assertThat(result.isPublished).isFalse()
    }

    private fun fakeActivity() =
        dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.Activity(
            headBgUrl = "",
            id = 0,
            title = "",
        )

    private fun fakePositive() =
        dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.Positive(
            id = 0,
            title = "",
        )

    private fun fakePublish() =
        dev.frost819.newbv.biliapi.http.entity.season.Publish(
            _isFinish = 1,
            _isStarted = 1,
            pubTime = "2024-01-01 00:00:00",
            pubTimeShow = "2024-01-01",
            unknowPubDate = 0,
            weekday = 0,
        )

    private fun fakeSeasonRights() =
        dev.frost819.newbv.biliapi.http.entity.season.SeasonRights(
            allowBp = 0,
            allowBpRank = 0,
            allowDownload = 0,
            allowReview = 0,
            areaLimit = 0,
            banAreaShow = 0,
            canWatch = 0,
            copyright = "bilibili",
            forbidPre = 0,
            isCoverShow = 0,
            isPreview = 0,
            onlyVipDownload = 0,
            resource = "",
            watchPlatform = 0,
        )

    private fun fakeSeries() =
        dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.Series(
            displayType = 0,
            seriesId = 0,
            seriesTitle = "",
        )

    private fun fakeShow() =
        dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.Show(
            wideScreen = 1,
        )

    private fun fakeSeasonStat() =
        dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.SeasonStat(
            coins = 0,
            danmakus = 0,
            favorites = 0,
            likes = 0,
            reply = 0,
            share = 0,
            views = 0L,
        )

    private fun fakeWebUserStatus(
        follow: Int,
        pay: Int,
        progress: dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.UserStatus.Progress? = null,
    ) = dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData.UserStatus(
        areaLimit = 0,
        banAreaShow = 0,
        dialog = null,
        follow = follow,
        followStatus = 0,
        login = 0,
        pay = pay,
        payPackPaid = 0,
        progress = progress,
        sponsor = 0,
        vipInfo = null,
    )

    private fun fakeSeasonSection() =
        dev.frost819.newbv.biliapi.http.entity.season.SeasonSection(
            attr = 0,
            episodeId = 0,
            episodes = listOf(fakeSeasonEpisode(aid = 100L, id = 1)),
            id = 100L,
            title = "花絮",
            type = 1,
        )

    private fun fakeOtherSeason() =
        dev.frost819.newbv.biliapi.http.entity.season.OtherSeason(
            badge = "",
            badgeInfo =
                dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                    bgColor = "",
                    bgColorNight = "",
                    text = "",
                ),
            badgeType = 0,
            cover = "http://cover.test",
            horizontalCover = null,
            mediaId = 100,
            newEp = fakeNewEP(),
            seasonId = 400,
            seasonTitle = "第二季",
            seasonType = 1,
            title = "测试番剧",
        )

    private fun fakeSeasonEpisode(
        aid: Long = 100L,
        id: Int = 1,
    ) = dev.frost819.newbv.biliapi.http.entity.season.Episode(
        aid = aid,
        badge = "",
        badgeInfo =
            dev.frost819.newbv.biliapi.http.entity.season.Episode.BadgeInfo(
                bgColor = "",
                bgColorNight = "",
                text = "",
            ),
        bvid = "BV$aid",
        cid = aid * 10,
        cover = "",
        dimension = null,
        duration = 0,
        enableVt = false,
        id = id,
        isViewHide = false,
        link = "",
        pubTime = 0L,
        pv = 0,
        status = 0,
        title = "第${id}话",
    )
}
