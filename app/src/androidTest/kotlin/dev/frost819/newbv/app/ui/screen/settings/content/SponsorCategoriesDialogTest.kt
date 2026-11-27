package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [SponsorCategoriesDialog] 的插桩测试。
 *
 * 验证分类色点指示渲染与开关点击回调。
 */
@RunWith(AndroidJUnit4::class)
class SponsorCategoriesDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displays_category_color_indicator_and_notifies_toggle() {
        var latestCategories = emptySet<String>()

        composeRule.setContent {
            TvMaterialTheme {
                SponsorCategoriesDialog(
                    enabledCategories = setOf("sponsor"),
                    onDismiss = {},
                    onCategoriesChanged = { latestCategories = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("赞助/恰饭 进度条标记颜色").assertIsDisplayed()
        composeRule.onNodeWithText("赞助/恰饭").performClick()

        assertThat(latestCategories).isEmpty()
    }
}
