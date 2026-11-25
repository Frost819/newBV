package dev.frost819.newbv.app.ui.component.player

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** 验证确认键短按、长按和按键序列中断的回调。 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConfirmKeyHandlerTest {
    @Test
    fun `releasing before the long press timeout dispatches only a short press`() =
        runTest {
            // Given
            val presses = mutableListOf<Press>()
            val handler = createHandler(presses)
            handler.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, isRepeat = false)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS - 1)

            // When
            val consumed = handler.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, isCanceled = false)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)
            runCurrent()

            // Then
            assertThat(consumed).isTrue()
            assertThat(presses).containsExactly(Press.Short)
        }

    @Test
    fun `holding fires once even without repeats and release ends the hold`() =
        runTest {
            // Given
            val presses = mutableListOf<Press>()
            val handler = createHandler(presses)
            handler.onKeyDown(KeyEvent.KEYCODE_ENTER, isRepeat = false)

            // When
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)
            runCurrent()
            handler.onKeyDown(KeyEvent.KEYCODE_ENTER, isRepeat = true)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)

            // Then
            assertThat(presses).containsExactly(Press.LongStart)
            assertThat(handler.onKeyUp(KeyEvent.KEYCODE_ENTER, isCanceled = false)).isTrue()
            assertThat(presses).containsExactly(Press.LongStart, Press.LongEnd).inOrder()
        }

    @Test
    fun `a canceled key up never triggers a short press or a delayed long press`() =
        runTest {
            // Given
            val presses = mutableListOf<Press>()
            val handler = createHandler(presses)
            handler.onKeyDown(KeyEvent.KEYCODE_SPACE, isRepeat = false)

            // When
            val consumed = handler.onKeyUp(KeyEvent.KEYCODE_SPACE, isCanceled = true)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)
            runCurrent()

            // Then
            assertThat(consumed).isTrue()
            assertThat(presses).isEmpty()
        }

    @Test
    fun `interrupting an active hold ends it and ignores orphaned events`() =
        runTest {
            // Given
            val presses = mutableListOf<Press>()
            val handler = createHandler(presses)
            handler.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, isRepeat = false)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)
            runCurrent()

            // When
            val unrelatedKeyUp = handler.onKeyUp(KeyEvent.KEYCODE_ENTER, isCanceled = false)
            handler.cancel()
            handler.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, isRepeat = true)
            val orphanedRelease = handler.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, isCanceled = false)
            advanceTimeBy(LONG_PRESS_TIMEOUT_MS)
            runCurrent()

            // Then
            assertThat(unrelatedKeyUp).isFalse()
            assertThat(orphanedRelease).isFalse()
            assertThat(presses).containsExactly(Press.LongStart, Press.LongEnd).inOrder()
        }

    private fun CoroutineScope.createHandler(presses: MutableList<Press>) =
        PlayerConfirmKeyHandler(
            scope = this,
            longPressTimeoutMs = LONG_PRESS_TIMEOUT_MS,
            onShortPress = { presses.add(Press.Short) },
            onLongPress = { presses.add(Press.LongStart) },
            onLongPressEnd = { presses.add(Press.LongEnd) },
        )

    private enum class Press {
        Short,
        LongStart,
        LongEnd,
    }

    private companion object {
        const val LONG_PRESS_TIMEOUT_MS = 500L
    }
}
