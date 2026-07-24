package dev.frost819.newbv.danmaku.config

import dev.frost819.newbv.danmaku.entity.DanmakuType

/**
 * 弹幕状态数据类。
 *
 * 包含弹幕播放时的所有配置参数：
 * - [scale] 字体缩放（默认 1.0）
 * - [opacity] 透明度（0.0-1.0，默认 0.7）
 * - [area] 滚动弹幕显示区域比例（0.0-1.0，默认 0.5 = 下半屏）
 * - [speedFactor] 滚动速度因子（默认 1.0）
 * - [maskEnabled] 是否启用弹幕防遮挡蒙版（默认 false）
 * - [enabledTypes] 启用的弹幕类型列表（默认包含 Rolling + Top + Bottom）
 */
data class DanmakuState(
    val scale: Float = 1.0f,
    val opacity: Float = 0.7f,
    val area: Float = 0.5f,
    val speedFactor: Float = 1.0f,
    val maskEnabled: Boolean = false,
    val enabledTypes: List<DanmakuType> = listOf(
        DanmakuType.Rolling,
        DanmakuType.Top,
        DanmakuType.Bottom
    )
) {
    /**
     * 是否显示所有类型的弹幕。
     */
    val isShowAll: Boolean
        get() = enabledTypes.contains(DanmakuType.All) || enabledTypes.size == 3

    /**
     * 返回要传给 akdanmaku 的屏幕显示区域比例。
     * akdanmaku 的 screenPart: 1.0 = 全屏，0.5 = 下半屏，0.25 = 下四分之一。
     */
    val screenPart: Float
        get() = area

    /**
     * 返回要传给 akdanmaku 的 alpha 值（0.0-1.0）。
     */
    val alpha: Float
        get() = opacity

    /**
     * 返回要传给 akdanmaku 的字体缩放因子。
     */
    val textSizeScale: Float
        get() = scale
}
