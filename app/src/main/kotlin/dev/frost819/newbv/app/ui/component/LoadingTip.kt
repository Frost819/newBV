package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.R

/**
 * 加载中提示组件。
 *
 * 圆形进度指示器 + "加载中…" 文本，用于列表底部加载更多。
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
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 加载失败提示组件。
 *
 * 显示 "加载失败" 文本，用于网络超时或请求报错场景。
 */
@Composable
fun ErrorTip(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.load_failed),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * 列表底部统一提示组件。
 *
 * 封装加载/错误/无更多三种状态的判断逻辑，供所有分页列表页复用。
 *
 * 状态优先级：loading > error > noMore。
 * - [isLoading] 为 true 时显示 [LoadingTip]。
 * - [isError] 为 true 时显示 [ErrorTip]。
 * - [hasMore] 为 false 且列表非空时显示 "没有更多了捏"。
 *
 * @param isLoading 是否正在加载。
 * @param isError 是否加载失败（超时或报错）。
 * @param hasMore 是否还有更多数据。
 * @param itemsIsEmpty 列表是否为空（空列表时不显示"没有更多"）。
 */
@Composable
fun ListFooterTip(
    isLoading: Boolean,
    isError: Boolean,
    hasMore: Boolean,
    itemsIsEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingTip()
            }
        }

        isError -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ErrorTip()
            }
        }

        !hasMore && !itemsIsEmpty -> {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "没有更多了捏",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun LoadingTipPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        LoadingTip()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorTipPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        ErrorTip()
    }
}

@Preview(showBackground = true)
@Composable
private fun ListFooterTipLoadingPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        ListFooterTip(isLoading = true, isError = false, hasMore = true, itemsIsEmpty = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun ListFooterTipErrorPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        ListFooterTip(isLoading = false, isError = true, hasMore = true, itemsIsEmpty = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun ListFooterTipNoMorePreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        ListFooterTip(isLoading = false, isError = false, hasMore = false, itemsIsEmpty = false)
    }
}

// endregion
