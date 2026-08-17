package dev.frost819.newbv.app.ui.state.player

/**
 * 播放器一次性 UI 事件。
 *
 * 通过 SharedFlow 发送，UI 层消费后不再重复触发。
 */
sealed interface PlayerUiEffect {
    /** 播放器操作提示。 */
    data class ShowToast(val message: String) : PlayerUiEffect

    /** 视频播放结束，需要检查播放结束动作（暂停/下一集/退出）。 */
    data object PlayEnded : PlayerUiEffect

    /** 关闭播放器页面。 */
    data object FinishActivity : PlayerUiEffect
}
