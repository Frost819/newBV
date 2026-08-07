package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ListFooterTip] 的插桩测试。
 *
 * 验证 loading / error / no-more 三态显示逻辑及优先级。
 */
@RunWith(AndroidJUnit4::class)
class ListFooterTipTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: ListFooterTipState) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(400.dp)) {
                    ListFooterTip(
                        isLoading = state.isLoading,
                        isError = state.isError,
                        hasMore = state.hasMore,
                        itemsIsEmpty = state.itemsIsEmpty,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun loading_showsLoadingText() {
        setContent(ListFooterTipState(isLoading = true))

        composeRule.onNodeWithText("加载中…").assertIsDisplayed()
    }

    @Test
    fun loading_takes_priority_over_error() {
        setContent(ListFooterTipState(isLoading = true, isError = true))

        composeRule.onNodeWithText("加载中…").assertIsDisplayed()
        composeRule.onNodeWithText("加载失败").assertDoesNotExist()
    }

    @Test
    fun error_showsErrorText() {
        setContent(ListFooterTipState(isError = true))

        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
    }

    @Test
    fun noMore_with_nonEmptyList_showsNoMoreText() {
        setContent(ListFooterTipState(hasMore = false, itemsIsEmpty = false))

        composeRule.onNodeWithText("没有更多了捏").assertIsDisplayed()
    }

    @Test
    fun noMore_with_emptyList_showsNothing() {
        setContent(ListFooterTipState(hasMore = false, itemsIsEmpty = true))

        composeRule.onNodeWithText("没有更多了捏").assertDoesNotExist()
        composeRule.onNodeWithText("加载中…").assertDoesNotExist()
        composeRule.onNodeWithText("加载失败").assertDoesNotExist()
    }

    @Test
    fun hasMore_showsNothing() {
        setContent(ListFooterTipState(hasMore = true, itemsIsEmpty = false))

        composeRule.onNodeWithText("没有更多了捏").assertDoesNotExist()
        composeRule.onNodeWithText("加载中…").assertDoesNotExist()
        composeRule.onNodeWithText("加载失败").assertDoesNotExist()
    }

    @Test
    fun error_takes_priority_over_noMore() {
        setContent(ListFooterTipState(isError = true, hasMore = false, itemsIsEmpty = false))

        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("没有更多了捏").assertDoesNotExist()
    }

    private data class ListFooterTipState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val hasMore: Boolean = true,
        val itemsIsEmpty: Boolean = false,
    )
}
