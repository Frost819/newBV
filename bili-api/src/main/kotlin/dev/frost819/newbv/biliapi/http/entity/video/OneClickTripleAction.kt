package dev.frost819.newbv.biliapi.http.entity.video

import kotlinx.serialization.Serializable

@Serializable
data class OneClickTripleAction(
    val like: Boolean,
    val coin: Boolean,
    val fav: Boolean,
)
