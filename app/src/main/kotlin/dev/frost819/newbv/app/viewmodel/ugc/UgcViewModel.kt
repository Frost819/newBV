package dev.frost819.newbv.app.viewmodel.ugc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.ugc.UgcTypeV2
import dev.frost819.newbv.biliapi.entity.ugc.region.UgcFeedData
import dev.frost819.newbv.biliapi.entity.ugc.region.UgcFeedPage
import dev.frost819.newbv.biliapi.repositories.UgcRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val LOAD_TIMEOUT_MS = 10_000L

/**
 * UGC 分区 UI 状态。
 *
 * @property items 当前分区的视频列表。
 * @property loading 是否正在加载。
 * @property hasMore 是否还有更多数据。
 * @property error 是否加载失败。
 */
data class UgcUiState(
    val items: List<UgcItem> = emptyList(),
    val loading: Boolean = false,
    val hasMore: Boolean = true,
    val error: Boolean = false,
)

/**
 * UGC 分区 ViewModel。
 *
 * 管理分区顶部导航切换和数据加载。每个分区独立维护分页状态，
 * 切换 Tab 时保留已加载数据（不重新请求）。
 *
 * @property ugcRepository UGC 分区数据仓库。
 */
@HiltViewModel
class UgcViewModel @Inject constructor(
    private val ugcRepository: UgcRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger("UgcViewModel")

    private val _uiState = MutableStateFlow(UgcUiState())
    val uiState: StateFlow<UgcUiState> = _uiState.asStateFlow()

    /** 当前选中的分区。 */
    private var currentType: UgcTypeV2 = UgcTypeV2.Douga

    /** 当前分区的下一页游标。 */
    private var nextPage: UgcFeedPage = UgcFeedPage()

    init {
        loadMore()
    }

    /**
     * 切换到指定分区。
     *
     * 如果该分区已有数据则不重新加载，否则触发首次加载。
     *
     * @param type 目标分区。
     */
    fun switchType(type: UgcTypeV2) {
        if (type == currentType && _uiState.value.items.isNotEmpty()) return
        currentType = type
        nextPage = UgcFeedPage()
        _uiState.value = UgcUiState()
        loadMore()
    }

    /**
     * 加载更多当前分区视频。
     *
     * 超时或失败时标记 error，不中断已有数据。
     */
    fun loadMore() {
        val current = _uiState.value
        if (current.loading || !current.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = false) }

            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    val data: UgcFeedData = ugcRepository.getRegionFeedRcmd(
                        ugcType = currentType,
                        page = nextPage,
                    )
                    nextPage = data.nextPage
                    _uiState.update {
                        it.copy(
                            items = it.items + data.items,
                            hasMore = data.hasNext,
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to load UGC region: $currentType" }
                _uiState.update { it.copy(error = true) }
            }

            _uiState.update { it.copy(loading = false) }
        }
    }

    /**
     * 刷新当前分区数据。
     */
    fun refresh() {
        nextPage = UgcFeedPage()
        _uiState.value = UgcUiState()
        loadMore()
    }
}
