package dev.frost819.newbv.app.entity.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import org.junit.jupiter.api.Test

/**
 * [VideoListItem] 的单元测试。
 *
 * 验证默认值、相等性、copy 及 ugcPages 字段。
 */
class VideoListItemTest {

    @Test
    fun `default values for optional fields are null`() {
        val item = VideoListItem(aid = 1L, cid = 10L, title = "test")

        assertThat(item.epid).isNull()
        assertThat(item.seasonId).isNull()
        assertThat(item.ugcPages).isNull()
    }

    @Test
    fun `required fields are set correctly`() {
        val item = VideoListItem(aid = 100L, cid = 200L, title = "视频标题")

        assertThat(item.aid).isEqualTo(100L)
        assertThat(item.cid).isEqualTo(200L)
        assertThat(item.title).isEqualTo("视频标题")
    }

    @Test
    fun `all fields set correctly`() {
        val pages = listOf(
            VideoPage(cid = 10L, index = 1, title = "P1", duration = 60, dimension = Dimension(1920, 1080)),
        )
        val item = VideoListItem(
            aid = 1L,
            cid = 10L,
            epid = 100,
            seasonId = 200,
            title = "番剧分集",
            ugcPages = pages,
        )

        assertThat(item.epid).isEqualTo(100)
        assertThat(item.seasonId).isEqualTo(200)
        assertThat(item.ugcPages).hasSize(1)
        assertThat(item.ugcPages!![0].title).isEqualTo("P1")
    }

    @Test
    fun `two items with same fields are equal`() {
        val item1 = VideoListItem(aid = 1L, cid = 10L, title = "test")
        val item2 = VideoListItem(aid = 1L, cid = 10L, title = "test")

        assertThat(item1).isEqualTo(item2)
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode())
    }

    @Test
    fun `items with different aid are not equal`() {
        val item1 = VideoListItem(aid = 1L, cid = 10L, title = "test")
        val item2 = VideoListItem(aid = 2L, cid = 10L, title = "test")

        assertThat(item1).isNotEqualTo(item2)
    }

    @Test
    fun `copy with different cid preserves other fields`() {
        val original = VideoListItem(
            aid = 1L,
            cid = 10L,
            epid = 100,
            seasonId = 200,
            title = "original",
        )
        val copied = original.copy(cid = 20L)

        assertThat(copied.cid).isEqualTo(20L)
        assertThat(copied.aid).isEqualTo(original.aid)
        assertThat(copied.epid).isEqualTo(original.epid)
        assertThat(copied.seasonId).isEqualTo(original.seasonId)
        assertThat(copied.title).isEqualTo(original.title)
    }

    @Test
    fun `copy with different title creates new instance`() {
        val original = VideoListItem(aid = 1L, cid = 10L, title = "original")
        val copied = original.copy(title = "updated")

        assertThat(original.title).isEqualTo("original")
        assertThat(copied.title).isEqualTo("updated")
    }

    @Test
    fun `toString contains class name and fields`() {
        val item = VideoListItem(aid = 1L, cid = 10L, title = "test")

        assertThat(item.toString()).contains("VideoListItem")
        assertThat(item.toString()).contains("aid=1")
        assertThat(item.toString()).contains("cid=10")
        assertThat(item.toString()).contains("test")
    }
}
