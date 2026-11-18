package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [calculateBrightness] 与 [calculateVolume] 的单元测试。
 *
 * 两者同构：都是「float 状态 + 满量程比例增量 + clamp」，保证同样距离的竖直滑动
 * 对亮度与音量的调整幅度一致。
 */
class PlayerGestureMathTest {
    @Test
    fun `calculateBrightness uses system brightness when not overridden`() {
        val brightness = calculateBrightness(currentBrightness = -1f, deltaFraction = 0.1f, systemBrightness = 0.3f)

        assertThat(brightness).isWithin(0.001f).of(0.4f)
    }

    @Test
    fun `calculateVolume uses system volume when not overridden`() {
        val volume = calculateVolume(currentVolume = -1f, deltaFraction = -0.2f, systemVolume = 0.6f)

        assertThat(volume).isWithin(0.001f).of(0.4f)
    }

    @Test
    fun `calculateBrightness clamps to valid range`() {
        assertThat(calculateBrightness(currentBrightness = 0.9f, deltaFraction = 0.5f, systemBrightness = 0.5f))
            .isEqualTo(1f)
        assertThat(calculateBrightness(currentBrightness = 0.05f, deltaFraction = -0.5f, systemBrightness = 0.5f))
            .isEqualTo(0.01f)
    }

    @Test
    fun `calculateVolume clamps to valid range`() {
        assertThat(calculateVolume(currentVolume = 0.9f, deltaFraction = 0.5f, systemVolume = 0.5f))
            .isEqualTo(1f)
        assertThat(calculateVolume(currentVolume = 0.05f, deltaFraction = -0.5f, systemVolume = 0.5f))
            .isEqualTo(0f)
    }

    @Test
    fun `brightness and volume apply the same delta fraction`() {
        val start = 0.5f
        val deltaFraction = 0.1f

        val brightnessDelta = calculateBrightness(start, deltaFraction, systemBrightness = start) - start
        val volumeDelta = calculateVolume(start, deltaFraction, systemVolume = start) - start

        assertThat(brightnessDelta).isWithin(1e-6f).of(volumeDelta)
    }

    @Test
    fun `vertical drag range ratio is one screen height`() {
        assertThat(VERTICAL_DRAG_RANGE_RATIO).isEqualTo(1f)
    }
}
