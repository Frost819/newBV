package dev.frost819.newbv.biliapi.entity.video

import bilibili.app.archive.v1.author
import bilibili.app.archive.v1.dimension
import bilibili.app.archive.v1.page
import bilibili.app.archive.v1.stat
import bilibili.app.view.v1.tag
import bilibili.app.view.v1.viewPage
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.user.Author
import dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoOwner
import dev.frost819.newbv.biliapi.http.entity.video.VideoRights
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import org.junit.jupiter.api.Test
import dev.frost819.newbv.biliapi.http.entity.video.Dimension as HttpDimension
import dev.frost819.newbv.biliapi.http.entity.video.VideoPage as HttpVideoPage

/**
 * [Author]、[Dimension]、[VideoPage]、[VideoDetail.Stat]、[RelatedVideo]
 * 实体 HTTP→Domain 转换方法的单元测试。
 */
class VideoEntityTest {
    // ------------------------------------------------------------------
    // Author.fromVideoOwner
    // ------------------------------------------------------------------

    @Test
    fun `Author fromVideoOwner maps all fields`() {
        val owner = VideoOwner(mid = 123L, name = "UP主", face = "https://example.com/face.jpg")

        val author = Author.fromVideoOwner(owner)

        assertThat(author.mid).isEqualTo(123L)
        assertThat(author.name).isEqualTo("UP主")
        assertThat(author.face).isEqualTo("https://example.com/face.jpg")
    }

    // ------------------------------------------------------------------
    // Dimension.fromDimension(HTTP)
    // ------------------------------------------------------------------

    @Test
    fun `Dimension fromDimension maps width and height and infers isVertical`() {
        val httpDim = HttpDimension(width = 1920, height = 1080, rotate = 0)

        val dim = Dimension.fromDimension(httpDim)

        assertThat(dim.width).isEqualTo(1920)
        assertThat(dim.height).isEqualTo(1080)
        assertThat(dim.isVertical).isFalse()
    }

    @Test
    fun `Dimension fromDimension with portrait infers isVertical true`() {
        val httpDim = HttpDimension(width = 720, height = 1280, rotate = 0)

        val dim = Dimension.fromDimension(httpDim)

        assertThat(dim.isVertical).isTrue()
    }

    // ------------------------------------------------------------------
    // VideoPage.fromVideoPage(HTTP)
    // ------------------------------------------------------------------

    @Test
    fun `VideoPage fromVideoPage maps all fields`() {
        val httpPage =
            HttpVideoPage(
                cid = 456L,
                page = 2,
                from = "vupload",
                part = "第二P",
                duration = 300,
                vid = "",
                weblink = "",
                dimension = HttpDimension(width = 1920, height = 1080, rotate = 0),
            )

        val page = VideoPage.fromVideoPage(httpPage)

        assertThat(page.cid).isEqualTo(456L)
        assertThat(page.index).isEqualTo(2)
        assertThat(page.title).isEqualTo("第二P")
        assertThat(page.duration).isEqualTo(300)
        assertThat(page.dimension.width).isEqualTo(1920)
        assertThat(page.dimension.height).isEqualTo(1080)
    }

    // ------------------------------------------------------------------
    // VideoDetail.Stat.fromVideoStat
    // ------------------------------------------------------------------

    @Test
    fun `Stat fromVideoStat maps all stat fields`() {
        val videoStat =
            VideoStat(
                aid = 100L,
                _view = 50000L,
                danmaku = 1200,
                reply = 300,
                favorite = 800,
                coin = 200,
                share = 50,
                hisRank = 10,
                like = 5000,
                argueMsg = "争议提示",
            )

        val stat = VideoDetail.Stat.fromVideoStat(videoStat)

        assertThat(stat.view).isEqualTo(50000)
        assertThat(stat.danmaku).isEqualTo(1200)
        assertThat(stat.reply).isEqualTo(300)
        assertThat(stat.favorite).isEqualTo(800)
        assertThat(stat.coin).isEqualTo(200)
        assertThat(stat.share).isEqualTo(50)
        assertThat(stat.like).isEqualTo(5000)
        assertThat(stat.historyRank).isEqualTo(10)
    }

    @Test
    fun `Stat fromVideoStat clamps view exceeding Int MAX_VALUE`() {
        val videoStat = VideoStat(aid = 1L, _view = Int.MAX_VALUE.toLong() + 1)

        val stat = VideoDetail.Stat.fromVideoStat(videoStat)

        assertThat(stat.view).isEqualTo(Int.MIN_VALUE)
    }

    // ------------------------------------------------------------------
    // RelatedVideo.fromRelate(HTTP RelatedVideoInfo)
    // ------------------------------------------------------------------

    @Test
    fun `RelatedVideo fromRelate HTTP maps all fields`() {
        val httpRelate =
            RelatedVideoInfo(
                bvid = "BV1xx411c7mD",
                aid = 993403941L,
                videos = 1,
                tid = 1,
                tname = "综合",
                copyright = 1,
                pic = "https://example.com/cover.jpg",
                title = "相关视频",
                pubdate = 1700000000,
                ctime = 1700000000,
                desc = "描述",
                state = 0,
                duration = 600,
                rights = fakeRights(),
                owner = VideoOwner(mid = 123L, name = "UP主", face = "https://example.com/face.jpg"),
                stat = VideoStat(aid = 993403941L, _view = 100000L, danmaku = 500),
                dynamic = "",
                cid = 1051761130L,
                dimension = HttpDimension(width = 1920, height = 1080, rotate = 0),
                rcmdReason = "",
            )

        val related = RelatedVideo.fromRelate(httpRelate)

        assertThat(related.aid).isEqualTo(993403941L)
        assertThat(related.cid).isEqualTo(1051761130L)
        assertThat(related.cover).isEqualTo("https://example.com/cover.jpg")
        assertThat(related.title).isEqualTo("相关视频")
        assertThat(related.duration).isEqualTo(600)
        assertThat(related.author!!.mid).isEqualTo(123L)
        assertThat(related.author.name).isEqualTo("UP主")
        assertThat(related.jumpToSeason).isFalse()
        assertThat(related.epid).isNull()
        assertThat(related.view).isEqualTo(100000)
        assertThat(related.danmaku).isEqualTo(500)
    }

    // ------------------------------------------------------------------
    // Dimension.fromDimension(gRPC bilibili.app.archive.v1.Dimension)
    // ------------------------------------------------------------------

    @Test
    fun `Dimension fromDimension gRPC maps width and height`() {
        val grpcDim =
            dimension {
                width = 1920L
                height = 1080L
            }

        val dim = Dimension.fromDimension(grpcDim)

        assertThat(dim.width).isEqualTo(1920)
        assertThat(dim.height).isEqualTo(1080)
        assertThat(dim.isVertical).isFalse()
    }

    @Test
    fun `Dimension fromDimension gRPC with portrait infers isVertical true`() {
        val grpcDim =
            dimension {
                width = 720L
                height = 1280L
            }

        val dim = Dimension.fromDimension(grpcDim)

        assertThat(dim.isVertical).isTrue()
    }

    @Test
    fun `Dimension fromDimension gRPC with zero dimensions`() {
        val grpcDim =
            dimension {
                width = 0L
                height = 0L
            }

        val dim = Dimension.fromDimension(grpcDim)

        assertThat(dim.width).isEqualTo(0)
        assertThat(dim.height).isEqualTo(0)
        assertThat(dim.isVertical).isFalse()
    }

    // ------------------------------------------------------------------
    // Tag.fromTag(gRPC bilibili.app.view.v1.Tag)
    // ------------------------------------------------------------------

    @Test
    fun `Tag fromTag gRPC maps id and name`() {
        val grpcTag =
            tag {
                id = 42L
                name = "测试标签"
            }

        val tag = Tag.fromTag(grpcTag)

        assertThat(tag.id).isEqualTo(42)
        assertThat(tag.name).isEqualTo("测试标签")
    }

    @Test
    fun `Tag fromTag gRPC with zero id`() {
        val grpcTag =
            tag {
                id = 0L
                name = ""
            }

        val tag = Tag.fromTag(grpcTag)

        assertThat(tag.id).isEqualTo(0)
        assertThat(tag.name).isEqualTo("")
    }

    // ------------------------------------------------------------------
    // VideoPage.fromViewPage(gRPC ViewPage)
    // ------------------------------------------------------------------

    @Test
    fun `VideoPage fromViewPage gRPC maps all fields`() {
        val grpcPage =
            viewPage {
                page =
                    bilibili.app.archive.v1.page {
                        cid = 456L
                        page = 2
                        part = "第二P"
                        duration = 300L
                        dimension =
                            dimension {
                                width = 1920L
                                height = 1080L
                            }
                    }
            }

        val page = VideoPage.fromViewPage(grpcPage)

        assertThat(page.cid).isEqualTo(456L)
        assertThat(page.index).isEqualTo(2)
        assertThat(page.title).isEqualTo("第二P")
        assertThat(page.duration).isEqualTo(300)
        assertThat(page.dimension.width).isEqualTo(1920)
        assertThat(page.dimension.height).isEqualTo(1080)
    }

    @Test
    fun `VideoPage fromViewPage gRPC with portrait dimension`() {
        val grpcPage =
            viewPage {
                page =
                    bilibili.app.archive.v1.page {
                        cid = 789L
                        page = 1
                        part = "竖屏P"
                        duration = 60L
                        dimension =
                            dimension {
                                width = 720L
                                height = 1280L
                            }
                    }
            }

        val page = VideoPage.fromViewPage(grpcPage)

        assertThat(page.dimension.isVertical).isTrue()
    }

    // ------------------------------------------------------------------
    // VideoDetail.Stat.fromStat(gRPC bilibili.app.archive.v1.Stat)
    // ------------------------------------------------------------------

    @Test
    fun `Stat fromStat gRPC maps all stat fields`() {
        val grpcStat =
            stat {
                view = 10000
                danmaku = 500
                reply = 200
                fav = 100
                coin = 50
                share = 10
                hisRank = 5
                like = 1000
            }

        val stat = VideoDetail.Stat.fromStat(grpcStat)

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
    fun `Stat fromStat gRPC with zero values`() {
        val grpcStat = stat { }

        val stat = VideoDetail.Stat.fromStat(grpcStat)

        assertThat(stat.view).isEqualTo(0)
        assertThat(stat.danmaku).isEqualTo(0)
        assertThat(stat.reply).isEqualTo(0)
        assertThat(stat.favorite).isEqualTo(0)
        assertThat(stat.coin).isEqualTo(0)
        assertThat(stat.share).isEqualTo(0)
        assertThat(stat.like).isEqualTo(0)
        assertThat(stat.historyRank).isEqualTo(0)
    }

    // ------------------------------------------------------------------
    // Author.fromAuthor(gRPC bilibili.app.archive.v1.Author)
    // ------------------------------------------------------------------

    @Test
    fun `Author fromAuthor gRPC maps mid name face`() {
        val grpcAuthor =
            author {
                mid = 123L
                name = "UP主"
                face = "http://face.test"
            }

        val author = Author.fromAuthor(grpcAuthor)

        assertThat(author.mid).isEqualTo(123L)
        assertThat(author.name).isEqualTo("UP主")
        assertThat(author.face).isEqualTo("http://face.test")
    }

    @Test
    fun `Author fromAuthor gRPC with empty strings`() {
        val grpcAuthor =
            author {
                mid = 0L
                name = ""
                face = ""
            }

        val author = Author.fromAuthor(grpcAuthor)

        assertThat(author.mid).isEqualTo(0L)
        assertThat(author.name).isEqualTo("")
        assertThat(author.face).isEqualTo("")
    }

    private fun fakeRights() =
        VideoRights(
            bp = 0,
            elec = 0,
            download = 1,
            movie = 0,
            pay = 0,
            hd5 = 0,
            noReprint = 0,
            autoplay = 1,
            ugcPay = 0,
            isCooperation = 0,
            ugcPayPreview = 0,
            arcPay = 0,
        )
}
