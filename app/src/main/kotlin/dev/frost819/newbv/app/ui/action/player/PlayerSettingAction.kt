package dev.frost819.newbv.app.ui.action.player

import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.danmaku.entity.DanmakuType

/**
 * 弹幕设置动作。
 *
 * 使用 sealed interface 实现类型安全的状态更新，避免在 ViewModel 中写大量 when 分支。
 */
sealed interface DanmakuSettingAction {
    data class SetScale(val scale: Float) : DanmakuSettingAction
    data class SetOpacity(val opacity: Float) : DanmakuSettingAction
    data class SetArea(val area: Float) : DanmakuSettingAction
    data class SetSpeedFactor(val factor: Float) : DanmakuSettingAction
    data class SetMaskEnabled(val enabled: Boolean) : DanmakuSettingAction
    data class SetEnabledTypes(val types: List<DanmakuType>) : DanmakuSettingAction
}

/**
 * 字幕设置动作。
 */
sealed interface SubtitleSettingAction {
    data class SetFontSize(val sp: Int) : SubtitleSettingAction
    data class SetOpacity(val opacity: Float) : SubtitleSettingAction
    data class SetBottomPadding(val dp: Int) : SubtitleSettingAction
}

/**
 * 媒体格式设置动作。
 */
sealed interface MediaProfileSettingAction {
    data class SetQuality(val qualityId: Int) : MediaProfileSettingAction
    data class SetVideoCodec(val codec: VideoCodec) : MediaProfileSettingAction
    data class SetAudio(val audio: Audio) : MediaProfileSettingAction
}
