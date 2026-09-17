package dev.frost819.newbv.app.ui.navigation

import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs

private val logger = Loggers.get("VideoNavigation")

/**
 * 视频卡统一导航入口。
 *
 * 按优先级决定目标：
 * 1. **番剧**（有有效 EP ID）→ 番剧详情页（[PgcFeatureRoute]）
 * 2. **普通视频** + `forceDetail || showVideoInfo` → 视频详情页（[VideoDetailRoute]）
 * 3. **普通视频** + 直进播放 → 播放器（[VideoPlayerRoute]），并弹出已有播放器保证单例
 *
 * @param data 卡片数据（含 aid/cid/bvid/epid/title/cover）
 * @param forceDetail 强制进入详情页（用于卡片的“详情”操作，忽略 `showVideoInfo`）
 */
fun NavController.navigateFromVideoCard(
    data: VideoCardData,
    forceDetail: Boolean = false,
) {
    logger.info {
        "[CARD] click aid=${data.avid}, cid=${data.cid ?: 0L}, epid=${data.epid}, title=${data.title.take(80)}"
    }
    val epid = data.epid?.takeIf { it != 0 }
    when {
        epid != null -> {
            navigate(PgcFeatureRoute(epid = epid.toLong()))
        }

        forceDetail || Prefs.showVideoInfo -> {
            navigate(VideoDetailRoute(aid = data.avid, bvid = data.bvid))
        }

        else -> {
            navigate(
                VideoPlayerRoute(
                    aid = data.avid,
                    cid = data.cid ?: 0L,
                    bvid = data.bvid,
                    title = data.title,
                    cover = data.cover,
                ),
            ) {
                popUpTo<VideoPlayerRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

/**
 * 播放器内“打开视频详情”导航。
 *
 * 始终在播放器之上新建视频详情页并保留播放器，返回后可继续播放。
 * 无论播放器下层是什么页面（首页、详情页等），都再开一个详情页，
 * 避免回退到旧详情页而丢掉当前正在播放的播放器。
 *
 * @param aid 当前播放视频的 AV 号
 */
fun NavController.navigateToVideoDetailFromPlayer(aid: Long) {
    navigate(VideoDetailRoute(aid = aid)) {
        launchSingleTop = true
    }
}
