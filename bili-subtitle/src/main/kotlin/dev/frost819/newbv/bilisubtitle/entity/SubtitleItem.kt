package dev.frost819.newbv.bilisubtitle.entity

data class SubtitleItem(
    val from: Timestamp,
    val to: Timestamp,
    val content: String,
) {
    fun isShowing(time: Long) = from.totalMills <= time && to.totalMills >= time
}
