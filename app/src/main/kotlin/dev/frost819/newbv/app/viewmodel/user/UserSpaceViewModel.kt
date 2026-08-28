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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
)

/**
 * 用户空间页 ViewModel。
 *
 * 管理用户投稿视频列表分页。
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
            if (_uiState.value.mid == mid && _uiState.value.videos.isNotEmpty()) return
            _uiState.update {
                it.copy(
                    mid = mid,
                    name = name,
                    face = face ?: "",
                )
            }
            loadVideos(mid)
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
