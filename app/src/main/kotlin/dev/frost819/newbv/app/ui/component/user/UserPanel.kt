package dev.frost819.newbv.app.ui.component.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
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
    viewModel: UserViewModel = hiltViewModel(),
    onHide: () -> Unit = {},
    onGoUserSwitch: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxWidth(),
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

                val expProgress = if (uiState.nextExp > 0) {
                    (uiState.exp.toFloat() / uiState.nextExp.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
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
                Button(onClick = { viewModel.toggleIncognitoMode() }) {
                    Text(text = stringResource(R.string.user_panel_incognito))
                }
                Button(onClick = onGoUserSwitch) {
                    Text(text = stringResource(R.string.user_panel_account))
                }
            }
        }
    }
}
