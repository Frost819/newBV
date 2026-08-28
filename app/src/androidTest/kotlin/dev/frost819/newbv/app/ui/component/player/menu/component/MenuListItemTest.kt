package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
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
 * [MenuListItem] 的插桩测试。
 *
 * 验证展开/折叠状态下的文本显示逻辑。
 */
@RunWith(AndroidJUnit4::class)
class MenuListItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        text: String,
        expanded: Boolean,
        selected: Boolean = false,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(300.dp)) {
                    MenuListItem(
                        text = text,
                        icon = Icons.Rounded.Speed,
                        expanded = expanded,
                        selected = selected,
                        onClick = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun expanded_showsText() {
        setContent(text = "倍速", expanded = true)

        composeRule.onNodeWithText("倍速").assertIsDisplayed()
    }

    @Test
    fun collapsed_hidesText() {
        setContent(text = "倍速", expanded = false)

        composeRule.onNodeWithText("倍速").assertDoesNotExist()
    }
}
