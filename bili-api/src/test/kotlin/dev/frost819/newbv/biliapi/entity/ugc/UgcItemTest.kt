package dev.frost819.newbv.biliapi.entity.ugc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.video.Dimension
import dev.frost819.newbv.biliapi.http.entity.video.VideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoOwner
import dev.frost819.newbv.biliapi.http.entity.video.VideoRights
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import org.junit.jupiter.api.Test

/**
 * [UgcItem] 实体 HTTP→Domain 转换方法的单元测试。
 */
class UgcItemTest {
    @Test
    fun `fromVideoInfo maps all fields`() {
        val videoInfo = fakeVideoInfo(aid = 100L, title = "测试视频")

        val item = UgcItem.fromVideoInfo(videoInfo)

        assertThat(item.aid).isEqualTo(100L)
        assertThat(item.bvid).isEmpty()
        assertThat(item.title).isEqualTo("测试视频")
        assertThat(item.cover).isEqualTo("http://pic.test")
        assertThat(item.author).isEqualTo("UP主")
        assertThat(item.authorMid).isEqualTo(1L)
        assertThat(item.play).isEqualTo(5000)
        assertThat(item.danmaku).isEqualTo(100)
        assertThat(item.duration).isEqualTo(300)
        assertThat(item.idx).isEqualTo(-1)
    }

    @Test
    fun `fromVideoInfo with large view count maps correctly`() {
        val videoInfo =
            fakeVideoInfo(aid = 200L, title = "热门视频").copy(
                stat = VideoStat(aid = 200L, _view = 100000L, danmaku = 5000, like = 10000),
            )

        val item = UgcItem.fromVideoInfo(videoInfo)

        assertThat(item.play).isEqualTo(100000)
        assertThat(item.danmaku).isEqualTo(5000)
    }

    private fun fakeVideoInfo(
        aid: Long = 1L,
        title: String = "title",
    ) = VideoInfo(
        bvid = "BV$aid",
        aid = aid,
        videos = 1,
        tid = 0,
        tname = "test",
        copyright = 1,
        pic = "http://pic.test",
        title = title,
        pubdate = 1700000000,
        desc = "desc",
        state = 0,
        duration = 300,
        rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
        owner = VideoOwner(mid = 1L, name = "UP主", face = "http://face.test"),
        stat = VideoStat(aid = aid, _view = 5000L, danmaku = 100, like = 200),
        dynamic = "",
        cid = aid * 10,
        dimension = Dimension(1920, 1080, 0),
    )
}
