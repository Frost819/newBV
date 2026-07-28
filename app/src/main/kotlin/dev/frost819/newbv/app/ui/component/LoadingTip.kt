package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.frost819.newbv.R

/**
 * 加载中提示组件。
 *
 * 圆形进度指示器 + "加载中..." 文本，用于列表底部加载更多。
 */
@Composable
fun LoadingTip(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(id = R.string.loading),
            color = Color.White,
        )
    }
}
