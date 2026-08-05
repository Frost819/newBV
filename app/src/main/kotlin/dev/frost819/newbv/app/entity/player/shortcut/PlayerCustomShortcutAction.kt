package dev.frost819.newbv.app.entity.player.shortcut

import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 播放器自定义快捷键动作。
 *
 * 分为两类：
 * - **简单动作**：无参数，如 [TogglePlayPause]、[PlayNext]
 * - **参数化动作**：带一个参数，如 [SetPlaybackSpeed]、[SetResolution]
 *
 * 参数化动作支持 Toggle 记忆：同一快捷键按两次可在当前值和目标值间切换。
 */
sealed interface PlayerCustomShortcutAction {
    data object ShowInfo : PlayerCustomShortcutAction
    data object OpenSettings : PlayerCustomShortcutAction
    data object OpenVideoList : PlayerCustomShortcutAction
    data object OpenRelatedVideos : PlayerCustomShortcutAction
    data object TogglePlayPause : PlayerCustomShortcutAction
    data object PlayPrevious : PlayerCustomShortcutAction
    data object PlayNext : PlayerCustomShortcutAction
    data object OpenVideoDetail : PlayerCustomShortcutAction
    data object OpenUpPage : PlayerCustomShortcutAction
    data object ToggleLoop : PlayerCustomShortcutAction
    data object ToggleDanmaku : PlayerCustomShortcutAction
    data object ToggleSubtitle : PlayerCustomShortcutAction
    data object TogglePersistentBottomProgress : PlayerCustomShortcutAction

    data class SetPlaybackSpeed(val speed: Float) : PlayerCustomShortcutAction
    data class SetResolution(val qualityId: Int) : PlayerCustomShortcutAction
    data class SetAudio(val audio: Audio) : PlayerCustomShortcutAction
    data class SetVideoCodec(val codec: VideoCodec) : PlayerCustomShortcutAction
    data class SetAspectRatio(val aspectRatio: VideoAspectRatio) : PlayerCustomShortcutAction
    data class SetDanmakuScale(val scale: Float) : PlayerCustomShortcutAction
    data class SetDanmakuOpacity(val opacity: Float) : PlayerCustomShortcutAction
    data class SetDanmakuSpeedFactor(val factor: Float) : PlayerCustomShortcutAction
    data class SetDanmakuArea(val area: Float) : PlayerCustomShortcutAction
    data class SetDanmakuMaskEnabled(val enabled: Boolean) : PlayerCustomShortcutAction
    data class SetSubtitleFontSize(val sp: Int) : PlayerCustomShortcutAction
    data class SetSubtitleBackgroundOpacity(val opacity: Float) : PlayerCustomShortcutAction
    data class SetSubtitleBottomPadding(val dp: Int) : PlayerCustomShortcutAction
}
