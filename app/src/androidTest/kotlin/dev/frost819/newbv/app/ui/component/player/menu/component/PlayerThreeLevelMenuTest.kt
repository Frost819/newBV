package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Text
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.viewmodel.player.LocalMenuFocusStateData
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.MenuFocusStateData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [PlayerThreeLevelMenu] 的插桩测试。
 *
 * 重点验证子项列表为空时的防空行为（不渲染、不调用值面板），
 * 以及非空时默认选中第一项并渲染。
 */
@RunWith(AndroidJUnit4::class)
class PlayerThreeLevelMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * 提供 [MenuFocusState.Items] 焦点状态，使值面板处于可见分支，
     * 从而确保「空列表不调用值面板」是防空逻辑生效而非焦点状态导致。
     */
    private fun setContent(
        categories: List<String>,
        onValuePanel: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                CompositionLocalProvider(
                    LocalMenuFocusStateData provides MenuFocusStateData(focusState = MenuFocusState.Items),
                ) {
                    Box(modifier = Modifier.fillMaxSize().width(300.dp)) {
                        PlayerThreeLevelMenu(
                            categories = categories,
                            categoryLabel = { it },
                            onFocusStateChange = {},
                        ) { selected, _, _ ->
                            onValuePanel(selected)
                            Text(text = "value-$selected")
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun emptyCategories_doesNotInvokeValuePanel() {
        // Given
        var invoked = false

        // When
        setContent(categories = emptyList()) { invoked = true }

        // Then
        assertThat(invoked).isFalse()
    }

    @Test
    fun nonEmptyCategories_invokesValuePanelWithFirstItem() {
        // Given
        var selected: String? = null

        // When
        setContent(categories = listOf("A", "B")) { selected = it }

        // Then
        assertThat(selected).isEqualTo("A")
        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
        composeRule.onNodeWithText("value-A").assertIsDisplayed()
    }
}
