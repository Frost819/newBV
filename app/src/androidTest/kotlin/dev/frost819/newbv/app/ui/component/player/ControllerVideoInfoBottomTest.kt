package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [ControllerVideoInfoBottom] 的插桩测试。
 *
 * 重点验证 PGC 播放时隐藏“视频信息 / up主页 / 相关视频”三个按钮，
 * 以及非 PGC 时正常显示。
 */
@RunWith(AndroidJUnit4::class)
class ControllerVideoInfoBottomTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(isPgc: Boolean) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ControllerVideoInfoBottom(
                        isSeeking = false,
                        goTime = 0L,
                        seekerState = SeekerState(totalDuration = 600_000L, currentTime = 120_000L),
                        videoShot = null,
                        videoShotCache = VideoShotImageCache(),
                        isPgc = isPgc,
                        danmakuEnabled = true,
                        isLooping = false,
                        onDirectionLeft = {},
                        onDirectionRight = {},
                        onSeekGoTime = {},
                        onSeekToPosition = {},
                        onPlayPause = {},
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
    }

    @Test
    fun pgc_hides_video_info_up_and_related_buttons() {
        setContent(isPgc = true)

        composeRule.onNodeWithContentDescription("视频信息").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("up主页").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("相关视频").assertDoesNotExist()
    }

    @Test
    fun non_pgc_shows_video_info_up_and_related_buttons() {
        setContent(isPgc = false)

        composeRule.onNodeWithContentDescription("视频信息").assertExists()
        composeRule.onNodeWithContentDescription("up主页").assertExists()
        composeRule.onNodeWithContentDescription("相关视频").assertExists()
    }

    @Test
    fun pgc_still_shows_play_danmaku_and_settings_buttons() {
        setContent(isPgc = true)

        composeRule.onNodeWithContentDescription("播放/暂停").assertExists()
        composeRule.onNodeWithContentDescription("弹幕开关").assertExists()
        composeRule.onNodeWithContentDescription("打开设置").assertExists()
    }
}
