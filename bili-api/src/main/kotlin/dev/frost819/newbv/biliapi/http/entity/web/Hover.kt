package dev.frost819.newbv.biliapi.http.entity.web

import kotlinx.serialization.Serializable

@Serializable
data class Hover(
    val text: List<String>,
    val img: String
)