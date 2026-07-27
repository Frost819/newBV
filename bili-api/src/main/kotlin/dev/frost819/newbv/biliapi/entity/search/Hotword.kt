package dev.frost819.newbv.biliapi.entity.search

data class Hotword(
    val keyword: String,
    val showName: String,
    val icon: String?,
) {
    companion object {
        fun fromHttpWebHotword(hotword: dev.frost819.newbv.biliapi.http.entity.search.Hotword) =
            Hotword(
                keyword = hotword.keyword,
                showName = hotword.showName,
                icon = hotword.icon,
            )

        fun fromHttpAppSquareDataItem(
            squareDataItem: dev.frost819.newbv.biliapi.http.entity.search.AppSearchSquareData.SquareData.SquareDataItem,
        ) = Hotword(
            keyword = squareDataItem.keyword ?: "",
            showName = squareDataItem.showName ?: "",
            icon = squareDataItem.icon,
        )

        fun fromHttpAppSearchTrendingHotword(
            hotword: dev.frost819.newbv.biliapi.http.entity.search.SearchTendingData.Hotword,
        ) = Hotword(
            keyword = hotword.keyword,
            showName = hotword.showName,
            icon = hotword.icon,
        )
    }
}
