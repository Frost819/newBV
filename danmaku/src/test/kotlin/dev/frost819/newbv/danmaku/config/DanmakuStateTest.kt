package dev.frost819.newbv.danmaku.config

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.danmaku.entity.DanmakuType
import org.junit.jupiter.api.Test

class DanmakuStateTest {
    @Test
    fun `default values are correct`() {
        val state = DanmakuState()

        assertThat(state.scale).isEqualTo(1.0f)
        assertThat(state.opacity).isEqualTo(0.7f)
        assertThat(state.area).isEqualTo(0.5f)
        assertThat(state.speedFactor).isEqualTo(1.0f)
        assertThat(state.maskEnabled).isFalse()
        assertThat(state.enabledTypes).containsExactly(
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom,
        )
    }

    @Test
    fun `isShowAll is true when All type is in list`() {
        val state = DanmakuState(enabledTypes = listOf(DanmakuType.All))
        assertThat(state.isShowAll).isTrue()
    }

    @Test
    fun `isShowAll is true when all three types present`() {
        val state =
            DanmakuState(
                enabledTypes =
                    listOf(
                        DanmakuType.Rolling,
                        DanmakuType.Top,
                        DanmakuType.Bottom,
                    ),
            )
        assertThat(state.isShowAll).isTrue()
    }

    @Test
    fun `isShowAll is false when subset of types`() {
        val state = DanmakuState(enabledTypes = listOf(DanmakuType.Rolling))
        assertThat(state.isShowAll).isFalse()
    }

    @Test
    fun `screenPart returns area value`() {
        assertThat(DanmakuState(area = 0.5f).screenPart).isEqualTo(0.5f)
        assertThat(DanmakuState(area = 1.0f).screenPart).isEqualTo(1.0f)
        assertThat(DanmakuState(area = 0.25f).screenPart).isEqualTo(0.25f)
    }

    @Test
    fun `alpha returns opacity value`() {
        assertThat(DanmakuState(opacity = 0.5f).alpha).isEqualTo(0.5f)
        assertThat(DanmakuState(opacity = 1.0f).alpha).isEqualTo(1.0f)
        assertThat(DanmakuState(opacity = 0.0f).alpha).isEqualTo(0.0f)
    }

    @Test
    fun `textSizeScale returns scale value`() {
        assertThat(DanmakuState(scale = 1.5f).textSizeScale).isEqualTo(1.5f)
        assertThat(DanmakuState(scale = 2.0f).textSizeScale).isEqualTo(2.0f)
        assertThat(DanmakuState(scale = 1.0f).textSizeScale).isEqualTo(1.0f)
    }

    @Test
    fun `copy creates new instance with updated values`() {
        val original = DanmakuState()
        val updated = original.copy(scale = 2.0f, maskEnabled = true)

        assertThat(updated.scale).isEqualTo(2.0f)
        assertThat(updated.maskEnabled).isTrue()
        assertThat(updated.opacity).isEqualTo(original.opacity)
    }
}
