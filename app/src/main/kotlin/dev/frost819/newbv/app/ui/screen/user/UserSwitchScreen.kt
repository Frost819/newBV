package dev.frost819.newbv.app.ui.screen.user

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.rememberScreenFocusSaver
import dev.frost819.newbv.app.viewmodel.user.UserSwitchViewModel
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 账号管理页面。
 *
 * 显示所有已登录的账户列表，支持：
 * - 点击切换账户
 * - 添加新账户（跳转登录页）
 * - 删除账户
 *
 * 从登录页返回时自动刷新用户列表。
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
    val focusSaver = rememberScreenFocusSaver()
    focusSaver.RestoreFocus()

    LifecycleResumeEffect(Unit) {
        viewModel.updateData()
        onPauseOrDispose { }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.user_switch_title),
                style = MaterialTheme.typography.displayMedium,
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.login_requesting))
                }
            } else if (uiState.users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.user_switch_no_users),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateLogin,
                            modifier =
                                Modifier
                                    .focusSaverItem(focusSaver, "add_user_empty")
                                    .touchClickable(onClick = onNavigateLogin),
                        ) {
                            Text(text = stringResource(R.string.user_switch_add))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.users, key = { it.uid }) { user ->
                        UserListItem(
                            user = user,
                            isCurrentUser = user.uid == uiState.currentUid,
                            onClick = { viewModel.switchUser(user) },
                            onDelete = { viewModel.deleteUser(user) },
                            modifier = Modifier.focusSaverItem(focusSaver, "user_${user.uid}"),
                        )
                    }
                    item {
                        AddUserButton(
                            onClick = onNavigateLogin,
                            modifier = Modifier.focusSaverItem(focusSaver, "add_user"),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 用户列表项。
 *
 * 头像 + 用户名 + 当前用户标识 + 删除按钮。
 *
 * @param user 用户数据。
 * @param isCurrentUser 是否为当前登录用户。
 * @param onClick 点击切换用户回调。
 * @param onDelete 删除用户回调。
 */
@Composable
private fun UserListItem(
    user: UserEntity,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .width(500.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .then(modifier)
                    .touchClickable(onClick = onClick),
            onClick = onClick,
            colors =
                ClickableSurfaceDefaults.colors(
                    containerColor =
                        if (isCurrentUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    contentColor =
                        if (isCurrentUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (user.avatar.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = user.username,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user.username.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Column {
                    Text(
                        text = user.username.ifEmpty { "UID: ${user.uid}" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isCurrentUser) {
                        Text(
                            text = stringResource(R.string.user_switch_current),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.touchClickable(onClick = onDelete),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除用户",
            )
        }
    }
}

/**
 * 添加用户按钮。
 */
@Composable
private fun AddUserButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.touchClickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
            Text(text = stringResource(R.string.user_switch_add))
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun UserListItemPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        UserListItem(
            user =
                UserEntity(
                    uid = 12345L,
                    username = "测试用户",
                    avatar = "",
                    auth = "",
                ),
            isCurrentUser = true,
            onClick = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddUserButtonPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        AddUserButton(onClick = {})
    }
}

// endregion
