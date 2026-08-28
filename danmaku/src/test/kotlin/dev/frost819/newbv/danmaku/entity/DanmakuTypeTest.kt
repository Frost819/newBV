package dev.frost819.newbv.danmaku.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DanmakuTypeTest {
    @Test
    fun `fromAkDanmakuMode with rolling mode returns Rolling`() {
        assertThat(DanmakuType.fromAkDanmakuMode(1)).isEqualTo(DanmakuType.Rolling)
    }

    @Test
    fun `fromAkDanmakuMode with top mode returns Top`() {
        assertThat(DanmakuType.fromAkDanmakuMode(5)).isEqualTo(DanmakuType.Top)
    }

    @Test
    fun `fromAkDanmakuMode with bottom mode returns Bottom`() {
        assertThat(DanmakuType.fromAkDanmakuMode(4)).isEqualTo(DanmakuType.Bottom)
    }

    @Test
    fun `fromAkDanmakuMode with unknown mode returns All`() {
        assertThat(DanmakuType.fromAkDanmakuMode(999)).isEqualTo(DanmakuType.All)
    }

    @Test
    fun `fromAkDanmakuMode with All mode returns All`() {
        assertThat(DanmakuType.fromAkDanmakuMode(-1)).isEqualTo(DanmakuType.All)
    }

    @Test
    fun `modeValue Rolling returns correct value`() {
        assertThat(DanmakuType.Rolling.modeValue).isEqualTo(1)
    }

    @Test
    fun `modeValue Top returns correct value`() {
        assertThat(DanmakuType.Top.modeValue).isEqualTo(5)
    }

    @Test
    fun `modeValue Bottom returns correct value`() {
        assertThat(DanmakuType.Bottom.modeValue).isEqualTo(4)
    }

    @Test
    fun `modeValue All returns minus one`() {
        assertThat(DanmakuType.All.modeValue).isEqualTo(-1)
    }

    @Test
    fun `modeValue constants are correct`() {
        assertThat(DanmakuType.All.modeValue).isEqualTo(-1)
        assertThat(DanmakuType.Rolling.modeValue).isEqualTo(1)
        assertThat(DanmakuType.Top.modeValue).isEqualTo(5)
        assertThat(DanmakuType.Bottom.modeValue).isEqualTo(4)
    }
}
