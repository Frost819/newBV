package dev.frost819.newbv.app.ui.screen.user.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.frost819.newbv.app.viewmodel.user.UserSwitchViewModel
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 启动解锁页面。
 *
 * 应用启动时如果有用户设置了用户锁，需要先解锁才能进入主界面。
 *
 * 两阶段状态机：
 * 1. [UnlockState.ChooseUser]：选择要解锁的用户
 * 2. [UnlockState.InputPassword]：D-Pad 方向键输入密码
 *
 * 密码为方向键序列（u/d/l/r），按 DirectionCenter 确认，Back 删除上一位。
 *
 * @param viewModel 账号管理 ViewModel。
 * @param onUnlockSuccess 解锁成功回调。
 */
@Composable
fun UnlockUserScreen(
    modifier: Modifier = Modifier,
    viewModel: UserSwitchViewModel = hiltViewModel(),
    onUnlockSuccess: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val userList = uiState.users
    var selectedUser: UserEntity? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) { viewModel.updateData() }

    UnlockUserContent(
        modifier = modifier,
        userList = userList,
        selectedUser = selectedUser,
        onSelectedUserChange = { user -> selectedUser = user },
        onUnlockSuccess = { user ->
            scope.launch {
                viewModel.switchUser(user)
                onUnlockSuccess()
            }
        },
    )
}

@Composable
private fun UnlockUserContent(
    modifier: Modifier = Modifier,
    userList: List<UserEntity>,
    selectedUser: UserEntity?,
    onSelectedUserChange: (UserEntity) -> Unit,
    onUnlockSuccess: (UserEntity) -> Unit,
) {
    val inputFocusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    var inputPassword by remember { mutableStateOf("") }
    val inputShow by remember {
        derivedStateOf {
            inputPassword.replace("u", "*").replace("d", "*").replace("l", "*").replace("r", "*")
        }
    }
    var unlockState by remember { mutableStateOf(UnlockState.ChooseUser) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userList) {
        scope.launch {
            delay(200)
            defaultFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(unlockState) {
        scope.launch {
            delay(100)
            inputFocusRequester.requestFocus()
        }
    }

    BackHandler(true) { }

    Surface(
        modifier = modifier
            .focusRequester(inputFocusRequester)
            .onPreviewKeyEvent { event ->
                if (unlockState == UnlockState.ChooseUser) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { inputPassword += "u"; true }
                    Key.DirectionDown -> { inputPassword += "d"; true }
                    Key.DirectionLeft -> { inputPassword += "l"; true }
                    Key.DirectionRight -> { inputPassword += "r"; true }
                    Key.DirectionCenter -> {
                        if (selectedUser?.lock == inputPassword) {
                            onUnlockSuccess(selectedUser)
                        } else {
                            inputPassword = ""
                        }
                        true
                    }
                    Key.Back -> {
                        if (inputPassword.isNotEmpty()) {
                            inputPassword = inputPassword.drop(1)
                        } else {
                            unlockState = UnlockState.ChooseUser
                            defaultFocusRequester.requestFocus()
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (unlockState) {
                        UnlockState.ChooseUser -> stringResource(R.string.lock_choose_user)
                        UnlockState.InputPassword -> stringResource(R.string.lock_input_password)
                    },
                    style = MaterialTheme.typography.displaySmall,
                )
            }

            LazyRow(
                modifier = Modifier.focusRequester(defaultFocusRequester),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                items(userList, key = { it.uid }) { user ->
                    val alpha = when (unlockState) {
                        UnlockState.ChooseUser -> 1f
                        UnlockState.InputPassword -> if (user == selectedUser) 1f else 0.4f
                    }
                    UserSelectCard(
                        user = user,
                        alpha = alpha,
                        onClick = {
                            if (unlockState == UnlockState.ChooseUser) {
                                onSelectedUserChange(user)
                                if (user.lock.isNotBlank()) {
                                    unlockState = UnlockState.InputPassword
                                } else {
                                    onUnlockSuccess(user)
                                }
                            }
                        },
                    )
                }
            }

            if (unlockState == UnlockState.InputPassword) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp),
                    text = inputShow,
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
    }
}

private enum class UnlockState {
    ChooseUser,
    InputPassword,
}
