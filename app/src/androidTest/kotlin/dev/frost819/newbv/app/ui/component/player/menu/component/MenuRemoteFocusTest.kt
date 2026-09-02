package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 播放器菜单的遥控器焦点和方向键行为测试。 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MenuRemoteFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun radioMenu_rightKey_returnsToParent() {
        var returnedToParent = false

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(400.dp)) {
                    RadioMenuList(
                        items = listOf("选项一", "选项二"),
                        selected = 0,
                        onSelectedChanged = {},
                        onFocusBackToParent = { returnedToParent = true },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("选项一").requestFocus()
        composeRule.onNodeWithText("选项一").performKeyInput {
            pressKey(Key.DirectionRight)
        }

        assertTrue(returnedToParent)
    }

    @Test
    fun checkBoxMenu_rightKey_returnsToParent_withoutChangingSelection() {
        var returnedToParent = false
        var selectedIndexes = listOf(0)

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(400.dp)) {
                    CheckBoxMenuList(
                        items = listOf("选项一", "选项二"),
                        selected = selectedIndexes,
                        onSelectedChanged = { selectedIndexes = it },
                        onFocusBackToParent = { returnedToParent = true },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("选项一").requestFocus()
        composeRule.onNodeWithText("选项一").performKeyInput {
            pressKey(Key.DirectionRight)
        }

        assertTrue(returnedToParent)
        assertEquals(listOf(0), selectedIndexes)
    }

    @Test
    fun stepLess_float_upAndDownKeys_changeValueWithinRange() {
        var value by mutableStateOf(0.5f)

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(400.dp)) {
                    StepLessMenuItem(
                        value = value,
                        text = "50%",
                        step = 0.1f,
                        range = 0f..1f,
                        onValueChange = { value = it },
                        onFocusBackToParent = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("50%").requestFocus()
        composeRule.onNodeWithText("50%").performKeyInput {
            pressKey(Key.DirectionUp)
        }
        assertEquals(0.6f, value)

        composeRule.onNodeWithText("50%").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        assertEquals(0.5f, value)
    }

    @Test
    fun stepLess_int_rightKey_returnsToParent() {
        var returnedToParent = false

        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(400.dp)) {
                    StepLessMenuItem(
                        value = 20,
                        text = "20dp",
                        onValueChange = {},
                        onFocusBackToParent = { returnedToParent = true },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("20dp").requestFocus()
        composeRule.onNodeWithText("20dp").performKeyInput {
            pressKey(Key.DirectionRight)
        }

        assertTrue(returnedToParent)
    }
}
