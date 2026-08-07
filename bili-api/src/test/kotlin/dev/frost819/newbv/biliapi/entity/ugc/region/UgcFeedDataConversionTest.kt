package dev.frost819.newbv.biliapi.entity.ugc.region

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.region.RegionFeedRcmd
import org.junit.jupiter.api.Test

/**
 * [UgcFeedData] 实体 `fromRegionFeedRcmd` 转换方法的单元测试。
 */
class UgcFeedDataConversionTest {
    @Test
    fun `fromRegionFeedRcmd maps archives to items and sets hasNext true when nonEmpty`() {
        val rcmdData =
            RegionFeedRcmd(
                archives =
                    listOf(
                        fakeRegionArchive(aid = 100L, title = "video1"),
                        fakeRegionArchive(aid = 200L, title = "video2"),
                    ),
            )

        val result = UgcFeedData.fromRegionFeedRcmd(rcmdData)

        assertThat(result.hasNext).isTrue()
        assertThat(result.items).hasSize(2)
        assertThat(result.items[0].aid).isEqualTo(100L)
        assertThat(result.items[0].title).isEqualTo("video1")
        assertThat(result.items[1].aid).isEqualTo(200L)
        assertThat(result.nextPage.nextPage).isEqualTo(1)
    }

    @Test
    fun `fromRegionFeedRcmd with empty archives sets hasNext false`() {
        val rcmdData = RegionFeedRcmd(archives = emptyList())

        val result = UgcFeedData.fromRegionFeedRcmd(rcmdData)

        assertThat(result.hasNext).isFalse()
        assertThat(result.items).isEmpty()
    }

    private fun fakeRegionArchive(
        aid: Long = 1L,
        title: String = "title",
    ) = RegionFeedRcmd.Archive(
        aid = aid,
        bvid = "BV$aid",
        cid = aid * 10,
        title = title,
        cover = "http://pic.test/$aid",
        duration = 120,
        pubdate = 1700000000L,
        stat = RegionFeedRcmd.Archive.Stat(view = 1000, like = 100, danmaku = 50),
        author = RegionFeedRcmd.Archive.Author(mid = 1L, name = "up"),
        trackid = "t$aid",
        goto = "av",
        recReason = "",
    )
}
