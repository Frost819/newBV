package dev.frost819.newbv.app.ui.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
import dev.frost819.newbv.app.viewmodel.user.UserSwitchViewModel
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 账号管理页面。
 *
 * 显示所有已登录的账户列表，支持：
 * - 点击切换账户
 * - 添加新账户（跳转登录页）
 * - 管理模式：查看凭证、用户锁设置、删除账户
 *
 * @param viewModel 账号管理 ViewModel。
 * @param onNavigateLogin 跳转登录页回调。
 */
@Composable
fun UserSwitchScreen(
    modifier: Modifier = Modifier,
    viewModel: UserSwitchViewModel = hiltViewModel(),
    onNavigateLogin: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.user_switch_title),
                style = MaterialTheme.typography.displayMedium,
            )

            if (uiState.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.login_requesting))
                }
            } else if (uiState.users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.user_switch_no_users),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateLogin) {
                            Text(text = stringResource(R.string.user_switch_add))
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(uiState.users, key = { it.uid }) { user ->
                        UserCard(
                            user = user,
                            isCurrentUser = user.uid == uiState.currentUid,
                            onClick = { viewModel.switchUser(user) },
                        )
                    }
                    item {
                        AddUserButton(onClick = onNavigateLogin)
                    }
                }
            }
        }
    }
}

/**
 * 用户卡片。
 *
 * 显示头像和用户名，点击切换用户。
 *
 * @param user 用户数据。
 * @param isCurrentUser 是否为当前登录用户。
 * @param onClick 点击回调。
 */
@Composable
private fun UserCard(
    user: UserEntity,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            if (user.avatar.isNotEmpty()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = user.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = user.username.firstOrNull()?.toString() ?: "?")
                }
            }
            Text(
                text = user.username.ifEmpty { "UID: ${user.uid}" },
                style = MaterialTheme.typography.titleMedium,
            )
            if (isCurrentUser) {
                Text(
                    text = stringResource(R.string.user_switch_current),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * 添加用户按钮。
 */
@Composable
private fun AddUserButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", style = MaterialTheme.typography.displayLarge)
        }
    }
}
