package dev.frost819.newbv.biliapi.entity.video

import bilibili.app.archive.v1.arc
import bilibili.app.archive.v1.author
import bilibili.app.archive.v1.stat
import bilibili.app.view.v1.activitySeason
import bilibili.app.view.v1.history
import bilibili.app.view.v1.playerIcon
import bilibili.app.view.v1.relate
import bilibili.app.view.v1.reqUser
import bilibili.app.view.v1.tag
import bilibili.app.view.v1.viewPage
import bilibili.app.view.v1.viewReply
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [VideoDetail.fromViewReply] gRPC（bilibili.app.view.v1.ViewReply）→ Domain 转换方法的单元测试。
 *
 * 覆盖非 activity_season 和 activity_season 两个分支，以及 argueTip、playerIcon、
 * redirectToEp、epid 提取等子逻辑。
 */
class VideoDetailGrpcTest {
    @Test
    fun `fromViewReply gRPC without activity season maps all fields`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 993403941L
                        firstCid = 1051761130L
                        pic = "http://pic.test"
                        title = "测试视频"
                        pubdate = 1700000000L
                        desc = "描述"
                        redirectUrl = ""
                        stat =
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
                        author =
                            author {
                                mid = 123L
                                name = "UP主"
                                face = "http://face.test"
                            }
                    }
                reqUser =
                    reqUser {
                        like = 1
                        favorite = 0
                        coin = 1
                        dislike = 0
                    }
                history =
                    history {
                        cid = 1051761130L
                        progress = 120L
                    }
                argueMsg = ""
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.bvid).isEqualTo("BV1xx")
        assertThat(detail.aid).isEqualTo(993403941L)
        assertThat(detail.cid).isEqualTo(1051761130L)
        assertThat(detail.cover).isEqualTo("http://pic.test")
        assertThat(detail.title).isEqualTo("测试视频")
        assertThat(detail.description).isEqualTo("描述")
        assertThat(detail.stat.view).isEqualTo(10000)
        assertThat(detail.stat.danmaku).isEqualTo(500)
        assertThat(detail.author.mid).isEqualTo(123L)
        assertThat(detail.pages).isEmpty()
        assertThat(detail.relatedVideos).isEmpty()
        assertThat(detail.tags).isEmpty()
        assertThat(detail.redirectToEp).isFalse()
        assertThat(detail.epid).isNull()
        assertThat(detail.argueTip).isNull()
        assertThat(detail.userActions.like).isTrue()
        assertThat(detail.userActions.favorite).isFalse()
        assertThat(detail.userActions.coin).isTrue()
        assertThat(detail.history.progress).isEqualTo(120)
        assertThat(detail.history.lastPlayedCid).isEqualTo(1051761130L)
        assertThat(detail.playerIcon).isNotNull()
        assertThat(detail.playerIcon!!.idle).isEqualTo("")
        assertThat(detail.playerIcon!!.moving).isEqualTo("")
        assertThat(detail.ugcSeason).isNull()
    }

    @Test
    fun `fromViewReply gRPC with non-empty argueMsg returns argueTip`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                    }
                argueMsg = "争议提示信息"
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.argueTip).isEqualTo("争议提示信息")
    }

    @Test
    fun `fromViewReply gRPC with redirectUrl containing ep sets redirectToEp true`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                        redirectUrl = "https://www.bilibili.com/bangumi/play/ep12345"
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.redirectToEp).isTrue()
        assertThat(detail.epid).isEqualTo(12345)
    }

    @Test
    fun `fromViewReply gRPC with redirectUrl without ep sets redirectToEp false`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                        redirectUrl = "https://www.bilibili.com/video/BV1xx"
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.redirectToEp).isFalse()
        assertThat(detail.epid).isNull()
    }

    @Test
    fun `fromViewReply gRPC with playerIcon maps icon`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                    }
                playerIcon =
                    playerIcon {
                        url1 = "http://moving.test"
                        url2 = "http://idle.test"
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.playerIcon).isNotNull()
        assertThat(detail.playerIcon!!.idle).isEqualTo("http://idle.test")
        assertThat(detail.playerIcon!!.moving).isEqualTo("http://moving.test")
    }

    @Test
    fun `fromViewReply gRPC with pages maps video pages`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                    }
                pages +=
                    viewPage {
                        page =
                            bilibili.app.archive.v1.page {
                                cid = 100L
                                page = 1
                                part = "第一P"
                                duration = 300L
                            }
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.pages).hasSize(1)
        assertThat(detail.pages[0].cid).isEqualTo(100L)
        assertThat(detail.pages[0].index).isEqualTo(1)
        assertThat(detail.pages[0].title).isEqualTo("第一P")
        assertThat(detail.pages[0].duration).isEqualTo(300)
    }

    @Test
    fun `fromViewReply gRPC with tags maps tags`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                    }
                tag +=
                    tag {
                        id = 42L
                        name = "标签1"
                    }
                tag +=
                    tag {
                        id = 99L
                        name = "标签2"
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.tags).hasSize(2)
        assertThat(detail.tags[0].id).isEqualTo(42)
        assertThat(detail.tags[0].name).isEqualTo("标签1")
        assertThat(detail.tags[1].id).isEqualTo(99)
        assertThat(detail.tags[1].name).isEqualTo("标签2")
    }

    @Test
    fun `fromViewReply gRPC with relates maps related videos`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                arc =
                    arc {
                        aid = 1L
                        firstCid = 2L
                        pic = ""
                        title = ""
                        pubdate = 0L
                        desc = ""
                    }
                relates +=
                    relate {
                        aid = 200L
                        cid = 300L
                        pic = "http://related.test"
                        title = "相关视频"
                        duration = 600L
                        goto = "av"
                        stat =
                            stat {
                                view = 500
                                danmaku = 50
                            }
                        author =
                            author {
                                mid = 456L
                                name = "UP2"
                                face = ""
                            }
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.relatedVideos).hasSize(1)
        assertThat(detail.relatedVideos[0].aid).isEqualTo(200L)
        assertThat(detail.relatedVideos[0].title).isEqualTo("相关视频")
    }

    @Test
    fun `fromViewReply gRPC with activity season maps from activity season data`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                activitySeason =
                    activitySeason {
                        bvid = "BV2xx"
                        arc =
                            arc {
                                aid = 999L
                                firstCid = 888L
                                pic = "http://activity-pic.test"
                                title = "活动视频"
                                pubdate = 1700000001L
                                desc = "活动描述"
                                redirectUrl = ""
                                stat =
                                    stat {
                                        view = 50000
                                        danmaku = 1000
                                    }
                                author =
                                    author {
                                        mid = 777L
                                        name = "活动UP"
                                        face = ""
                                    }
                            }
                        history =
                            history {
                                cid = 888L
                                progress = 60L
                            }
                        argueMsg = ""
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.bvid).isEqualTo("BV2xx")
        assertThat(detail.aid).isEqualTo(999L)
        assertThat(detail.cid).isEqualTo(888L)
        assertThat(detail.cover).isEqualTo("http://activity-pic.test")
        assertThat(detail.title).isEqualTo("活动视频")
        assertThat(detail.description).isEqualTo("活动描述")
        assertThat(detail.stat.view).isEqualTo(50000)
        assertThat(detail.author.mid).isEqualTo(777L)
        assertThat(detail.history.progress).isEqualTo(60)
        assertThat(detail.history.lastPlayedCid).isEqualTo(888L)
    }

    @Test
    fun `fromViewReply gRPC with activity season and argueMsg returns argueTip`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                activitySeason =
                    activitySeason {
                        bvid = "BV2xx"
                        arc =
                            arc {
                                aid = 1L
                                firstCid = 2L
                                pic = ""
                                title = ""
                                pubdate = 0L
                                desc = ""
                            }
                    }
                argueMsg = "活动争议"
            }

        // When activitySeason is present, argueTip comes from viewReply.argueMsg (top level),
        // NOT from activitySeason.argueMsg. Wait — let me re-check the source...
        val detail = VideoDetail.fromViewReply(grpcReply)

        // Activity season branch reads viewReply.activitySeason.argueMsg
        // The argueMsg on viewReply is not used in activity season branch
        assertThat(detail.argueTip).isNull()
    }

    @Test
    fun `fromViewReply gRPC with activity season with argueMsg on activitySeason returns argueTip`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                activitySeason =
                    activitySeason {
                        bvid = "BV2xx"
                        arc =
                            arc {
                                aid = 1L
                                firstCid = 2L
                                pic = ""
                                title = ""
                                pubdate = 0L
                                desc = ""
                            }
                        argueMsg = "活动争议"
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.argueTip).isEqualTo("活动争议")
    }

    @Test
    fun `fromViewReply gRPC with activity season and ep redirectUrl extracts epid`() {
        val grpcReply =
            viewReply {
                bvid = "BV1xx"
                activitySeason =
                    activitySeason {
                        bvid = "BV2xx"
                        arc =
                            arc {
                                aid = 1L
                                firstCid = 2L
                                pic = ""
                                title = ""
                                pubdate = 0L
                                desc = ""
                                redirectUrl = "https://www.bilibili.com/bangumi/play/ep99999"
                            }
                    }
            }

        val detail = VideoDetail.fromViewReply(grpcReply)

        assertThat(detail.redirectToEp).isTrue()
        assertThat(detail.epid).isEqualTo(99999)
    }
}
