package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.core.focus.touchClickable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [playerGestures] 的插桩测试。
 *
 * 覆盖单击/双击时序、拖动方向判定、长按倍速，以及与子组件（按钮）的事件边界。
 * 其中「带轻微抖动的按钮点击」是 bug 回归用例：父节点若在越过 touchSlop 前消费移动事件，
 * 子组件的 `detectTapGestures` 会判定手势取消，表现为播放器按钮点不动。
 */
@RunWith(AndroidJUnit4::class)
class PlayerGestureInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 单击画面空白区域应触发一次交互回调，并在双击窗口后派发单击。 */
    @Test
    fun tapOnEmptyArea_triggersInteractionAndSingleTap() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput { click() }

        assertThat(recorder.interactions).isEqualTo(1)
        composeRule.waitUntil(TAP_DELIVERY_TIMEOUT_MS) { recorder.singleTaps == 1 }
    }

    /** 触摸子按钮（事件被子组件消费）应触发子按钮点击与根节点交互，但不应派发单击。 */
    @Test
    fun tapOnChildButton_clicksChildOnly() {
        val recorder = Recorder()
        var childClicks = 0
        setPlayerContent(recorder) {
            TestButton(onClick = { childClicks++ })
        }

        composeRule.onNodeWithTag(CHILD_TAG).performTouchInput { click() }

        assertThat(childClicks).isEqualTo(1)
        assertThat(recorder.interactions).isEqualTo(1)
        // 双击窗口结束后仍不应有延迟派发的单击（否则控制器显隐会与按钮点击打架）
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_TIMEOUT_MS)
        composeRule.waitForIdle()
        assertThat(recorder.singleTaps).isEqualTo(0)
    }

    /** 按钮点击时手指有轻微抖动（小于 touchSlop）仍应正常触发点击。 */
    @Test
    fun tapWithJitter_onChildButton_stillClicks() {
        val recorder = Recorder()
        var childClicks = 0
        setPlayerContent(recorder) {
            TestButton(onClick = { childClicks++ })
        }

        composeRule.onNodeWithTag(CHILD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            up()
        }

        assertThat(childClicks).isEqualTo(1)
        assertThat(recorder.singleTaps).isEqualTo(0)
    }

    /** 双击只应派发双击回调，不能再补一次单击回调。 */
    @Test
    fun doubleTap_triggersDoubleTapOnly() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput { doubleClick() }

        composeRule.waitUntil(TAP_DELIVERY_TIMEOUT_MS) { recorder.doubleTaps == 1 }
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_TIMEOUT_MS)
        composeRule.waitForIdle()
        assertThat(recorder.singleTaps).isEqualTo(0)
    }

    /** 水平拖动应持续上报 seek 增量，松手时提交一次。 */
    @Test
    fun horizontalDrag_seeksAndCommits() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(200f, 0f))
            moveBy(Offset(200f, 0f))
            up()
        }

        assertThat(recorder.seekDeltas).isNotEmpty()
        assertThat(recorder.seekDeltas.sum()).isGreaterThan(0L)
        assertThat(recorder.seekCommits).isEqualTo(1)
        assertThat(recorder.brightnessDeltas).isEmpty()
        assertThat(recorder.volumeDeltas).isEmpty()
    }

    /** 左半屏竖直上滑应按屏幕比例增大亮度。 */
    @Test
    fun verticalDragOnLeftHalf_changesBrightness() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        var expectedFraction = 0f
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.25f, height * 0.5f))
            moveBy(Offset(0f, -200f))
            up()
            expectedFraction = 200f / (height * VERTICAL_DRAG_RANGE_RATIO)
        }

        assertThat(recorder.brightnessDeltas.sum()).isWithin(1e-4f).of(expectedFraction)
        assertThat(recorder.volumeDeltas).isEmpty()
        assertThat(recorder.seekDeltas).isEmpty()
    }

    /** 右半屏竖直上滑应按屏幕比例增大音量。 */
    @Test
    fun verticalDragOnRightHalf_changesVolume() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        var expectedFraction = 0f
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.75f, height * 0.5f))
            moveBy(Offset(0f, -200f))
            up()
            expectedFraction = 200f / (height * VERTICAL_DRAG_RANGE_RATIO)
        }

        assertThat(recorder.volumeDeltas.sum()).isWithin(1e-4f).of(expectedFraction)
        assertThat(recorder.brightnessDeltas).isEmpty()
        assertThat(recorder.seekDeltas).isEmpty()
    }

    /** 同样距离的竖直滑动，亮度与音量的调整幅度应一致。 */
    @Test
    fun verticalDrag_sameDistance_adjustsBrightnessAndVolumeEqually() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.25f, height * 0.5f))
            moveBy(Offset(0f, -200f))
            up()
        }
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.75f, height * 0.5f))
            moveBy(Offset(0f, -200f))
            up()
        }

        assertThat(recorder.brightnessDeltas.sum()).isWithin(1e-4f).of(recorder.volumeDeltas.sum())
    }

    /** 长按不动应在长按超时后开始倍速，抬手后结束。 */
    @Test
    fun longPress_startsAndEndsSpeedPress() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput { down(center) }
        // 长按计时由协程 delay 驱动，测试环境的时间是虚拟时钟，用 mainClock 推进
        composeRule.mainClock.advanceTimeBy(LONG_PRESS_HOLD_MS)
        composeRule.waitForIdle()
        assertThat(recorder.speedPressStarts).isEqualTo(1)

        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
        assertThat(recorder.speedPressEnds).isEqualTo(1)
        assertThat(recorder.singleTaps).isEqualTo(0)
    }

    /** 连续两次独立触摸应累计两次交互（用于验证"每次交互都续期"）。 */
    @Test
    fun twoTaps_accumulateInteractions() {
        val recorder = Recorder()
        setPlayerContent(recorder)

        composeRule.onRoot().performTouchInput { click() }
        composeRule.onRoot().performTouchInput { click() }

        assertThat(recorder.interactions).isEqualTo(2)
    }

    /** 播放器底部快捷按钮（TV Material3 Surface + touchClickable）应能正常点击。 */
    @Test
    fun controllerButtonRow_isClickable() {
        val recorder = Recorder()
        var playPauseClicks = 0
        composeRule.setContent {
            TvMaterialTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .playerGestures(
                                totalDuration = { TOTAL_DURATION_MS },
                                callbacks = recorder.callbacks(),
                                gestureTipState = rememberGestureTipState(),
                            ),
                ) {
                    ControllerVideoInfoBottom(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        isSeeking = false,
                        goTime = 0L,
                        seekerState = SeekerState(totalDuration = TOTAL_DURATION_MS),
                        videoShot = null,
                        videoShotCache = VideoShotImageCache(),
                        isPgc = false,
                        danmakuEnabled = true,
                        isLooping = false,
                        onDirectionLeft = {},
                        onDirectionRight = {},
                        onSeekGoTime = {},
                        onSeekToPosition = {},
                        onPlayPause = { playPauseClicks++ },
                        onDanmakuSwitchChange = {},
                        onShowSettings = {},
                        onShowRelatedVideos = {},
                        onGoToVideoInfo = {},
                        onToggleLoop = {},
                        onGoToUpPage = {},
                        onShowInteraction = {},
                        onShowComments = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // 带轻微抖动更接近真实触屏
        composeRule.onNodeWithContentDescription("播放/暂停").performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            up()
        }

        assertThat(playPauseClicks).isEqualTo(1)
        assertThat(recorder.singleTaps).isEqualTo(0)
    }

    private fun setPlayerContent(
        recorder: Recorder,
        content: @Composable BoxScope.() -> Unit = {},
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .playerGestures(
                                totalDuration = { TOTAL_DURATION_MS },
                                callbacks = recorder.callbacks(),
                                gestureTipState = rememberGestureTipState(),
                            ),
                    content = content,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * 模拟播放器快捷按钮：TV Material3 组件同样依赖 [touchClickable] 补充触屏点击。
     */
    @Composable
    private fun TestButton(onClick: () -> Unit) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .testTag(CHILD_TAG)
                    .touchClickable(onClick = onClick)
                    .focusable(),
        )
    }

    /** 记录所有手势回调。 */
    private class Recorder {
        var interactions = 0
        var singleTaps = 0
        var doubleTaps = 0
        var seekCommits = 0
        var speedPressStarts = 0
        var speedPressEnds = 0
        val seekDeltas = mutableListOf<Long>()
        val brightnessDeltas = mutableListOf<Float>()
        val volumeDeltas = mutableListOf<Float>()

        fun callbacks(): PlayerGestureCallbacks =
            PlayerGestureCallbacks(
                onSingleTap = { singleTaps++ },
                onDoubleTap = { doubleTaps++ },
                onSeekDelta = { seekDeltas += it },
                onSeekCommit = { seekCommits++ },
                onBrightnessChange = { brightnessDeltas += it },
                onVolumeChange = { volumeDeltas += it },
                onUserInteraction = { interactions++ },
                onSpeedPressStart = { speedPressStarts++ },
                onSpeedPressEnd = { speedPressEnds++ },
            )
    }

    private companion object {
        const val CHILD_TAG = "gesture_child"
        const val TOTAL_DURATION_MS = 600_000L
        const val TAP_DELIVERY_TIMEOUT_MS = 1_000L
        const val DOUBLE_TAP_TIMEOUT_MS = 600L
        const val LONG_PRESS_HOLD_MS = 600L
    }
}
