package dev.frost819.newbv.biliapi.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SpiData] 与 [SpiResult] 实体的单元测试。
 */
class SpiDataTest {
    @Test
    fun `SpiData defaults are null`() {
        val data = SpiData()
        assertThat(data.b3).isNull()
        assertThat(data.b4).isNull()
    }

    @Test
    fun `SpiData holds b3 and b4 values`() {
        val data = SpiData(b3 = "buvid3-value", b4 = "buvid4-value")
        assertThat(data.b3).isEqualTo("buvid3-value")
        assertThat(data.b4).isEqualTo("buvid4-value")
    }

    @Test
    fun `SpiResult holds buvid3 and deviceCookies`() {
        val result = SpiResult(buvid3 = "b3-value", deviceCookies = "buvid3=b3-value; b_nut=100")
        assertThat(result.buvid3).isEqualTo("b3-value")
        assertThat(result.deviceCookies).isEqualTo("buvid3=b3-value; b_nut=100")
    }
}
