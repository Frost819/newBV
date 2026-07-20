package dev.frost819.newbv.core.interaction

import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [InteractionModeDetector] 的单元测试。
 *
 * 验证根据 [Configuration.touchscreen] 值正确判断交互模式。
 */
class InteractionModeTest {

    @Test
    fun `detect returns DPad for TOUCHSCREEN_UNDEFINED`() {
        assertThat(InteractionModeDetector.detect(Configuration.TOUCHSCREEN_UNDEFINED))
            .isEqualTo(InteractionMode.DPad)
    }

    @Test
    fun `detect returns Touch for TOUCHSCREEN_FINGER`() {
        assertThat(InteractionModeDetector.detect(Configuration.TOUCHSCREEN_FINGER))
            .isEqualTo(InteractionMode.Touch)
    }

    @Test
    fun `detect returns Touch for TOUCHSCREEN_STYLUS`() {
        assertThat(InteractionModeDetector.detect(Configuration.TOUCHSCREEN_STYLUS))
            .isEqualTo(InteractionMode.Touch)
    }

    @Test
    fun `isTouch and isDPad properties are mutually exclusive`() {
        assertThat(InteractionMode.Touch.isTouch).isTrue()
        assertThat(InteractionMode.Touch.isDPad).isFalse()
        assertThat(InteractionMode.DPad.isTouch).isFalse()
        assertThat(InteractionMode.DPad.isDPad).isTrue()
    }
}
