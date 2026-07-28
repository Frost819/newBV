package dev.frost819.newbv.app.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 Tab 状态。
 */
data class HomeUiState(
    val recommendItems: List<UgcItem> = emptyList(),
    val recommendLoading: Boolean = false,
    val recommendHasMore: Boolean = true,
    val popularItems: List<UgcItem> = emptyList(),
    val popularLoading: Boolean = false,
    val popularHasMore: Boolean = true,
    val dynamicItems: List<DynamicVideo> = emptyList(),
    val dynamicLoading: Boolean = false,
    val dynamicHasMore: Boolean = true,
    val isLogin: Boolean = false,
)

/**
 * 首页 ViewModel。
 *
 * 管理推荐、热门、动态三个 Tab 的数据加载、分页、刷新。
 * 使用 [StateFlow] 暴露状态，UI 通过 [uiState] 观察。
 *
 * @property recommendVideoRepository 推荐/热门数据仓库。
 * @property userRepository 动态数据仓库。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recommendVideoRepository: RecommendVideoRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger("HomeViewModel")

    /** 将 data 层 ApiType 映射为 bili-api 层 ApiType。 */
    private fun prefApiType(): BiliApiType = when (Prefs.apiType) {
        DataApiType.Web -> BiliApiType.Web
        DataApiType.App -> BiliApiType.App
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var recommendNextPage = RecommendPage()
    private var popularNextPage = PopularVideoPage()
    private var dynamicCurrentPage = 0
    private var dynamicHistoryOffset: String? = null
    private var dynamicUpdateBaseline: String? = null

    init {
        _uiState.update { it.copy(isLogin = Prefs.isLogin) }
        loadRecommend()
        loadPopular()
        if (Prefs.isLogin) loadDynamic()
    }

    /**
     * 加载更多推荐视频。
     *
     * 首次加载时连续请求直到 >= 24 条或达到 3 次重试上限。
     */
    fun loadRecommend() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.recommendLoading) return@launch

            _uiState.update { it.copy(recommendLoading = true) }

            var loadCount = 0
            val maxLoadCount = 3
            runCatching {
                while (_uiState.value.recommendItems.size < 24 && loadCount < maxLoadCount) {
                    val data = recommendVideoRepository.getRecommendVideos(
                        page = recommendNextPage,
                        preferApiType = prefApiType(),
                    )
                    recommendNextPage = data.nextPage
                    _uiState.update {
                        it.copy(recommendItems = it.recommendItems + data.items)
                    }
                    loadCount++
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to load recommend videos" }
            }

            _uiState.update { it.copy(recommendLoading = false) }
        }
    }

    /**
     * 清空推荐数据并重新加载。
     */
    fun refreshRecommend() {
        recommendNextPage = RecommendPage()
        _uiState.update {
            it.copy(recommendItems = emptyList(), recommendHasMore = true)
        }
        loadRecommend()
    }

    /**
     * 加载更多热门视频。
     */
    fun loadPopular() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.popularLoading) return@launch

            _uiState.update { it.copy(popularLoading = true) }

            runCatching {
                val data = recommendVideoRepository.getPopularVideos(
                    page = popularNextPage,
                    preferApiType = prefApiType(),
                )
                popularNextPage = data.nextPage
                _uiState.update {
                    it.copy(
                        popularItems = it.popularItems + data.list,
                        popularHasMore = !data.noMore,
                    )
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to load popular videos" }
            }

            _uiState.update { it.copy(popularLoading = false) }
        }
    }

    /**
     * 清空热门数据并重新加载。
     */
    fun refreshPopular() {
        popularNextPage = PopularVideoPage()
        _uiState.update {
            it.copy(popularItems = emptyList(), popularHasMore = true)
        }
        loadPopular()
    }

    /**
     * 加载更多动态视频。
     *
     * 需要登录，未登录时不执行。
     */
    fun loadDynamic() {
        if (!_uiState.value.isLogin) return
        viewModelScope.launch {
            val current = _uiState.value
            if (current.dynamicLoading || !current.dynamicHasMore) return@launch

            _uiState.update { it.copy(dynamicLoading = true) }

            val nextPage = dynamicCurrentPage + 1
            runCatching {
                val data = userRepository.getDynamicVideos(
                    page = nextPage,
                    offset = dynamicHistoryOffset.orEmpty(),
                    updateBaseline = dynamicUpdateBaseline.orEmpty(),
                    preferApiType = prefApiType(),
                )
                dynamicCurrentPage = nextPage
                dynamicHistoryOffset = data.historyOffset
                dynamicUpdateBaseline = data.updateBaseline
                _uiState.update {
                    it.copy(
                        dynamicItems = it.dynamicItems + data.videos,
                        dynamicHasMore = data.hasMore,
                    )
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to load dynamic videos" }
            }

            _uiState.update { it.copy(dynamicLoading = false) }
        }
    }

    /**
     * 清空动态数据并重新加载。
     */
    fun refreshDynamic() {
        dynamicCurrentPage = 0
        dynamicHistoryOffset = null
        dynamicUpdateBaseline = null
        _uiState.update {
            it.copy(dynamicItems = emptyList(), dynamicHasMore = true)
        }
        loadDynamic()
    }

    /**
     * 刷新指定 Tab 的数据。
     *
     * @param tab 目标 Tab。
     */
    fun refresh(tab: dev.frost819.newbv.data.datastore.HomeTopNavItem) {
        when (tab) {
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend -> refreshRecommend()
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular -> refreshPopular()
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics -> refreshDynamic()
        }
    }

    /**
     * 加载指定 Tab 的更多数据。
     *
     * @param tab 目标 Tab。
     */
    fun loadMore(tab: dev.frost819.newbv.data.datastore.HomeTopNavItem) {
        when (tab) {
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend -> loadRecommend()
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular -> loadPopular()
            dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics -> loadDynamic()
        }
    }

    /**
     * 更新登录状态（登录/登出时调用）。
     */
    fun updateLoginState(isLogin: Boolean) {
        _uiState.update { it.copy(isLogin = isLogin) }
        if (isLogin) {
            if (_uiState.value.dynamicItems.isEmpty()) loadDynamic()
        } else {
            dynamicCurrentPage = 0
            dynamicHistoryOffset = null
            dynamicUpdateBaseline = null
            _uiState.update {
                it.copy(dynamicItems = emptyList(), dynamicHasMore = true)
            }
        }
    }
}
