package dev.frost819.newbv.app.ui.navigation

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.component.videocard.isPgc
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import org.junit.jupiter.api.Test

/**
 * [VideoCardData.isPgc] 与 [VideoCardData.fromRelatedVideo] 的单元测试。
 */
class VideoCardDataTest {
    @Test
    fun `isPgc is true only for positive epid`() {
        assertThat(videoCard(epid = null).isPgc).isFalse()
        assertThat(videoCard(epid = 0).isPgc).isFalse()
        assertThat(videoCard(epid = 123).isPgc).isTrue()
    }

    @Test
    fun `fromRelatedVideo keeps epid only when jumpToSeason`() {
        val pgc = VideoCardData.fromRelatedVideo(related(epid = 123, jumpToSeason = true))
        assertThat(pgc.epid).isEqualTo(123)
        assertThat(pgc.isPgc).isTrue()

        val ugc = VideoCardData.fromRelatedVideo(related(epid = 123, jumpToSeason = false))
        assertThat(ugc.epid).isNull()
        assertThat(ugc.cid).isEqualTo(999L)
    }

    @Test
    fun `fromRelatedVideo formats pubdate as year-included date for past years`() {
        val card = VideoCardData.fromRelatedVideo(related(epid = null, jumpToSeason = false, pubdate = 1700000000L))
        assertThat(card.pubTime).contains("2023年")
    }

    @Test
    fun `fromRelatedVideo has no pubTime when pubdate unknown`() {
        val card = VideoCardData.fromRelatedVideo(related(epid = null, jumpToSeason = false, pubdate = 0L))
        assertThat(card.pubTime).isNull()
    }

    private fun videoCard(epid: Int?): VideoCardData =
        VideoCardData(
            avid = 1L,
            title = "t",
            cover = "",
            upName = "",
            epid = epid,
        )

    private fun related(
        epid: Int?,
        jumpToSeason: Boolean,
        pubdate: Long = 0L,
    ): RelatedVideo =
        RelatedVideo(
            aid = 1L,
            cid = 999L,
            cover = "",
            title = "t",
            duration = 0,
            author = null,
            jumpToSeason = jumpToSeason,
            epid = epid,
            view = 0,
            danmaku = 0,
            pubdate = pubdate,
        )
}
