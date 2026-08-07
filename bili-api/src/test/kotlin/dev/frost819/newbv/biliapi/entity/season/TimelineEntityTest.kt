package dev.frost819.newbv.biliapi.entity.season

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import dev.frost819.newbv.biliapi.http.entity.video.Timeline as HttpTimeline

/**
 * [Timeline]、[TimelineEp] 实体 HTTP→Domain 转换方法的单元测试。
 */
class TimelineEntityTest {
    // ------------------------------------------------------------------
    // Timeline.fromTimeline
    // ------------------------------------------------------------------

    @Test
    fun `Timeline fromTimeline maps all fields`() {
        val httpTimeline =
            HttpTimeline(
                date = "2024-01-15",
                dateTs = 1705276800,
                dayOfWeek = 1,
                episodes = listOf(fakeEpisode()),
                _isToday = 1,
            )

        val timeline = Timeline.fromTimeline(httpTimeline)

        assertThat(timeline.dateString).isEqualTo("2024-01-15")
        assertThat(timeline.date.time).isEqualTo(1705276800 * 1000L)
        assertThat(timeline.dayOfWeek).isEqualTo(1)
        assertThat(timeline.isToday).isTrue()
        assertThat(timeline.episodes).hasSize(1)
    }

    @Test
    fun `Timeline fromTimeline with isToday 0 maps to false`() {
        val httpTimeline =
            HttpTimeline(
                date = "2024-01-16",
                dateTs = 1705363200,
                dayOfWeek = 2,
                episodes = emptyList(),
                _isToday = 0,
            )

        val timeline = Timeline.fromTimeline(httpTimeline)

        assertThat(timeline.isToday).isFalse()
        assertThat(timeline.episodes).isEmpty()
    }

    // ------------------------------------------------------------------
    // TimelineEp.fromTimelineEpisode
    // ------------------------------------------------------------------

    @Test
    fun `TimelineEp fromTimelineEpisode maps all fields`() {
        val httpEpisode = fakeEpisode()

        val ep = TimelineEp.fromTimelineEpisode(httpEpisode)

        assertThat(ep.cover).isEqualTo("https://example.com/ep_cover.jpg")
        assertThat(ep.title).isEqualTo("番剧标题")
        assertThat(ep.seasonId).isEqualTo(40000)
        assertThat(ep.publishIndex).isEqualTo("第1话")
        assertThat(ep.publishTime).isEqualTo("2024-01-15 20:00")
        assertThat(ep.publishDate.time).isEqualTo(1705314000 * 1000L)
    }

    private fun fakeEpisode() =
        HttpTimeline.Episode(
            cover = "https://example.com/ep_cover.jpg",
            delay = 0,
            delayId = 0,
            delayIndex = "",
            delayReason = "",
            episodeId = 800001,
            pubIndex = "第1话",
            pubTime = "2024-01-15 20:00",
            pubTs = 1705314000,
            seasonId = 40000,
            squareCover = "https://example.com/square.jpg",
            title = "番剧标题",
            _published = 1,
        )
}
