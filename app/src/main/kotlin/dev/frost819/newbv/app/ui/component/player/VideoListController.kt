package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 分集列表覆盖层。
 *
 * 从左侧滑入的半透明面板，显示视频分集列表。
 * 支持嵌套 UGC 分页（有 ugcPages 的视频可展开/折叠子分集）。
 * 显示时自动滚动到当前播放项并请求焦点。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param currentCid 当前播放视频的 CID
 * @param videoList 视频列表
 * @param onPlayNewVideo 点击播放新视频回调
 */
@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    currentCid: Long,
    videoList: List<VideoListItem>,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val parentFocusRequester = remember { FocusRequester() }
    val childFocusRequester = remember { FocusRequester() }

    // 显示时自动滚动到当前集并请求焦点
    LaunchedEffect(show) {
        if (show) {
            val currentIndex = videoList.indexOfFirst { video ->
                video.cid == currentCid ||
                    video.ugcPages?.any { it.cid == currentCid } == true
            }

            if (currentIndex != -1) {
                listState.animateScrollToItem(currentIndex)

                val isChild = videoList
                    .getOrNull(currentIndex)
                    ?.ugcPages
                    ?.any { it.cid == currentCid } == true

                if (isChild) {
                    childFocusRequester.requestFocus()
                } else {
                    parentFocusRequester.requestFocus()
                }
            }
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally(),
    ) {
        Surface(
            modifier = modifier,
            colors = SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f),
            ),
        ) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 60.dp),
                ) {
                    items(
                        items = videoList,
                        key = { it.cid },
                    ) { video ->
                        VideoListItemRow(
                            video = video,
                            currentCid = currentCid,
                            parentFocusRequester = parentFocusRequester,
                            childFocusRequester = childFocusRequester,
                            onPlayNewVideo = onPlayNewVideo,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个视频列表项（支持嵌套 UGC 分页）。
 */
@Composable
private fun VideoListItemRow(
    video: VideoListItem,
    currentCid: Long,
    parentFocusRequester: FocusRequester,
    childFocusRequester: FocusRequester,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val hasSubPages = !video.ugcPages.isNullOrEmpty()
    val isParentSelected = video.cid == currentCid
    val isChildSelected = video.ugcPages?.any { it.cid == currentCid } == true

    var expanded by remember(video.cid) { mutableStateOf(isChildSelected) }

    // 当前播放的是子分页时自动展开父项
    LaunchedEffect(isChildSelected) {
        if (isChildSelected) expanded = true
    }

    Column(modifier = Modifier.animateContentSize()) {
        // 父级视频项
        val parentModifier = if (isParentSelected) {
            Modifier.focusRequester(parentFocusRequester)
        } else {
            Modifier
        }

        PlayerListItem(
            modifier = Modifier.padding(horizontal = 16.dp).then(parentModifier),
            text = video.title,
            selected = isParentSelected && !isChildSelected,
            textAlign = TextAlign.Start,
            trailingContent = if (hasSubPages) {
                {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                }
            } else {
                null
            },
            onClick = {
                if (hasSubPages) {
                    expanded = !expanded
                } else if (!isParentSelected) {
                    onPlayNewVideo(video)
                }
            },
        )

        // 子分页列表
        if (expanded && hasSubPages) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                video.ugcPages?.forEach { page ->
                    val isPageSelected = page.cid == currentCid
                    val childModifier = if (isPageSelected) {
                        Modifier.focusRequester(childFocusRequester)
                    } else {
                        Modifier
                    }

                    PlayerListItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .then(childModifier),
                        text = page.title,
                        selected = isPageSelected,
                        textAlign = TextAlign.Start,
                        onClick = {
                            if (!isPageSelected) {
                                onPlayNewVideo(video.copy(cid = page.cid))
                            }
                        },
                    )
                }
            }
        }

        // 折叠时恢复焦点到父项
        LaunchedEffect(expanded) {
            if (!expanded && isParentSelected) {
                parentFocusRequester.requestFocus()
            }
        }
    }
}

/**
 * 播放器列表项。
 *
 * 通用可聚焦列表项，支持选中状态、尾部图标和自定义对齐。
 *
 * @param modifier 修饰符
 * @param text 文本内容
 * @param selected 是否选中
 * @param textAlign 文本对齐方式
 * @param trailingContent 尾部内容（如展开/折叠图标）
 * @param onFocus 获得焦点回调
 * @param onClick 点击回调
 */
@Composable
fun PlayerListItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    trailingContent: (@Composable () -> Unit)? = null,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onFocus() },
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                Color.Transparent
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingContent?.invoke()
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun VideoListControllerPreview() {
    val sampleList = listOf(
        VideoListItem(
            aid = 1,
            cid = 101,
            title = "第一集",
            ugcPages = listOf(
                VideoPage(cid = 201, index = 1, title = "P1 上半", duration = 600, dimension = dev.frost819.newbv.biliapi.entity.video.Dimension(1920, 1080)),
                VideoPage(cid = 202, index = 2, title = "P1 下半", duration = 600, dimension = dev.frost819.newbv.biliapi.entity.video.Dimension(1920, 1080)),
            ),
        ),
        VideoListItem(aid = 2, cid = 102, title = "第二集"),
    )

    BVTheme {
        VideoListController(
            show = true,
            currentCid = 201L,
            videoList = sampleList,
            onPlayNewVideo = {},
        )
    }
}

// endregion
