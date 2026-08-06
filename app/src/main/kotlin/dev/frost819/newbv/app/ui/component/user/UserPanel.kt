package dev.frost819.newbv.app.ui.component.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
import dev.frost819.newbv.app.data.AccountUiState
import dev.frost819.newbv.app.viewmodel.user.UserViewModel

/**
 * 用户信息面板。
 *
 * 在首页侧边栏中显示当前登录用户的信息：
 * - 头像、用户名、等级
 * - EXP 进度条
 * - 无痕模式切换
 * - 关注列表入口（后续实现）
 * - 账号管理入口
 *
 * @param viewModel 用户 ViewModel。
 * @param onHide 隐藏面板回调。
 * @param onGoUserSwitch 跳转账号管理回调。
 */
@Composable
fun UserPanel(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    viewModel: UserViewModel = hiltViewModel(),
    onHide: () -> Unit = {},
    onGoUserSwitch: () -> Unit = {},
    onGoFollowList: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    UserPanelContent(
        modifier = modifier,
        focusRequester = focusRequester,
        uiState = uiState,
        onToggleIncognito = { viewModel.toggleIncognitoMode() },
        onGoUserSwitch = onGoUserSwitch,
        onGoFollowList = onGoFollowList,
    )
}

/**
 * 用户面板内容（无 ViewModel 依赖，可 Preview）。
 */
@Composable
private fun UserPanelContent(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    uiState: AccountUiState,
    onToggleIncognito: () -> Unit = {},
    onGoUserSwitch: () -> Unit = {},
    onGoFollowList: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLogin) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (uiState.avatar.isNotEmpty()) {
                        AsyncImage(
                            model = uiState.avatar,
                            contentDescription = uiState.username,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    }
                    Column {
                        Text(
                            text = uiState.username,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "LV${uiState.level}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                val expProgress = if (uiState.nextExp > uiState.currentMin) {
                    ((uiState.exp - uiState.currentMin).toFloat() /
                        (uiState.nextExp - uiState.currentMin).toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
                LinearProgressIndicator(
                    progress = { expProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.user_panel_not_login),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onToggleIncognito) {
                    Text(
                        text = stringResource(R.string.user_panel_incognito) +
                            if (uiState.incognitoMode) "：开" else "：关",
                    )
                }
                Button(onClick = onGoUserSwitch) {
                    Text(text = stringResource(R.string.user_panel_account))
                }
                if (uiState.isLogin) {
                    Button(onClick = onGoFollowList) {
                        Text(text = "关注列表")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserPanelContentPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        UserPanelContent(
            uiState = AccountUiState(
                isLogin = true,
                uid = 12345L,
                username = "测试用户",
                avatar = "",
                level = 6,
                exp = 5000,
                nextExp = 8000,
                incognitoMode = false,
            ),
            onGoFollowList = {},
        )
    }
}
