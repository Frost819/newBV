package dev.frost819.newbv.app.ui.screen.search

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.search.SearchKeyword
import dev.frost819.newbv.app.ui.component.search.SoftKeyboard
import dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep

/**
 * 搜索输入页内容。
 *
 * 三列水平布局：搜索框+软键盘 | 热词/建议 | 搜索历史。
 */
@Composable
fun SearchInputContent(
    modifier: Modifier = Modifier,
    viewModel: SearchInputViewModel,
    focusRequester: FocusRequester,
    onSearch: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 列 1：搜索框 + 软键盘
        SearchInputColumn(
            focusRequester = focusRequester,
            keyword = uiState.keyword,
            onKeywordChange = { viewModel.updateKeyword(it) },
            onSearch = { onSearch(uiState.keyword) },
        )

        // 列 2：热词或建议
        if (uiState.keyword.isEmpty()) {
            SearchHotwordsColumn(
                hotwords = uiState.hotwords,
                onSearch = onSearch,
            )
        } else {
            SearchSuggestsColumn(
                suggests = uiState.suggests,
                onSearch = onSearch,
            )
        }

        // 列 3：搜索历史
        SearchHistoryColumn(
            histories = uiState.histories,
            onSearch = onSearch,
            onDelete = { viewModel.deleteHistory(it) },
            onDeleteAll = { viewModel.clearAllHistories() },
        )
    }
}

@Composable
private fun SearchInputColumn(
    focusRequester: FocusRequester,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .focusGroup(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.width(258.dp),
                value = keyword,
                onValueChange = onKeywordChange,
                maxLines = 1,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.inverseSurface,
                    cursorColor = MaterialTheme.colorScheme.inverseSurface,
                ),
                placeholder = { Text("搜索") },
            )
            SoftKeyboard(
                firstButtonFocusRequester = focusRequester,
                onClick = { onKeywordChange(keyword + it) },
                onClear = { onKeywordChange("") },
                onDelete = {
                    if (keyword.isNotEmpty()) {
                        onKeywordChange(keyword.dropLast(1))
                    }
                },
                onSearch = onSearch,
            )
        }
    }
}

@Composable
private fun SearchHotwordsColumn(
    hotwords: List<dev.frost819.newbv.biliapi.entity.search.Hotword>,
    onSearch: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .focusGroup(),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = "热搜",
            style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(hotwords) { hotword ->
                SearchKeyword(
                    keyword = hotword.showName,
                    onClick = { onSearch(hotword.showName) },
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestsColumn(
    suggests: List<String>,
    onSearch: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .focusGroup(),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = "搜索建议",
            style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(suggests) { suggest ->
                SearchKeyword(
                    keyword = suggest,
                    onClick = { onSearch(suggest) },
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryColumn(
    histories: List<dev.frost819.newbv.data.db.entity.SearchHistoryEntity>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    var deleteMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .focusGroup(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = "搜索历史",
                style = MaterialTheme.typography.titleLarge,
            )
            Row {
                if (deleteMode && histories.isNotEmpty()) {
                    IconButton(
                        onClick = onDeleteAll,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "清空全部",
                        )
                    }
                }
                IconButton(
                    onClick = { deleteMode = !deleteMode },
                ) {
                    if (deleteMode) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "退出删除模式",
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "进入删除模式",
                        )
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(histories) { history ->
                SearchKeyword(
                    keyword = history.keyword,
                    onClick = {
                        if (deleteMode) {
                            onDelete(history.keyword)
                        } else {
                            onSearch(history.keyword)
                        }
                    },
                    trailingIcon = if (deleteMode) {
                        { Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        ) }
                    } else null,
                )
            }
        }
    }
}
