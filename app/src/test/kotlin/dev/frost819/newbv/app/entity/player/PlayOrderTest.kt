package dev.frost819.newbv.app.entity.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import org.junit.jupiter.api.Test

/**
 * [inPlayOrder] 的单元测试。
 *
 * 验证正序/倒序下列表与分 P 两层的排列结果。
 */
class PlayOrderTest {
    private fun page(
        cid: Long,
        title: String,
    ) = VideoPage(
        cid = cid,
        index = cid.toInt(),
        title = title,
        duration = 600,
        dimension = Dimension(1920, 1080),
    )

    private val list =
        listOf(
            VideoListItem(
                aid = 1,
                cid = 11,
                title = "第一集",
                ugcPages = listOf(page(101, "P1"), page(102, "P2"), page(103, "P3")),
            ),
            VideoListItem(aid = 2, cid = 21, title = "第二集"),
            VideoListItem(aid = 3, cid = 31, title = "第三集"),
        )

    @Test
    fun `正序时原样返回`() {
        val result = list.inPlayOrder(reverse = false)

        assertThat(result.map { it.aid }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(result[0].ugcPages?.map { it.cid }).containsExactly(101L, 102L, 103L).inOrder()
    }

    @Test
    fun `倒序时列表整体翻转`() {
        val result = list.inPlayOrder(reverse = true)

        assertThat(result.map { it.aid }).containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun `倒序时分P也要翻转`() {
        val result = list.inPlayOrder(reverse = true)

        // 第一集翻到了末尾，其内部分 P 也必须是倒的，否则会出现「倒序里夹正序」
        assertThat(result.last().ugcPages?.map { it.cid }).containsExactly(103L, 102L, 101L).inOrder()
    }

    @Test
    fun `倒序不修改原列表`() {
        list.inPlayOrder(reverse = true)

        assertThat(list.map { it.aid }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(list[0].ugcPages?.map { it.cid }).containsExactly(101L, 102L, 103L).inOrder()
    }

    @Test
    fun `空列表倒序仍为空`() {
        assertThat(emptyList<VideoListItem>().inPlayOrder(reverse = true)).isEmpty()
    }

    @Test
    fun `单条且无分P时倒序不变`() {
        val single = listOf(VideoListItem(aid = 9, cid = 91, title = "唯一"))

        assertThat(single.inPlayOrder(reverse = true).map { it.aid }).containsExactly(9L)
    }

    @Test
    fun `单条但有多个分P时倒序翻转分P`() {
        val single =
            listOf(
                VideoListItem(
                    aid = 9,
                    cid = 91,
                    title = "唯一",
                    ugcPages = listOf(page(901, "P1"), page(902, "P2")),
                ),
            )

        val result = single.inPlayOrder(reverse = true)

        assertThat(result[0].ugcPages?.map { it.cid }).containsExactly(902L, 901L).inOrder()
    }
}
