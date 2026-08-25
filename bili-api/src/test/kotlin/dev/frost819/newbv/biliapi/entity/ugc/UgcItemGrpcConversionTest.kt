package dev.frost819.newbv.biliapi.entity.ugc

import bilibili.app.card.v1.base
import bilibili.app.card.v1.smallCoverV5
import bilibili.app.card.v1.up
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.home.RcmdIndexData
import org.junit.jupiter.api.Test

/**
 * [UgcItem] App gRPC 转换方法的单元测试。
 *
 * 覆盖 [UgcItem.fromRcmdItem]（App feed index）与 [UgcItem.fromSmallCoverV5]（App 分区卡片）
 * 的字段映射、播放量解析与时长解析逻辑。
 */
class UgcItemGrpcConversionTest {
    // ===== fromRcmdItem(RcmdIndexData.RcmdItem) — App feed index =====

    @Test
    fun `fromRcmdItem with app index maps all fields`() {
        val rcmdItem =
            RcmdIndexData.RcmdItem(
                args =
                    RcmdIndexData.RcmdItem.Args(
                        aid = 100L,
                        upId = 999L,
                        upName = "测试UP",
                    ),
                cardGoto = "av",
                cardType = "small_cover_v5",
                cover = "http://cover.test",
                coverLeftText1 = "1.2万",
                coverLeftText2 = "300",
                coverRightText = "10:30",
                idx = 5,
                title = "测试视频",
                threePointV2 = emptyList(),
            )

        val item = UgcItem.fromRcmdItem(rcmdItem)

        assertThat(item.aid).isEqualTo(100L)
        assertThat(item.title).isEqualTo("测试视频")
        assertThat(item.cover).isEqualTo("http://cover.test")
        assertThat(item.author).isEqualTo("测试UP")
        assertThat(item.authorMid).isEqualTo(999L)
        assertThat(item.play).isEqualTo(12000)
        assertThat(item.danmaku).isEqualTo(300)
        assertThat(item.duration).isEqualTo(630)
        assertThat(item.idx).isEqualTo(5)
    }

    @Test
    fun `fromRcmdItem parses plain number play count`() {
        val rcmdItem =
            fakeRcmdItem(coverLeftText1 = "5000")
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.play).isEqualTo(5000)
    }

    @Test
    fun `fromRcmdItem parses wan suffix play count`() {
        val rcmdItem = fakeRcmdItem(coverLeftText1 = "3.5万")
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.play).isEqualTo(35000)
    }

    @Test
    fun `fromRcmdItem returns minus one for unparseable play count`() {
        val rcmdItem = fakeRcmdItem(coverLeftText1 = null)
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.play).isEqualTo(-1)
    }

    @Test
    fun `fromRcmdItem returns minus one for null danmaku count`() {
        val rcmdItem = fakeRcmdItem(coverLeftText2 = null)
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.danmaku).isEqualTo(-1)
    }

    @Test
    fun `fromRcmdItem uses playerArgs duration when available`() {
        val rcmdItem =
            fakeRcmdItem(
                playerArgs = RcmdIndexData.RcmdItem.PlayerArgs(aid = 100L, cid = 200L, duration = 600, type = "av"),
                coverRightText = "10:30",
            )
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.duration).isEqualTo(600)
    }

    @Test
    fun `fromRcmdItem falls back to coverRightText when playerArgs is null`() {
        val rcmdItem = fakeRcmdItem(playerArgs = null, coverRightText = "10:30")
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.duration).isEqualTo(630)
    }

    @Test
    fun `fromRcmdItem returns zero duration when no duration source`() {
        val rcmdItem = fakeRcmdItem(playerArgs = null, coverRightText = null)
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.duration).isEqualTo(0)
    }

    @Test
    fun `fromRcmdItem with null aid defaults to zero`() {
        val rcmdItem = fakeRcmdItem(aid = null)
        val item = UgcItem.fromRcmdItem(rcmdItem)
        assertThat(item.aid).isEqualTo(0L)
    }

    // ===== fromSmallCoverV5 — App gRPC 分区卡片 =====

    @Test
    fun `fromSmallCoverV5 maps all fields`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "200"
                        cover = "http://cover.grpc"
                        title = "gRPC卡片视频"
                        idx = 10L
                    }
                up =
                    up {
                        id = 888L
                        name = "gRPC UP"
                    }
                coverRightText1 = "05:00"
                rightDesc1 = "gRPC UP"
                rightDesc2 = "1.5万观看 · 2天前"
            }

        val item = UgcItem.fromSmallCoverV5(card)

        assertThat(item.aid).isEqualTo(200L)
        assertThat(item.title).isEqualTo("gRPC卡片视频")
        assertThat(item.cover).isEqualTo("http://cover.grpc")
        assertThat(item.author).isEqualTo("gRPC UP")
        assertThat(item.authorMid).isEqualTo(888L)
        assertThat(item.duration).isEqualTo(300)
        assertThat(item.play).isEqualTo(15000)
        assertThat(item.danmaku).isEqualTo(-1)
        assertThat(item.idx).isEqualTo(10)
    }

    @Test
    fun `fromSmallCoverV5 parses yi suffix play count`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "1"
                        title = "test"
                        cover = ""
                        idx = 1L
                    }
                rightDesc2 = "2.5亿观看 · 1天前"
            }
        val item = UgcItem.fromSmallCoverV5(card)
        assertThat(item.play).isEqualTo(250000000)
    }

    @Test
    fun `fromSmallCoverV5 parses plain number play count`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "1"
                        title = "test"
                        cover = ""
                        idx = 1L
                    }
                rightDesc2 = "8000观看 · 1天前"
            }
        val item = UgcItem.fromSmallCoverV5(card)
        assertThat(item.play).isEqualTo(8000)
    }

    @Test
    fun `fromSmallCoverV5 returns minus one for blank play count`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "1"
                        title = "test"
                        cover = ""
                        idx = 1L
                    }
                rightDesc2 = " · 1天前"
            }
        val item = UgcItem.fromSmallCoverV5(card)
        assertThat(item.play).isEqualTo(-1)
    }

    @Test
    fun `fromSmallCoverV5 handles hours minutes seconds duration`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "1"
                        title = "test"
                        cover = ""
                        idx = 1L
                    }
                coverRightText1 = "1:30:45"
                rightDesc2 = "100观看"
            }
        val item = UgcItem.fromSmallCoverV5(card)
        assertThat(item.duration).isEqualTo(5445)
    }

    @Test
    fun `fromSmallCoverV5 handles pubTime extraction`() {
        val card =
            smallCoverV5 {
                base =
                    base {
                        param = "1"
                        title = "test"
                        cover = ""
                        idx = 1L
                    }
                rightDesc2 = "100观看 · 3天前"
            }
        val item = UgcItem.fromSmallCoverV5(card)
        assertThat(item.pubTime).isEqualTo("3天前")
    }

    // ===== 夹具 =====

    private fun fakeRcmdItem(
        aid: Long? = 100L,
        coverLeftText1: String? = "1000",
        coverLeftText2: String? = "100",
        coverRightText: String? = "05:00",
        playerArgs: RcmdIndexData.RcmdItem.PlayerArgs? = null,
    ) = RcmdIndexData.RcmdItem(
        args = RcmdIndexData.RcmdItem.Args(aid = aid, upId = 999L, upName = "UP"),
        cardGoto = "av",
        cardType = "small_cover_v5",
        cover = "http://cover.test",
        coverLeftText1 = coverLeftText1,
        coverLeftText2 = coverLeftText2,
        coverRightText = coverRightText,
        idx = 1,
        playerArgs = playerArgs,
        title = "title",
        threePointV2 = emptyList(),
    )
}
