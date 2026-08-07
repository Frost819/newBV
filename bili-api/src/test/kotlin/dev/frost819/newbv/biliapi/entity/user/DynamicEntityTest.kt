package dev.frost819.newbv.biliapi.entity.user

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.user.Pendant
import dev.frost819.newbv.biliapi.http.entity.user.Vip
import org.junit.jupiter.api.Test
import dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicItem as HttpDynamicItem

/**
 * [DynamicVideoData]、[DynamicVideo] 实体 HTTP→Domain 转换方法的单元测试。
 *
 * 通过构造 HTTP [DynamicItem] 验证 `fromDynamicVideoItem` 的字段映射、
 * 标题清理与播放量/弹幕数解析逻辑。
 */
class DynamicEntityTest {
    @Test
    fun `fromDynamicData maps items and pagination fields`() {
        val httpData =
            dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData(
                hasMore = true,
                offset = "offset-123",
                updateBaseline = "baseline-456",
                updateNum = 10,
                items = listOf(fakeDynamicItem(aid = "100", title = "动态视频｜测试标题")),
            )

        val result = DynamicVideoData.fromDynamicData(httpData)

        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].title).isEqualTo("测试标题")
        assertThat(result.hasMore).isTrue()
        assertThat(result.historyOffset).isEqualTo("offset-123")
        assertThat(result.updateBaseline).isEqualTo("baseline-456")
    }

    @Test
    fun `fromDynamicData with empty items returns empty videos`() {
        val httpData =
            dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData(
                hasMore = false,
                offset = "",
                updateBaseline = "",
                updateNum = 0,
            )

        val result = DynamicVideoData.fromDynamicData(httpData)

        assertThat(result.videos).isEmpty()
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromDynamicVideoItem maps archive fields correctly`() {
        val item =
            fakeDynamicItem(
                aid = "200",
                title = "普通视频",
                play = "5万",
                danmaku = "1000",
                durationText = "10:30",
            )

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.aid).isEqualTo(200L)
        assertThat(video.bvid).isEqualTo("BV200")
        assertThat(video.cid).isEqualTo(0L)
        assertThat(video.title).isEqualTo("普通视频")
        assertThat(video.cover).isEqualTo("http://cover.test")
        assertThat(video.author).isEqualTo("UP主")
        assertThat(video.authorMid).isEqualTo(999L)
        assertThat(video.duration).isEqualTo(630)
        assertThat(video.play).isEqualTo(50000)
        assertThat(video.danmaku).isEqualTo(1000)
        assertThat(video.pubTime).isEqualTo("2024-01-01 12:00")
    }

    @Test
    fun `fromDynamicVideoItem strips 动态视频 prefix from title`() {
        val item = fakeDynamicItem(title = "动态视频｜实际标题")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.title).isEqualTo("实际标题")
    }

    @Test
    fun `fromDynamicVideoItem parses play count with 万 suffix`() {
        val item = fakeDynamicItem(play = "3.5万播放")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.play).isEqualTo(35000)
    }

    @Test
    fun `fromDynamicVideoItem parses danmaku count with 弹幕 suffix`() {
        val item = fakeDynamicItem(danmaku = "200弹幕")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.danmaku).isEqualTo(200)
    }

    @Test
    fun `fromDynamicVideoItem handles NaN duration text`() {
        val item = fakeDynamicItem(durationText = "NaN:NaN:NaN")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.duration).isEqualTo(0)
    }

    @Test
    fun `fromDynamicVideoItem handles blank duration text`() {
        val item = fakeDynamicItem(durationText = "")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.duration).isEqualTo(0)
    }

    @Test
    fun `fromDynamicVideoItem handles hours minutes seconds duration`() {
        val item = fakeDynamicItem(durationText = "1:30:45")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.duration).isEqualTo(5445)
    }

    @Test
    fun `fromDynamicVideoItem returns minus one for negative play count`() {
        val item = fakeDynamicItem(play = "-1")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.play).isEqualTo(0)
    }

    @Test
    fun `fromDynamicVideoItem parses plain number play count without 万 suffix`() {
        val item = fakeDynamicItem(play = "5000")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.play).isEqualTo(5000)
    }

    @Test
    fun `fromDynamicVideoItem parses plain number danmaku count without suffix`() {
        val item = fakeDynamicItem(danmaku = "300")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.danmaku).isEqualTo(300)
    }

    @Test
    fun `fromDynamicVideoItem returns minus one for unparseable play count`() {
        val item = fakeDynamicItem(play = "abc")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.play).isEqualTo(-1)
    }

    @Test
    fun `fromDynamicVideoItem handles 观看 suffix in play count`() {
        val item = fakeDynamicItem(play = "2.5万观看")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.play).isEqualTo(25000)
    }

    @Test
    fun `fromDynamicVideoItem handles negative danmaku count returns zero`() {
        val item = fakeDynamicItem(danmaku = "-5")

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video.danmaku).isEqualTo(0)
    }

    private fun fakeDynamicItem(
        aid: String = "100",
        title: String = "title",
        play: String = "1000",
        danmaku: String = "100",
        durationText: String = "05:00",
    ) = HttpDynamicItem(
        basic =
            HttpDynamicItem.Basic(
                commentIdStr = "",
                commentType = 0,
                likeIcon =
                    HttpDynamicItem.Basic.LikeIcon(
                        actionUrl = "",
                        endUrl = "",
                        id = 0L,
                        startUrl = "",
                    ),
                ridStr = "",
            ),
        idStr = "id-1",
        modules =
            HttpDynamicItem.Modules(
                moduleAuthor =
                    HttpDynamicItem.Modules.Author(
                        face = "http://face.test",
                        faceNft = false,
                        following = false,
                        jumpUrl = "",
                        label = "",
                        mid = 999L,
                        name = "UP主",
                        officialVerify =
                            HttpDynamicItem.Modules.Author.OfficialVerify(
                                desc = "",
                                type = -1,
                            ),
                        pendant =
                            Pendant(
                                pid = 0,
                                name = "",
                                image = "",
                                expire = 0,
                                imageEnhance = "",
                                imageEnhanceFrame = "",
                            ),
                        pubAction = "",
                        pubLocationText = "",
                        pubTime = "2024-01-01 12:00",
                        pubTs = 1704067200,
                        type = "DYNAMIC_TYPE_WORD",
                        vip =
                            Vip(
                                type = 0,
                                status = 0,
                                dueDate = 0L,
                                vipPayType = 0,
                                themeType = 0,
                                label =
                                    Vip.Label(
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
                    ),
                moduleDynamic =
                    HttpDynamicItem.Modules.Dynamic(
                        major =
                            HttpDynamicItem.Modules.Dynamic.Major(
                                type = "MAJOR_TYPE_ARCHIVE",
                                archive =
                                    HttpDynamicItem.Modules.Dynamic.Major.Archive(
                                        aid = aid,
                                        bvid = "BV$aid",
                                        cover = "http://cover.test",
                                        desc = "desc",
                                        disablePreview = 0,
                                        durationText = durationText,
                                        jumpUrl = "",
                                        title = title,
                                        type = 1,
                                        badge =
                                            HttpDynamicItem
                                                .Modules
                                                .Dynamic
                                                .Major
                                                .Archive
                                                .Badge(
                                                    bgColor = "",
                                                    color = "",
                                                    text = "",
                                                ),
                                        stat =
                                            HttpDynamicItem
                                                .Modules
                                                .Dynamic
                                                .Major
                                                .Archive
                                                .Stat(
                                                    danmaku = danmaku,
                                                    play = play,
                                                ),
                                    ),
                            ),
                    ),
                moduleMore = HttpDynamicItem.Modules.More(),
                moduleStat =
                    HttpDynamicItem.Modules.Stat(
                        comment =
                            HttpDynamicItem.Modules.Stat.StatItem(
                                count = 0,
                                forbidden = false,
                            ),
                        forward =
                            HttpDynamicItem.Modules.Stat.StatItem(
                                count = 0,
                                forbidden = false,
                            ),
                        like =
                            HttpDynamicItem.Modules.Stat.StatItem(
                                count = 0,
                                forbidden = false,
                            ),
                    ),
            ),
        type = "DYNAMIC_TYPE_AV",
        visible = true,
    )
}
