package dev.frost819.newbv.app.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/** 网络请求超时时间（毫秒）。 */
private const val LOAD_TIMEOUT_MS = 10_000L

/**
 * 首页 Tab 状态。
 */
data class HomeUiState(
    val recommendItems: List<UgcItem> = emptyList(),
    val recommendLoading: Boolean = false,
    val recommendHasMore: Boolean = true,
    val recommendError: Boolean = false,
    val popularItems: List<UgcItem> = emptyList(),
    val popularLoading: Boolean = false,
    val popularHasMore: Boolean = true,
    val popularError: Boolean = false,
    val dynamicItems: List<DynamicVideo> = emptyList(),
    val dynamicLoading: Boolean = false,
    val dynamicHasMore: Boolean = true,
    val dynamicError: Boolean = false,
    val isLogin: Boolean = false,
    val currentUid: Long = 0L,
)

/**
 * 首页 ViewModel。
 *
 * 管理推荐、热门、动态三个 Tab 的数据加载、分页、刷新。
 * 使用 [StateFlow] 暴露状态，UI 通过 [uiState] 观察。
 *
 * @property recommendVideoRepository 推荐/热门数据仓库。
 * @property userRepository 动态数据仓库。
 * @property accountRepository 账户仓库（监听登录状态变化）。
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val recommendVideoRepository: RecommendVideoRepository,
        private val userRepository: UserRepository,
        private val accountRepository: AccountRepositoryImpl,
    ) : ViewModel() {
        private val logger = Loggers.get("HomeViewModel")

        /** 将 data 层 ApiType 映射为 bili-api 层 ApiType。 */
        private fun prefApiType(): BiliApiType =
            when (Prefs.apiType) {
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

            viewModelScope.launch {
                accountRepository.uiState
                    .map { it.uid to it.isLogin }
                    .distinctUntilChanged()
                    .collect { (uid, isLogin) ->
                        if (isLogin != _uiState.value.isLogin) {
                            updateLoginState(isLogin)
                        } else if (isLogin && uid != 0L && uid != _uiState.value.currentUid) {
                            onUserSwitched(uid)
                        }
                    }
            }
        }

        /**
         * 加载推荐视频。
         *
         * 列表为空时视为首次加载，连续请求直到 >= 24 条或达到 3 次上限；
         * 列表非空时视为加载更多，只请求一页。
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadRecommend() {
            viewModelScope.launch {
                val current = _uiState.value
                if (current.recommendLoading) return@launch

                _uiState.update { it.copy(recommendLoading = true, recommendError = false) }

                // 首次加载（列表为空）需要连续请求补齐首屏；已有数据时每次只追加一页，
                val isFirstLoad = current.recommendItems.isEmpty()
                val maxLoadCount = if (isFirstLoad) 3 else 1
                var loadCount = 0
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        while (loadCount < maxLoadCount) {
                            val data =
                                recommendVideoRepository.getRecommendVideos(
                                    page = recommendNextPage,
                                    preferApiType = prefApiType(),
                                )
                            recommendNextPage = data.nextPage
                            if (data.items.isEmpty()) {
                                _uiState.update { it.copy(recommendHasMore = false) }
                                break
                            }
                            _uiState.update {
                                it.copy(recommendItems = it.recommendItems + data.items)
                            }
                            loadCount++
                            if (!isFirstLoad || _uiState.value.recommendItems.size >= 24) break
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load recommend videos" }
                    _uiState.update { it.copy(recommendError = true) }
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
                it.copy(recommendItems = emptyList(), recommendHasMore = true, recommendError = false)
            }
            loadRecommend()
        }

        /**
         * 加载更多热门视频。
         *
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadPopular() {
            viewModelScope.launch {
                val current = _uiState.value
                if (current.popularLoading) return@launch

                _uiState.update { it.copy(popularLoading = true, popularError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data =
                            recommendVideoRepository.getPopularVideos(
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
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load popular videos" }
                    _uiState.update { it.copy(popularError = true) }
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
                it.copy(popularItems = emptyList(), popularHasMore = true, popularError = false)
            }
            loadPopular()
        }

        /**
         * 加载更多动态视频。
         *
         * 需要登录，未登录时不执行。
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadDynamic() {
            if (!_uiState.value.isLogin) return
            viewModelScope.launch {
                val current = _uiState.value
                if (current.dynamicLoading || !current.dynamicHasMore) return@launch

                _uiState.update { it.copy(dynamicLoading = true, dynamicError = false) }

                val nextPage = dynamicCurrentPage + 1
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data =
                            userRepository.getDynamicVideos(
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
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load dynamic videos" }
                    _uiState.update { it.copy(dynamicError = true) }
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
                it.copy(dynamicItems = emptyList(), dynamicHasMore = true, dynamicError = false)
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
                    it.copy(dynamicItems = emptyList(), dynamicHasMore = true, dynamicError = false, currentUid = 0L)
                }
            }
        }

        /**
         * 切换用户后刷新所有数据。
         *
         * 清空推荐/热门/动态列表并重新加载，确保展示新用户的个性化内容。
         *
         * @param uid 新用户 UID。
         */
        private fun onUserSwitched(uid: Long) {
            _uiState.update { it.copy(currentUid = uid) }
            refreshRecommend()
            refreshPopular()
            refreshDynamic()
        }
    }
