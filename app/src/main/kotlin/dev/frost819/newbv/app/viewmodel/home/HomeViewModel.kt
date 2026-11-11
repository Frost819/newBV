package dev.frost819.newbv.app.viewmodel.home

import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.delay
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
 * 首屏之后补齐其余 Tab 的延迟（毫秒）。
 *
 * 冷启动时首帧、图片解码已经占满带宽和 CPU，补 Tab 必须让开；
 * 但完全等到用户切过去再请求，切 Tab 会先闪一下「加载中」，那一次网络往返
 * 就是遥控器上感受到的「不跟手」。
 */
private const val PREFETCH_DELAY_MS = 2_000L

/**
 * 首页 Tab 状态。
 *
 * 所有字段都是 val，列表每次变更都生成新实例（不会原地修改）。标 [Immutable]
 * 让 Compose 把它当稳定类型，任意 Tab 的 loading/error 变化不必连累整页网格的跳过判断。
 */
@Immutable
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
            // 首屏只加载默认 Tab。原来三个 Tab 一起发请求（推荐还要串行补 3 页），
            // 冷启动时和图片解码、Compose 首帧抢网络与 CPU，用户看到的就是「慢」。
            // 其余 Tab 在 HomeContent 里首次切到时才加载。
            val startTab = Prefs.firstHomeTopNavItem
            when {
                startTab == dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend -> loadRecommend()
                startTab == dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular -> loadPopular()
                startTab == dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics && Prefs.isLogin ->
                    loadDynamic()
                // 默认落在动态页但未登录时动态列表是空的，回退加载推荐，避免首屏一片空白。
                else -> loadRecommend()
            }

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

            // 首屏落定之后再补齐另外两个 Tab。用户左右切 Tab 时列表已经在了，
            // 不会出现「先空一屏 → 加载中 → 再出图」这段等待。
            // 已有数据的 Tab 直接跳过；正在加载的由各 load 方法内部的 loading 判断挡住。
            viewModelScope.launch {
                delay(PREFETCH_DELAY_MS)
                if (_uiState.value.popularItems.isEmpty()) loadPopular()
                if (_uiState.value.dynamicItems.isEmpty() && Prefs.isLogin) loadDynamic()
            }
        }

        /**
         * 加载推荐视频。
         *
         * 列表为空时视为首次加载，连续请求直到 >= 24 条或达到 3 次上限；
         * 列表非空时视为加载更多，只请求一页。
         *
         * 每页请求使用独立的 [LOAD_TIMEOUT_MS] 超时：首次补齐首屏时某一页超时
         * 不应导致已加载的数据被标记为失败（避免"部分列表 + 报错"）。仅当列表
         * 最终为空时才置 [HomeUiState.recommendError]。
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
                var failed = false
                while (loadCount < maxLoadCount) {
                    val data =
                        try {
                            withTimeout(LOAD_TIMEOUT_MS) {
                                recommendVideoRepository.getRecommendVideos(
                                    page = recommendNextPage,
                                    preferApiType = prefApiType(),
                                )
                            }
                        } catch (error: TimeoutCancellationException) {
                            logger.error(error) { "Load recommend videos timeout" }
                            failed = true
                            break
                        } catch (error: CancellationException) {
                            // 非超时的取消（如 ViewModel cleared）必须重新抛出，否则破坏取消机制
                            throw error
                        } catch (error: Throwable) {
                            logger.error(error) { "Failed to load recommend videos" }
                            failed = true
                            break
                        }

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

                // 首屏补齐失败时，只要已拿到部分数据就静默停止（避免"部分列表 + 报错"）；
                // 加载更多失败则照常报错，让底部提示可重试
                val hasItems = _uiState.value.recommendItems.isNotEmpty()
                val showError = failed && (!hasItems || !isFirstLoad)
                _uiState.update { it.copy(recommendLoading = false, recommendError = showError) }
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
                                popularItems = (it.popularItems + data.list).distinctBy { item -> item.aid },
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
                                dynamicItems = (it.dynamicItems + data.videos).distinctBy { item -> item.aid },
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
