package dev.frost819.newbv.app.ui.component.comment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.util.ToastUtils
import dev.frost819.newbv.app.viewmodel.comment.CommentListState
import dev.frost819.newbv.app.viewmodel.comment.CommentSort
import dev.frost819.newbv.app.viewmodel.comment.CommentUiEffect
import dev.frost819.newbv.app.viewmodel.comment.CommentUiState
import dev.frost819.newbv.app.viewmodel.comment.CommentViewModel
import dev.frost819.newbv.app.viewmodel.comment.ReplyListState
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.core.focus.isDpadDown
import dev.frost819.newbv.core.focus.isDpadLeft
import dev.frost819.newbv.core.focus.isDpadRight
import dev.frost819.newbv.core.focus.isDpadUp
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** 楼中楼回复相对根评论的左侧缩进。 */
private val REPLY_INDENT = 40.dp

/** 评论弹窗展示模式。 */
enum class CommentDialogMode {
    /** 详情页展示，使用更宽的弹窗。 */
    Detail,

    /** 播放器展示，使用右侧窄弹窗。 */
    Player,
}

/**
 * 评论弹窗 LazyColumn 的扁平化行模型。
 *
 * 主评论、楼中楼回复与各自分页 footer 使用稳定 key，便于分页追加时维持焦点与滚动位置。
 */
private sealed interface CommentRow {
    /** 稳定且全局唯一的 LazyColumn key。 */
    val key: String

    /** 主评论行。 */
    data class MainComment(
        val comment: Comment,
    ) : CommentRow {
        override val key: String get() = "comment:${comment.rpid}"
    }

    /** 楼中楼回复行。 */
    data class ReplyComment(
        val rootRpid: Long,
        val comment: Comment,
    ) : CommentRow {
        override val key: String get() = "reply:$rootRpid:${comment.rpid}"
    }

    /** 某个根评论的楼中楼分页 footer。 */
    data class ReplyFooter(
        val rootRpid: Long,
    ) : CommentRow {
        override val key: String get() = "reply-footer:$rootRpid"
    }

    /** 主评论分页 footer。 */
    data object MainFooter : CommentRow {
        override val key: String get() = "comment-footer"
    }
}

/**
 * 将当前排序的评论分页状态扁平化为 LazyColumn 行。
 *
 * 楼中楼展开时，在根评论后依次追加回复行与分页 footer，从而支持滚动到 footer 时
 * 按需加载下一页，而无需把全部回复塞进单个 Lazy item。
 *
 * @param commentList 当前排序的评论分页状态
 */
private fun buildCommentRows(commentList: CommentListState): List<CommentRow> =
    buildList {
        commentList.comments.forEach { root ->
            add(CommentRow.MainComment(root))
            val replyList = commentList.replyList(root.rpid)
            if (replyList?.expanded == true) {
                replyList.replies.forEach { reply ->
                    add(CommentRow.ReplyComment(rootRpid = root.rpid, comment = reply))
                }
                val showFooter =
                    replyList.initialLoading ||
                        replyList.initialError ||
                        replyList.loadingMore ||
                        replyList.loadMoreError ||
                        replyList.hasMore
                if (showFooter) add(CommentRow.ReplyFooter(root.rpid))
            }
        }
        add(CommentRow.MainFooter)
    }

/**
 * 视频评论弹窗。
 *
 * 详情页和播放器共用该组件，不创建独立评论路由。主评论热门/最新各自缓存，楼中楼
 * 默认收起，点击回复数量后在对应根评论下方按需分页加载并展开。
 *
 * @param aid 视频 AV 号
 * @param mode 弹窗展示模式
 * @param viewModel 评论状态 ViewModel
 * @param onDismiss 关闭回调
 */
@Composable
fun CommentsDialog(
    aid: Long,
    mode: CommentDialogMode,
    viewModel: CommentViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var imageViewerPictures by remember { mutableStateOf<List<String>?>(null) }
    var imageViewerIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(aid) {
        viewModel.load(aid)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CommentUiEffect.ShowToast -> ToastUtils.show(context, effect.message)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { focusRequester.requestFocus() }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f)),
        ) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(if (mode == CommentDialogMode.Player) 520.dp else 680.dp)
                        .focusRequester(focusRequester),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                colors =
                    SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                CommentsContent(
                    state = state,
                    mode = mode,
                    onRefresh = viewModel::refresh,
                    onSortChange = viewModel::changeSort,
                    onLoadMoreComments = viewModel::loadMoreComments,
                    onToggleReplies = viewModel::toggleReplies,
                    onRetryReplies = viewModel::retryReplies,
                    onLoadMoreReplies = viewModel::loadMoreReplies,
                    onToggleLike = viewModel::toggleLike,
                    onImageClick = { pictures, index ->
                        imageViewerPictures = pictures
                        imageViewerIndex = index
                    },
                )
            }

            imageViewerPictures?.let { pictures ->
                CommentImageOverlay(
                    pictures = pictures,
                    currentIndex = imageViewerIndex,
                    onDismiss = { imageViewerPictures = null },
                    onIndexChange = { imageViewerIndex = it },
                )
            }
        }
    }
}

@Composable
private fun CommentsContent(
    state: CommentUiState,
    mode: CommentDialogMode,
    onRefresh: () -> Unit,
    onSortChange: (CommentSort) -> Unit,
    onLoadMoreComments: () -> Unit,
    onToggleReplies: (Long) -> Unit,
    onRetryReplies: (Long) -> Unit,
    onLoadMoreReplies: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    // 热门与最新各自维护滚动状态：共用一个 LazyListState 时，Compose 会按首个可见 item 的
    // key 在新列表中重新定位，导致切换排序后上一条排序的评论被锚定在顶部
    val hotListState = rememberLazyListState()
    val latestListState = rememberLazyListState()
    val listState = if (state.sort == CommentSort.Hot) hotListState else latestListState
    val rows = remember(state.commentList) { buildCommentRows(state.commentList) }
    val rowsByKey = remember(rows) { rows.associateBy { it.key } }

    // 评论文本展开状态：纯 UI 态，按 rpid 记录，切换排序/分页重排时保持
    val expandedCommentIds = remember { mutableStateMapOf<Long, Boolean>() }

    // 主评论滚动接近末尾时自动加载下一页
    LaunchedEffect(listState, state.hasMore, state.loadingMore, state.loadMoreError, rows.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        }.distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                if (state.hasMore &&
                    !state.loadingMore &&
                    !state.loadMoreError &&
                    lastVisibleIndex >= rows.size - 3
                ) {
                    onLoadMoreComments()
                }
            }
    }

    // 楼中楼分页 footer 进入可见区时自动加载下一页
    LaunchedEffect(listState, rowsByKey) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { info -> info.key } }
            .distinctUntilChanged()
            .collect { visibleKeys ->
                visibleKeys.forEach { key ->
                    val row = rowsByKey[key]
                    if (row is CommentRow.ReplyFooter) {
                        val replyList = state.commentList.replyList(row.rootRpid)
                        // 错误态停止自动加载，改由 footer 的重试按钮触发
                        if (replyList != null && !replyList.initialError && !replyList.loadMoreError) {
                            onLoadMoreReplies(row.rootRpid)
                        }
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "评论${state.total.takeIf { it > 0 }?.let { " ($it)" } ?: ""}",
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortButton(CommentSort.Hot, state.sort, onSortChange)
            SortButton(CommentSort.Latest, state.sort, onSortChange)
        }

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("评论加载中…")
                }
            }
            state.error && state.comments.isEmpty() -> {
                val retryFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    runCatching { retryFocusRequester.requestFocus() }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("评论加载失败")
                        Spacer(Modifier.height(12.dp))
                        DialogActionButton(
                            text = "重试",
                            icon = Icons.Outlined.Refresh,
                            onClick = onRefresh,
                            focusRequester = retryFocusRequester,
                        )
                    }
                }
            }
            state.comments.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无评论")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is CommentRow.MainComment -> {
                                val replyList = state.commentList.replyList(row.comment.rpid)
                                CommentCard(
                                    comment = row.comment,
                                    compact = mode == CommentDialogMode.Player,
                                    isLiking = row.comment.rpid in state.likingIds,
                                    showReplyButton = row.comment.replyCount > 0,
                                    isExpanded = replyList?.expanded == true,
                                    isLoadingReplies = replyList?.initialLoading == true,
                                    isTextExpanded = expandedCommentIds[row.comment.rpid] == true,
                                    onToggleTextExpand = {
                                        expandedCommentIds[row.comment.rpid] =
                                            expandedCommentIds[row.comment.rpid] != true
                                    },
                                    onToggleReplies = { onToggleReplies(row.comment.rpid) },
                                    onToggleLike = { onToggleLike(row.comment.rpid) },
                                    onImageClick = onImageClick,
                                )
                            }
                            is CommentRow.ReplyComment -> {
                                CommentCard(
                                    comment = row.comment,
                                    compact = true,
                                    isLiking = row.comment.rpid in state.likingIds,
                                    showReplyButton = false,
                                    isExpanded = false,
                                    isLoadingReplies = false,
                                    isTextExpanded = expandedCommentIds[row.comment.rpid] == true,
                                    onToggleTextExpand = {
                                        expandedCommentIds[row.comment.rpid] =
                                            expandedCommentIds[row.comment.rpid] != true
                                    },
                                    indent = REPLY_INDENT,
                                    onToggleReplies = {},
                                    onToggleLike = { onToggleLike(row.comment.rpid) },
                                    onImageClick = onImageClick,
                                )
                            }
                            is CommentRow.ReplyFooter -> {
                                state.commentList.replyList(row.rootRpid)?.let { replyList ->
                                    ReplyFooterRow(
                                        replyList = replyList,
                                        onRetry = { onRetryReplies(row.rootRpid) },
                                        onLoadMore = { onLoadMoreReplies(row.rootRpid) },
                                    )
                                }
                            }
                            CommentRow.MainFooter -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ListFooterTip(
                                        isLoading = state.loadingMore,
                                        isError = state.loadMoreError,
                                        hasMore = state.hasMore,
                                        itemsIsEmpty = state.comments.isEmpty(),
                                    )
                                    if (state.loadMoreError) {
                                        Spacer(Modifier.height(8.dp))
                                        DialogActionButton(
                                            text = "重试",
                                            icon = Icons.Outlined.Refresh,
                                            onClick = onLoadMoreComments,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单条评论卡片。
 *
 * 主评论与楼中楼回复共用；[showReplyButton] 为 false 时（回复行）不展示回复展开按钮。
 * 评论文本超出最大行数时可展开全文，仅在实际溢出时展示"展开/收起"按钮。
 */
@Composable
internal fun CommentCard(
    comment: Comment,
    compact: Boolean,
    isLiking: Boolean,
    showReplyButton: Boolean,
    isExpanded: Boolean,
    isLoadingReplies: Boolean,
    isTextExpanded: Boolean,
    onToggleTextExpand: () -> Unit,
    onToggleReplies: () -> Unit,
    onToggleLike: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    indent: Dp = 0.dp,
) {
    // 记录文本在折叠状态下是否溢出；溢出过一次后按钮常驻，避免展开后回落到 false 导致按钮消失
    var isTextOverflowing by remember(comment.rpid) { mutableStateOf(false) }
    val textMaxLines =
        when {
            isTextExpanded -> Int.MAX_VALUE
            compact -> 4
            else -> 8
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = indent)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = comment.avatar,
                contentDescription = "${comment.userName}头像",
                modifier = Modifier.size(if (compact) 32.dp else 40.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        buildString {
                            append(comment.userName)
                            if (comment.level > 0) append("  Lv.${comment.level}")
                            if (comment.isUp) append("  UP主")
                        },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = comment.message,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = textMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result -> if (result.hasVisualOverflow) isTextOverflowing = true },
                    modifier = Modifier.padding(top = 4.dp).animateContentSize(),
                )
                if (comment.pictures.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        comment.pictures.forEachIndexed { index, url ->
                            val picFocusRequester = remember { FocusRequester() }
                            Surface(
                                modifier =
                                    Modifier
                                        .size(if (compact) 60.dp else 80.dp)
                                        .focusRequester(picFocusRequester)
                                        .touchClickable(onClick = { onImageClick(comment.pictures, index) }),
                                onClick = { onImageClick(comment.pictures, index) },
                                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                                border =
                                    ClickableSurfaceDefaults.border(
                                        focusedBorder =
                                            Border(
                                                border =
                                                    androidx.compose.foundation.BorderStroke(
                                                        2.dp,
                                                        MaterialTheme.colorScheme.border,
                                                    ),
                                                shape = MaterialTheme.shapes.small,
                                            ),
                                    ),
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "评论图片 ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogActionButton(
                        text = "赞 ${comment.likeCount}",
                        icon = if (comment.isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                        onClick = onToggleLike,
                        enabled = !isLiking,
                    )
                    if (showReplyButton) {
                        DialogActionButton(
                            text = if (isLoadingReplies) "加载中" else "回复 ${comment.replyCount}",
                            icon = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            onClick = onToggleReplies,
                            enabled = !isLoadingReplies,
                        )
                    }
                    if (isTextOverflowing) {
                        DialogActionButton(
                            text = if (isTextExpanded) "收起" else "展开",
                            icon = if (isTextExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            onClick = onToggleTextExpand,
                        )
                    }
                }
            }
        }
    }
}

/** 楼中楼分页 footer：展示加载中、加载失败重试或加载更多。 */
@Composable
private fun ReplyFooterRow(
    replyList: ReplyListState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = REPLY_INDENT + 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            replyList.initialError -> {
                Text(
                    text = "回复加载失败",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                DialogActionButton(text = "重试", icon = Icons.Outlined.Refresh, onClick = onRetry)
            }
            replyList.loadMoreError -> {
                Text(
                    text = "加载失败",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                DialogActionButton(text = "重试", icon = Icons.Outlined.Refresh, onClick = onLoadMore)
            }
            replyList.initialLoading || replyList.loadingMore -> {
                Text(text = "回复加载中…", style = MaterialTheme.typography.labelMedium)
            }
            else -> {
                DialogActionButton(text = "加载更多回复", icon = Icons.Outlined.ExpandMore, onClick = onLoadMore)
            }
        }
    }
}

@Composable
private fun SortButton(
    sort: CommentSort,
    selected: CommentSort,
    onClick: (CommentSort) -> Unit,
) {
    Surface(
        modifier = Modifier.touchClickable(onClick = { onClick(sort) }),
        onClick = { onClick(sort) },
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor =
                    if (sort ==
                        selected
                    ) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (sort ==
                        selected
                    ) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder =
                    Border(
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.border),
                        shape = MaterialTheme.shapes.small,
                    ),
            ),
    ) {
        Text(sort.displayName, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun DialogActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    Surface(
        modifier =
            Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * 图片查看覆盖层。
 *
 * 使用独立 Dialog 创建独立焦点窗口，D-Pad 左右翻页，上下键被消费以避免焦点逃逸，
 * 关闭后回到评论列表的缩略图焦点。
 *
 * @param pictures 图片 URL 列表
 * @param currentIndex 当前显示的图片索引
 * @param onDismiss 关闭回调
 * @param onIndexChange 图片切换回调
 */
@Composable
private fun CommentImageOverlay(
    pictures: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onIndexChange: (Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                        when {
                            event.isDpadLeft() && currentIndex > 0 -> {
                                onIndexChange(currentIndex - 1)
                                true
                            }
                            event.isDpadRight() && currentIndex < pictures.lastIndex -> {
                                onIndexChange(currentIndex + 1)
                                true
                            }
                            event.isDpadUp() || event.isDpadDown() -> true
                            else -> false
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = pictures[currentIndex],
                contentDescription = "评论图片 ${currentIndex + 1}/${pictures.size}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (pictures.size > 1) {
                Text(
                    text = "${currentIndex + 1} / ${pictures.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// region Previews

private fun fakeComment(
    rpid: Long = 1L,
    userName: String = "用户名",
    message: String = "这是一条评论内容",
    likeCount: Long = 42,
    replyCount: Int = 3,
    isLiked: Boolean = false,
    isUp: Boolean = false,
    level: Int = 5,
): Comment =
    Comment(
        rpid = rpid,
        oid = 1L,
        type = 1,
        mid = 100L,
        rootRpid = 0L,
        parentRpid = 0L,
        userName = userName,
        avatar = "",
        level = level,
        message = message,
        pictures = emptyList(),
        ctime = System.currentTimeMillis() / 1000,
        likeCount = likeCount,
        replyCount = replyCount,
        isLiked = isLiked,
        isUp = isUp,
    )

@Preview(device = "id:tv_1080p", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CommentsContentPreview() {
    BVTheme {
        val comments =
            listOf(
                fakeComment(
                    rpid = 1,
                    userName = "测试用户A",
                    message = "这个视频做得太好了，学到了很多！",
                    likeCount = 128,
                    replyCount = 5,
                    level = 6,
                ),
                fakeComment(
                    rpid = 2,
                    userName = "UP主本人",
                    message = "感谢大家的支持！下期视频已经在做了。",
                    likeCount = 56,
                    replyCount = 12,
                    isUp = true,
                    level = 6,
                ),
                fakeComment(
                    rpid = 3,
                    userName = "路人乙",
                    message = "沙发沙发，第一次这么靠前",
                    likeCount = 3,
                    replyCount = 0,
                    level = 2,
                ),
            )
        CommentsContent(
            state =
                CommentUiState(
                    aid = 1L,
                    commentLists =
                        mapOf(
                            CommentSort.Hot to
                                dev.frost819.newbv.app.viewmodel.comment.CommentListState(
                                    comments = comments,
                                    page = 1,
                                    total = 328,
                                    hasMore = false,
                                    loaded = true,
                                ),
                        ),
                    sort = CommentSort.Hot,
                ),
            mode = CommentDialogMode.Player,
            onRefresh = {},
            onSortChange = {},
            onLoadMoreComments = {},
            onToggleReplies = {},
            onRetryReplies = {},
            onLoadMoreReplies = {},
            onToggleLike = {},
            onImageClick = { _, _ -> },
        )
    }
}

// endregion
