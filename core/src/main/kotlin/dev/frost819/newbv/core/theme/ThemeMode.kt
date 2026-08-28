package dev.frost819.newbv.core.theme

/**
 * 主题模式。
 *
 * 控制应用使用深色、浅色还是跟随系统。
 * 通过 [isDark] 方法结合系统当前状态计算出最终是否使用深色主题。
 *
 * @property displayName 用于设置页显示的名称。
 */
enum class ThemeMode(
    val displayName: String,
) {
    /** 跟随系统暗色模式。 */
    FollowSystem("跟随系统"),

    /** 强制深色。 */
    Dark("深色"),

    /** 强制浅色。 */
    Light("浅色"),
    ;

    /**
     * 根据当前 [ThemeMode] 与系统是否深色，计算最终是否使用深色主题。
     *
     * @param systemIsDark 系统当前是否处于深色模式。
     * @return `true` 表示应使用深色主题。
     */
    fun isDark(systemIsDark: Boolean): Boolean =
        when (this) {
            FollowSystem -> systemIsDark
            Dark -> true
            Light -> false
        }

    companion object {
        /**
         * 从序号安全解析，用于 DataStore 持久化恢复。
         *
         * @param ordinal 序号，若越界则返回 [FollowSystem]。
         */
        fun fromOrdinal(ordinal: Int): ThemeMode = entries.getOrElse(ordinal) { FollowSystem }
    }
}
