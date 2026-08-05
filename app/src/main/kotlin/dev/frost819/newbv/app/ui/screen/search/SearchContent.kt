package dev.frost819.newbv.app.ui.screen.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchResultViewModel

/**
 * 搜索内容区（嵌入 MainScreen）。
 *
 * 管理"输入"和"结果"两个子页面的切换：
 * - 输入页：软键盘 + 热词/建议 + 搜索历史
 * - 结果页：4 Tab 网格 + 筛选弹窗 + 无限滚动
 *
 * @param navFocusRequester 从 MainScreen 传入的焦点请求器
 * @param navController 导航控制器（跳转详情页/UP 主页）
 */
@Composable
fun SearchContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    navController: NavController,
    searchInputViewModel: SearchInputViewModel = hiltViewModel(),
    searchResultViewModel: SearchResultViewModel = hiltViewModel(),
) {
    var searchMode by rememberSaveable { mutableStateOf(SearchMode.Input) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }

    AnimatedContent(
        targetState = searchMode,
        label = "search-mode",
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        modifier = modifier,
    ) { mode ->
        when (mode) {
            SearchMode.Input -> {
                SearchInputContent(
                    viewModel = searchInputViewModel,
                    focusRequester = navFocusRequester,
                    onSearch = { keyword ->
                        searchKeyword = keyword
                        searchInputViewModel.commitSearch(keyword)
                        searchResultViewModel.search(keyword)
                        searchMode = SearchMode.Result
                    },
                )
            }
            SearchMode.Result -> {
                SearchResultContent(
                    viewModel = searchResultViewModel,
                    keyword = searchKeyword,
                    navController = navController,
                    onBack = {
                        searchMode = SearchMode.Input
                    },
                )
            }
        }
    }
}

private enum class SearchMode {
    Input,
    Result,
}
