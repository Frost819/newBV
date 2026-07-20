package dev.frost819.newbv.core.interaction

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 在 Composable 中获取当前 [InteractionMode]。
 *
 * 根据设备 touchscreen 配置自动判断，记住结果避免重复计算。
 *
 * @return 当前设备的交互模式。
 */
@Composable
fun rememberInteractionMode(): InteractionMode {
    val context = LocalContext.current
    return remember {
        InteractionModeDetector.detect(context.resources.configuration.touchscreen)
    }
}

/**
 * 在 Composable 中获取当前 [InteractionMode]，
 * 并根据模式执行不同逻辑。
 *
 * @param dPad D-Pad 模式下执行的内容。
 * @param touch Touch 模式下执行的内容。
 */
@Composable
fun InteractionModeSwitch(
    dPad: @Composable () -> Unit,
    touch: @Composable () -> Unit
) {
    when (rememberInteractionMode()) {
        InteractionMode.DPad -> dPad()
        InteractionMode.Touch -> touch()
    }
}
