package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.biliapi.entity.user.SpaceVideo
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.entity.user.UserSpaceInfo
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val LOAD_TIMEOUT_MS = 10_000L

/**
 * 用户空间页 UiState。
 */
data class UserSpaceUiState(
    val mid: Long = 0L,
    val userInfo: UserSpaceInfo? = null,
    val isFollowing: Boolean = false,
    val videos: List<SpaceVideo> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val hasMore: Boolean = true,
    val userInfoLoading: Boolean = true,
    val userInfoError: Boolean = false,
    val followLoading: Boolean = false,
)

sealed interface UserSpaceUiEffect {
    data class ShowToast(val message: String) : UserSpaceUiEffect
}

/**
 * 用户空间页 ViewModel。
 *
 * 管理用户信息加载、视频列表分页、关注/取关操作。
 *
 * @param userRepository 用户数据仓库。
 */
@HiltViewModel
class UserSpaceViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger("UserSpaceViewModel")

    private val _uiState = MutableStateFlow(UserSpaceUiState())
    val uiState: StateFlow<UserSpaceUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<UserSpaceUiEffect>()
    val effect = _effect.asSharedFlow()

    private var videoPage = SpaceVideoPage()

    private fun prefApiType(): BiliApiType = when (Prefs.apiType) {
        DataApiType.Web -> BiliApiType.Web
        DataApiType.App -> BiliApiType.App
    }

    /**
     * 初始化：加载用户信息和视频列表。
     */
    fun init(mid: Long) {
        if (_uiState.value.mid == mid && _uiState.value.userInfo != null) return
        _uiState.update { it.copy(mid = mid, userInfoLoading = true, userInfoError = false) }
        loadUserInfo(mid)
        loadVideos(mid)
    }

    fun refresh(mid: Long) {
        videoPage = SpaceVideoPage()
        _uiState.update {
            it.copy(
                videos = emptyList(),
                hasMore = true,
                error = false,
                userInfoLoading = true,
                userInfoError = false,
            )
        }
        loadUserInfo(mid)
        loadVideos(mid)
    }

    private fun loadUserInfo(mid: Long) {
        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    userRepository.getUserInfo(mid)
                }
            }.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        userInfo = info,
                        isFollowing = info.isFollowed,
                        userInfoLoading = false,
                        userInfoError = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to load user info" }
                _uiState.update { it.copy(userInfoLoading = false, userInfoError = true) }
            }
        }
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

    /**
     * 关注/取关用户。
     */
    fun toggleFollow() {
        val current = _uiState.value
        val mid = current.mid
        val isFollowing = current.isFollowing
        if (mid == 0L || current.followLoading) return

        _uiState.update { it.copy(followLoading = true) }

        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    if (isFollowing) {
                        userRepository.unfollowUser(mid, prefApiType())
                    } else {
                        userRepository.followUser(mid, prefApiType())
                    }
                }
            }.onSuccess { success ->
                _uiState.update { it.copy(followLoading = false) }
                if (success) {
                    _uiState.update { it.copy(isFollowing = !isFollowing) }
                    val msg = if (isFollowing) "已取消关注" else "关注成功"
                    _effect.emit(UserSpaceUiEffect.ShowToast(msg))
                } else {
                    _effect.emit(UserSpaceUiEffect.ShowToast("操作失败"))
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to toggle follow" }
                _uiState.update { it.copy(followLoading = false) }
                _effect.emit(UserSpaceUiEffect.ShowToast("操作失败"))
            }
        }
    }
}
