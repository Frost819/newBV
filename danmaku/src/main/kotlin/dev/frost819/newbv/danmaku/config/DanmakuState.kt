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
 *
 * 以及弹幕屏蔽/过滤相关参数：
 * - [blockColorful] 是否屏蔽彩色弹幕（默认 false）
 * - [blockRepeat] 是否屏蔽重复弹幕（默认 false）
 * - [blockSpecial] 是否屏蔽高级弹幕（默认 false）
 * - [blockByUserFilter] 是否应用账号云端屏蔽规则（关键词/正则/用户，默认 true）
 * - [aiCloudLevel] AI 智能云屏蔽等级（0 关闭，1-10 等级递增，直接作为 weight 阈值；默认 0）
 * - [userFilterRuleCount] 已加载的账号云端屏蔽规则条数（仅用于菜单展示）
 */
data class DanmakuState(
    val scale: Float = 1.0f,
    val opacity: Float = 0.7f,
    val area: Float = 0.5f,
    val speedFactor: Float = 1.0f,
    val maskEnabled: Boolean = false,
    val enabledTypes: List<DanmakuType> =
        listOf(
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom,
        ),
    val blockColorful: Boolean = false,
    val blockRepeat: Boolean = false,
    val blockSpecial: Boolean = false,
    val blockByUserFilter: Boolean = true,
    /**
     * AI 云屏蔽等级，直接作为丢弃阈值使用（weight 低于该值的弹幕被过滤）。
     *
     * 0 表示关闭；有效范围 1-10（与服务端 `aiLevel` 同尺度，weight 实测可达 11）。
     * UI 五档「关闭/弱/中/强/极强」只是 [AI_LEVEL_PRESETS] 的预设映射，见菜单实现。
     */
    val aiCloudLevel: Int = 0,
    /** 已加载的账号云端屏蔽规则数（关键词+正则+用户合计），0 表示未加载或无规则。 */
    val userFilterRuleCount: Int = 0,
) {
    companion object {
        /**
         * AI 云屏蔽 UI 五档（关闭/弱/中/强/极强）对应的阈值预设；状态内存的是阈值而非档位序号。
         *
         * 弱档取 2 而非 1：实测 weight 最小为 1，阈值 1 时 `weight < 1` 永不成立，
         * 会出现「弱档等同关闭」的零过滤问题。
         * 中档取 4 而非 5：真机实测阈值 5 在高弹幕密度视频会丢弃 97%，
         * 下调一级使中档落在弱与强之间。
         */
        val AI_LEVEL_PRESETS = intArrayOf(0, 2, 4, 7, 10)
    }

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
