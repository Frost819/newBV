package dev.frost819.newbv.app.entity.player

/**
 * 视频宽高比模式。
 *
 * 控制播放器画面的缩放方式。显示名称由 UI 层提供，避免实体类依赖 Android 资源。
 *
 * @property ratio 宽高比数值，[Default] 使用视频原始尺寸
 */
enum class VideoAspectRatio(val ratio: Float?) {
    /** 默认（使用视频原始宽高比）。 */
    Default(null),

    /** 4:3。 */
    FourToThree(4f / 3f),

    /** 16:9。 */
    SixteenToNine(16f / 9f);

    companion object {
        /** 从序号安全解析，越界返回 [Default]。 */
        fun fromOrdinal(ordinal: Int): VideoAspectRatio =
            entries.getOrElse(ordinal) { Default }
    }
}
