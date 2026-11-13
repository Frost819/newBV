package dev.frost819.newbv.app.ui.component.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutAction
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutsStore
import dev.frost819.newbv.app.entity.player.shortcut.findShortcut

/**
 * 在播放器内弹窗（Dialog 独立窗口）中转发自定义快捷键的动作触发。
 *
 * `Dialog` 拥有独立 Window，按键事件不会回到播放器根节点的 `onPreviewKeyEvent`，
 * 导致「同一个键再次按下关闭弹窗」之类的动作失效，需要在弹窗根节点自行识别。
 * 只转发首次 KeyDown；KeyUp 与长按重复留给弹窗自身的导航逻辑，避免误吞方向键。
 *
 * @param onAction 命中绑定动作时回调，返回 true 表示已处理并消费该事件
 */
fun Modifier.playerCustomShortcutKeyInterceptor(onAction: (PlayerCustomShortcutAction) -> Boolean): Modifier =
    composed {
        val currentOnAction by rememberUpdatedState(onAction)
        onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount != 0) {
                return@onPreviewKeyEvent false
            }
            val shortcut =
                findShortcut(
                    keyCode = event.nativeKeyEvent.keyCode,
                    bindings = PlayerCustomShortcutsStore.getByKey(),
                ) ?: return@onPreviewKeyEvent false
            currentOnAction(shortcut.action)
        }
    }
