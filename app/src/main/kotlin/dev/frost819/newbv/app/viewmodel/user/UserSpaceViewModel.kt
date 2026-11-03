package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.user.SpaceVideo
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

private const val LOAD_TIMEOUT_MS = 10_000L

/**
 * 用户空间页 UiState。
 *
 * 与原版 BV 一致：用户名和头像由来源页通过路由传入，不单独调用 API 获取。
 */
data class UserSpaceUiState(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = "",
    val videos: List<SpaceVideo> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val hasMore: Boolean = true,
    val isFollowing: Boolean = false,
    val followLoading: Boolean = false,
)

/**
 * 用户空间页一次性 UI 事件。
 */
sealed interface UserSpaceUiEffect {
    /** 显示 Toast 消息。 */
    data class ShowToast(
        val message: String,
    ) : UserSpaceUiEffect
}

/**
 * 用户空间页 ViewModel。
 *
 * 管理用户投稿视频列表分页和当前用户对 TA 的关注状态。
 * 用户名和头像由 [init] 从路由参数传入，不调用 getUserInfo API（与原版 BV 行为一致）。
 *
 * @param userRepository 用户数据仓库。
 */
@HiltViewModel
class UserSpaceViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("UserSpaceViewModel")

        private val _uiState = MutableStateFlow(UserSpaceUiState())
        val uiState: StateFlow<UserSpaceUiState> = _uiState.asStateFlow()

        private val _uiEffect = MutableSharedFlow<UserSpaceUiEffect>()
        val uiEffect: SharedFlow<UserSpaceUiEffect> = _uiEffect.asSharedFlow()

        private var videoPage = SpaceVideoPage()

        private fun prefApiType(): BiliApiType =
            when (Prefs.apiType) {
                DataApiType.Web -> BiliApiType.Web
                DataApiType.App -> BiliApiType.App
            }

        /**
         * 初始化：设置用户信息（由路由传入），加载投稿视频列表。
         *
         * @param mid 用户 MID
         * @param name 用户名（从详情页/播放器/卡片传入）
         * @param face 头像 URL（可选）
         */
        fun init(
            mid: Long,
            name: String = "",
            face: String? = null,
        ) {
            val sameMid = _uiState.value.mid == mid
            val hasCachedVideos = sameMid && _uiState.value.videos.isNotEmpty()

            if (!hasCachedVideos) {
                _uiState.update {
                    it.copy(
                        mid = mid,
                        name = name,
                        face = face ?: "",
                        // 切换到其他用户时先清空关注状态，等待本次查询返回
                        isFollowing = if (sameMid) it.isFollowing else false,
                    )
                }
                loadVideos(mid)
            }

            // 列表可复用，但关注状态每次进入都要刷新：用户可能刚从详情页关注/取关后返回本页
            viewModelScope.launch { refreshFollowStatus(mid) }
        }

        /**
         * 切换关注状态。
         *
         * 未关注时关注，已关注时取消关注。请求失败或接口返回非 0 码时
         * 通过 [uiEffect] 发出 Toast，并保持原状态不变。
         */
        fun toggleFollow() {
            val state = _uiState.value
            if (state.mid == 0L || state.followLoading) return

            val wasFollowing = state.isFollowing
            val preferApiType = prefApiType()
            _uiState.update { it.copy(followLoading = true) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        if (wasFollowing) {
                            userRepository.unfollowUser(mid = state.mid, preferApiType = preferApiType)
                        } else {
                            userRepository.followUser(mid = state.mid, preferApiType = preferApiType)
                        }
                    }
                }.onSuccess { success ->
                    if (success) {
                        _uiState.update { it.copy(isFollowing = !wasFollowing, followLoading = false) }
                    } else {
                        _uiState.update { it.copy(followLoading = false) }
                        _uiEffect.emit(
                            UserSpaceUiEffect.ShowToast(
                                "${if (wasFollowing) "取消关注" else "关注"}失败",
                            ),
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to toggle follow" }
                    _uiState.update { it.copy(followLoading = false) }
                    _uiEffect.emit(
                        UserSpaceUiEffect.ShowToast(
                            "${if (wasFollowing) "取消关注" else "关注"}失败: ${error.message ?: "未知错误"}",
                        ),
                    )
                }
            }
        }

        /**
         * 拉取当前用户对 [mid] 的关注状态。
         *
         * 用户在本页手动切换关注期间（[UserSpaceUiState.followLoading] 为 true）忽略结果，
         * 避免慢查询返回后用旧值覆盖刚切换的状态。
         */
        private suspend fun refreshFollowStatus(mid: Long) {
            if (mid == 0L) return

            runCatching {
                userRepository.checkIsFollowing(mid)
            }.onSuccess { following ->
                if (!_uiState.value.followLoading) {
                    _uiState.update { it.copy(isFollowing = following ?: false) }
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to fetch following status" }
            }
        }

        fun refresh(mid: Long) {
            videoPage = SpaceVideoPage()
            _uiState.update {
                it.copy(
                    videos = emptyList(),
                    hasMore = true,
                    error = false,
                )
            }
            loadVideos(mid)
        }

        fun loadVideos(mid: Long) {
            if (_uiState.value.loading || !_uiState.value.hasMore) return

            _uiState.update { it.copy(loading = true, error = false) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        userRepository.getSpaceVideos(
                            mid = mid,
                            order = SpaceVideoOrder.PubDate,
                            page = videoPage,
                            preferApiType = prefApiType(),
                        )
                    }
                }.onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            videos = it.videos + data.videos,
                            loading = false,
                            hasMore = data.page.hasNext,
                        )
                    }
                    videoPage = data.page
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load space videos" }
                    _uiState.update { it.copy(loading = false, error = true) }
                }
            }
        }
    }
