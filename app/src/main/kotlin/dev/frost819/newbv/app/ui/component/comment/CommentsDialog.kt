package dev.frost819.newbv.app.ui.component.comment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import dev.frost819.newbv.app.viewmodel.comment.CommentSort
import dev.frost819.newbv.app.viewmodel.comment.CommentUiState
import dev.frost819.newbv.app.viewmodel.comment.CommentViewModel
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.core.focus.isDpadDown
import dev.frost819.newbv.core.focus.isDpadLeft
import dev.frost819.newbv.core.focus.isDpadRight
import dev.frost819.newbv.core.focus.isDpadUp
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** 评论弹窗展示模式。 */
enum class CommentDialogMode {
    /** 详情页展示，使用更宽的弹窗。 */
    Detail,

    /** 播放器展示，使用右侧窄弹窗。 */
    Player,
}

/**
 * 视频评论弹窗。
 *
 * 详情页和播放器共用该组件，不创建独立评论路由。楼中楼默认收起，点击回复数量后
 * 在对应根评论下方懒加载并展开。
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
    var imageViewerPictures by remember { mutableStateOf<List<String>?>(null) }
    var imageViewerIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(aid) {
        viewModel.load(aid)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(50)
            runCatching { focusRequester.requestFocus() }
        }

        val currentPictures = imageViewerPictures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .onPreviewKeyEvent { event ->
                    if (currentPictures == null) return@onPreviewKeyEvent false
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when {
                        event.isDpadLeft() && imageViewerIndex > 0 -> {
                            imageViewerIndex--
                            true
                        }
                        event.isDpadRight() && imageViewerIndex < currentPictures.lastIndex -> {
                            imageViewerIndex++
                            true
                        }
                        event.isDpadUp() || event.isDpadDown() -> true
                        else -> false
                    }
                },
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(if (mode == CommentDialogMode.Player) 520.dp else 680.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                CommentsContent(
                    state = state,
                    mode = mode,
                    onDismiss = onDismiss,
                    onRefresh = viewModel::refresh,
                    onSortChange = viewModel::changeSort,
                    onLoadMore = viewModel::loadMore,
                    onToggleReplies = viewModel::toggleReplies,
                    onToggleLike = viewModel::toggleLike,
                    onImageClick = { pictures, index ->
                        imageViewerPictures = pictures
                        imageViewerIndex = index
                    },
                )
            }

            currentPictures?.let { pictures ->
                CommentImageOverlay(
                    pictures = pictures,
                    currentIndex = imageViewerIndex,
                    onDismiss = { imageViewerPictures = null },
                )
            }
        }
    }
}

@Composable
private fun CommentsContent(
    state: CommentUiState,
    mode: CommentDialogMode,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSortChange: (CommentSort) -> Unit,
    onLoadMore: () -> Unit,
    onToggleReplies: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.comments.size, state.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                if (state.hasMore && lastVisibleIndex >= state.comments.size - 3) onLoadMore()
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("评论加载失败")
                        Spacer(Modifier.height(12.dp))
                        DialogActionButton("重试", Icons.Outlined.Refresh, onRefresh)
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
                    items(state.comments, key = { it.rpid }) { comment ->
                        CommentItem(
                            comment = comment,
                            compact = mode == CommentDialogMode.Player,
                            isLoadingReplies = comment.rpid in state.loadingReplyIds,
                            isLiking = comment.rpid in state.likingIds,
                            onToggleReplies = { onToggleReplies(comment.rpid) },
                            onToggleLike = { onToggleLike(comment.rpid) },
                            loadingReplyIds = state.loadingReplyIds,
                            likingIds = state.likingIds,
                            onReplyToggle = onToggleReplies,
                            onLikeToggle = onToggleLike,
                            onImageClick = onImageClick,
                        )
                    }
                    item(key = "footer") {
                        if (state.hasMore) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (state.loadingMore) "加载更多…" else "继续加载")
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("没有更多评论")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    compact: Boolean,
    isLoadingReplies: Boolean,
    isLiking: Boolean,
    onToggleReplies: () -> Unit,
    onToggleLike: () -> Unit,
    loadingReplyIds: Set<Long>,
    likingIds: Set<Long>,
    onReplyToggle: (Long) -> Unit,
    onLikeToggle: (Long) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                    text = buildString {
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
                    maxLines = if (compact) 4 else 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (comment.pictures.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        comment.pictures.forEachIndexed { index, url ->
                            val picFocusRequester = remember { FocusRequester() }
                            Surface(
                                modifier = Modifier
                                    .size(if (compact) 60.dp else 80.dp)
                                    .focusRequester(picFocusRequester)
                                    .touchClickable(onClick = { onImageClick(comment.pictures, index) }),
                                onClick = { onImageClick(comment.pictures, index) },
                                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        border = androidx.compose.foundation.BorderStroke(
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
                        text = if (isLiking) "处理中" else "赞 ${comment.likeCount}",
                        icon = if (comment.isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                        onClick = onToggleLike,
                        enabled = !isLiking,
                    )
                    if (comment.replyCount > 0) {
                        DialogActionButton(
                            text = if (isLoadingReplies) "加载中" else "回复 ${comment.replyCount}",
                            icon = if (comment.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            onClick = onToggleReplies,
                            enabled = !isLoadingReplies,
                        )
                    }
                }
            }
        }

        if (comment.isExpanded) {
            if (comment.repliesError) {
                Text(
                    text = "回复加载失败，请再次点击重试",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 50.dp, top = 8.dp),
                )
            }
            comment.replies.forEach { reply ->
                CommentItem(
                    comment = reply,
                    compact = true,
                    isLoadingReplies = reply.rpid in loadingReplyIds,
                    isLiking = reply.rpid in likingIds,
                    onToggleReplies = { onReplyToggle(reply.rpid) },
                    onToggleLike = { onLikeToggle(reply.rpid) },
                    loadingReplyIds = loadingReplyIds,
                    likingIds = likingIds,
                    onReplyToggle = onReplyToggle,
                    onLikeToggle = onLikeToggle,
                    onImageClick = onImageClick,
                )
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
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (sort == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (sort == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
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
) {
    Surface(
        modifier = Modifier.touchClickable(onClick = onClick),
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
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
 * 图片查看覆盖层（在 CommentsDialog 内部叠加，非独立 Dialog）。
 *
 * 半透明黑背景遮罩评论列表，D-Pad 左右翻页由父级 Box 的 onPreviewKeyEvent 处理，
 * 焦点始终保留在评论列表的缩略图上，关闭后无需恢复焦点。
 *
 * @param pictures 图片 URL 列表
 * @param currentIndex 当前显示的图片索引
 * @param onDismiss 关闭回调
 */
@Composable
private fun CommentImageOverlay(
    pictures: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = pictures[currentIndex],
                contentDescription = "评论图片 ${currentIndex + 1}/${pictures.size}",
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentScale = ContentScale.Fit,
            )
            if (pictures.size > 1) {
                Text(
                    text = "${currentIndex + 1} / ${pictures.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
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
): Comment = Comment(
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
        CommentsContent(
            state = CommentUiState(
                aid = 1L,
                comments = listOf(
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
                ),
                sort = CommentSort.Hot,
                total = 328,
                hasMore = false,
            ),
            mode = CommentDialogMode.Player,
            onDismiss = {},
            onRefresh = {},
            onSortChange = {},
            onLoadMore = {},
            onToggleReplies = {},
            onToggleLike = {},
            onImageClick = { _, _ -> },
        )
    }
}

@Preview(device = "id:tv_1080p", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CommentsContentLoadingPreview() {
    BVTheme {
        CommentsContent(
            state = CommentUiState(loading = true, total = 0),
            mode = CommentDialogMode.Detail,
            onDismiss = {},
            onRefresh = {},
            onSortChange = {},
            onLoadMore = {},
            onToggleReplies = {},
            onToggleLike = {},
            onImageClick = { _, _ -> },
        )
    }
}

@Preview(device = "id:tv_1080p", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CommentsContentEmptyPreview() {
    BVTheme {
        CommentsContent(
            state = CommentUiState(loading = false, total = 0, hasMore = false),
            mode = CommentDialogMode.Player,
            onDismiss = {},
            onRefresh = {},
            onSortChange = {},
            onLoadMore = {},
            onToggleReplies = {},
            onToggleLike = {},
            onImageClick = { _, _ -> },
        )
    }
}

// endregion
