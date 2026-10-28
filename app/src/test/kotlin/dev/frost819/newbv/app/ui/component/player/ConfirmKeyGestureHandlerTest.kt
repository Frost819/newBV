package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** 验证播放器确认键的短按/长按分工，避免续播时单击误跳回开头。 */
class ConfirmKeyGestureHandlerTest {
    @Test
    fun `short press while resume hint is visible toggles playback`() {
        val handler = ConfirmKeyGestureHandler()

        assertThat(handler.onKeyDown(isLongPress = false, resumeHintVisible = true)).isNull()
        assertThat(handler.onKeyUp()).isEqualTo(ConfirmKeyAction.TogglePlayback)
    }

    @Test
    fun `long press while resume hint is visible replays from start without toggling playback`() {
        val handler = ConfirmKeyGestureHandler()

        assertThat(handler.onKeyDown(isLongPress = true, resumeHintVisible = true))
            .isEqualTo(ConfirmKeyAction.ReplayFromStart)
        assertThat(handler.onKeyUp()).isNull()
    }

    @Test
    fun `long press without resume hint opens menu and consumes repeated key events`() {
        val handler = ConfirmKeyGestureHandler()

        assertThat(handler.onKeyDown(isLongPress = true, resumeHintVisible = false))
            .isEqualTo(ConfirmKeyAction.OpenMenu)
        assertThat(handler.onKeyDown(isLongPress = true, resumeHintVisible = false)).isNull()
        assertThat(handler.onKeyUp()).isNull()
    }
}
