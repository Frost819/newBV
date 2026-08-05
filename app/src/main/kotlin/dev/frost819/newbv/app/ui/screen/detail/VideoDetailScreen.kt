package dev.frost819.newbv.app.ui.screen.detail

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SuggestionChip
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.component.LoadingTip
import dev.frost819.newbv.app.ui.component.rememberScreenFocusSaver
import dev.frost819.newbv.app.ui.component.ScreenFocusSaver
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.ui.navigation.SearchRoute
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.VideoDetailRoute
import dev.frost819.newbv.app.ui.navigation.VideoPlayerRoute
import dev.frost819.newbv.app.viewmodel.detail.VideoDetailUiEffect
import dev.frost819.newbv.app.viewmodel.detail.VideoDetailViewModel
import dev.frost819.newbv.app.viewmodel.detail.VideoDetailUiState
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import dev.frost819.newbv.biliapi.entity.video.Tag
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.entity.video.season.Episode
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 视频详情页路由注册。
 *
 * 从 [VideoDetailRoute] 读取 aid，通过 Hilt 注入 [VideoDetailViewModel]。
 */
fun NavGraphBuilder.videoDetailScreen(navController: NavController) {
    composable<VideoDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VideoDetailRoute>()
        VideoDetailScreen(
            navController = navController,
            aid = route.aid,
        )
    }
}

/**
 * 视频详情页。
 *
 * 布局自上而下：
 * 1. 封面 + 标题/统计/UP/操作按钮
 * 2. 视频简介
 * 3. 分 P 列表
 * 4. 合集列表
 * 5. 相关视频
 */
@Composable
private fun VideoDetailScreen(
    navController: NavController,
    aid: Long,
) {
    val viewModel: VideoDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is VideoDetailUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is VideoDetailUiEffect.NavigateToSeason -> {
                    // TODO: Navigate to PGC season detail page
                }
            }
        }
    }

    when {
        state.loading && state.detail == null -> LoadingScreen()
        state.error && state.detail == null -> {
            ErrorScreen(
                message = state.errorTip,
                onRetry = { viewModel.loadVideoDetail() },
            )
        }
        state.detail != null -> {
            VideoDetailContent(
                detail = state.detail!!,
                state = state,
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingTip()
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message.ifBlank { "加载失败" },
                color = Color.White,
            )
            Surface(
                modifier = Modifier.focusRequester(focusRequester),
                onClick = onRetry,
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.border,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    text = "重试",
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun VideoDetailContent(
    detail: VideoDetail,
    state: VideoDetailUiState,
    viewModel: VideoDetailViewModel,
    navController: NavController,
) {
    val scrollState = rememberScrollState()
    val focusSaver = rememberScreenFocusSaver()
    focusSaver.RestoreFocus()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VideoInfoHeader(
            detail = detail,
            isLiked = state.isLiked,
            isCoined = state.isCoined,
            isFavorite = state.isFavorite,
            isFollowing = state.isFollowing,
            onToggleLike = { viewModel.toggleLike(!state.isLiked) },
            onSendCoin = { viewModel.sendCoin() },
            onOneClickTriple = { viewModel.oneClickTripleAction() },
            onToggleFavorite = { viewModel.toggleFavorite() },
            onToggleFollow = { viewModel.toggleFollow() },
            onClickTag = { tag ->
                navController.navigate(SearchRoute)
            },
            onPlayVideo = {
                viewModel.updateVideoList(detail.aid, detail.cid, detail.title)
                navController.navigate(
                    VideoPlayerRoute(
                        aid = detail.aid,
                        cid = detail.cid,
                        title = detail.title,
                        cover = detail.cover,
                    ),
                ) {
                    popUpTo<VideoPlayerRoute> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onClickUp = {
                navController.navigate(UserSpaceRoute(mid = detail.author.mid))
            },
            focusSaver = focusSaver,
        )

        if (detail.description.isNotBlank()) {
            VideoDescription(
                description = detail.description,
                focusSaver = focusSaver,
            )
        }

        if (detail.pages.size > 1) {
            VideoPartRow(
                pages = detail.pages,
                currentCid = detail.cid,
                onClick = { page ->
                    viewModel.updateVideoList(detail.aid, page.cid, detail.title)
                    navController.navigate(
                        VideoPlayerRoute(
                            aid = detail.aid,
                            cid = page.cid,
                            title = detail.title,
                            cover = detail.cover,
                        ),
                    ) {
                        popUpTo<VideoPlayerRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                focusSaver = focusSaver,
            )
        }

        detail.ugcSeason?.let { season ->
            season.sections.forEachIndexed { sectionIndex, section ->
                VideoUgcSeasonRow(
                    title = if (season.sections.size == 1) season.title else section.title,
                    episodes = section.episodes,
                    onClick = { episode ->
                        viewModel.updateVideoList(sectionIndex)
                        navController.navigate(
                            VideoPlayerRoute(
                                aid = episode.aid,
                                cid = episode.cid,
                                title = episode.title,
                                cover = episode.cover,
                            ),
                        ) {
                            popUpTo<VideoPlayerRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    focusSaver = focusSaver,
                )
            }
        }

        if (detail.relatedVideos.isNotEmpty()) {
            RelatedVideoRow(
                videos = detail.relatedVideos,
                onClick = { relatedVideo ->
                    val epid = relatedVideo.epid
                    if (relatedVideo.jumpToSeason && epid != null) {
                        navController.navigate(
                            PgcFeatureRoute(seasonId = epid.toLong())
                        )
                    } else {
                        navController.navigate(
                            VideoDetailRoute(aid = relatedVideo.aid)
                        )
                    }
                },
                focusSaver = focusSaver,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoInfoHeader(
    detail: VideoDetail,
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorite: Boolean,
    isFollowing: Boolean,
    onToggleLike: () -> Unit,
    onSendCoin: () -> Unit,
    onOneClickTriple: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollow: () -> Unit,
    onClickTag: (Tag) -> Unit,
    onPlayVideo: () -> Unit,
    onClickUp: () -> Unit,
    focusSaver: ScreenFocusSaver,
) {
    val coverFocusRequester = focusSaver.focusRequesterFor("cover")
    val upFocusRequester = focusSaver.focusRequesterFor("up")
    val followFocusRequester = focusSaver.focusRequesterFor("follow")
    val likeFocusRequester = focusSaver.focusRequesterFor("like")
    val coinFocusRequester = focusSaver.focusRequesterFor("coin")
    val favoriteFocusRequester = focusSaver.focusRequesterFor("favorite")
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (focusSaver.let { it.savedKeyValue() }.isEmpty()) {
            runCatching { coverFocusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp, vertical = 16.dp),
    ) {
        Card(
            modifier = Modifier
                .focusRequester(coverFocusRequester)
                .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("cover") }
                .weight(3f)
                .fillMaxHeight()
                .aspectRatio(1.6f),
            onClick = onPlayVideo,
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.border,
                    ),
                    shape = MaterialTheme.shapes.large,
                ),
            ),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = detail.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier
                .weight(7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val stat = detail.stat
                    StatText("播放 ${stat.view.toWanString()}")
                    StatSeparator()
                    StatText("弹幕 ${stat.danmaku.toWanString()}")
                    StatSeparator()
                    StatText("点赞 ${stat.like.toWanString()}")
                    StatSeparator()
                    StatText("投币 ${stat.coin.toWanString()}")
                    StatSeparator()
                    StatText("收藏 ${stat.favorite.toWanString()}")
                    StatSeparator()
                    StatText(dateFormat.format(detail.publishDate))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = onClickUp,
                        modifier = Modifier
                            .focusRequester(upFocusRequester)
                            .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("up") },
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
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AsyncImage(
                                model = detail.author.face,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = detail.author.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                    ActionButton(
                        text = if (isFollowing) "已关注" else "关注",
                        icon = if (isFollowing) Icons.Rounded.PersonAdd else Icons.Outlined.PersonAdd,
                        highlighted = isFollowing,
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .focusRequester(followFocusRequester)
                            .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("follow") },
                    )
                }
            }

            if (detail.tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(detail.tags) { tag ->
                        val tagKey = "tag_${tag.id}"
                        SuggestionChip(
                            onClick = { onClickTag(tag) },
                            modifier = Modifier
                                .focusRequester(focusSaver.focusRequesterFor(tagKey))
                                .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey(tagKey) },
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton(
                    text = "点赞",
                    icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                    highlighted = isLiked,
                    onClick = onToggleLike,
                    onLongClick = onOneClickTriple,
                    modifier = Modifier
                        .focusRequester(likeFocusRequester)
                        .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("like") },
                )
                ActionButton(
                    text = "投币",
                    icon = if (isCoined) Icons.Rounded.Paid else Icons.Outlined.Paid,
                    highlighted = isCoined,
                    onClick = onSendCoin,
                    modifier = Modifier
                        .focusRequester(coinFocusRequester)
                        .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("coin") },
                )
                ActionButton(
                    text = "收藏",
                    icon = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                    highlighted = isFavorite,
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .focusRequester(favoriteFocusRequester)
                        .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("favorite") },
                )
            }
        }
    }
}

@Composable
private fun StatText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.7f),
    )
}

@Composable
private fun StatSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.4f),
    )
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(
                if (highlighted) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.border,
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp),
                tint = if (highlighted) MaterialTheme.colorScheme.border else Color.White,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun VideoDescription(
    description: String,
    focusSaver: ScreenFocusSaver,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = focusSaver.focusRequesterFor("description")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("description") },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.border,
                ),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        onClick = { expanded = !expanded },
    ) {
        Text(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VideoPartRow(
    pages: List<VideoPage>,
    currentCid: Long,
    onClick: (VideoPage) -> Unit,
    focusSaver: ScreenFocusSaver,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "分P",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 50.dp),
        )
        val focusRequester = focusSaver.focusRequesterFor("parts")
        LazyRow(
            modifier = Modifier
                .focusRestorer(focusRequester)
                .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("parts") }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 50.dp),
        ) {
            items(pages) { page ->
                PartChip(
                    page = page,
                    isCurrent = page.cid == currentCid,
                    onClick = { onClick(page) },
                    modifier = if (page == pages.first()) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun PartChip(
    page: VideoPage,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(
                if (isCurrent) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.border,
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
            contentColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "P${page.index}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.border
                } else {
                    Color.White.copy(alpha = 0.6f)
                },
            )
            Text(
                text = page.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VideoUgcSeasonRow(
    title: String,
    episodes: List<Episode>,
    onClick: (Episode) -> Unit,
    focusSaver: ScreenFocusSaver,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 50.dp),
        )
        val focusRequester = focusSaver.focusRequesterFor("seasons")
        LazyRow(
            modifier = Modifier
                .focusRestorer(focusRequester)
                .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("seasons") }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 50.dp),
        ) {
            items(episodes) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = { onClick(episode) },
                    modifier = if (episode == episodes.first()) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(200.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            onClick = onClick,
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.border,
                    ),
                    shape = MaterialTheme.shapes.large,
                ),
            ),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = episode.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = episode.title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RelatedVideoRow(
    videos: List<RelatedVideo>,
    onClick: (RelatedVideo) -> Unit,
    focusSaver: ScreenFocusSaver,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "相关视频",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 50.dp),
        )
        val focusRequester = focusSaver.focusRequesterFor("related")
        LazyRow(
            modifier = Modifier
                .focusRestorer(focusRequester)
                .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("related") }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 50.dp),
        ) {
            items(videos) { video ->
                RelatedVideoCard(
                    video = video,
                    onClick = { onClick(video) },
                    modifier = if (video == videos.first()) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun RelatedVideoCard(
    video: RelatedVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(200.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            onClick = onClick,
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.border,
                    ),
                    shape = MaterialTheme.shapes.large,
                ),
            ),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = video.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
