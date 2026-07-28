package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 账号管理 UI 状态。
 *
 * @property loading 是否正在加载。
 * @property users 用户列表。
 * @property currentUid 当前登录用户 UID（0 表示未登录）。
 */
data class UserSwitchUiState(
    val loading: Boolean = true,
    val users: List<UserEntity> = emptyList(),
    val currentUid: Long = 0L,
)

/**
 * 账号管理 ViewModel。
 *
 * 负责加载已登录的账户列表、切换账户、删除账户。
 *
 * @property accountRepository 账户仓库。
 */
@HiltViewModel
class UserSwitchViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSwitchUiState())
    val uiState: StateFlow<UserSwitchUiState> = _uiState.asStateFlow()

    init {
        updateData()
    }

    /**
     * 刷新账户列表。
     */
    fun updateData() {
        viewModelScope.launch {
            val users = accountRepository.getAllUsers()
            _uiState.update {
                it.copy(
                    loading = false,
                    users = users,
                    currentUid = accountRepository.currentUid(),
                )
            }
        }
    }

    /**
     * 切换到指定用户。
     *
     * @param user 目标用户。
     */
    fun switchUser(user: UserEntity) {
        viewModelScope.launch {
            accountRepository.setCurrentUser(user)
            updateData()
        }
    }

    /**
     * 删除指定用户。
     *
     * 如果删除的是当前用户，且有其他账户，自动切换到第一个；
     * 若无其他账户，则执行退出登录。
     *
     * @param user 待删除的用户。
     */
    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            accountRepository.deleteUser(user)
            val remaining = accountRepository.getAllUsers()
            if (remaining.isNotEmpty()) {
                accountRepository.setCurrentUser(remaining.first())
            } else {
                accountRepository.logout()
            }
            updateData()
        }
    }
}
