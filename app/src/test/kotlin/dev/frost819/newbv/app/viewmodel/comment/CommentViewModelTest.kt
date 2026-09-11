package dev.frost819.newbv.app.viewmodel.comment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.entity.comment.CommentPage
import dev.frost819.newbv.biliapi.repositories.CommentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [CommentViewModel] 的排序缓存、分页、楼中楼和点赞测试。 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: CommentRepository
    private lateinit var viewModel: CommentViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        viewModel = CommentViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load requests hot comments and exposes page`() =
        runTest(dispatcher) {
            val root = comment(rpid = 1L, replyCount = 2)
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(root), page = 1, total = 1, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.comments).containsExactly(root)
            assertThat(viewModel.uiState.value.hasMore).isFalse()
            coVerify { repository.getComments(100L, 1, 1, null, any()) }
        }

    @Test
    fun `switching sort caches each comment list and does not reload`() =
        runTest(dispatcher) {
            val hot = comment(rpid = 1L)
            val latest = comment(rpid = 2L)
            coEvery { repository.getComments(100L, 1, 1, null, any()) } returns
                page(listOf(hot), page = 1, total = 1, hasMore = false)
            coEvery { repository.getComments(100L, 0, 1, null, any()) } returns
                page(listOf(latest), page = 1, total = 1, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()

            viewModel.changeSort(CommentSort.Latest)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.comments).containsExactly(latest)

            viewModel.changeSort(CommentSort.Hot)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.comments).containsExactly(hot)

            viewModel.changeSort(CommentSort.Latest)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.comments).containsExactly(latest)

            coVerify(exactly = 1) { repository.getComments(100L, 1, 1, null, any()) }
            coVerify(exactly = 1) { repository.getComments(100L, 0, 1, null, any()) }
        }

    @Test
    fun `refresh only reloads current sort and keeps other cache`() =
        runTest(dispatcher) {
            val hot = comment(rpid = 1L)
            val latest = comment(rpid = 2L)
            coEvery { repository.getComments(100L, 1, 1, null, any()) } returns
                page(listOf(hot), page = 1, total = 1, hasMore = false)
            coEvery { repository.getComments(100L, 0, 1, null, any()) } returns
                page(listOf(latest), page = 1, total = 1, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.changeSort(CommentSort.Latest)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getComments(100L, 1, 1, null, any()) }
            coVerify(exactly = 2) { repository.getComments(100L, 0, 1, null, any()) }

            viewModel.changeSort(CommentSort.Hot)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.comments).containsExactly(hot)
            coVerify(exactly = 1) { repository.getComments(100L, 1, 1, null, any()) }
        }

    @Test
    fun `changing aid discards stale response`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<CommentPage>()
            coEvery { repository.getComments(1L, 1, 1, null, any()) } coAnswers { gate.await() }
            coEvery { repository.getComments(2L, 1, 1, null, any()) } returns
                page(listOf(comment(rpid = 20L)), page = 1, total = 1, hasMore = false)

            viewModel.load(1L)
            runCurrent()
            viewModel.load(2L)
            advanceUntilIdle()

            gate.complete(page(listOf(comment(rpid = 10L)), page = 1, total = 1, hasMore = false))
            runCurrent()

            assertThat(viewModel.uiState.value.aid).isEqualTo(2L)
            assertThat(
                viewModel.uiState.value.comments
                    .map { it.rpid },
            ).containsExactly(20L)
        }

    @Test
    fun `load more appends and deduplicates by rpid`() =
        runTest(dispatcher) {
            coEvery { repository.getComments(100L, 1, 1, null, any()) } returns
                page(listOf(comment(1L), comment(2L)), page = 1, total = 3, hasMore = true)
            coEvery { repository.getComments(100L, 1, 2, null, any()) } returns
                page(listOf(comment(2L), comment(3L)), page = 2, total = 3, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.loadMoreComments()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.comments
                    .map { it.rpid },
            ).containsExactly(1L, 2L, 3L).inOrder()
            assertThat(viewModel.uiState.value.hasMore).isFalse()
        }

    @Test
    fun `load more failure keeps list and allows retry`() =
        runTest(dispatcher) {
            var attempt = 0
            coEvery { repository.getComments(100L, 1, 1, null, any()) } returns
                page(listOf(comment(1L), comment(2L)), page = 1, total = 3, hasMore = true)
            coEvery { repository.getComments(100L, 1, 2, null, any()) } coAnswers {
                attempt++
                if (attempt == 1) throw RuntimeException("boom")
                page(listOf(comment(3L)), page = 2, total = 3, hasMore = false)
            }

            viewModel.load(100L)
            advanceUntilIdle()

            viewModel.loadMoreComments()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.loadMoreError).isTrue()
            assertThat(viewModel.uiState.value.comments).hasSize(2)

            viewModel.loadMoreComments()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.loadMoreError).isFalse()
            assertThat(
                viewModel.uiState.value.comments
                    .map { it.rpid },
            ).containsExactly(1L, 2L, 3L).inOrder()
        }

    @Test
    fun `load more empty page stops pagination`() =
        runTest(dispatcher) {
            coEvery { repository.getComments(100L, 1, 1, null, any()) } returns
                page(listOf(comment(1L)), page = 1, total = 10, hasMore = true)
            coEvery { repository.getComments(100L, 1, 2, null, any()) } returns
                page(emptyList(), page = 2, total = 10, hasMore = true)

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.loadMoreComments()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.hasMore).isFalse()
            assertThat(viewModel.uiState.value.comments).hasSize(1)
        }

    @Test
    fun `toggle replies loads and expands root comment`() =
        runTest(dispatcher) {
            val root = comment(rpid = 1L, replyCount = 1)
            val reply = comment(rpid = 2L, rootRpid = 1L)
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(root), page = 1, total = 1, hasMore = false)
            coEvery { repository.getReplies(any(), any(), any(), any(), any()) } returns
                page(listOf(reply), page = 1, total = 1, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.toggleReplies(1L)
            advanceUntilIdle()

            val replyList =
                viewModel.uiState.value.commentList
                    .replyList(1L)
            assertThat(replyList?.expanded).isTrue()
            assertThat(replyList?.replies).containsExactly(reply)
        }

    @Test
    fun `reply first page failure can be retried with one call`() =
        runTest(dispatcher) {
            var attempt = 0
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(comment(rpid = 1L, replyCount = 1)), page = 1, total = 1, hasMore = false)
            coEvery { repository.getReplies(any(), any(), any(), any(), any()) } coAnswers {
                attempt++
                if (attempt == 1) throw RuntimeException("boom")
                page(listOf(comment(rpid = 2L, rootRpid = 1L)), page = 1, total = 1, hasMore = false)
            }

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.toggleReplies(1L)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.commentList
                    .replyList(1L)
                    ?.initialError,
            ).isTrue()

            viewModel.retryReplies(1L)
            advanceUntilIdle()

            val replyList =
                viewModel.uiState.value.commentList
                    .replyList(1L)
            assertThat(replyList?.initialError).isFalse()
            assertThat(replyList?.replies).hasSize(1)
            assertThat(replyList?.expanded).isTrue()
        }

    @Test
    fun `load more replies appends next page`() =
        runTest(dispatcher) {
            val root = comment(rpid = 1L, replyCount = 25)
            val firstPage = (1L..20L).map { comment(rpid = 100L + it, rootRpid = 1L) }
            val secondPage = (21L..25L).map { comment(rpid = 100L + it, rootRpid = 1L) }
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(root), page = 1, total = 1, hasMore = false)
            coEvery { repository.getReplies(100L, 1L, 1, null, any()) } returns
                page(firstPage, page = 1, total = 25, hasMore = true)
            coEvery { repository.getReplies(100L, 1L, 2, null, any()) } returns
                page(secondPage, page = 2, total = 25, hasMore = false)

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.toggleReplies(1L)
            advanceUntilIdle()
            assertThat(
                viewModel.uiState.value.commentList
                    .replyList(1L)
                    ?.replies,
            ).hasSize(20)

            viewModel.loadMoreReplies(1L)
            advanceUntilIdle()

            val replyList =
                viewModel.uiState.value.commentList
                    .replyList(1L)
            assertThat(replyList?.replies).hasSize(25)
            assertThat(replyList?.hasMore).isFalse()
        }

    @Test
    fun `toggle like updates nested reply only after success`() =
        runTest(dispatcher) {
            val root = comment(rpid = 1L, replyCount = 1)
            val reply = comment(rpid = 2L, rootRpid = 1L)
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(root), page = 1, total = 1, hasMore = false)
            coEvery { repository.getReplies(any(), any(), any(), any(), any()) } returns
                page(listOf(reply), page = 1, total = 1, hasMore = false)
            coEvery { repository.toggleCommentLike(any(), any(), any(), any()) } returns Unit

            viewModel.load(100L)
            advanceUntilIdle()
            viewModel.toggleReplies(1L)
            advanceUntilIdle()

            viewModel.toggleLike(2L)
            assertThat(
                viewModel.uiState.value
                    .findComment(2L)
                    ?.isLiked,
            ).isFalse()

            advanceUntilIdle()
            val updated = viewModel.uiState.value.findComment(2L)
            assertThat(updated?.isLiked).isTrue()
            assertThat(updated?.likeCount).isEqualTo(1L)
            coVerify { repository.toggleCommentLike(100L, 2L, true, any()) }
        }

    @Test
    fun `toggle like emits toast and keeps state on failure`() =
        runTest(dispatcher) {
            val root = comment(rpid = 1L)
            coEvery { repository.getComments(any(), any(), any(), any(), any()) } returns
                page(listOf(root), page = 1, total = 1, hasMore = false)
            coEvery { repository.toggleCommentLike(any(), any(), any(), any()) } throws
                RuntimeException("网络错误")

            viewModel.load(100L)
            advanceUntilIdle()

            viewModel.uiEffect.test {
                viewModel.toggleLike(1L)
                advanceUntilIdle()

                assertThat(
                    viewModel.uiState.value
                        .findComment(1L)
                        ?.isLiked,
                ).isFalse()
                val effect = awaitItem() as CommentUiEffect.ShowToast
                assertThat(effect.message).contains("评论点赞失败")
            }
        }

    private fun page(
        comments: List<Comment>,
        page: Int,
        total: Int = comments.size,
        hasMore: Boolean,
    ) = CommentPage(comments = comments, page = page, total = total, hasMore = hasMore)

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
