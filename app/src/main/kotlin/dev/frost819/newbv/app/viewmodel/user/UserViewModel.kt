package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.data.AccountUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户信息 ViewModel。
 *
 * 暴露当前登录用户的 UI 状态，提供刷新用户信息、切换无痕模式操作。
 *
 * @property accountRepository 账户仓库。
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
) : ViewModel() {

    /**
     * 当前账户 UI 状态。
     *
     * 直接映射 [AccountRepositoryImpl.uiState]，随登录态变化自动更新。
     */
    val uiState: StateFlow<AccountUiState> = accountRepository.uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountUiState(),
        )

    init {
        viewModelScope.launch {
            if (accountRepository.isLogin()) {
                accountRepository.reloadAvatar()
                accountRepository.refreshUserInfo()
            }
        }
    }

    /**
     * 强制刷新用户信息（从网络）。
     */
    fun refreshUserInfo() {
        viewModelScope.launch { accountRepository.refreshUserInfo() }
    }

    /**
     * 切换无痕模式。
     */
    fun toggleIncognitoMode() {
        accountRepository.toggleIncognitoMode()
    }
}
