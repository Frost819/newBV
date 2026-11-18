package dev.frost819.newbv.app.ui.component.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [QualitySelectDialog] 的插桩测试。
 *
 * 验证弹窗渲染全部可用画质档位（qn 降序）、标题显示，
 * 以及不在可用列表中的档位不出现。
 *
 * 点击交互因 TV Material3 alpha 限制不纳入断言（AGENTS §11.5.3）。
 */
@RunWith(AndroidJUnit4::class)
class QualitySelectDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        availableQualityIds: List<Int>,
        currentQualityId: Int,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                QualitySelectDialog(
                    availableQualityIds = availableQualityIds,
                    currentQualityId = currentQualityId,
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun shows_title_and_all_available_qualities() {
        setContent(availableQualityIds = listOf(16, 32, 64, 80, 116), currentQualityId = 80)

        composeRule.onNodeWithText("清晰度").assertIsDisplayed()
        composeRule.onNodeWithText("360P").assertIsDisplayed()
        composeRule.onNodeWithText("480P").assertIsDisplayed()
        composeRule.onNodeWithText("720P").assertIsDisplayed()
        composeRule.onNodeWithText("1080P").assertIsDisplayed()
        composeRule.onNodeWithText("1080P60").assertIsDisplayed()
    }

    @Test
    fun hides_unavailable_qualities() {
        setContent(availableQualityIds = listOf(32, 64), currentQualityId = 64)

        composeRule.onNodeWithText("480P").assertIsDisplayed()
        composeRule.onNodeWithText("720P").assertIsDisplayed()
        composeRule.onNodeWithText("1080P").assertDoesNotExist()
        composeRule.onNodeWithText("4K").assertDoesNotExist()
    }

    @Test
    fun current_quality_not_in_list_still_renders_all_items() {
        // 解码回退瞬间当前档位可能不在可用列表：列表应正常渲染且不崩溃
        setContent(availableQualityIds = listOf(16, 64), currentQualityId = 80)

        composeRule.onNodeWithText("720P").assertIsDisplayed()
        composeRule.onNodeWithText("360P").assertIsDisplayed()
    }
}
