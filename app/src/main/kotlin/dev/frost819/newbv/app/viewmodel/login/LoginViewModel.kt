package dev.frost819.newbv.app.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.biliapi.entity.login.QrLoginState
import dev.frost819.newbv.biliapi.entity.login.WebCookies
import dev.frost819.newbv.biliapi.repositories.LoginRepository
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 扫码登录 UI 状态。
 *
 * @property state QR 登录状态机当前状态。
 * @property qrUrl 二维码内容 URL（用于渲染二维码图片）。
 * @property errorMessage 错误信息（仅 Error 状态有值）。
 */
data class QrLoginUiState(
    val state: QrLoginState = QrLoginState.Ready,
    val qrUrl: String = "",
    val errorMessage: String = "",
)

/**
 * 扫码登录 ViewModel。
 *
 * 支持 TV QR 登录（App 接口）和 Web QR 登录（Web 接口）。
 *
 * QR 登录流程：
 * 1. [requestQrCode]：请求二维码 URL，生成二维码图片
 * 2. 自动启动轮询，每 1 秒检查扫码状态
 * 3. 状态流转：Ready → RequestingQRCode → WaitingForScan → WaitingForConfirm → Success
 * 4. Success 时：构造 [AuthData]，调用 [AccountRepositoryImpl.addUser] 持久化
 *
 * @property loginRepository 登录接口仓库。
 * @property accountRepository 账户管理仓库。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val accountRepository: AccountRepositoryImpl,
) : ViewModel() {

    private val logger = Loggers.get("LoginViewModel")

    private val _uiState = MutableStateFlow(QrLoginUiState())
    val uiState: StateFlow<QrLoginUiState> = _uiState.asStateFlow()

    /** 轮询协程 Job，用于取消轮询。 */
    private var pollingJob: Job? = null

    /**
     * 请求 TV QR 二维码。
     *
     * 调用 App 接口获取二维码 URL 和 authCode，启动轮询。
     */
    fun requestAppQrCode() {
        cancelPolling()
        _uiState.update { it.copy(state = QrLoginState.RequestingQRCode, errorMessage = "") }
        viewModelScope.launch {
            runCatching {
                val qrData = loginRepository.requestAppQrLogin()
                _uiState.update {
                    it.copy(
                        state = QrLoginState.WaitingForScan,
                        qrUrl = qrData.url,
                    )
                }
                startPolling(qrData.key, isAppQr = true)
            }.onFailure { error ->
                logger.error(error) { "Failed to request app QR code" }
                _uiState.update {
                    it.copy(
                        state = QrLoginState.Error,
                        errorMessage = error.message ?: "请求二维码失败",
                    )
                }
            }
        }
    }

    /**
     * 请求 Web QR 二维码。
     *
     * 调用 Web 接口获取二维码 URL 和 qrcodeKey，启动轮询。
     */
    fun requestWebQrCode() {
        cancelPolling()
        _uiState.update { it.copy(state = QrLoginState.RequestingQRCode, errorMessage = "") }
        viewModelScope.launch {
            runCatching {
                val qrData = loginRepository.requestWebQrLogin()
                _uiState.update {
                    it.copy(
                        state = QrLoginState.WaitingForScan,
                        qrUrl = qrData.url,
                    )
                }
                startPolling(qrData.key, isAppQr = false)
            }.onFailure { error ->
                logger.error(error) { "Failed to request web QR code" }
                _uiState.update {
                    it.copy(
                        state = QrLoginState.Error,
                        errorMessage = error.message ?: "请求二维码失败",
                    )
                }
            }
        }
    }

    /**
     * 启动轮询检查扫码状态。
     *
     * 每 1 秒轮询一次，根据返回状态更新 UI。
     * Success 时自动取消轮询并保存登录凭证。
     *
     * @param key 二维码 key（WebQR 为 qrcodeKey，AppQR 为 authCode）。
     * @param isAppQr 是否为 TV QR（App 接口）。
     */
    private fun startPolling(key: String, isAppQr: Boolean) {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                runCatching {
                    if (isAppQr) {
                        loginRepository.checkAppQrLoginState(key)
                    } else {
                        loginRepository.checkWebQrLoginState(key)
                    }
                }.onSuccess { result ->
                    when (result.state) {
                        QrLoginState.WaitingForScan,
                        QrLoginState.WaitingForConfirm,
                        -> {
                            _uiState.update { it.copy(state = result.state) }
                        }

                        QrLoginState.Expired -> {
                            _uiState.update { it.copy(state = QrLoginState.Expired) }
                            cancelPolling()
                        }

                        QrLoginState.Success -> {
                            handleLoginSuccess(result.cookies, result.accessToken, result.refreshToken)
                            cancelPolling()
                        }

                        else -> {
                            logger.warn { "Unknown QR login state: ${result.state}" }
                        }
                    }
                }.onFailure { error ->
                    logger.error(error) { "Failed to check QR login state" }
                    _uiState.update {
                        it.copy(
                            state = QrLoginState.Error,
                            errorMessage = error.message ?: "检查登录状态失败",
                        )
                    }
                    cancelPolling()
                }
            }
        }
    }

    /**
     * 处理登录成功。
     *
     * 从 [WebCookies] 构造 [AuthData]，调用 [AccountRepositoryImpl.addUser] 持久化。
     *
     * @param cookies 登录返回的 Cookie 信息。
     * @param accessToken App 接口 token（WebQR 为 null）。
     * @param refreshToken App 接口 refresh token（WebQR 为 null）。
     */
    private suspend fun handleLoginSuccess(
        cookies: WebCookies?,
        accessToken: String?,
        refreshToken: String?,
    ) {
        val cookie = cookies ?: run {
            _uiState.update {
                it.copy(
                    state = QrLoginState.Error,
                    errorMessage = "登录成功但 Cookie 为空",
                )
            }
            return
        }
        val authData = dev.frost819.newbv.app.data.AuthData(
            uid = cookie.dedeUserId,
            uidCkMd5 = cookie.dedeUserIdCkMd5,
            sid = cookie.sid,
            biliJct = cookie.biliJct,
            sessData = cookie.sessData,
            tokenExpiredDate = cookie.expiredDate.time,
            accessToken = accessToken ?: "",
            refreshToken = refreshToken ?: "",
        )
        accountRepository.addUser(authData)
        _uiState.update { it.copy(state = QrLoginState.Success) }
    }

    /**
     * 取消轮询。
     *
     * 在页面退出或重新请求二维码时调用。
     */
    fun cancelPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 重置为初始状态。
     *
     * 用户在 Expired/Error 状态下重新请求二维码前调用。
     */
    fun reset() {
        cancelPolling()
        _uiState.value = QrLoginUiState()
    }

    override fun onCleared() {
        super.onCleared()
        cancelPolling()
    }
}
