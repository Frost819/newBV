package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.FollowedUser
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

private const val LOAD_TIMEOUT_MS = 30_000L

/**
 * 关注列表页 UiState。
 */
data class FollowUiState(
    val mid: Long = 0L,
    val users: List<FollowedUser> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
)

/**
 * 关注列表 ViewModel。
 *
 * 一次性加载全量关注列表（`getFollowedUsers` 内部已分页处理）。
 *
 * @param userRepository 用户数据仓库。
 */
@HiltViewModel
class FollowViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val logger = Loggers.get("FollowViewModel")

    private fun prefApiType(): ApiType =
        if (Prefs.apiType == dev.frost819.newbv.data.datastore.ApiType.App) ApiType.App else ApiType.Web

    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()

    fun init(mid: Long) {
        if (_uiState.value.mid == mid && _uiState.value.users.isNotEmpty()) return
        _uiState.update { it.copy(mid = mid, loading = true, error = false, users = emptyList()) }
        load(mid)
    }

    fun refresh(mid: Long) {
        _uiState.update { it.copy(mid = mid, loading = true, error = false, users = emptyList()) }
        load(mid)
    }

    private fun load(mid: Long) {
        viewModelScope.launch {
            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    userRepository.getFollowedUsers(mid, preferApiType = prefApiType())
                }
            }.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        users = users,
                        loading = false,
                        error = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to load followed users" }
                _uiState.update { it.copy(loading = false, error = true) }
            }
        }
    }
}
