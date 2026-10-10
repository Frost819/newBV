package dev.frost819.newbv.app.ui.component.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 控制器自动隐藏倒计时的统一管理。
 *
 * 负责在控制器可见时计时，超时后回调 [onHide]；控制器隐藏时自动取消计时。
 * 返回的回调每次被调用都会重置倒计时，实现"用户交互自动续期"。
 *
 * 相比手动管理 `Job` 的实现，倒计时由 [LaunchedEffect] 派生：可见性变化时自动
 * 取消/重启，隐藏瞬间即回收，不会有计时器空转或残留引用。
 *
 * ```kotlin
 * val onUserInteraction =
 *     rememberControllerAutoHide(
 *         visible = showInfoController,
 *         onHide = { showInfoController = false },
 *     )
 * // 输入入口调用 onUserInteraction() 即可续期
 * ```
 *
 * @param visible 控制器当前是否可见。为 false 时不计时。
 * @param timeoutMs 无交互后自动隐藏的等待时长（毫秒）。
 * @param onHide 超时回调，用于隐藏控制器。
 * @return 交互续期回调，用户每次操作时调用即可重置倒计时。
 */
@Composable
fun rememberControllerAutoHide(
    visible: Boolean,
    timeoutMs: Long = CONTROLLER_AUTO_HIDE_TIMEOUT_MS,
    onHide: () -> Unit,
): () -> Unit {
    val interactionTick = remember { mutableIntStateOf(0) }
    // onHide 每次重组可能是新 lambda，用 rememberUpdatedState 保证 effect 内始终调用最新实现
    val currentOnHide by rememberUpdatedState(onHide)

    // 以 visible + 交互次数为 key：可见时启动倒计时，交互时重启，隐藏时取消
    LaunchedEffect(visible, interactionTick.intValue) {
        if (!visible) return@LaunchedEffect
        delay(timeoutMs)
        currentOnHide()
    }

    return remember { { interactionTick.intValue++ } }
}

/** 控制器默认自动隐藏时长（毫秒）。 */
const val CONTROLLER_AUTO_HIDE_TIMEOUT_MS: Long = 5_000L
