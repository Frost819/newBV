package dev.frost819.newbv.app.entity.player.shortcut

/**
 * 查找按键绑定的自定义快捷键。
 *
 * 播放器根节点与播放器内弹窗共用该查找逻辑，保证「禁用键过滤 + 键码匹配」一致；
 * 事件消费策略（KeyUp、长按重复是否吞掉）由各调用方按自己的窗口语义决定。
 *
 * @param keyCode Android 键码
 * @param bindings 键码到快捷键绑定的映射
 * @return 命中的绑定；未绑定或键码被禁用时返回 null
 */
internal fun findShortcut(
    keyCode: Int,
    bindings: Map<Int, PlayerCustomShortcut>,
): PlayerCustomShortcut? = bindings[keyCode]?.takeIf { PlayerCustomShortcutKeys.isAllowedKeyCode(keyCode) }
