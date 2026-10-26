package dev.frost819.newbv.biliapi.entity.video

import bilibili.app.view.v1.authorOrNull
import dev.frost819.newbv.biliapi.entity.user.Author

/**
 * 相关视频条目。
 *
 * @property pubdate 稿件发布时间（秒级时间戳）；0 表示未知。
 *   仅 Web 接口返回该字段；App gRPC 的 `Relate` 不含发布时间，此时为 0（卡片不显示日期）。
 */
data class RelatedVideo(
    val aid: Long,
    val cid: Long,
    val cover: String,
    val title: String,
    val duration: Int,
    val author: Author?,
    val jumpToSeason: Boolean,
    val epid: Int?,
    val view: Int,
    val danmaku: Int,
    val pubdate: Long = 0L,
) {
    companion object {
        fun fromRelate(relate: bilibili.app.view.v1.Relate): RelatedVideo {
            val epId = parseEpIdFromUri(relate.uri).takeIf { relate.goto.needJumpToSeason() }
            return RelatedVideo(
                aid = relate.aid,
                cid = relate.cid,
                cover = relate.pic,
                title = relate.title,
                duration = relate.duration.toInt(),
                author =
                    relate.authorOrNull?.let { Author.fromAuthor(it) }
                        ?: relate.desc?.let { Author(0, it, "") },
                jumpToSeason = epId != null,
                epid = epId,
                view = relate.stat.view,
                danmaku = relate.stat.danmaku,
            )
        }

        fun fromRelate(relate: dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo): RelatedVideo {
            val epId = parseEpIdFromUri(relate.redirectUrl)
            return RelatedVideo(
                aid = relate.aid,
                cid = relate.cid,
                cover = relate.pic,
                title = relate.title,
                duration = relate.duration,
                author = relate.owner.let { Author.fromVideoOwner(it) },
                jumpToSeason = epId != null,
                epid = epId,
                view = relate.stat.view,
                danmaku = relate.stat.danmaku,
                pubdate = relate.pubdate.toLong(),
            )
        }
    }
}

private val EP_ID_REGEX = Regex("""/ep(\d+)""")

/**
 * 从跳转链接 / uri 中解析番剧 EP ID。
 *
 * 兼容 `https://www.bilibili.com/bangumi/play/ep1364037?theme=movie`、
 * `.../ep284272/`、`bilibili://.../ep12345` 等格式；解析失败返回 null。
 */
internal fun parseEpIdFromUri(url: String?): Int? {
    val match = url?.let { EP_ID_REGEX.find(it) } ?: return null
    return match.groupValues.getOrNull(1)?.toIntOrNull()
}

private fun String.needJumpToSeason() = this.contains("bangumi_ep") || this.contains("special")
