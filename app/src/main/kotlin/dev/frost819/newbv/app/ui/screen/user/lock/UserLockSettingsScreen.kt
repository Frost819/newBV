package dev.frost819.newbv.app.ui.screen.user.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.entity.UserEntity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * 用户锁设置 ViewModel。
 *
 * 管理当前用户的锁密码设置流程。
 *
 * @property accountRepository 账户仓库。
 */
@HiltViewModel
class UserLockViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
) : ViewModel() {

    /**
     * 保存用户锁密码。
     *
     * @param uid 用户 UID。
     * @param lock 密码字符串（空字符串取消锁）。
     */
    fun saveLock(uid: Long, lock: String) {
        viewModelScope.launch {
            accountRepository.updateUserLock(uid, lock)
        }
    }
}

/**
 * 用户锁设置页面。
 *
 * 三阶段状态机：
 * 1. [InputState.InputOldPassword]（已有锁时）：验证旧密码
 * 2. [InputState.InputNewPassword]：输入新密码
 * 3. [InputState.ConfirmNewPassword]：确认新密码
 *
 * 密码为 D-Pad 方向键序列（u/d/l/r），DirectionCenter 确认，Back 删除。
 * 在 InputNewPassword 状态下输入空密码并按确认键可取消用户锁。
 *
 * @param viewModel 用户锁 ViewModel。
 * @param onSaved 保存成功回调。
 */
@Composable
fun UserLockSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: UserLockViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
) {
    val currentUid = Prefs.uid
    var currentLock by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var inputState by remember { mutableStateOf(InputState.InputNewPassword) }
    var message by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(inputState) {
        focusRequester.requestFocus()
    }

    BackHandler(true) {
        if (inputState == InputState.ConfirmNewPassword) {
            inputState = InputState.InputNewPassword
            inputPassword = ""
            newPassword = ""
        } else if (inputState == InputState.InputOldPassword) {
            onSaved()
        }
    }

    Surface(
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { inputPassword += "u"; true }
                    Key.DirectionDown -> { inputPassword += "d"; true }
                    Key.DirectionLeft -> { inputPassword += "l"; true }
                    Key.DirectionRight -> { inputPassword += "r"; true }
                    Key.DirectionCenter -> {
                        when (inputState) {
                            InputState.InputOldPassword -> {
                                if (inputPassword == currentLock) {
                                    inputState = InputState.InputNewPassword
                                    inputPassword = ""
                                    message = ""
                                } else {
                                    message = "密码错误"
                                    inputPassword = ""
                                }
                            }
                            InputState.InputNewPassword -> {
                                newPassword = inputPassword
                                if (inputPassword.isEmpty()) {
                                    viewModel.saveLock(currentUid, "")
                                    onSaved()
                                } else {
                                    inputState = InputState.ConfirmNewPassword
                                    inputPassword = ""
                                    message = ""
                                }
                            }
                            InputState.ConfirmNewPassword -> {
                                if (inputPassword == newPassword) {
                                    viewModel.saveLock(currentUid, inputPassword)
                                    onSaved()
                                } else {
                                    message = "两次密码不一致"
                                    inputState = InputState.InputNewPassword
                                    inputPassword = ""
                                    newPassword = ""
                                }
                            }
                        }
                        true
                    }
                    Key.Back -> {
                        if (inputPassword.isNotEmpty()) {
                            inputPassword = inputPassword.drop(1)
                        } else {
                            when (inputState) {
                                InputState.InputOldPassword -> onSaved()
                                InputState.InputNewPassword -> onSaved()
                                InputState.ConfirmNewPassword -> {
                                    inputState = InputState.InputNewPassword
                                    newPassword = ""
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(48.dp),
            ) {
                Text(
                    text = when (inputState) {
                        InputState.InputOldPassword -> stringResource(R.string.lock_input_old)
                        InputState.InputNewPassword -> stringResource(R.string.lock_set_new)
                        InputState.ConfirmNewPassword -> stringResource(R.string.lock_confirm_new)
                    },
                    style = MaterialTheme.typography.displaySmall,
                )

                val displayPassword = inputPassword
                    .replace("u", "↑")
                    .replace("d", "↓")
                    .replace("l", "←")
                    .replace("r", "→")
                Text(
                    text = displayPassword.ifEmpty { " " },
                    style = MaterialTheme.typography.displayLarge,
                )

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (inputState == InputState.InputNewPassword) {
                    Text(
                        text = stringResource(R.string.lock_hint_blank_remove),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

private enum class InputState {
    InputOldPassword,
    InputNewPassword,
    ConfirmNewPassword,
}
