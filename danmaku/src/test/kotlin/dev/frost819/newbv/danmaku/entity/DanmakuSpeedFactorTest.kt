package dev.frost819.newbv.danmaku.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DanmakuSpeedFactorTest {
    @Test
    fun `factor values are correct`() {
        assertThat(DanmakuSpeedFactor.S1.factor).isEqualTo(1.5f)
        assertThat(DanmakuSpeedFactor.S2.factor).isEqualTo(1.25f)
        assertThat(DanmakuSpeedFactor.S3.factor).isEqualTo(1.0f)
        assertThat(DanmakuSpeedFactor.S4.factor).isEqualTo(0.75f)
        assertThat(DanmakuSpeedFactor.S5.factor).isEqualTo(0.5f)
    }

    @Test
    fun `fromFactor with valid factor returns correct enum`() {
        assertThat(DanmakuSpeedFactor.fromFactor(1.5f)).isEqualTo(DanmakuSpeedFactor.S1)
        assertThat(DanmakuSpeedFactor.fromFactor(1.25f)).isEqualTo(DanmakuSpeedFactor.S2)
        assertThat(DanmakuSpeedFactor.fromFactor(1.0f)).isEqualTo(DanmakuSpeedFactor.S3)
        assertThat(DanmakuSpeedFactor.fromFactor(0.75f)).isEqualTo(DanmakuSpeedFactor.S4)
        assertThat(DanmakuSpeedFactor.fromFactor(0.5f)).isEqualTo(DanmakuSpeedFactor.S5)
    }

    @Test
    fun `fromFactor with unknown factor returns default`() {
        assertThat(DanmakuSpeedFactor.fromFactor(0.3f)).isEqualTo(DanmakuSpeedFactor.S3)
        assertThat(DanmakuSpeedFactor.fromFactor(2.0f)).isEqualTo(DanmakuSpeedFactor.S3)
        assertThat(DanmakuSpeedFactor.fromFactor(0f)).isEqualTo(DanmakuSpeedFactor.S3)
    }

    @Test
    fun `entries has correct count`() {
        assertThat(DanmakuSpeedFactor.entries).hasSize(5)
    }
}
