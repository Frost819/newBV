package dev.frost819.newbv.app.viewmodel.comment

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.entity.comment.CommentPage
import dev.frost819.newbv.biliapi.repositories.CommentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [CommentViewModel] 的分页、楼中楼和点赞测试。 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: CommentRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load requests hot comments and exposes page`() = runTest(dispatcher) {
        val root = comment(rpid = 1L, replyCount = 2)
        coEvery { repository.getComments(any(), any(), any(), any()) } returns CommentPage(
            comments = listOf(root),
            page = 1,
            total = 1,
            hasMore = false,
        )
        val viewModel = CommentViewModel(repository)

        viewModel.load(100L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.comments).containsExactly(root)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
        coVerify { repository.getComments(100L, 1, 1, any()) }
    }

    @Test
    fun `toggle replies loads and expands root comment`() = runTest(dispatcher) {
        val root = comment(rpid = 1L, replyCount = 1)
        val reply = comment(rpid = 2L, rootRpid = 1L)
        coEvery { repository.getComments(any(), any(), any(), any()) } returns CommentPage(
            comments = listOf(root),
            page = 1,
            total = 1,
            hasMore = false,
        )
        coEvery { repository.getReplies(any(), any(), any(), any()) } returns CommentPage(
            comments = listOf(reply),
            page = 1,
            total = 1,
            hasMore = false,
        )
        val viewModel = CommentViewModel(repository)
        viewModel.load(100L)
        advanceUntilIdle()

        viewModel.toggleReplies(1L)
        advanceUntilIdle()

        val result = viewModel.uiState.value.comments.single()
        assertThat(result.isExpanded).isTrue()
        assertThat(result.replies).containsExactly(reply)
    }

    @Test
    fun `toggle like updates comment and calls repository`() = runTest(dispatcher) {
        val root = comment(rpid = 1L)
        coEvery { repository.getComments(any(), any(), any(), any()) } returns CommentPage(
            comments = listOf(root),
            page = 1,
            total = 1,
            hasMore = false,
        )
        coEvery { repository.toggleCommentLike(any(), any(), any(), any()) } returns Unit
        val viewModel = CommentViewModel(repository)
        viewModel.load(100L)
        advanceUntilIdle()

        viewModel.toggleLike(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.comments.single().isLiked).isTrue()
        assertThat(viewModel.uiState.value.comments.single().likeCount).isEqualTo(1L)
        coVerify { repository.toggleCommentLike(100L, 1L, true, any()) }
    }

    private fun comment(
        rpid: Long,
        rootRpid: Long = 0L,
        replyCount: Int = 0,
    ) = Comment(
        rpid = rpid,
        oid = 100L,
        type = 1,
        mid = 10L,
        rootRpid = rootRpid,
        parentRpid = 0L,
        userName = "测试用户",
        avatar = "",
        level = 6,
        message = "测试评论",
        pictures = emptyList(),
        ctime = 0L,
        likeCount = 0L,
        replyCount = replyCount,
        isLiked = false,
        isUp = false,
    )
}
