package dev.frost819.newbv.app.ui.component.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.viewmodel.comment.ReplyListState
import dev.frost819.newbv.biliapi.entity.comment.Comment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [ReplyFooterRow] 的插桩测试。
 *
 * 验证楼中楼分页 footer 的「更多回复」按钮：点击只触发一次加载回调，
 * 以及加载中/失败状态下的文案与重试入口。
 */
@RunWith(AndroidJUnit4::class)
class ReplyFooterRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeReply(index: Int): Comment =
        Comment(
            rpid = index.toLong(),
            oid = 1L,
            type = 1,
            mid = 100L,
            rootRpid = 1L,
            parentRpid = 1L,
            userName = "用户$index",
            avatar = "",
            level = 3,
            message = "回复$index",
            pictures = emptyList(),
            ctime = 0L,
            likeCount = 0L,
            replyCount = 0,
            isLiked = false,
            isUp = false,
        )

    @Composable
    private fun renderFooter(
        replyList: ReplyListState,
        onRetry: () -> Unit = {},
        onLoadMore: () -> Unit = {},
    ) {
        TvMaterialTheme {
            Box(modifier = Modifier.fillMaxSize().width(480.dp)) {
                ReplyFooterRow(replyList = replyList, onRetry = onRetry, onLoadMore = onLoadMore)
            }
        }
    }

    @Test
    fun moreButton_showsPlainLabelAndTriggersOnce() {
        var loadMoreCalls = 0
        val replyList =
            ReplyListState(
                rootRpid = 1L,
                replies = List(20) { fakeReply(it) },
                page = 1,
                total = 53,
                hasMore = true,
                expanded = true,
                loaded = true,
            )
        composeRule.setContent { renderFooter(replyList, onLoadMore = { loadMoreCalls++ }) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("更多回复").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertThat(loadMoreCalls).isEqualTo(1)
    }

    @Test
    fun loadingMore_showsLoadingText() {
        val replyList =
            ReplyListState(
                rootRpid = 1L,
                replies = List(20) { fakeReply(it) },
                page = 1,
                total = 53,
                hasMore = true,
                expanded = true,
                loaded = true,
                loadingMore = true,
            )
        composeRule.setContent { renderFooter(replyList) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("回复加载中…").assertIsDisplayed()
        composeRule.onNodeWithText("更多回复").assertDoesNotExist()
    }

    @Test
    fun loadMoreError_showsRetry() {
        var retryCalls = 0
        val replyList =
            ReplyListState(
                rootRpid = 1L,
                replies = List(20) { fakeReply(it) },
                page = 1,
                total = 53,
                hasMore = true,
                expanded = true,
                loaded = true,
                loadMoreError = true,
            )
        composeRule.setContent { renderFooter(replyList, onLoadMore = { retryCalls++ }) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertThat(retryCalls).isEqualTo(1)
    }

    @Test
    fun initialError_showsRetry() {
        var retryCalls = 0
        val replyList = ReplyListState(rootRpid = 1L, expanded = true, initialError = true)
        composeRule.setContent { renderFooter(replyList, onRetry = { retryCalls++ }) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("回复加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertThat(retryCalls).isEqualTo(1)
    }
}
