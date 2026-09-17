package dev.frost819.newbv.app.ui.screen.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.videocard.SeasonCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCardData
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonStatus
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.touchClickable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 追番页面。
 *
 * 5 列网格 + 无限滚动 + 菜单键筛选弹窗。
 *
 * @param viewModel 个人页 ViewModel。
 * @param navController 导航控制器。
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 */
@Composable
fun FollowingSeasonScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
    focusSaver: FocusSaver,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    var showFilter by remember { mutableStateOf(false) }

    if (state.followingSeasons.isEmpty() && !state.followingLoading && !state.followingError) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Text(
                text = "没有追番",
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.followingSeasons.size - 30
            }.collect {
                viewModel.loadFollowingSeasons()
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                        showFilter = true
                        return@onPreviewKeyEvent true
                    }
                    false
                },
    ) {
        TvLazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "追番",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(
                        onClick = { showFilter = true },
                        modifier = Modifier.touchClickable(onClick = { showFilter = true }),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "筛选",
                        )
                    }
                }
            }

            itemsIndexed(
                items = state.followingSeasons,
                key = { _, item -> item.seasonId },
            ) { index, item ->
                val cardData =
                    remember(item) {
                        SeasonCardData(
                            seasonId = item.seasonId,
                            title = item.title,
                            cover = item.cover,
                        )
                    }
                SeasonCard(
                    data = cardData,
                    onClick = {
                        navController.navigate(PgcFeatureRoute(seasonId = item.seasonId.toLong()))
                    },
                    modifier = Modifier.focusSaverItem(focusSaver, "season_$index"),
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ListFooterTip(
                    isLoading = state.followingLoading,
                    isError = state.followingError,
                    hasMore = state.followingHasMore,
                    itemsIsEmpty = state.followingSeasons.isEmpty(),
                )
            }
        }
    }

    if (showFilter) {
        FollowingSeasonFilterDialog(
            currentType = state.followingType,
            currentStatus = state.followingStatus,
            onApply = { type, status ->
                viewModel.setFollowingFilter(type, status)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

/**
 * 追番筛选弹窗。
 *
 * @param currentType 当前类型筛选。
 * @param currentStatus 当前状态筛选。
 * @param onApply 应用筛选回调。
 * @param onDismiss 关闭回调。
 */
@Composable
private fun FollowingSeasonFilterDialog(
    currentType: FollowingSeasonType,
    currentStatus: FollowingSeasonStatus,
    onApply: (FollowingSeasonType, FollowingSeasonStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedStatus by remember { mutableStateOf(currentStatus) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Surface(
                modifier = Modifier.padding(24.dp),
                shape = androidx.tv.material3.MaterialTheme.shapes.large,
                colors =
                    androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = androidx.tv.material3.MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    androidx.tv.material3.Text(
                        text = "筛选",
                        style = androidx.tv.material3.MaterialTheme.typography.titleLarge,
                    )

                    androidx.tv.material3.Text(
                        text = "类型",
                        style = androidx.tv.material3.MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FollowingSeasonType.entries.forEach { type ->
                            FilterChip(
                                text = if (type == FollowingSeasonType.Bangumi) "番剧" else "影视",
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                            )
                        }
                    }

                    androidx.tv.material3.Text(
                        text = "状态",
                        style = androidx.tv.material3.MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FollowingSeasonStatus.entries.forEach { status ->
                            FilterChip(
                                text =
                                    when (status) {
                                        FollowingSeasonStatus.All -> "全部"
                                        FollowingSeasonStatus.Want -> "想看"
                                        FollowingSeasonStatus.Watching -> "在看"
                                        FollowingSeasonStatus.Watched -> "看过"
                                    },
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        androidx.tv.material3.Button(
                            onClick = { onApply(selectedType, selectedStatus) },
                            modifier = Modifier.touchClickable(onClick = { onApply(selectedType, selectedStatus) }),
                        ) {
                            androidx.tv.material3.Text("确定")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 筛选 Chip。
 */
@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier.touchClickable(onClick = onClick),
        shape =
            androidx.tv.material3.ClickableSurfaceDefaults.shape(
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(50),
            ),
        colors =
            focusInvertedColors(
                containerColor =
                    if (selected) {
                        androidx.tv.material3.MaterialTheme.colorScheme.primary
                    } else {
                        androidx.tv.material3.MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (selected) {
                        androidx.tv.material3.MaterialTheme.colorScheme.onPrimary
                    } else {
                        androidx.tv.material3.MaterialTheme.colorScheme.onSurface
                    },
            ),
    ) {
        androidx.tv.material3.Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
