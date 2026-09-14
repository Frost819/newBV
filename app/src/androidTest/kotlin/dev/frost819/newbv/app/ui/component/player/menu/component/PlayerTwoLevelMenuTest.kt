package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.MenuFocusStateData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [PlayerTwoLevelMenu] 的插桩测试。
 *
 * 验证选项渲染、选中项回退（selected 不在 items 中）与空列表不崩溃。
 */
@RunWith(AndroidJUnit4::class)
class PlayerTwoLevelMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * 提供 [MenuFocusState.Items] 焦点状态，使列表处于可见分支。
     */
    private fun setContent(
        items: List<String>,
        selected: String?,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                CompositionLocalProvider(
                    LocalMenuFocusStateData provides MenuFocusStateData(focusState = MenuFocusState.Items),
                ) {
                    Box(modifier = Modifier.fillMaxSize().width(300.dp)) {
                        PlayerTwoLevelMenu(
                            items = items,
                            selected = selected,
                            itemLabel = { it },
                            onItemSelected = {},
                            navFocusRequester = remember { FocusRequester() },
                            onFocusStateChange = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun displays_allItems() {
        setContent(items = listOf("A", "B"), selected = "A")

        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun selectedNotInItems_fallsBackToFirstItem() {
        setContent(items = listOf("A", "B"), selected = "C")

        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun emptyItems_doesNotCrash() {
        setContent(items = emptyList(), selected = null)

        composeRule.onNodeWithText("A").assertDoesNotExist()
    }
}
