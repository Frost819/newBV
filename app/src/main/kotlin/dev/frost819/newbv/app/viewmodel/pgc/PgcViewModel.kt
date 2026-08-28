package dev.frost819.newbv.app.viewmodel.pgc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.CarouselData
import dev.frost819.newbv.biliapi.entity.pgc.PgcFeedData
import dev.frost819.newbv.biliapi.entity.pgc.PgcItem
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.repositories.PgcRepository
import dev.frost819.newbv.core.log.Loggers
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
 * PGC 影视分区 UI 状态。
 *
 * @property carouselItems 轮播图数据。
 * @property items 当前分区的番剧/影视列表。
 * @property loading 是否正在加载 Feed 数据。
 * @property carouselLoading 是否正在加载轮播图。
 * @property hasMore 是否还有更多数据。
 * @property error Feed 加载是否失败。
 */
data class PgcUiState(
    val carouselItems: List<CarouselData.CarouselItem> = emptyList(),
    val items: List<PgcItem> = emptyList(),
    val loading: Boolean = false,
    val carouselLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: Boolean = false,
)

/**
 * PGC 影视分区 ViewModel。
 *
 * 管理番剧/国创/电影/纪录片/电视剧/综艺的轮播图和推荐 Feed 数据加载。
 * 轮播图从 PGC 页面 HTML 解析，Feed 使用 cursor 分页。
 * 切换 Tab 时清空重新加载轮播图和 Feed。
 *
 * @param pgcRepository PGC 数据仓库。
 */
@HiltViewModel
class PgcViewModel
    @Inject
    constructor(
        private val pgcRepository: PgcRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("PgcViewModel")

        private val _uiState = MutableStateFlow(PgcUiState())
        val uiState: StateFlow<PgcUiState> = _uiState.asStateFlow()

        /** 当前选中的分区。 */
        private var currentType: PgcType = PgcType.Anime

        /** 当前分区的分页游标。 */
        private var cursor: Int = 0

        init {
            loadCarousel()
            loadMore()
        }

        /**
         * 切换到指定分区。
         *
         * @param type 目标分区。
         */
        fun switchType(type: PgcType) {
            if (type == currentType && _uiState.value.items.isNotEmpty()) return
            currentType = type
            cursor = 0
            _uiState.value = PgcUiState()
            loadCarousel()
            loadMore()
        }

        /**
         * 加载轮播图数据。
         *
         * 失败时静默处理（轮播图为辅助展示，不阻塞 Feed）。
         */
        fun loadCarousel() {
            viewModelScope.launch {
                _uiState.update { it.copy(carouselLoading = true) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val carouselData = pgcRepository.getCarousel(currentType)
                        _uiState.update {
                            it.copy(carouselItems = carouselData.items)
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load PGC carousel: $currentType" }
                }

                _uiState.update { it.copy(carouselLoading = false) }
            }
        }

        /**
         * 加载更多当前分区 Feed 数据。
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
                        val data: PgcFeedData =
                            pgcRepository.getFeed(
                                pgcType = currentType,
                                cursor = cursor,
                            )
                        cursor = data.cursor
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
                    logger.error(error) { "Failed to load PGC feed: $currentType" }
                    _uiState.update { it.copy(error = true) }
                }

                _uiState.update { it.copy(loading = false) }
            }
        }

        /**
         * 刷新当前分区数据（轮播图 + Feed）。
         */
        fun refresh() {
            cursor = 0
            _uiState.value = PgcUiState()
            loadCarousel()
            loadMore()
        }
    }
