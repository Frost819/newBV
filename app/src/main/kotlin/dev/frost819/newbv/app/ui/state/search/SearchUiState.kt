package dev.frost819.newbv.app.ui.state.search

import dev.frost819.newbv.biliapi.entity.search.Hotword
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.biliapi.repositories.SearchTypePage
import dev.frost819.newbv.biliapi.repositories.SearchTypeResult
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity

/**
 * 搜索输入页 UI 状态。
 *
 * @param keyword 当前输入的关键词
 * @param hotwords 热搜词列表
 * @param suggests 搜索建议列表（keyword 非空时显示）
 * @param histories 搜索历史列表
 * @param isLoadingHotwords 是否正在加载热搜
 * @param isLoadingSuggests 是否正在加载建议
 * @param hotwordsError 热搜加载是否失败
 */
data class SearchInputUiState(
    val keyword: String = "",
    val hotwords: List<Hotword> = emptyList(),
    val suggests: List<String> = emptyList(),
    val histories: List<SearchHistoryEntity> = emptyList(),
    val isLoadingHotwords: Boolean = false,
    val isLoadingSuggests: Boolean = false,
    val hotwordsError: Boolean = false,
)

/**
 * 搜索结果页单项数据（4 种类型统一封装）。
 */
sealed class SearchResultItem {
    data class VideoItem(val video: SearchTypeResult.Video) : SearchResultItem()
    data class PgcItem(val pgc: SearchTypeResult.Pgc) : SearchResultItem()
    data class UserItem(val user: SearchTypeResult.User) : SearchResultItem()
    data class LiveRoomItem(val room: SearchTypeResult.LiveRoom) : SearchResultItem()
}

/**
 * 单个搜索类型的结果状态。
 *
 * @param type 搜索类型
 * @param items 当前已加载的结果列表
 * @param page 分页游标
 * @param isLoading 是否正在加载
 * @param hasMore 是否还有更多
 * @param error 是否加载失败
 */
data class TypedSearchResult(
    val type: SearchType,
    val items: List<SearchResultItem> = emptyList(),
    val page: SearchTypePage = SearchTypePage(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: Boolean = false,
) {
    val count: Int get() = items.size
}

/**
 * 搜索结果页 UI 状态。
 *
 * @param keyword 搜索关键词
 * @param activeType 当前选中的搜索类型 Tab
 * @param results 4 种类型的搜索结果
 * @param selectedOrder 排序方式
 * @param selectedDuration 时长筛选
 * @param showFilter 是否显示筛选弹窗
 */
data class SearchResultUiState(
    val keyword: String = "",
    val activeType: SearchType = SearchType.Video,
    val results: Map<SearchType, TypedSearchResult> = SearchType.entries.associateWith { TypedSearchResult(it) },
    val selectedOrder: SearchFilterOrderType = SearchFilterOrderType.ComprehensiveSort,
    val selectedDuration: SearchFilterDuration = SearchFilterDuration.All,
    val showFilter: Boolean = false,
)
