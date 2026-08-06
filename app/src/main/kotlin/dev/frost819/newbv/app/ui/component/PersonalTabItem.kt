package dev.frost819.newbv.app.ui.component

import dev.frost819.newbv.data.datastore.PersonalTopNavItem

/**
 * 个人页顶部 Tab 项。
 *
 * 将 data 模块的 [PersonalTopNavItem] 映射为 app 层 [TopNavItem]，
 * 提供显示名称。
 *
 * @property item 原始 [PersonalTopNavItem] 枚举。
 */
data class PersonalTabItem(val item: PersonalTopNavItem) : TopNavItem {
    override val displayName: String = when (item) {
        PersonalTopNavItem.ToView -> "稍后再看"
        PersonalTopNavItem.History -> "历史"
        PersonalTopNavItem.Favorite -> "收藏"
        PersonalTopNavItem.FollowingSeason -> "追番"
    }
}
