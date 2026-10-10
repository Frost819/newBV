package dev.frost819.newbv.app.ui.component.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [rememberControllerAutoHide] 的插桩测试。
 *
 * 使用虚拟时钟（[androidx.compose.ui.test.MainTestClock]）精确控制自动隐藏倒计时，
 * 验证：超时隐藏、交互续期、不可见不计时、由隐藏变可见后开始计时。
 */
@RunWith(AndroidJUnit4::class)
class ControllerAutoHideTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 可见状态下，超过 timeoutMs 无交互应触发一次 onHide。 */
    @Test
    fun whenVisible_autoHidesAfterTimeout() {
        composeRule.mainClock.autoAdvance = false
        var hideCount = 0
        composeRule.setContent {
            rememberControllerAutoHide(
                visible = true,
                timeoutMs = TIMEOUT_MS,
                onHide = { hideCount++ },
            )
        }

        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS - 1_000)
        assertThat(hideCount).isEqualTo(0)

        // 继续推进超过 timeoutMs 才触发（advanceTimeBy 按帧对齐，留出余量）
        composeRule.mainClock.advanceTimeBy(2_000)
        assertThat(hideCount).isEqualTo(1)
    }

    /** 交互（调用返回的 onInteraction）应重置倒计时，延长隐藏时间。 */
    @Test
    fun interaction_resetsCountdown() {
        composeRule.mainClock.autoAdvance = false
        var hideCount = 0
        var interact: () -> Unit = {}
        composeRule.setContent {
            interact =
                rememberControllerAutoHide(
                    visible = true,
                    timeoutMs = TIMEOUT_MS,
                    onHide = { hideCount++ },
                )
        }

        // 距超时还有 1s 时交互
        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS - 1_000)
        composeRule.runOnUiThread { interact() }
        // 让重组生效，倒计时从此刻重新开始
        composeRule.mainClock.advanceTimeByFrame()

        // 从原超时点已过 1s，但因交互续期，此时不应隐藏
        composeRule.mainClock.advanceTimeBy(1_000)
        assertThat(hideCount).isEqualTo(0)

        // 续期后的完整 timeout 走到，才隐藏
        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS)
        assertThat(hideCount).isEqualTo(1)
    }

    /** 控制器不可见时不应计时，也不应触发 onHide。 */
    @Test
    fun whenNotVisible_doesNotHide() {
        composeRule.mainClock.autoAdvance = false
        var hideCount = 0
        composeRule.setContent {
            rememberControllerAutoHide(
                visible = false,
                timeoutMs = TIMEOUT_MS,
                onHide = { hideCount++ },
            )
        }

        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS * 4)
        assertThat(hideCount).isEqualTo(0)
    }

    /** 由隐藏变为可见时应开始计时，超时后隐藏。 */
    @Test
    fun whenBecomesVisible_startsCountdown() {
        composeRule.mainClock.autoAdvance = false
        var hideCount = 0
        var visible by mutableStateOf(false)
        composeRule.setContent {
            rememberControllerAutoHide(
                visible = visible,
                timeoutMs = TIMEOUT_MS,
                onHide = { hideCount++ },
            )
        }

        // 隐藏期间无论过多久都不触发
        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS * 2)
        assertThat(hideCount).isEqualTo(0)

        composeRule.runOnUiThread { visible = true }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS)
        assertThat(hideCount).isEqualTo(1)
    }

    /** 已隐藏后再推进时间，不应重复触发 onHide。 */
    @Test
    fun whenVisibleTurnsFalse_afterHide_doesNotHideAgain() {
        composeRule.mainClock.autoAdvance = false
        var hideCount = 0
        var visible by mutableStateOf(true)
        composeRule.setContent {
            rememberControllerAutoHide(
                visible = visible,
                timeoutMs = TIMEOUT_MS,
                onHide = {
                    hideCount++
                    visible = false
                },
            )
        }

        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS)
        assertThat(hideCount).isEqualTo(1)

        composeRule.mainClock.advanceTimeBy(TIMEOUT_MS * 3)
        assertThat(hideCount).isEqualTo(1)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
