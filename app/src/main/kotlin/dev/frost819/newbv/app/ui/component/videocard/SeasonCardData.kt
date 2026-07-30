package dev.frost819.newbv.app.ui.component.videocard

/**
 * 番剧/影视卡片数据。
 *
 * 用于 [SeasonCard] 的展示数据，由 [dev.frost819.newbv.biliapi.entity.pgc.PgcItem] 转换而来。
 *
 * @property seasonId 番剧 season ID。
 * @property title 标题。
 * @property subTitle 副标题。
 * @property cover 封面 URL。
 * @property rating 评分（如 "9.8"），null 或 "0" 表示无评分。
 */
data class SeasonCardData(
    val seasonId: Int,
    val title: String,
    val subTitle: String? = null,
    val cover: String,
    val rating: String? = null,
) {
    /**
     * 是否有有效评分。
     */
    val hasRating: Boolean
        get() = !rating.isNullOrEmpty() && rating != "0"
}
