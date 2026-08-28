package dev.frost819.newbv.app.ui.navigation

import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs

private val logger = Loggers.get("VideoCardNavigation")

/**
 * 根据用户偏好决定点击视频卡片后的导航行为。
 *
 * - `showVideoInfo = true`（默认）：跳转到详情页
 * - `showVideoInfo = false`：直接跳转到播放器（cid 由播放器内部加载详情获取）
 */
fun NavController.navigateFromVideoCard(data: VideoCardData) {
    logger.info {
        "[CARD] click aid=${data.avid}, cid=${data.cid ?: 0L}, title=${data.title.take(80)}"
    }
    if (Prefs.showVideoInfo) {
        navigate(VideoDetailRoute(aid = data.avid, bvid = data.bvid))
    } else {
        navigate(
            VideoPlayerRoute(
                aid = data.avid,
                cid = data.cid ?: 0L,
                bvid = data.bvid,
                epid = data.epid?.toLong(),
                title = data.title,
                cover = data.cover,
            ),
        )
    }
}
