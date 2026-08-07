package dev.frost819.newbv.biliapi.entity.video

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [HeartbeatVideoType] 枚举值的单元测试。
 */
class HeartbeatTest {
    @Test
    fun `HeartbeatVideoType Video has value 3`() {
        assertThat(HeartbeatVideoType.Video.value).isEqualTo(3)
    }

    @Test
    fun `HeartbeatVideoType Season has value 4`() {
        assertThat(HeartbeatVideoType.Season.value).isEqualTo(4)
    }

    @Test
    fun `HeartbeatVideoType Course has value 10`() {
        assertThat(HeartbeatVideoType.Course.value).isEqualTo(10)
    }

    @Test
    fun `HeartbeatVideoType has exactly three entries`() {
        assertThat(HeartbeatVideoType.entries).hasSize(3)
    }
}
