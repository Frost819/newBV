package dev.frost819.newbv.app.ui.navigation

import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 根据用户偏好决定点击视频卡片后的导航行为。
 *
 * - `showVideoInfo = true`（默认）：跳转到详情页
 * - `showVideoInfo = false` 且 cid 已知：直接跳转到播放器
 * - `showVideoInfo = false` 但 cid 未知：仍跳转详情页（需要获取 cid）
 */
fun NavController.navigateFromVideoCard(data: VideoCardData) {
    if (Prefs.showVideoInfo || data.cid == null) {
        navigate(VideoDetailRoute(aid = data.avid))
    } else {
        navigate(
            VideoPlayerRoute(
                aid = data.avid,
                cid = data.cid,
                epid = data.epid?.toLong(),
                title = data.title,
                cover = data.cover,
            )
        )
    }
}
