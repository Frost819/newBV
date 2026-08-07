package dev.frost819.newbv.biliapi.entity.video

import bilibili.app.archive.v1.author
import bilibili.app.archive.v1.stat
import bilibili.app.view.v1.relate
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [RelatedVideo.fromRelate] gRPC（bilibili.app.view.v1.Relate）→ Domain 转换方法的单元测试。
 *
 * 覆盖 author 映射、desc fallback、goto 类型判断（bangumi_ep / special / av）、
 * epid 提取等分支逻辑。
 */
class RelatedVideoGrpcTest {
    @Test
    fun `fromRelate gRPC maps all fields with author present`() {
        val grpcRelate =
            relate {
                aid = 993403941L
                cid = 1051761130L
                pic = "http://pic.test"
                title = "相关视频"
                duration = 600L
                goto = "av"
                uri = "http://uri.test"
                stat =
                    stat {
                        view = 100000
                        danmaku = 500
                    }
                author =
                    author {
                        mid = 123L
                        name = "UP主"
                        face = "http://face.test"
                    }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.aid).isEqualTo(993403941L)
        assertThat(related.cid).isEqualTo(1051761130L)
        assertThat(related.cover).isEqualTo("http://pic.test")
        assertThat(related.title).isEqualTo("相关视频")
        assertThat(related.duration).isEqualTo(600)
        assertThat(related.author!!.mid).isEqualTo(123L)
        assertThat(related.author!!.name).isEqualTo("UP主")
        assertThat(related.jumpToSeason).isFalse()
        assertThat(related.epid).isNull()
        assertThat(related.view).isEqualTo(100000)
        assertThat(related.danmaku).isEqualTo(500)
    }

    @Test
    fun `fromRelate gRPC falls back to desc when author is null`() {
        val grpcRelate =
            relate {
                aid = 100L
                cid = 200L
                pic = ""
                title = "无作者"
                duration = 0L
                goto = "av"
                desc = "fallback author"
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.author).isNotNull()
        assertThat(related.author!!.mid).isEqualTo(0L)
        assertThat(related.author!!.name).isEqualTo("fallback author")
        assertThat(related.author!!.face).isEqualTo("")
    }

    @Test
    fun `fromRelate gRPC with no author falls back to desc string`() {
        val grpcRelate =
            relate {
                aid = 1L
                cid = 2L
                pic = ""
                title = ""
                duration = 0L
                goto = "av"
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.author).isNotNull()
        assertThat(related.author!!.mid).isEqualTo(0L)
        assertThat(related.author!!.name).isEqualTo("")
        assertThat(related.author!!.face).isEqualTo("")
    }

    @Test
    fun `fromRelate gRPC with bangumi_ep goto sets jumpToSeason true and extracts epid`() {
        val grpcRelate =
            relate {
                aid = 0L
                cid = 0L
                pic = "http://cover.test"
                title = "番剧"
                duration = 0L
                goto = "bangumi_ep"
                uri = "https://www.bilibili.com/bangumi/play/ep12345?from=xxx"
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.jumpToSeason).isTrue()
        assertThat(related.epid).isEqualTo(12345)
    }

    @Test
    fun `fromRelate gRPC with special goto sets jumpToSeason true and extracts epid`() {
        val grpcRelate =
            relate {
                aid = 0L
                cid = 0L
                pic = ""
                title = "特别篇"
                duration = 0L
                goto = "special"
                uri = "https://www.bilibili.com/bangumi/play/ep67890"
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.jumpToSeason).isTrue()
        assertThat(related.epid).isEqualTo(67890)
    }

    @Test
    fun `fromRelate gRPC with av goto does not jump to season`() {
        val grpcRelate =
            relate {
                aid = 1L
                cid = 2L
                pic = ""
                title = ""
                duration = 0L
                goto = "av"
                uri = "https://www.bilibili.com/video/BV1xx"
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.jumpToSeason).isFalse()
        assertThat(related.epid).isNull()
    }

    @Test
    fun `fromRelate gRPC with empty goto does not jump to season`() {
        val grpcRelate =
            relate {
                aid = 1L
                cid = 2L
                pic = ""
                title = ""
                duration = 0L
                goto = ""
                uri = ""
                stat = stat { }
            }

        val related = RelatedVideo.fromRelate(grpcRelate)

        assertThat(related.jumpToSeason).isFalse()
        assertThat(related.epid).isNull()
    }
}
