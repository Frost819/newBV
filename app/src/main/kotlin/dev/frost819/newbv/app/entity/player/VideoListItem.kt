package dev.frost819.newbv.app.entity.player

import dev.frost819.newbv.biliapi.entity.video.VideoPage

/**
 * 播放列表项。
 *
 * 表示播放器中可播放的视频条目，可以是 UGC 视频、合集分 P 或番剧分集。
 * 对于 UGC 合集，[ugcPages] 包含该视频的子分 P 列表。
 *
 * @property aid 视频 AV 号
 * @property cid 视频 CID
 * @property epid 番剧分集 ID，UGC 视频为 null
 * @property seasonId 番剧 season ID，UGC 视频为 null
 * @property title 视频标题
 * @property ugcPages UGC 合集内的分 P 列表，无分 P 时为 null
 */
data class VideoListItem(
    val aid: Long,
    val cid: Long,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val title: String,
    val ugcPages: List<VideoPage>? = null,
)
