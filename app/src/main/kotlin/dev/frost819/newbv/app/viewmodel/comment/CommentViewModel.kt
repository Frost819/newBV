package dev.frost819.newbv.app.viewmodel.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.repositories.CommentRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val COMMENT_LOAD_TIMEOUT_MS = 10_000L

/** 评论排序方式。 */
enum class CommentSort(val apiValue: Int, val displayName: String) {
    /** 按热度排序。 */
    Hot(1, "热门"),

    /** 按发布时间排序。 */
    Latest(0, "最新"),
}

/** 评论弹窗 UI 状态。 */
data class CommentUiState(
    val aid: Long = 0L,
    val comments: List<Comment> = emptyList(),
    val sort: CommentSort = CommentSort.Hot,
    val page: Int = 0,
    val total: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: Boolean = false,
    val loadingReplyIds: Set<Long> = emptySet(),
    val likingIds: Set<Long> = emptySet(),
)

/**
 * 评论弹窗 ViewModel。
 *
 * 负责主评论分页、楼中楼按需展开和评论点赞。详情页与播放器各自拥有弹窗状态，
 * 视频点赞/投币/收藏则通过 [dev.frost819.newbv.app.data.VideoInfoRepository] 共享。
 *
 * @param commentRepository 评论数据仓库
 */
@HiltViewModel
class CommentViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger("CommentViewModel")

    private val _uiState = MutableStateFlow(CommentUiState())
    /** 当前评论弹窗状态。 */
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    /**
     * 加载指定视频的第一页评论。
     *
     * @param aid 视频 AV 号
     */
    fun load(aid: Long) {
        if (aid <= 0L) return
        if (_uiState.value.aid == aid && (_uiState.value.loading || _uiState.value.comments.isNotEmpty())) return
        _uiState.update { it.copy(aid = aid) }
        refresh()
    }

    /** 刷新评论列表并清理分页和楼中楼状态。 */
    fun refresh() {
        val aid = _uiState.value.aid
        if (aid <= 0L || _uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = false,
                    comments = emptyList(),
                    page = 0,
                    total = 0,
                    hasMore = true,
                    loadingReplyIds = emptySet(),
                    likingIds = emptySet(),
                )
            }
            try {
                val page = withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                    commentRepository.getComments(
                        aid = aid,
                        sort = _uiState.value.sort.apiValue,
                        page = 1,
                        preferApiType = ApiType.Web,
                    )
                }
                _uiState.update {
                    it.copy(
                        comments = page.comments,
                        page = page.page,
                        total = page.total,
                        hasMore = page.hasMore,
                    )
                }
            } catch (error: Throwable) {
                rethrowCancellation(error)
                logger.error(error) { "Failed to load comments: aid=$aid" }
                _uiState.update { it.copy(error = true) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    /** 切换热门/最新排序并重新加载。 */
    fun changeSort(sort: CommentSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        refresh()
    }

    /** 加载下一页主评论。 */
    fun loadMore() {
        val state = _uiState.value
        if (state.aid <= 0L || state.loading || state.loadingMore || !state.hasMore) return
        val nextPage = state.page + 1
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val page = withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                    commentRepository.getComments(
                        aid = state.aid,
                        sort = state.sort.apiValue,
                        page = nextPage,
                        preferApiType = ApiType.Web,
                    )
                }
                _uiState.update {
                    it.copy(
                        comments = it.comments + page.comments,
                        page = page.page,
                        total = page.total,
                        hasMore = page.hasMore,
                    )
                }
            } catch (error: Throwable) {
                rethrowCancellation(error)
                logger.error(error) { "Failed to load more comments: aid=${state.aid}" }
            } finally {
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    /**
     * 展开或收起根评论的楼中楼。
     *
     * 首次展开才请求子评论，后续收起不会丢弃已经加载的数据。
     *
     * @param rpid 根评论 ID
     */
    fun toggleReplies(rpid: Long) {
        val comment = findComment(_uiState.value.comments, rpid) ?: return
        if (comment.isExpanded) {
            updateComment(rpid) { it.copy(isExpanded = false) }
            return
        }
        if (comment.replies.isNotEmpty()) {
            updateComment(rpid) { it.copy(isExpanded = true) }
            return
        }

        val aid = _uiState.value.aid
        if (aid <= 0L || rpid <= 0L) return
        _uiState.update { it.copy(loadingReplyIds = it.loadingReplyIds + rpid) }
        viewModelScope.launch {
            try {
                val page = withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                    commentRepository.getReplies(
                        aid = aid,
                        rootRpid = rpid,
                        preferApiType = ApiType.Web,
                    )
                }
                updateComment(rpid) {
                    it.copy(
                        isExpanded = true,
                        replies = page.comments,
                        repliesError = false,
                    )
                }
            } catch (error: Throwable) {
                rethrowCancellation(error)
                logger.error(error) { "Failed to load replies: rpid=$rpid" }
                updateComment(rpid) { it.copy(isExpanded = true, repliesError = true) }
            } finally {
                _uiState.update { it.copy(loadingReplyIds = it.loadingReplyIds - rpid) }
            }
        }
    }

    /**
     * 点赞或取消点赞评论。
     *
     * @param rpid 评论 ID
     */
    fun toggleLike(rpid: Long) {
        val comment = findComment(_uiState.value.comments, rpid) ?: return
        if (rpid in _uiState.value.likingIds) return
        val target = !comment.isLiked
        _uiState.update { it.copy(likingIds = it.likingIds + rpid) }
        updateComment(rpid) {
            it.copy(
                isLiked = target,
                likeCount = (it.likeCount + if (target) 1 else -1).coerceAtLeast(0),
            )
        }
        viewModelScope.launch {
            try {
                commentRepository.toggleCommentLike(
                    aid = _uiState.value.aid,
                    rpid = rpid,
                    like = target,
                    preferApiType = ApiType.Web,
                )
            } catch (error: Throwable) {
                rethrowCancellation(error)
                logger.error(error) { "Failed to toggle comment like: rpid=$rpid" }
                updateComment(rpid) {
                    it.copy(
                        isLiked = comment.isLiked,
                        likeCount = comment.likeCount,
                    )
                }
            } finally {
                _uiState.update { it.copy(likingIds = it.likingIds - rpid) }
            }
        }
    }

    private fun updateComment(rpid: Long, transform: (Comment) -> Comment) {
        _uiState.update { state ->
            state.copy(comments = state.comments.map { updateNested(it, rpid, transform) })
        }
    }

    private fun updateNested(comment: Comment, rpid: Long, transform: (Comment) -> Comment): Comment {
        if (comment.rpid == rpid) return transform(comment)
        return comment.copy(replies = comment.replies.map { updateNested(it, rpid, transform) })
    }

    private fun findComment(comments: List<Comment>, rpid: Long): Comment? {
        comments.forEach { comment ->
            if (comment.rpid == rpid) return comment
            findComment(comment.replies, rpid)?.let { return it }
        }
        return null
    }

    private fun rethrowCancellation(error: Throwable) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
    }
}
