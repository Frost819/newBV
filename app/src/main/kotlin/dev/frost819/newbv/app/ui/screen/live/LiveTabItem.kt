package dev.frost819.newbv.app.ui.screen.live

import dev.frost819.newbv.app.ui.component.TopNavItem

enum class LiveTabItem(val displayLabel: String) : TopNavItem {
    Recommend("推荐"),
    Area("分区");

    override val displayName: String get() = displayLabel
}
