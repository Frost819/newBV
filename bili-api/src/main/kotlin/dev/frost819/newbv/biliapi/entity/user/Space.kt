package dev.frost819.newbv.biliapi.entity.user

import bilibili.app.space.v1.BiliSpaceVideo
import dev.frost819.newbv.biliapi.http.util.smartDate
import dev.frost819.newbv.biliapi.http.util.toSmartDate

data class SpaceVideoData(
    val videos: List<SpaceVideo>,
    val page: SpaceVideoPage,
) {
    companion object {
        fun fromWebSpaceVideoData(webSpaceVideoData: dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData) =
            SpaceVideoData(
                videos =
                    webSpaceVideoData.list
                        ?.vlist
                        ?.map { SpaceVideo.fromSpaceVideoItem(it) }
                        ?: emptyList(),
                page =
                    SpaceVideoPage(
                        hasNext =
                            (webSpaceVideoData.page?.count ?: 0)
                                > (
                                    (webSpaceVideoData.page?.pageNumber ?: 0) *
                                        (webSpaceVideoData.page?.pageSize ?: 0)
                                ),
                        nextWebPageSize = webSpaceVideoData.page?.pageSize ?: 0,
                        nextWebPageNumber = (webSpaceVideoData.page?.pageNumber ?: 0) + 1,
                    ),
            )

        fun fromAppSpaceVideoData(appSpaceVideoData: dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData) =
            SpaceVideoData(
                videos =
                    appSpaceVideoData.item
                        .map { SpaceVideo.fromSpaceVideoItem(it) },
                page =
                    SpaceVideoPage(
                        hasNext = appSpaceVideoData.hasNext,
                        lastAvid =
                            appSpaceVideoData.item
                                .lastOrNull()
                                ?.param
                                ?.toLong() ?: 0,
                    ),
            )

        /** 将 App gRPC `Space.Archive` 响应转换为用户空间视频数据。 */
        fun fromGrpcSpaceVideoData(
            items: List<BiliSpaceVideo>,
            page: SpaceVideoPage,
        ) = SpaceVideoData(
            videos = items.map { SpaceVideo.fromGrpcSpaceVideoItem(it) },
            page = page,
        )
    }
}

data class SpaceVideo(
    val aid: Long,
    val bvid: String,
    val title: String,
    val cover: String,
    val author: String,
    val duration: Int,
    val play: Int,
    val danmaku: Int,
    val pubTime: String?,
    val playbackPosition: Int = 0,
) {
    companion object {
        fun fromSpaceVideoItem(
            spaceVideoItem: dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem.VListItem,
        ) = SpaceVideo(
            aid = spaceVideoItem.aid,
            bvid = spaceVideoItem.bvid,
            title = spaceVideoItem.title,
            cover = spaceVideoItem.pic,
            author = spaceVideoItem.author,
            duration = convertMmSsToSeconds(spaceVideoItem.length),
            play = spaceVideoItem.play,
            danmaku = spaceVideoItem.videoReview,
            pubTime = spaceVideoItem.created.toSmartDate(),
            playbackPosition = spaceVideoItem.playbackPosition,
        )

        /** 将 App gRPC 用户空间视频转换为领域模型。 */
        fun fromGrpcSpaceVideoItem(item: BiliSpaceVideo) =
            SpaceVideo(
                aid = item.param.toLongOrNull() ?: 0L,
                bvid = item.bvid,
                title = item.title,
                cover = item.cover,
                author = "",
                duration = item.duration.toInt(),
                play = item.play,
                danmaku = item.danmaku.toInt(),
                pubTime = item.ctime.toInt().smartDate,
            )

        fun fromSpaceVideoItem(
            spaceVideoItem: dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData.SpaceVideoItem,
        ) = SpaceVideo(
            aid = spaceVideoItem.param.toLong(),
            bvid = spaceVideoItem.bvid ?: "",
            title = spaceVideoItem.title,
            cover = spaceVideoItem.cover,
            author = spaceVideoItem.author ?: "",
            duration = spaceVideoItem.duration,
            play = spaceVideoItem.play,
            danmaku = spaceVideoItem.danmaku,
            pubTime = spaceVideoItem.ctime.smartDate,
        )
    }
}

private fun convertMmSsToSeconds(time: String): Int {
    val parts = time.split(":")
    val minutes = parts[0].toInt()
    val seconds = parts[1].toInt()
    return (minutes * 60) + seconds
}

enum class SpaceVideoOrder(
    val value: String,
) {
    PubDate("pubdate"),
    Click("click"),
}

data class SpaceVideoPage(
    val hasNext: Boolean = true,
    // web
    val nextWebPageSize: Int = 20,
    val nextWebPageNumber: Int = 1,
    // app
    val lastAvid: Long = 0,
)
