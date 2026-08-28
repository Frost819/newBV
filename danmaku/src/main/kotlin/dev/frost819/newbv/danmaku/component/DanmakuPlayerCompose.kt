package dev.frost819.newbv.danmaku.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView

/**
 * 弹幕播放器 Compose 封装组件。
 *
 * 将 akdanmaku 的 [DanmakuPlayer] + [DanmakuView] 封装为 Compose 可用组件。
 * 通过 [AndroidView] 将 [DanmakuView] 嵌入 Compose 树，并通过 [update] 回调
 * 将 [DanmakuPlayer] 与视图绑定。
 *
 * @param modifier 应用于 [DanmakuView] 的 Modifier（默认铺满父容器）
 * @param danmakuPlayer 弹幕播放器实例，由外部 ViewModel 创建和管理。
 *                      传入 null 时不进行任何操作。
 *
 * @see com.kuaishou.akdanmaku.ui.DanmakuPlayer
 * @see com.kuaishou.akdanmaku.ui.DanmakuView
 */
@Composable
fun DanmakuPlayerCompose(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer?,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            DanmakuView(context)
        },
        update = { danmakuView ->
            danmakuPlayer?.bindView(danmakuView)
        },
        onRelease = { danmakuView ->
            if (danmakuView.danmakuPlayer === danmakuPlayer) {
                danmakuView.danmakuPlayer = null
            }
        },
    )
}
