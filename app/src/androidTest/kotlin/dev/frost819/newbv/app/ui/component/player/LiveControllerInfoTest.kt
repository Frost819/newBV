package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [LiveControllerInfo] 的插桩测试。
 *
 * 验证标题、人气 meta 行显示与隐藏。
 */
@RunWith(AndroidJUnit4::class)
class LiveControllerInfoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        title: String,
        areaName: String = "",
        onlineCount: String,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiveControllerInfo(
                        show = true,
                        title = title,
                        areaName = areaName,
                        onlineCount = onlineCount,
                        clock = Pair(20, 30),
                        isPlaying = true,
                        danmakuEnabled = true,
                        onPlayPause = {},
                        onRefresh = {},
                        onDanmakuSwitchChange = {},
                        onShowSettings = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun displays_title_and_online_count() {
        setContent(title = "测试直播标题", onlineCount = "1.2万")

        composeRule.onNodeWithText("测试直播标题").assertIsDisplayed()
        composeRule.onNodeWithText("人气 1.2万").assertIsDisplayed()
    }

    @Test
    fun hides_online_count_when_blank() {
        setContent(title = "标题", onlineCount = "")

        composeRule.onNodeWithText("人气", substring = true).assertDoesNotExist()
    }

    @Test
    fun displays_area_name_when_present() {
        setContent(title = "标题", areaName = "王者荣耀", onlineCount = "5769")

        composeRule.onNodeWithText("王者荣耀").assertIsDisplayed()
    }
}
