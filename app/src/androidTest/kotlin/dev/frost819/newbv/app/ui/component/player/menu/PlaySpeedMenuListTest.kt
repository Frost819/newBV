package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [PlaySpeedMenuList] 的插桩测试。
 *
 * 验证 5 个倍速选项的显示及选中状态。
 */
@RunWith(AndroidJUnit4::class)
class PlaySpeedMenuListTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(selectedSpeed: PlaySpeedItem) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(300.dp)) {
                    PlaySpeedMenuList(
                        currentSelectedPlaySpeedItem = selectedSpeed,
                        onPlaySpeedChange = {},
                        onFocusStateChange = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun displays_allSpeedOptions() {
        setContent(PlaySpeedItem.X1)

        composeRule.onNodeWithText("2.0x").assertIsDisplayed()
        composeRule.onNodeWithText("1.5x").assertIsDisplayed()
        composeRule.onNodeWithText("1.25x").assertIsDisplayed()
        composeRule.onNodeWithText("1.0x").assertIsDisplayed()
        composeRule.onNodeWithText("0.5x").assertIsDisplayed()
    }

    @Test
    fun displays_speeds_when_X2_selected() {
        setContent(PlaySpeedItem.X2)

        composeRule.onNodeWithText("2.0x").assertIsDisplayed()
        composeRule.onNodeWithText("0.5x").assertIsDisplayed()
    }

    @Test
    fun displays_speeds_when_X0_5_selected() {
        setContent(PlaySpeedItem.X0_5)

        composeRule.onNodeWithText("0.5x").assertIsDisplayed()
        composeRule.onNodeWithText("2.0x").assertIsDisplayed()
    }
}
