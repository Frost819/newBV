package dev.frost819.newbv.biliapi.entity.ugc.region

import dev.frost819.newbv.biliapi.entity.ugc.UgcItem

@Deprecated("User region v2 instead")
data class UgcRegionListData(
    val items: List<UgcItem>,
    val next: UgcRegionPage
) {
    companion object {
        fun fromRegionDynamicList(data: dev.frost819.newbv.biliapi.http.entity.region.RegionDynamicList): UgcRegionListData {
            return UgcRegionListData(
                items = data.new.map { UgcItem.fromRegionDynamicListItem(it) },
                next = UgcRegionPage(data.cBottom)
            )
        }
    }
}