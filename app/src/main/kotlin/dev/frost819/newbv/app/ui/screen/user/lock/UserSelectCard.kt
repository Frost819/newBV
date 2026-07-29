package dev.frost819.newbv.app.ui.screen.user.lock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 用户选择卡片（解锁页面使用）。
 *
 * @param user 用户数据。
 * @param alpha 透明度（未选中的用户降低透明度）。
 * @param onClick 点击回调。
 */
@Composable
internal fun UserSelectCard(
    user: UserEntity,
    alpha: Float,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.alpha(alpha),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            if (user.avatar.isNotEmpty()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = user.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = user.username.firstOrNull()?.toString() ?: "?")
                }
            }
            Text(
                text = user.username.ifEmpty { "UID: ${user.uid}" },
                style = MaterialTheme.typography.titleSmall,
            )
            if (user.lock.isNotBlank()) {
                Text(text = "🔒", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserSelectCardPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        UserSelectCard(
            user = UserEntity(
                uid = 12345L,
                username = "测试用户",
                avatar = "",
                auth = "",
                lock = "1234",
            ),
            alpha = 1f,
            onClick = {},
        )
    }
}
