package dev.frost819.newbv.biliapi.entity.pgc

import dev.frost819.newbv.biliapi.http.SeasonIndexType

data class PgcItem(
    var cover: String,
    var title: String,
    var subTitle: String,
    var seasonId: Int,
    var episodeId: Int,
    var seasonType: SeasonIndexType,
    var rating: String,
) {
    companion object {
        fun fromFeedSubItem(feedSubItem: dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedData.FeedSubItem): PgcItem {
            return PgcItem(
                cover = feedSubItem.cover,
                title = feedSubItem.title,
                subTitle = feedSubItem.subTitle,
                seasonId = feedSubItem.seasonId!!,
                episodeId = feedSubItem.episodeId,
                seasonType = SeasonIndexType.fromId(feedSubItem.seasonType!!),
                rating = feedSubItem.rating ?: "0",
            )
        }

        fun fromFeedSubItem(
            feedSubItem: dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedV3Data.FeedItem.FeedSubItem,
        ): PgcItem {
            return PgcItem(
                cover = feedSubItem.cover,
                title = feedSubItem.title,
                subTitle = feedSubItem.subTitle,
                seasonId = feedSubItem.seasonId!!,
                episodeId = feedSubItem.episodeId ?: feedSubItem.inline!!.epId,
                seasonType = SeasonIndexType.fromId(feedSubItem.seasonType!!),
                rating = feedSubItem.rating ?: "0",
            )
        }

        fun fromIndexResultItem(
            indexResultItem: dev.frost819.newbv.biliapi.http.entity.index.IndexResultData.IndexResultItem,
        ): PgcItem {
            return PgcItem(
                cover = indexResultItem.cover,
                title = indexResultItem.title,
                subTitle = indexResultItem.subTitle,
                seasonId = indexResultItem.seasonId,
                episodeId = indexResultItem.firstEp.epId,
                seasonType = SeasonIndexType.fromId(indexResultItem.seasonType),
                rating = indexResultItem.score,
            )
        }
    }
}
