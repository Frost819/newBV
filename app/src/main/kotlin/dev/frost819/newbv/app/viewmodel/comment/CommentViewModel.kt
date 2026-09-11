package dev.frost819.newbv.app.viewmodel.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.repositories.CommentRepository
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val COMMENT_LOAD_TIMEOUT_MS = 10_000L

/*
 * 评论区数据层级：
 *
 * CommentUiState
 * └── commentLists: Map<CommentSort, CommentListState>     每个排序一份主评论列表
 *     ├── comments: List<Comment>                          主评论（一级评论）
 *     └── replyLists: Map<Long, ReplyListState>        rootRpid -> 楼中楼（二级评论容器）
 *         └── replies: List<Comment>                       楼中楼内的回复
 */

/** 评论排序方式。 */
enum class CommentSort(
    val apiValue: Int,
    val displayName: String,
) {
    /** 按热度排序。 */
    Hot(1, "热门"),

    /** 按发布时间排序。 */
    Latest(0, "最新"),
}

/**
 * 单个根评论的楼中楼状态。
 *
 * 楼中楼按需加载并独立分页，展开、加载、错误状态与主评论分离。
 *
 * @property rootRpid 根评论 ID
 * @property replies 已加载的回复列表
 * @property page 当前已加载页码，0 表示尚未加载
 * @property total 服务端返回的回复总数，未知时为 0
 * @property hasMore 是否还有下一页回复
 * @property expanded 是否处于展开状态
 * @property loaded 是否已成功加载过第一页
 * @property initialLoading 第一页是否加载中
 * @property initialError 第一页是否加载失败
 * @property loadingMore 是否正在加载后续页
 * @property loadMoreError 后续页是否加载失败
 */
data class ReplyListState(
    val rootRpid: Long,
    val replies: List<Comment> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val expanded: Boolean = false,
    val loaded: Boolean = false,
    val initialLoading: Boolean = false,
    val initialError: Boolean = false,
    val loadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
)

/**
 * 单一排序下的主评论分页状态。
 *
 * 热门与最新各自持有一份 [CommentListState]，切换排序时可直接复用缓存。
 *
 * @property comments 主评论列表
 * @property page 当前已加载页码，0 表示尚未加载
 * @property total 评论总数，未知时为 0
 * @property hasMore 是否还有下一页
 * @property loaded 是否已成功加载过第一页
 * @property initialLoading 第一页是否加载中
 * @property initialError 第一页是否加载失败
 * @property loadingMore 是否正在加载后续页
 * @property loadMoreError 后续页是否加载失败
 * @property replyLists 根评论 ID 到楼中楼状态的映射
 */
data class CommentListState(
    val comments: List<Comment> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val loaded: Boolean = false,
    val initialLoading: Boolean = false,
    val initialError: Boolean = false,
    val loadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val replyLists: Map<Long, ReplyListState> = emptyMap(),
) {
    /** 获取指定根评论的楼中楼状态。 */
    fun replyList(rootRpid: Long): ReplyListState? = replyLists[rootRpid]

    /** 当前排序是否具备加载下一页的条件。 */
    internal fun canLoadMore(): Boolean = loaded && !initialLoading && !loadingMore && hasMore
}

/** 评论弹窗 UI 状态。 */
data class CommentUiState(
    val aid: Long = 0L,
    val sort: CommentSort = CommentSort.Hot,
    val commentLists: Map<CommentSort, CommentListState> = emptyMap(),
    val likingIds: Set<Long> = emptySet(),
) {
    /** 当前排序对应的评论分页状态。 */
    val commentList: CommentListState get() = commentLists[sort] ?: CommentListState()

    /** 当前排序的评论列表。 */
    val comments: List<Comment> get() = commentList.comments

    /** 当前排序的评论总数。 */
    val total: Int get() = commentList.total

    /** 当前排序是否还有下一页。 */
    val hasMore: Boolean get() = commentList.hasMore

    /** 当前排序第一页是否加载中。 */
    val loading: Boolean get() = commentList.initialLoading

    /** 当前排序第一页是否加载失败。 */
    val error: Boolean get() = commentList.initialError

    /** 当前排序是否正在加载后续页。 */
    val loadingMore: Boolean get() = commentList.loadingMore

    /** 当前排序后续页是否加载失败。 */
    val loadMoreError: Boolean get() = commentList.loadMoreError

    /** 在全部排序缓存中查找指定评论（含楼中楼）。 */
    fun findComment(rpid: Long): Comment? {
        commentLists.values.forEach { commentList ->
            commentList.comments.firstOrNull { it.rpid == rpid }?.let { return it }
            commentList.replyLists.values.forEach { replyList ->
                replyList.replies.firstOrNull { it.rpid == rpid }?.let { return it }
            }
        }
        return null
    }
}

/** 评论弹窗一次性 UI 事件。 */
sealed interface CommentUiEffect {
    /** 显示 Toast 消息。 */
    data class ShowToast(
        val message: String,
    ) : CommentUiEffect
}

/** [ReplyListState] 是否具备加载下一页的条件。 */
internal fun ReplyListState.canLoadMore(): Boolean = loaded && !initialLoading && !loadingMore && hasMore

/**
 * 评论弹窗 ViewModel。
 *
 * 负责主评论分页、楼中楼按需分页加载、评论点赞。热门与最新各自缓存，切换排序时
 * 命中缓存则不重新请求。所有请求使用 generation + aid/sort/root 校验，避免切换
 * 视频、切换排序或快速刷新时旧响应污染当前状态。
 *
 * @param commentRepository 评论数据仓库
 */
@HiltViewModel
class CommentViewModel
    @Inject
    constructor(
        private val commentRepository: CommentRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("CommentViewModel")

        private val _uiState = MutableStateFlow(CommentUiState())

        /** 当前评论弹窗状态。 */
        val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

        private val _uiEffect = MutableSharedFlow<CommentUiEffect>(extraBufferCapacity = 1)

        /** 评论弹窗一次性事件（如 toast）。 */
        val uiEffect: SharedFlow<CommentUiEffect> = _uiEffect.asSharedFlow()

        private val commentJobs = mutableMapOf<CommentSort, Job>()
        private val commentGenerations = mutableMapOf<CommentSort, Long>()
        private val replyJobs = mutableMapOf<String, Job>()
        private val replyGenerations = mutableMapOf<String, Long>()

        /**
         * 加载指定视频的第一页评论。
         *
         * 若 [aid] 与当前一致且已有缓存则直接复用，不发起请求。
         *
         * @param aid 视频 AV 号
         */
        fun load(aid: Long) {
            if (aid <= 0L) return
            val state = _uiState.value
            if (state.aid == aid && state.commentLists.isNotEmpty()) return
            cancelAll()
            _uiState.value = CommentUiState(aid = aid)
            startCommentsLoad(sort = _uiState.value.sort, aid = aid)
        }

        /** 刷新当前排序的评论列表，另一个排序的缓存保留。 */
        fun refresh() {
            val state = _uiState.value
            val aid = state.aid
            if (aid <= 0L) return
            val sort = state.sort
            cancelComments(sort)
            updateCommentList(sort) { CommentListState() }
            startCommentsLoad(sort = sort, aid = aid)
        }

        /**
         * 切换热门/最新排序。
         *
         * 目标排序已有缓存时立即展示，仅在从未加载过时才请求第一页。
         *
         * @param sort 目标排序
         */
        fun changeSort(sort: CommentSort) {
            val state = _uiState.value
            if (state.sort == sort) return
            _uiState.update { it.copy(sort = sort) }
            val commentList = _uiState.value.commentLists[sort]
            if (commentList == null || (!commentList.loaded && !commentList.initialLoading)) {
                startCommentsLoad(sort = sort, aid = state.aid)
            }
        }

        /** 加载当前排序的下一页主评论。 */
        fun loadMoreComments() {
            val state = _uiState.value
            val sort = state.sort
            val commentList = state.commentLists[sort] ?: return
            val aid = state.aid
            // 允许在 loadMoreError 后显式重试；自动加载由 UI 在错误态停止触发
            if (aid <= 0L || !commentList.canLoadMore()) return

            val nextPage = commentList.page + 1
            val generation = nextCommentGeneration(sort)
            commentJobs[sort]?.cancel()
            commentJobs[sort] =
                viewModelScope.launch {
                    updateCommentList(sort) { it.copy(loadingMore = true, loadMoreError = false) }
                    try {
                        val page =
                            withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                                commentRepository.getComments(
                                    aid = aid,
                                    sort = sort.apiValue,
                                    page = nextPage,
                                    preferApiType = ApiType.Web,
                                )
                            }
                        if (!isCommentListCurrent(sort, aid, generation)) return@launch
                        val current = _uiState.value.commentLists[sort] ?: return@launch
                        val existing = current.comments.mapTo(HashSet()) { it.rpid }
                        val appended = page.comments.filter { it.rpid !in existing }
                        updateCommentList(sort) {
                            it.copy(
                                comments = it.comments + appended,
                                page = page.page,
                                total = page.total,
                                // 若本页全部为重复项则停止分页，避免服务端重叠导致死循环
                                hasMore = page.hasMore && appended.isNotEmpty(),
                                loadingMore = false,
                                loadMoreError = false,
                            )
                        }
                    } catch (error: Throwable) {
                        rethrowCancellation(error)
                        logger.error(error) { "Failed to load more comments: aid=$aid, sort=${sort.name}" }
                        if (isCommentListCurrent(sort, aid, generation)) {
                            updateCommentList(sort) { it.copy(loadingMore = false, loadMoreError = true) }
                        }
                    }
                }
        }

        /**
         * 展开或收起根评论的楼中楼。
         *
         * 首次展开才请求第一页子评论，收起不会丢弃已加载数据。
         *
         * @param rpid 根评论 ID
         */
        fun toggleReplies(rpid: Long) {
            if (rpid <= 0L) return
            val state = _uiState.value
            val sort = state.sort
            val commentList = state.commentLists[sort] ?: return
            val replyList = commentList.replyList(rpid) ?: ReplyListState(rootRpid = rpid)
            if (replyList.expanded) {
                updateReplyList(rpid, sort) { it.copy(expanded = false) }
                return
            }
            updateReplyList(rpid, sort) { it.copy(expanded = true) }
            if (!replyList.loaded && !replyList.initialLoading && !replyList.initialError) {
                startRepliesLoad(sort = sort, aid = state.aid, rootRpid = rpid, page = 1)
            }
        }

        /**
         * 重新加载楼中楼第一页。
         *
         * 与 [toggleReplies] 分离，修复失败后首次点击只会收起的缺陷。
         *
         * @param rpid 根评论 ID
         */
        fun retryReplies(rpid: Long) {
            if (rpid <= 0L) return
            val state = _uiState.value
            val sort = state.sort
            updateReplyList(rpid, sort) { it.copy(expanded = true, initialError = false) }
            startRepliesLoad(sort = sort, aid = state.aid, rootRpid = rpid, page = 1)
        }

        /**
         * 加载指定根评论楼中楼的下一页。
         *
         * @param rpid 根评论 ID
         */
        fun loadMoreReplies(rpid: Long) {
            if (rpid <= 0L) return
            val state = _uiState.value
            val sort = state.sort
            val replyList = state.commentLists[sort]?.replyList(rpid) ?: return
            // 允许在 loadMoreError 后显式重试；自动加载由 UI 在错误态停止触发
            if (!replyList.canLoadMore()) return
            startRepliesLoad(sort = sort, aid = state.aid, rootRpid = rpid, page = replyList.page + 1)
        }

        /**
         * 点赞或取消点赞评论。
         *
         * 请求成功后才更新 UI 状态。失败时弹出 toast 提示。
         *
         * @param rpid 评论 ID
         */
        fun toggleLike(rpid: Long) {
            val state = _uiState.value
            if (rpid <= 0L || rpid in state.likingIds) return
            val comment = state.findComment(rpid) ?: return
            val aid = state.aid
            val target = !comment.isLiked
            _uiState.update { it.copy(likingIds = it.likingIds + rpid) }
            viewModelScope.launch {
                try {
                    commentRepository.toggleCommentLike(
                        aid = aid,
                        rpid = rpid,
                        like = target,
                        preferApiType = ApiType.Web,
                    )
                    _uiState.update { current ->
                        current.copy(
                            commentLists =
                                current.commentLists.mapValues { (_, commentList) ->
                                    commentList.withCommentLike(rpid, target)
                                },
                        )
                    }
                } catch (error: Throwable) {
                    rethrowCancellation(error)
                    logger.error(error) { "Failed to toggle comment like: rpid=$rpid" }
                    _uiEffect.emit(CommentUiEffect.ShowToast("评论点赞失败: ${error.message ?: "未知错误"}"))
                } finally {
                    _uiState.update { it.copy(likingIds = it.likingIds - rpid) }
                }
            }
        }

        private fun startCommentsLoad(
            sort: CommentSort,
            aid: Long,
        ) {
            if (aid <= 0L) return
            val generation = nextCommentGeneration(sort)
            commentJobs[sort]?.cancel()
            commentJobs[sort] =
                viewModelScope.launch {
                    updateCommentList(sort) { it.copy(initialLoading = true, initialError = false) }
                    try {
                        val page =
                            withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                                commentRepository.getComments(
                                    aid = aid,
                                    sort = sort.apiValue,
                                    page = 1,
                                    preferApiType = ApiType.Web,
                                )
                            }
                        if (!isCommentListCurrent(sort, aid, generation)) return@launch
                        updateCommentList(sort) {
                            it.copy(
                                comments = page.comments.distinctBy { comment -> comment.rpid },
                                page = page.page,
                                total = page.total,
                                hasMore = page.hasMore,
                                loaded = true,
                                initialLoading = false,
                                initialError = false,
                                loadingMore = false,
                                loadMoreError = false,
                            )
                        }
                    } catch (error: Throwable) {
                        rethrowCancellation(error)
                        logger.error(error) { "Failed to load comments: aid=$aid, sort=${sort.name}" }
                        if (isCommentListCurrent(sort, aid, generation)) {
                            updateCommentList(sort) { it.copy(initialLoading = false, initialError = true) }
                        }
                    }
                }
        }

        private fun startRepliesLoad(
            sort: CommentSort,
            aid: Long,
            rootRpid: Long,
            page: Int,
        ) {
            if (aid <= 0L || rootRpid <= 0L) return
            val key = replyKey(sort, rootRpid)
            val generation = (replyGenerations[key] ?: 0L) + 1
            replyGenerations[key] = generation
            replyJobs.remove(key)?.cancel()
            replyJobs[key] =
                viewModelScope.launch {
                    val isFirst = page <= 1
                    updateReplyList(rootRpid, sort) {
                        if (isFirst) {
                            it.copy(expanded = true, initialLoading = true, initialError = false)
                        } else {
                            it.copy(loadingMore = true, loadMoreError = false)
                        }
                    }
                    try {
                        val result =
                            withTimeout(COMMENT_LOAD_TIMEOUT_MS) {
                                commentRepository.getReplies(
                                    aid = aid,
                                    rootRpid = rootRpid,
                                    page = page,
                                    preferApiType = ApiType.Web,
                                )
                            }
                        if (!isReplyListCurrent(key, sort, aid, generation)) return@launch
                        val current =
                            _uiState.value.commentLists[sort]?.replyList(rootRpid)
                                ?: ReplyListState(rootRpid = rootRpid)
                        val existing = current.replies.mapTo(HashSet()) { it.rpid }
                        val appended = result.comments.filter { it.rpid != rootRpid && it.rpid !in existing }
                        updateReplyList(rootRpid, sort) {
                            it.copy(
                                replies = if (isFirst) appended else it.replies + appended,
                                page = result.page,
                                total = result.total,
                                // 若后续页全部为重复项则停止分页
                                hasMore = result.hasMore && (isFirst || appended.isNotEmpty()),
                                loaded = true,
                                expanded = true,
                                initialLoading = false,
                                initialError = false,
                                loadingMore = false,
                                loadMoreError = false,
                            )
                        }
                    } catch (error: Throwable) {
                        rethrowCancellation(error)
                        logger.error(error) { "Failed to load replies: aid=$aid, rootRpid=$rootRpid, page=$page" }
                        if (isReplyListCurrent(key, sort, aid, generation)) {
                            updateReplyList(rootRpid, sort) {
                                if (isFirst) {
                                    it.copy(initialLoading = false, initialError = true, loaded = false)
                                } else {
                                    it.copy(loadingMore = false, loadMoreError = true)
                                }
                            }
                        }
                    } finally {
                        if (replyGenerations[key] == generation) replyJobs.remove(key)
                    }
                }
        }

        private fun nextCommentGeneration(sort: CommentSort): Long {
            val generation = (commentGenerations[sort] ?: 0L) + 1
            commentGenerations[sort] = generation
            return generation
        }

        private fun isCommentListCurrent(
            sort: CommentSort,
            aid: Long,
            generation: Long,
        ): Boolean = _uiState.value.aid == aid && commentGenerations[sort] == generation

        private fun isReplyListCurrent(
            key: String,
            sort: CommentSort,
            aid: Long,
            generation: Long,
        ): Boolean =
            _uiState.value.aid == aid &&
                replyGenerations[key] == generation &&
                _uiState.value.commentLists.containsKey(sort)

        private fun replyKey(
            sort: CommentSort,
            rootRpid: Long,
        ): String = "${sort.name}:$rootRpid"

        private fun updateCommentList(
            sort: CommentSort,
            transform: (CommentListState) -> CommentListState,
        ) {
            _uiState.update { state ->
                val commentList = state.commentLists[sort] ?: CommentListState()
                state.copy(commentLists = state.commentLists + (sort to transform(commentList)))
            }
        }

        private fun updateReplyList(
            rootRpid: Long,
            sort: CommentSort,
            transform: (ReplyListState) -> ReplyListState,
        ) {
            updateCommentList(sort) { commentList ->
                val replyList = commentList.replyLists[rootRpid] ?: ReplyListState(rootRpid = rootRpid)
                commentList.copy(replyLists = commentList.replyLists + (rootRpid to transform(replyList)))
            }
        }

        private fun cancelComments(sort: CommentSort) {
            commentJobs.remove(sort)?.cancel()
            commentGenerations.remove(sort)
            val prefix = "${sort.name}:"
            replyJobs.keys.filter { it.startsWith(prefix) }.forEach { key ->
                replyJobs.remove(key)?.cancel()
                replyGenerations.remove(key)
            }
        }

        private fun cancelAll() {
            commentJobs.values.forEach { it.cancel() }
            commentJobs.clear()
            commentGenerations.clear()
            replyJobs.values.forEach { it.cancel() }
            replyJobs.clear()
            replyGenerations.clear()
        }

        private fun rethrowCancellation(error: Throwable) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
        }
    }

/**
 * 返回将指定评论点赞状态更新后的评论分页状态。
 *
 * 同时更新主评论与楼中楼中的同一评论，保证多份缓存数据一致。
 */
private fun CommentListState.withCommentLike(
    rpid: Long,
    liked: Boolean,
): CommentListState {
    fun transform(comment: Comment): Comment =
        if (comment.rpid == rpid) {
            comment.copy(
                isLiked = liked,
                likeCount = (comment.likeCount + if (liked) 1 else -1).coerceAtLeast(0L),
            )
        } else {
            comment
        }
    return copy(
        comments = comments.map { transform(it) },
        replyLists =
            replyLists.mapValues { (_, replyList) ->
                replyList.copy(replies = replyList.replies.map { transform(it) })
            },
    )
}
