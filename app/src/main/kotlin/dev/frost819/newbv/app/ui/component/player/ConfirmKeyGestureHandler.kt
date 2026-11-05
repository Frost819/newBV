package dev.frost819.newbv.app.ui.component.player

/**
 * 播放器确认键手势解析器。
 *
 * 短按始终播放/暂停；长按保持原有行为，打开设置菜单。
 */
internal class ConfirmKeyGestureHandler {
    private var longPressConsumed = false

    /**
     * 处理确认键按下事件。
     *
     * @param isLongPress 系统是否已识别成长按
     * @return 长按首次触发时返回打开菜单；普通按下和重复长按事件返回 null
     */
    fun onKeyDown(isLongPress: Boolean): ConfirmKeyAction? {
        if (!isLongPress || longPressConsumed) return null
        longPressConsumed = true
        return ConfirmKeyAction.OpenMenu
    }

    /**
     * 处理确认键抬起事件。
     *
     * @return 普通短按时返回播放/暂停；长按已触发菜单时返回 null
     */
    fun onKeyUp(): ConfirmKeyAction? {
        if (longPressConsumed) {
            longPressConsumed = false
            return null
        }
        return ConfirmKeyAction.TogglePlayback
    }
}

/** 确认键手势对应的播放器动作。 */
internal enum class ConfirmKeyAction {
    TogglePlayback,
    OpenMenu,
}
