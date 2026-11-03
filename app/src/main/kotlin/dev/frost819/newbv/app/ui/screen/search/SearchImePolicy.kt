package dev.frost819.newbv.app.ui.screen.search

/**
 * 搜索页输入法可见性策略。
 *
 * 搜索页默认使用自绘软键盘（A–Z / 1–9），打不了中文；打开「搜索使用系统输入法」后
 * 改为唤起系统 IME（可切中文、支持语音与手机遥控输入）。这里把「焦点变化时该显示
 * 还是收起 IME」的判定抽成纯函数，便于单元测试，也避免 Compose 里堆难以覆盖的分支。
 */
internal enum class SearchImeAction {
    /** 唤起系统输入法。 */
    Show,

    /** 收起系统输入法。 */
    Hide,

    /** 不做处理（未启用系统输入法时，完全交给自绘软键盘）。 */
    None,
}

internal object SearchImePolicy {
    /**
     * 根据开关与输入框焦点决定要对系统输入法做什么。
     *
     * @param useSystemIme 是否启用系统输入法（对应 `Prefs.searchUseSystemIme`）
     * @param isFocused 搜索框当前是否持有焦点
     * @return 需要执行的 [SearchImeAction]
     */
    fun onFocusChanged(
        useSystemIme: Boolean,
        isFocused: Boolean,
    ): SearchImeAction =
        when {
            !useSystemIme -> SearchImeAction.None
            isFocused -> SearchImeAction.Show
            else -> SearchImeAction.Hide
        }
}
