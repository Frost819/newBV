package dev.frost819.newbv.app.ui.component.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable

/**
 * 搜索关键词列表项。
 *
 * 显示关键词文本，点击触发搜索。
 *
 * @param keyword 关键词文本
 * @param onClick 点击回调
 * @param trailingIcon 尾部图标（删除模式下使用）
 */
@Composable
fun SearchKeyword(
    modifier: Modifier = Modifier,
    keyword: String,
    onClick: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    DenseListItem(
        // tv-material3 的 DenseListItem.onClick 只处理 D-Pad 确认键；
        // 热搜、搜索建议、搜索历史这三处列表以前触屏点了没有反应。
        modifier = modifier.touchClickable(onClick = onClick),
        selected = false,
        onClick = onClick,
        headlineContent = {
            Text(
                text = keyword,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = trailingIcon,
    )
}
