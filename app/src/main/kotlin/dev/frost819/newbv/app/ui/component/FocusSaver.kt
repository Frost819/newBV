package dev.frost819.newbv.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * 焦点恢复器。
 *
 * 保存最后获得焦点的元素 key，在 composition 恢复后（如从详情页返回）
 * 通过 [FocusRequester] 重新请求焦点到对应元素。
 *
 * 统一使用 String key，适用于列表/网格（key = `"item_$index"`）
 * 和混合布局（key = `"cover"`、`"like"` 等）。
 *
 * 用法：
 * ```
 * val focusSaver = rememberFocusSaver()
 * focusSaver.RestoreFocus()
 * // 在可聚焦元素上：
 * Card(
 *     modifier = Modifier.focusSaverItem(focusSaver, "cover"),
 * )
 * ```
 */
class FocusSaver(
    private val savedKey: MutableState<String>,
) {
    private val requesters = mutableMapOf<String, FocusRequester>()

    /**
     * 获取指定 key 的 [FocusRequester]。
     * 每个 key 独立一个 requester，在组合恢复后仍有效。
     */
    fun focusRequesterFor(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    /**
     * 保存当前聚焦的 key。
     */
    fun saveFocusedKey(key: String) {
        savedKey.value = key
    }

    /**
     * 获取当前保存的 key（可能为空）。
     * 用于首次进入页面时判断是否需要手动聚焦默认元素。
     */
    fun savedKeyValue(): String = savedKey.value

    /**
     * 恢复焦点到之前保存的 key。
     *
     * 应在 composition 恢复后调用（通常在屏幕顶层调一次）。
     * 内置 50ms 延迟，确保 LazyRow/LazyColumn 子项完成组合后再请求焦点。
     */
    @Composable
    fun RestoreFocus() {
        LaunchedEffect(savedKey.value) {
            val key = savedKey.value
            if (key.isNotEmpty()) {
                requesters[key]?.let {
                    kotlinx.coroutines.delay(50)
                    runCatching { it.requestFocus() }
                }
            }
        }
    }
}

/**
 * 创建 [FocusSaver]，使用 [rememberSaveable] 持久化焦点 key。
 *
 * 焦点 key 在跨导航（页面移出再恢复 composition）和配置变更（如旋转）后保持。
 */
@Composable
fun rememberFocusSaver(): FocusSaver {
    val savedKey = rememberSaveable { mutableStateOf("") }
    return remember { FocusSaver(savedKey) }
}

// region Modifier 扩展

/**
 * 将 [FocusSaver] 绑定到可聚焦元素。
 *
 * 封装 `.focusRequester()` + `.onFocusChanged()` 两行 boilerplate 为一行。
 * 适用于列表/网格和混合布局中所有可聚焦元素的焦点追踪。
 *
 * 用法：
 * ```
 * SmallVideoCard(
 *     modifier = Modifier.focusSaverItem(focusSaver, "rcmd_$index"),
 *     ...
 * )
 * ```
 *
 * @param focusSaver 焦点恢复器。
 * @param key 元素的唯一标识（如 `"cover"`、`"tag_${tag.id}"`、`"rcmd_$index"`）。
 */
fun Modifier.focusSaverItem(
    focusSaver: FocusSaver,
    key: String,
): Modifier =
    this
        .focusRequester(focusSaver.focusRequesterFor(key))
        .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey(key) }

// endregion
