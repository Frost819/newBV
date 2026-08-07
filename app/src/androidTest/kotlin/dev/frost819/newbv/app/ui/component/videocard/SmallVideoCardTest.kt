package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [SmallVideoCard] 的插桩测试。
 *
 * 验证卡片显示内容（标题、UP 主名、播放数、时长）。
 * 点击与长按交互因 TV Material3 alpha 版本限制暂不纳入。
 */
@RunWith(AndroidJUnit4::class)
class SmallVideoCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeData = VideoCardData(
        avid = 12345L,
        title = "测试视频标题",
        cover = "http://example.com/cover.jpg",
        upName = "测试UP主",
        upMid = 100L,
        playString = "1.0万",
        danmakuString = "500",
        timeString = "02:00",
        pubTime = "2024-01-01",
    )

    private fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun displays_title() {
        setContent {
            Box(modifier = Modifier.width(300.dp)) {
                SmallVideoCard(data = fakeData, onClick = {})
            }
        }
        composeRule.onNodeWithText("测试视频标题").assertIsDisplayed()
    }

    @Test
    fun displays_upName() {
        setContent {
            Box(modifier = Modifier.width(300.dp)) {
                SmallVideoCard(data = fakeData, onClick = {})
            }
        }
        composeRule.onNodeWithText("测试UP主").assertIsDisplayed()
    }

    @Test
    fun displays_playCount() {
        setContent {
            Box(modifier = Modifier.width(300.dp)) {
                SmallVideoCard(data = fakeData, onClick = {})
            }
        }
        composeRule.onNodeWithText("1.0万").assertIsDisplayed()
    }

    @Test
    fun displays_duration() {
        setContent {
            Box(modifier = Modifier.width(300.dp)) {
                SmallVideoCard(data = fakeData, onClick = {})
            }
        }
        composeRule.onNodeWithText("02:00").assertIsDisplayed()
    }

    @Test
    fun displays_allFieldsTogether() {
        setContent {
            Box(modifier = Modifier.width(300.dp)) {
                SmallVideoCard(data = fakeData, onClick = {})
            }
        }
        composeRule.onNodeWithText("测试视频标题").assertIsDisplayed()
        composeRule.onNodeWithText("测试UP主").assertIsDisplayed()
        composeRule.onNodeWithText("1.0万").assertIsDisplayed()
        composeRule.onNodeWithText("02:00").assertIsDisplayed()
    }
}
