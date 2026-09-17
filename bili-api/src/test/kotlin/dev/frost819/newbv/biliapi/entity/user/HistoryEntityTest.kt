package dev.frost819.newbv.biliapi.entity.user

import bilibili.app.interfaces.v1.cardOGV
import bilibili.app.interfaces.v1.cardUGC
import bilibili.app.interfaces.v1.cursor
import bilibili.app.interfaces.v1.cursorItem
import bilibili.app.interfaces.v1.cursorV2Reply
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [HistoryData]、[HistoryItem] 实体 HTTP→Domain 转换方法的单元测试。
 * 同时覆盖 gRPC（CursorV2Reply / CursorItem）→ Domain 转换。
 */
class HistoryEntityTest {
    @Test
    fun `fromHistoryResponse filters only archive and pgc items`() {
        val httpHistoryData =
            dev.frost819.newbv.biliapi.http.entity.history.HistoryData(
                cursor =
                    dev.frost819.newbv.biliapi.http.entity.history.HistoryData.Cursor(
                        max = 100L,
                        viewAt = 1700000000L,
                        business = "archive",
                        ps = 20,
                    ),
                tab = emptyList(),
                list =
                    listOf(
                        fakeHistoryItem(business = "archive", title = "视频1"),
                        fakeHistoryItem(business = "pgc", title = "番剧1"),
                        fakeHistoryItem(business = "live", title = "直播1"),
                        fakeHistoryItem(business = "article", title = "文章1"),
                    ),
            )

        val result = HistoryData.fromHistoryResponse(httpHistoryData)

        assertThat(result.data).hasSize(2)
        assertThat(result.data[0].title).isEqualTo("视频1")
        assertThat(result.data[0].type).isEqualTo(HistoryItemType.Archive)
        assertThat(result.data[1].title).isEqualTo("番剧1")
        assertThat(result.data[1].type).isEqualTo(HistoryItemType.Pgc)
        assertThat(result.cursor).isEqualTo(1700000000L)
    }

    @Test
    fun `fromHistoryResponse with empty list returns empty data`() {
        val httpHistoryData =
            dev.frost819.newbv.biliapi.http.entity.history.HistoryData(
                cursor =
                    dev.frost819.newbv.biliapi.http.entity.history.HistoryData.Cursor(
                        max = 0L,
                        viewAt = 0L,
                        business = "archive",
                        ps = 20,
                    ),
                tab = emptyList(),
                list = emptyList(),
            )

        val result = HistoryData.fromHistoryResponse(httpHistoryData)

        assertThat(result.data).isEmpty()
        assertThat(result.cursor).isEqualTo(0L)
    }

    @Test
    fun `fromHistoryItem archive maps all fields`() {
        val httpItem = fakeHistoryItem(business = "archive", title = "测试视频")

        val item = HistoryItem.fromHistoryItem(httpItem)

        assertThat(item.oid).isEqualTo(993403941L)
        assertThat(item.bvid).isEqualTo("BV1xx")
        assertThat(item.cid).isEqualTo(1051761130L)
        assertThat(item.kid).isEqualTo(0L)
        assertThat(item.epid).isEqualTo(0)
        assertThat(item.seasonId).isNull()
        assertThat(item.title).isEqualTo("测试视频")
        assertThat(item.cover).isEqualTo("http://cover.test")
        assertThat(item.author).isEqualTo("UP主")
        assertThat(item.mid).isEqualTo(12345L)
        assertThat(item.duration).isEqualTo(300)
        assertThat(item.progress).isEqualTo(120)
        assertThat(item.type).isEqualTo(HistoryItemType.Archive)
    }

    @Test
    fun `fromHistoryItem pgc maps type to Pgc`() {
        val httpItem = fakeHistoryItem(business = "pgc", title = "番剧标题")

        val item = HistoryItem.fromHistoryItem(httpItem)

        assertThat(item.type).isEqualTo(HistoryItemType.Pgc)
    }

    @Test
    fun `fromHistoryItem unknown business maps type to Unknown`() {
        val httpItem = fakeHistoryItem(business = "live", title = "直播")

        val item = HistoryItem.fromHistoryItem(httpItem)

        assertThat(item.type).isEqualTo(HistoryItemType.Unknown)
    }

    // region ---- gRPC (CursorV2Reply / CursorItem) ----

    @Test
    fun `fromHistoryResponse gRPC filters CARD_UGC and CARD_OGV only`() {
        val reply =
            cursorV2Reply {
                cursor = cursor { max = 999L }
                items +=
                    cursorItem {
                        oid = 1L
                        title = "UGC视频"
                        cardUgc = cardUGC { bvid = "BV1xx" }
                    }
                items +=
                    cursorItem {
                        oid = 2L
                        title = "番剧"
                        cardOgv = cardOGV {}
                    }
                items +=
                    cursorItem {
                        oid = 3L
                        title = "文章"
                    }
            }

        val result = HistoryData.fromHistoryResponse(reply)

        assertThat(result.cursor).isEqualTo(999L)
        assertThat(result.data).hasSize(2)
        assertThat(result.data[0].title).isEqualTo("UGC视频")
        assertThat(result.data[0].type).isEqualTo(HistoryItemType.Archive)
        assertThat(result.data[1].title).isEqualTo("番剧")
        assertThat(result.data[1].type).isEqualTo(HistoryItemType.Pgc)
    }

    @Test
    fun `fromHistoryItem gRPC CARD_UGC maps all fields`() {
        val item =
            cursorItem {
                oid = 993403941L
                kid = 0L
                title = "测试视频"
                cardUgc =
                    cardUGC {
                        bvid = "BV1xx"
                        cid = 1051761130L
                        cover = "http://cover.test"
                        name = "UP主"
                        mid = 12345L
                        duration = 300L
                        progress = 120L
                    }
            }

        val result = HistoryItem.fromHistoryItem(item)

        assertThat(result.oid).isEqualTo(993403941L)
        assertThat(result.bvid).isEqualTo("BV1xx")
        assertThat(result.cid).isEqualTo(1051761130L)
        assertThat(result.title).isEqualTo("测试视频")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.author).isEqualTo("UP主")
        assertThat(result.mid).isEqualTo(12345L)
        assertThat(result.duration).isEqualTo(300)
        assertThat(result.progress).isEqualTo(120)
        assertThat(result.type).isEqualTo(HistoryItemType.Archive)
        assertThat(result.seasonId).isNull()
        assertThat(result.epid).isNull()
    }

    @Test
    fun `fromHistoryItem gRPC CARD_OGV does not map seasonId from kid`() {
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

        val result = HistoryItem.fromHistoryItem(item)

        assertThat(result.oid).isEqualTo(100L)
        assertThat(result.bvid).isEmpty()
        assertThat(result.cid).isEqualTo(0L)
        assertThat(result.title).isEqualTo("番剧标题")
        assertThat(result.cover).isEqualTo("http://pgc-cover.test")
        assertThat(result.author).isEmpty()
        assertThat(result.mid).isNull()
        assertThat(result.duration).isEqualTo(1200)
        assertThat(result.progress).isEqualTo(600)
        assertThat(result.type).isEqualTo(HistoryItemType.Pgc)
        // kid 是历史记录 id，不是 season id，不能用于导航
        assertThat(result.seasonId).isNull()
        assertThat(result.epid).isNull()
    }

    @Test
    fun `fromHistoryItem gRPC unknown card type maps to Unknown`() {
        val item =
            cursorItem {
                oid = 1L
                kid = 0L
                title = "未知类型"
            }

        val result = HistoryItem.fromHistoryItem(item)

        assertThat(result.type).isEqualTo(HistoryItemType.Unknown)
        assertThat(result.bvid).isEmpty()
        assertThat(result.cid).isEqualTo(0L)
        assertThat(result.cover).isEmpty()
        assertThat(result.author).isEmpty()
        assertThat(result.mid).isNull()
        assertThat(result.duration).isEqualTo(0)
        assertThat(result.progress).isEqualTo(0)
    }

    // endregion

    private fun fakeHistoryItem(
        business: String = "archive",
        title: String = "title",
    ) = dev.frost819.newbv.biliapi.http.entity.history.HistoryItem(
        title = title,
        longTitle = "",
        cover = "http://cover.test",
        covers = null,
        uri = "",
        history =
            dev.frost819.newbv.biliapi.http.entity.history.HistoryItem.HistoryInfo(
                oid = 993403941L,
                epid = 0,
                bvid = "BV1xx",
                page = 1,
                cid = 1051761130L,
                part = "第一P",
                business = business,
                dt = 33,
            ),
        videos = 1,
        authorName = "UP主",
        authorFace = "http://face.test",
        authorMid = 12345L,
        viewAt = 1700000000,
        progress = 120,
        badge = "",
        showTitle = "",
        duration = 300,
        current = "",
        total = 0,
        newDesc = "",
        isFinish = 0,
        isFav = 0,
        kid = 0L,
        tagName = "",
        liveStatus = 0,
    )
}
