package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 封装了 TV 焦点定轴逻辑的 [LazyVerticalGrid]。
 *
 * TV 端 D-Pad 导航时，聚焦的 item 会自动滚动到屏幕上方 [pivotFraction] 比例处，
 * 避免焦点被顶部/底部遮挡。默认 0.3f（屏幕上方 30%），符合 TV 端习惯。
 *
 * @param pivotFraction 焦点 item 在屏幕上的停留位置比例 (0.0 - 1.0)。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    pivotFraction: Float = 0.3f,
    content: LazyGridScope.() -> Unit,
) {
    val bringIntoViewSpec =
        remember(pivotFraction) {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float {
                    // 旧实现无论焦点项是否已经可见，都返回「滚动到 30% 处」的距离，
                    // 于是每按一次方向键都要跑一轮滚动动画，快速连按时动画互相叠加。
                    // 只有在焦点项进入上下安全区之外时才滚动，同屏内移动保持零滚动。
                    val safeTop = containerSize * 0.12f
                    val safeBottom = containerSize * 0.88f
                    if (offset >= safeTop && offset + size <= safeBottom) return 0f
                    return offset - containerSize * pivotFraction
                }
            }
        }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides bringIntoViewSpec,
    ) {
        LazyVerticalGrid(
            columns = columns,
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    }
}
