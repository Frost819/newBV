package dev.frost819.newbv.app.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.frost819.newbv.app.util.ToastUtils

/**
 * 双击返回退出/返回的状态持有器。
 *
 * 在 [intervalMs] 时间内按两次返回键才会触发 [onExit]，
 * 第一次按下时显示 [message] Toast 提示。
 *
 * 返回一个 lambda，既可用于 [BackHandler] 也可传递给子组件
 * （如播放器控制器的 D-Pad Back 键处理）。
 *
 * 典型用法：
 * ```kotlin
 * val handleBack = rememberDoublePressExit(
 *     onExit = { navController.popBackStack() },
 *     message = "再按一次退出播放",
 * )
 * BackHandler { handleBack() }
 * VideoPlayerController(onExit = handleBack, ...)
 * ```
 *
 * @param onExit 双击间隔内触发的退出动作。
 * @param message 第一次按下时显示的 Toast 文案。
 * @param intervalMs 双击间隔（毫秒），默认 3000。
 *
 * @return 返回键处理 lambda，内部管理计时器与 Toast。
 */
@Composable
fun rememberDoublePressExit(
    onExit: () -> Unit,
    message: String,
    intervalMs: Long = 3000L,
): () -> Unit {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    return {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < intervalMs) {
            onExit()
        } else {
            lastBackPressTime = now
            ToastUtils.show(context, message)
        }
    }
}
