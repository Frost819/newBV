package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** 验证短按确认键不会触发续播回到开头，并保留长按开菜单行为。 */
class ConfirmKeyGestureHandlerTest {
    @Test
    fun `short press toggles playback`() {
        val handler = ConfirmKeyGestureHandler()

        assertThat(handler.onKeyDown(isLongPress = false)).isNull()
        assertThat(handler.onKeyUp()).isEqualTo(ConfirmKeyAction.TogglePlayback)
    }

    @Test
    fun `long press opens menu and does not also toggle playback`() {
        val handler = ConfirmKeyGestureHandler()

        assertThat(handler.onKeyDown(isLongPress = true)).isEqualTo(ConfirmKeyAction.OpenMenu)
        assertThat(handler.onKeyDown(isLongPress = true)).isNull()
        assertThat(handler.onKeyUp()).isNull()
    }

    @Test
    fun `short press after a long press toggles playback normally`() {
        val handler = ConfirmKeyGestureHandler()

        handler.onKeyDown(isLongPress = true)
        handler.onKeyUp()

        assertThat(handler.onKeyDown(isLongPress = false)).isNull()
        assertThat(handler.onKeyUp()).isEqualTo(ConfirmKeyAction.TogglePlayback)
    }
}
