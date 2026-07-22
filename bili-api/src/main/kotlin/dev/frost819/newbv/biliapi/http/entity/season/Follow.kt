package dev.frost819.newbv.biliapi.http.entity.season

import kotlinx.serialization.Serializable

@Serializable
data class SeasonFollowData(
    val fmid: Int,
    val relation: Boolean,
    val status: Int,
    val toast: String
)