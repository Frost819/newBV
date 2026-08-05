package dev.frost819.newbv.app.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.state.search.SearchInputUiState
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.repository.SearchHistoryRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/**
 * 搜索输入页 ViewModel。
 *
 * 管理搜索关键词输入、热搜词加载、搜索建议、搜索历史增删。
 *
 * @param searchRepository 搜索数据仓库
 * @param searchHistoryRepository 搜索历史持久化仓库
 */
@HiltViewModel
class SearchInputViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger { }

    companion object {
        private const val LOAD_TIMEOUT_MS = 10_000L
    }

    private val _uiState = MutableStateFlow(SearchInputUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHotwords()
        loadHistories()
    }

    /**
     * 更新搜索关键词。
     *
     * keyword 非空时自动触发搜索建议加载。
     */
    fun updateKeyword(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        if (keyword.isNotEmpty()) {
            loadSuggests(keyword)
        } else {
            _uiState.update { it.copy(suggests = emptyList()) }
        }
    }

    /**
     * 提交搜索：记录历史。
     *
     * @param keyword 搜索关键词
     */
    fun commitSearch(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.addHistory(keyword)
            loadHistories()
        }
    }

    /** 删除指定搜索历史。 */
    fun deleteHistory(keyword: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteHistory(keyword)
            loadHistories()
        }
    }

    /** 清空全部搜索历史。 */
    fun clearAllHistories() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
            loadHistories()
        }
    }

    /** 刷新热搜词。 */
    fun refreshHotwords() {
        _uiState.update { it.copy(hotwordsError = false) }
        loadHotwords()
    }

    private fun loadHotwords() {
        if (_uiState.value.isLoadingHotwords) return
        _uiState.update { it.copy(isLoadingHotwords = true, hotwordsError = false) }

        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    searchRepository.getSearchHotwords(
                        limit = 50,
                        preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                    )
                }
            }.onSuccess { hotwords ->
                _uiState.update {
                    it.copy(hotwords = hotwords, isLoadingHotwords = false, hotwordsError = false)
                }
                logger.info { "Loaded hotwords: ${hotwords.size}" }
            }.onFailure { e ->
                if (e is CancellationException && e !is TimeoutCancellationException) {
                    throw e
                }
                logger.warn { "Failed to load hotwords: $e" }
                _uiState.update { it.copy(isLoadingHotwords = false, hotwordsError = true) }
            }
        }
    }

    private fun loadSuggests(keyword: String) {
        _uiState.update { it.copy(isLoadingSuggests = true) }

        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    searchRepository.getSearchSuggest(
                        keyword = keyword,
                        preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                    )
                }
            }.onSuccess { suggests ->
                _uiState.update { it.copy(suggests = suggests, isLoadingSuggests = false) }
            }.onFailure { e ->
                if (e is CancellationException && e !is TimeoutCancellationException) {
                    throw e
                }
                logger.warn { "Failed to load suggests: $e" }
                _uiState.update { it.copy(suggests = emptyList(), isLoadingSuggests = false) }
            }
        }
    }

    private fun loadHistories() {
        viewModelScope.launch {
            runCatching {
                searchHistoryRepository.getHistories(20)
            }.onSuccess { histories ->
                _uiState.update { it.copy(histories = histories) }
            }.onFailure { e ->
                logger.warn { "Failed to load histories: $e" }
            }
        }
    }
}
