package dev.frost819.newbv.danmaku.entity

/**
 * 弹幕显示类型。
 *
 * 用于播放器设置中过滤弹幕显示模式。
 *
 * @property modeValue 对应的 akdanmaku [com.kuaishou.akdanmaku.data.DanmakuItemData] mode 值
 */
enum class DanmakuType(val modeValue: Int) {
    /**
     * 显示所有弹幕类型
     */
    All(-1),

    /**
     * 顶部居中弹幕
     */
    Top(5),

    /**
     * 滚动弹幕（从右向左移动）
     */
    Rolling(1),

    /**
     * 底部居中弹幕
     */
    Bottom(4);

    companion object {
        /**
         * 将 akdanmaku 的 mode 值转换为对应的 [DanmakuType]。
         *
         * @param mode akdanmaku [com.kuaishou.akdanmaku.data.DanmakuItemData] 中的 mode 字段
         * @return 对应的 [DanmakuType]，如果 mode 不在已知范围内则返回 [All]
         */
        fun fromAkDanmakuMode(mode: Int): DanmakuType {
            return entries.find { it.modeValue == mode && it != All } ?: All
        }
    }
}
