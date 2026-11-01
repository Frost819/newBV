package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.app.entity.player.SponsorCategories
import dev.frost819.newbv.app.ui.component.settings.SettingSwitchListItem

/**
 * 空降助手「跳过分类」配置弹窗。
 *
 * 每个分类独立开关，实时写回持久化；分类变更对后续的片段拉取生效。
 *
 * @param enabledCategories 当前启用的分类集合
 * @param onDismiss 关闭弹窗回调
 * @param onCategoriesChanged 分类集合变化回调（含持久化后的最新值）
 */
@Composable
fun SponsorCategoriesDialog(
    enabledCategories: Set<String>,
    onDismiss: () -> Unit,
    onCategoriesChanged: (Set<String>) -> Unit,
) {
    // 本地镜像：开关切换即时回显，持久化由回调方负责
    var enabled by remember { mutableStateOf(enabledCategories) }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) { (windowInfo.containerSize.height * 0.7f).toDp() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "空降助手跳过分类",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "勾选的片段类型会在播放中自动跳过或静音；全部关闭时不请求片段数据",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .wrapContentHeight()
                            .heightIn(max = maxHeightDp)
                            .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(SponsorCategories.OPTIONS) { option ->
                        SettingSwitchListItem(
                            title = option.displayName,
                            supportText = option.category,
                            checked = option.category in enabled,
                            onCheckedChange = { checked ->
                                val next =
                                    if (checked) {
                                        enabled + option.category
                                    } else {
                                        enabled - option.category
                                    }
                                enabled = next
                                onCategoriesChanged(next)
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismiss,
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}
