package dev.frost819.newbv.core.interaction

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * [InteractionTracker] 的单元测试。
 *
 * 验证输入方式状态流转：Touch ↔ DPad 切换、StateFlow 传播。
 */
class InteractionTrackerTest {

    @Test
    fun `initial value is DPad by default`() {
        val tracker = InteractionTracker()
        assertThat(tracker.current).isEqualTo(InputMethod.DPad)
    }

    @Test
    fun `initial value can be customized`() {
        val tracker = InteractionTracker(initial = InputMethod.Touch)
        assertThat(tracker.current).isEqualTo(InputMethod.Touch)
    }

    @Test
    fun `onTouch switches to Touch`() {
        val tracker = InteractionTracker()
        tracker.onTouch()
        assertThat(tracker.current).isEqualTo(InputMethod.Touch)
    }

    @Test
    fun `onDpadKey switches to DPad`() {
        val tracker = InteractionTracker(initial = InputMethod.Touch)
        tracker.onDpadKey()
        assertThat(tracker.current).isEqualTo(InputMethod.DPad)
    }

    @Test
    fun `reset returns to DPad`() {
        val tracker = InteractionTracker()
        tracker.onTouch()
        tracker.reset()
        assertThat(tracker.current).isEqualTo(InputMethod.DPad)
    }

    @Test
    fun `inputMethod StateFlow emits changes`() = runTest {
        val tracker = InteractionTracker()
        tracker.inputMethod.test {
            assertThat(awaitItem()).isEqualTo(InputMethod.DPad)
            tracker.onTouch()
            assertThat(awaitItem()).isEqualTo(InputMethod.Touch)
            tracker.onDpadKey()
            assertThat(awaitItem()).isEqualTo(InputMethod.DPad)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeated same input does not emit duplicate`() = runTest {
        val tracker = InteractionTracker()
        tracker.inputMethod.test {
            assertThat(awaitItem()).isEqualTo(InputMethod.DPad)
            tracker.onDpadKey()  // same as current, should not emit
            expectNoEvents()
            tracker.onTouch()
            assertThat(awaitItem()).isEqualTo(InputMethod.Touch)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `InputMethod isTouch and isDPad are mutually exclusive`() {
        assertThat(InputMethod.Touch.isTouch).isTrue()
        assertThat(InputMethod.Touch.isDPad).isFalse()
        assertThat(InputMethod.DPad.isTouch).isFalse()
        assertThat(InputMethod.DPad.isDPad).isTrue()
    }
}
