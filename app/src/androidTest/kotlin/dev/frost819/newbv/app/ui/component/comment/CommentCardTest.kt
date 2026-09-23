package dev.frost819.newbv.app.ui.component.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.comment.Comment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [CommentCard] 的插桩测试。
 *
 * 验证超长评论的"展开/收起"按钮仅在文本实际溢出时出现，并能切换全文显示。
 */
@RunWith(AndroidJUnit4::class)
class CommentCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val longMessage = "这是一条很长的评论内容，".repeat(40)

    private fun fakeComment(
        message: String,
        replyCount: Int = 0,
    ): Comment =
        Comment(
            rpid = 1L,
            oid = 1L,
            type = 1,
            mid = 100L,
            rootRpid = 0L,
            parentRpid = 0L,
            userName = "测试用户",
            avatar = "",
            level = 5,
            message = message,
            pictures = emptyList(),
            ctime = 0L,
            likeCount = 0L,
            replyCount = replyCount,
            isLiked = false,
            isUp = false,
        )

    /**
     * 以外部可控的 [isTextExpanded] 状态渲染 [CommentCard]，避免依赖点击驱动的 UI 状态。
     */
    private fun setContent(
        message: String,
        replyCount: Int = 0,
        initiallyExpanded: Boolean = false,
        onToggleTextExpand: () -> Unit = {},
    ) {
        composeRule.setContent {
            var expanded by remember { mutableStateOf(initiallyExpanded) }
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(320.dp)) {
                    CommentCard(
                        comment = fakeComment(message, replyCount),
                        compact = false,
                        isLiking = false,
                        showReplyButton = replyCount > 0,
                        isExpanded = false,
                        isLoadingReplies = false,
                        isTextExpanded = expanded,
                        onToggleTextExpand = {
                            expanded = !expanded
                            onToggleTextExpand()
                        },
                        onToggleReplies = {},
                        onToggleLike = {},
                        onImageClick = { _, _ -> },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun longComment_showsExpandButton() {
        setContent(longMessage)

        composeRule.onNodeWithText("展开").assertIsDisplayed()
    }

    @Test
    fun shortComment_hidesExpandButton() {
        setContent("这是一条评论内容")

        composeRule.onNodeWithText("展开").assertDoesNotExist()
        composeRule.onNodeWithText("收起").assertDoesNotExist()
    }

    @Test
    fun expandButton_click_togglesToCollapse() {
        var toggled = false
        setContent(longMessage, onToggleTextExpand = { toggled = true })

        composeRule.onNodeWithText("展开").performClick()
        composeRule.waitForIdle()

        assertThat(toggled).isTrue()
        composeRule.onNodeWithText("收起").assertIsDisplayed()
    }
}
