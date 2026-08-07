package dev.frost819.newbv.bilisubtitle.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SubtitleItem] 的单元测试。
 *
 * 验证 [isShowing] 时间匹配逻辑。
 */
class SubtitleItemTest {
    private fun item(
        fromMs: Long,
        toMs: Long,
        content: String = "test",
    ): SubtitleItem {
        val from =
            Timestamp(
                hours = (fromMs / 3_600_000L).toInt(),
                minutes = ((fromMs % 3_600_000L) / 60_000L).toInt(),
                seconds = ((fromMs % 60_000L) / 1_000L).toInt(),
                milliSeconds = (fromMs % 1_000L).toInt(),
            )
        val to =
            Timestamp(
                hours = (toMs / 3_600_000L).toInt(),
                minutes = ((toMs % 3_600_000L) / 60_000L).toInt(),
                seconds = ((toMs % 60_000L) / 1_000L).toInt(),
                milliSeconds = (toMs % 1_000L).toInt(),
            )
        return SubtitleItem(from = from, to = to, content = content)
    }

    @Test
    fun `isShowing returns true when time equals from`() {
        val sub = item(fromMs = 1_000L, toMs = 5_000L)
        assertThat(sub.isShowing(1_000L)).isTrue()
    }

    @Test
    fun `isShowing returns true when time equals to`() {
        val sub = item(fromMs = 1_000L, toMs = 5_000L)
        assertThat(sub.isShowing(5_000L)).isTrue()
    }

    @Test
    fun `isShowing returns true when time is between from and to`() {
        val sub = item(fromMs = 1_000L, toMs = 5_000L)
        assertThat(sub.isShowing(3_000L)).isTrue()
    }

    @Test
    fun `isShowing returns false when time is before from`() {
        val sub = item(fromMs = 1_000L, toMs = 5_000L)
        assertThat(sub.isShowing(999L)).isFalse()
    }

    @Test
    fun `isShowing returns false when time is after to`() {
        val sub = item(fromMs = 1_000L, toMs = 5_000L)
        assertThat(sub.isShowing(5_001L)).isFalse()
    }

    @Test
    fun `isShowing works with zero-length subtitle`() {
        val sub = item(fromMs = 2_000L, toMs = 2_000L)
        assertThat(sub.isShowing(2_000L)).isTrue()
        assertThat(sub.isShowing(1_999L)).isFalse()
        assertThat(sub.isShowing(2_001L)).isFalse()
    }

    @Test
    fun `isShowing works at zero time`() {
        val sub = item(fromMs = 0L, toMs = 3_000L)
        assertThat(sub.isShowing(0L)).isTrue()
    }

    @Test
    fun `content is retained`() {
        val sub = item(fromMs = 0L, toMs = 1_000L, content = "hello world")
        assertThat(sub.content).isEqualTo("hello world")
    }

    @Test
    fun `from and to timestamps have correct totalMills`() {
        val sub = item(fromMs = 1_500L, toMs = 4_500L)
        assertThat(sub.from.totalMills).isEqualTo(1_500L)
        assertThat(sub.to.totalMills).isEqualTo(4_500L)
    }
}
