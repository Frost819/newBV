package dev.frost819.newbv.app.ui.component

import dev.frost819.newbv.data.datastore.HomeTopNavItem

/**
 * 首页顶部 Tab 项。
 *
 * 将 data 模块的 [HomeTopNavItem] 映射为 app 层 [TopNavItem]，
 * 提供显示名称。
 *
 * @property item 原始 [HomeTopNavItem] 枚举。
 */
data class HomeTabItem(val item: HomeTopNavItem) : TopNavItem {
    override val displayName: String = when (item) {
        HomeTopNavItem.Dynamics -> "动态"
        HomeTopNavItem.Recommend -> "推荐"
        HomeTopNavItem.Popular -> "热门"
    }
}
