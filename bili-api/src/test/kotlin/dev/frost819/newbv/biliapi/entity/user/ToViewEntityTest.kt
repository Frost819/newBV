package dev.frost819.newbv.biliapi.entity.user

import bilibili.app.interfaces.v1.cardOGV
import bilibili.app.interfaces.v1.cardUGC
import bilibili.app.interfaces.v1.cursorItem
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [ToViewData] 与 [ToViewItem] 实体的单元测试。
 *
 * 覆盖 `fromToViewResponse` 与 `fromToViewItem`（HTTP 与 gRPC 版本）的字段映射
 * 与空列表处理。不依赖网络。
 */
class ToViewEntityTest {
    // ------------------------------------------------------------------
    // ToViewData.fromToViewResponse
    // ------------------------------------------------------------------

    @Test
    fun `fromToViewResponse maps list items correctly`() {
        val httpToViewData =
            dev.frost819.newbv.biliapi.http.entity.toview.ToViewData(
                list =
                    listOf(
                        dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem(
                            aid = 100L,
                            bvid = "BV100",
                            cid = 200L,
                            owner =
                                dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem.Owner(
                                    name = "up1",
                                    mid = 1L,
                                ),
                            title = "video-1",
                            pic = "http://pic.test/1",
                            videos = 3,
                            progress = 60,
                            duration = 300,
                        ),
                        dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem(
                            aid = 200L,
                            bvid = "BV200",
                            cid = 400L,
                            owner =
                                dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem.Owner(
                                    name = "up2",
                                    mid = 2L,
                                ),
                            title = "video-2",
                            pic = "http://pic.test/2",
                            videos = 1,
                            progress = 0,
                            duration = 600,
                        ),
                    ),
            )

        val result = ToViewData.fromToViewResponse(httpToViewData)

        assertThat(result.data).hasSize(2)
        assertThat(result.data[0].oid).isEqualTo(100L)
        assertThat(result.data[0].bvid).isEqualTo("BV100")
        assertThat(result.data[0].cid).isEqualTo(200L)
        assertThat(result.data[0].title).isEqualTo("video-1")
        assertThat(result.data[0].cover).isEqualTo("http://pic.test/1")
        assertThat(result.data[0].author).isEqualTo("up1")
        assertThat(result.data[0].mid).isEqualTo(1L)
        assertThat(result.data[0].duration).isEqualTo(300)
        assertThat(result.data[0].progress).isEqualTo(60)
        assertThat(result.data[0].type).isEqualTo(ToViewItemType.Archive)
    }

    @Test
    fun `fromToViewResponse sets cursor to 0`() {
        val httpToViewData =
            dev.frost819.newbv.biliapi.http.entity.toview.ToViewData(
                list = emptyList(),
            )

        val result = ToViewData.fromToViewResponse(httpToViewData)

        assertThat(result.cursor).isEqualTo(0L)
    }

    @Test
    fun `fromToViewResponse with empty list returns empty data`() {
        val httpToViewData =
            dev.frost819.newbv.biliapi.http.entity.toview.ToViewData(
                list = emptyList(),
            )

        val result = ToViewData.fromToViewResponse(httpToViewData)

        assertThat(result.data).isEmpty()
    }

    // ------------------------------------------------------------------
    // ToViewItem.fromToViewItem
    // ------------------------------------------------------------------

    @Test
    fun `fromToViewItem maps all fields correctly`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem(
                aid = 999L,
                bvid = "BV999",
                cid = 888L,
                owner =
                    dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem
                        .Owner(name = "test-up", mid = 777L),
                title = "test-title",
                pic = "http://cover.test",
                videos = 5,
                progress = 120,
                duration = 900,
            )

        val result = ToViewItem.fromToViewItem(httpItem)

        assertThat(result.oid).isEqualTo(999L)
        assertThat(result.bvid).isEqualTo("BV999")
        assertThat(result.cid).isEqualTo(888L)
        assertThat(result.kid).isEqualTo(0)
        assertThat(result.epid).isEqualTo(0)
        assertThat(result.seasonId).isNull()
        assertThat(result.title).isEqualTo("test-title")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.author).isEqualTo("test-up")
        assertThat(result.mid).isEqualTo(777L)
        assertThat(result.duration).isEqualTo(900)
        assertThat(result.progress).isEqualTo(120)
        assertThat(result.type).isEqualTo(ToViewItemType.Archive)
    }

    @Test
    fun `fromToViewItem always maps type to Archive`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem(
                aid = 1L,
                bvid = "BV1",
                cid = 1L,
                owner =
                    dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem
                        .Owner(name = "u", mid = 1L),
                title = "t",
                pic = "p",
                videos = 1,
                progress = 0,
                duration = 1,
            )

        val result = ToViewItem.fromToViewItem(httpItem)

        assertThat(result.type).isEqualTo(ToViewItemType.Archive)
    }

    // ------------------------------------------------------------------
    // ToViewItem.fromToViewItem (gRPC CursorItem)
    // ------------------------------------------------------------------

    @Test
    fun `fromToViewItem gRPC CARD_UGC maps all fields`() {
        val item =
            cursorItem {
                oid = 999L
                kid = 0L
                title = "测试视频"
                cardUgc =
                    cardUGC {
                        bvid = "BV999"
                        cid = 888L
                        cover = "http://cover.test"
                        name = "test-up"
                        mid = 777L
                        duration = 900L
                        progress = 120L
                    }
            }

        val result = ToViewItem.fromToViewItem(item)

        assertThat(result.oid).isEqualTo(999L)
        assertThat(result.bvid).isEqualTo("BV999")
        assertThat(result.cid).isEqualTo(888L)
        assertThat(result.kid).isEqualTo(0)
        assertThat(result.epid).isNull()
        assertThat(result.seasonId).isNull()
        assertThat(result.title).isEqualTo("测试视频")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.author).isEqualTo("test-up")
        assertThat(result.mid).isEqualTo(777L)
        assertThat(result.duration).isEqualTo(900)
        assertThat(result.progress).isEqualTo(120)
        assertThat(result.type).isEqualTo(ToViewItemType.Archive)
    }

    @Test
    fun `fromToViewItem gRPC CARD_OGV maps seasonId from kid`() {
        val item =
            cursorItem {
                oid = 100L
                kid = 40000L
                title = "番剧标题"
                cardOgv =
                    cardOGV {
                        cover = "http://pgc-cover.test"
                        duration = 1200L
                        progress = 600L
                    }
            }

        val result = ToViewItem.fromToViewItem(item)

        assertThat(result.oid).isEqualTo(100L)
        assertThat(result.bvid).isEmpty()
        assertThat(result.cid).isEqualTo(0L)
        assertThat(result.kid).isEqualTo(40000)
        assertThat(result.seasonId).isEqualTo(40000)
        assertThat(result.epid).isNull()
        assertThat(result.title).isEqualTo("番剧标题")
        assertThat(result.cover).isEqualTo("http://pgc-cover.test")
        assertThat(result.author).isEmpty()
        assertThat(result.mid).isNull()
        assertThat(result.duration).isEqualTo(1200)
        assertThat(result.progress).isEqualTo(600)
        assertThat(result.type).isEqualTo(ToViewItemType.Pgc)
    }

    @Test
    fun `fromToViewItem gRPC unknown card type maps to Unknown`() {
        val item =
            cursorItem {
                oid = 1L
                kid = 0L
                title = "未知类型"
            }

        val result = ToViewItem.fromToViewItem(item)

        assertThat(result.type).isEqualTo(ToViewItemType.Unknown)
        assertThat(result.bvid).isEmpty()
        assertThat(result.cid).isEqualTo(0L)
        assertThat(result.cover).isEmpty()
        assertThat(result.author).isEmpty()
        assertThat(result.mid).isNull()
        assertThat(result.duration).isEqualTo(0)
        assertThat(result.progress).isEqualTo(0)
    }
}
