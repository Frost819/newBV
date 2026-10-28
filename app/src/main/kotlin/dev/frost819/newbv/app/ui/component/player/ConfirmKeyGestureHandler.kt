package dev.frost819.newbv.app.ui.component.player

/**
 * 播放器确认键手势解析器。
 *
 * 普通短按始终用于播放/暂停；长按在续播提示期间用于从头播放，否则打开设置菜单。
 */
internal class ConfirmKeyGestureHandler {
    private var longPressConsumed = false

    /**
     * 处理确认键按下事件。
     *
     * @param isLongPress 系统是否已识别成长按
     * @param resumeHintVisible 是否正在显示续播提示
     * @return 本次按下需要执行的动作；普通按下与长按重复事件返回 null
     */
    fun onKeyDown(
        isLongPress: Boolean,
        resumeHintVisible: Boolean,
    ): ConfirmKeyAction? {
        if (!isLongPress || longPressConsumed) return null
        longPressConsumed = true
        return if (resumeHintVisible) ConfirmKeyAction.ReplayFromStart else ConfirmKeyAction.OpenMenu
    }

    /**
     * 处理确认键抬起事件。
     *
     * @return 短按时返回播放/暂停；已处理长按则返回 null，避免长按结束又触发短按动作
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
    ReplayFromStart,
    OpenMenu,
}
