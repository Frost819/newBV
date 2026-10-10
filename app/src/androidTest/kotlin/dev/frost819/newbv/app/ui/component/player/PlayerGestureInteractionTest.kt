package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [playerGestures] 交互回调的插桩测试。
 *
 * 重点验证：根节点的 [PlayerGestureCallbacks.onUserInteraction] 在触摸按下时即触发，
 * 即使该次按下已被子组件（如按钮）消费——这是控制器自动隐藏"统一续期"的前提。
 */
@RunWith(AndroidJUnit4::class)
class PlayerGestureInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 触摸画面空白区域应触发一次交互回调。 */
    @Test
    fun tapOnEmptyArea_triggersInteraction() {
        var interactions = 0
        composeRule.setContent {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .playerGestures(
                            totalDuration = { 0L },
                            controllerVisible = { false },
                            callbacks = callbacks(onUserInteraction = { interactions++ }),
                            gestureTipState = rememberGestureTipState(),
                        ),
            )
        }

        composeRule.onRoot().performTouchInput { click() }

        assertThat(interactions).isEqualTo(1)
    }

    /** 触摸子按钮（事件被子组件消费）也应触发根节点的交互回调。 */
    @Test
    fun tapOnChildButton_triggersRootInteraction() {
        var interactions = 0
        var childClicks = 0
        composeRule.setContent {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .playerGestures(
                            totalDuration = { 0L },
                            controllerVisible = { false },
                            callbacks = callbacks(onUserInteraction = { interactions++ }),
                            gestureTipState = rememberGestureTipState(),
                        ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .testTag(CHILD_TAG)
                            .clickable { childClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag(CHILD_TAG).performClick()

        // 子按钮自身收到点击，同时根节点也收到一次交互
        assertThat(childClicks).isEqualTo(1)
        assertThat(interactions).isEqualTo(1)
    }

    /** 连续两次触摸应累计两次交互（用于验证"每次交互都续期"）。 */
    @Test
    fun twoTaps_accumulateInteractions() {
        var interactions = 0
        composeRule.setContent {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .playerGestures(
                            totalDuration = { 0L },
                            controllerVisible = { false },
                            callbacks = callbacks(onUserInteraction = { interactions++ }),
                            gestureTipState = rememberGestureTipState(),
                        ),
            )
        }

        composeRule.onRoot().performTouchInput { click() }
        composeRule.onRoot().performTouchInput { click() }

        assertThat(interactions).isEqualTo(2)
    }

    private fun callbacks(onUserInteraction: () -> Unit): PlayerGestureCallbacks =
        PlayerGestureCallbacks(
            onSingleTap = {},
            onDoubleTap = {},
            onSeekDelta = {},
            onSeekCommit = {},
            onBrightnessChange = {},
            onVolumeChange = {},
            onUserInteraction = onUserInteraction,
        )

    private companion object {
        const val CHILD_TAG = "gesture_child"
    }
}
