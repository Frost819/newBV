package dev.frost819.newbv.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * 焦点恢复状态。
 *
 * 保存最后获得焦点的 item index，在 composition 恢复后（如从详情页返回）
 * 通过 [FocusRequester] 重新请求焦点到对应 item。
 *
 * 用法：
 * ```
 * val focusSaver = rememberFocusSaver()
 * // 在 grid item 中：
 * SmallVideoCard(
 *     modifier = Modifier.focusRequester(focusSaver.focusRequesterFor(index)),
 *     onFocusChange = { focusSaver.saveFocusedIndex(index) },
 * )
 * // 在 grid 外：
 * focusSaver.RestoreFocus()
 * ```
 */
class FocusSaver(
    private val savedIndex: androidx.compose.runtime.MutableState<Int>,
) {
    private val requesters = mutableMapOf<Int, FocusRequester>()

    /**
     * 获取指定 index 的 FocusRequester。
     * 每个 index 独立一个 requester，在组合恢复后仍有效。
     */
    fun focusRequesterFor(index: Int): FocusRequester {
        return requesters.getOrPut(index) { FocusRequester() }
    }

    /**
     * 保存当前聚焦的 index。
     */
    fun saveFocusedIndex(index: Int) {
        savedIndex.value = index
    }

    /**
     * 恢复焦点到之前保存的 index。
     * 应在 composition 恢复后调用。
     */
    @Composable
    fun RestoreFocus() {
        LaunchedEffect(savedIndex.value) {
            val index = savedIndex.value
            if (index >= 0) {
                val requester = requesters[index]
                if (requester != null) {
                    runCatching { requester.requestFocus() }
                }
            }
        }
    }
}

/**
 * 创建 [FocusSaver]，使用 [rememberSaveable] 持久化焦点 index。
 */
@Composable
fun rememberFocusSaver(): FocusSaver {
    val savedIndex = rememberSaveable { mutableIntStateOf(-1) }
    return remember { FocusSaver(savedIndex) }
}
