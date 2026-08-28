package dev.frost819.newbv.danmaku.entity

/**
 * 弹幕滚动速度因子。
 *
 * 用于在播放器中调整弹幕滚动速度，以匹配视频播放速度变化。
 * 原始弹幕速度基于视频 1.0x 播放速度设计，调整此因子可保持视觉同步。
 *
 * @property factor 速度倍数（1.0 = 正常速度，1.5 = 弹幕速度加快 50%）
 */
enum class DanmakuSpeedFactor(
    val factor: Float,
) {
    /**
     * 1.5x 速度（弹幕滚动加快）
     */
    S1(1.5f),

    /**
     * 1.25x 速度
     */
    S2(1.25f),

    /**
     * 1.0x 正常速度
     */
    S3(1.0f),

    /**
     * 0.75x 速度
     */
    S4(0.75f),

    /**
     * 0.5x 速度（弹幕滚动减慢）
     */
    S5(0.5f),
    ;

    companion object {
        /**
         * 根据速度因子查找对应的枚举值。
         *
         * @param targetFactor 目标速度因子
         * @return 匹配的枚举值，找不到则返回 [S3]（1.0x）
         */
        fun fromFactor(targetFactor: Float): DanmakuSpeedFactor = entries.find { it.factor == targetFactor } ?: S3
    }
}
