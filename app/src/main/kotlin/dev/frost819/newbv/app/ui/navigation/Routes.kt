package dev.frost819.newbv.app.ui.navigation

import kotlinx.serialization.Serializable

/*
 * Navigation 路由定义。
 *
 * 所有路由使用 `@Serializable` data class，通过 Navigation-Compose 类型安全路由导航。
 * 禁止使用字符串拼接 URL。
 */

// ── 主流程 ────────────────────────────────────────────────────────────

/** 首页（推荐/热门/分区）。 */
@Serializable
object HomeRoute

// ── 视频详情 ──────────────────────────────────────────────────────────

/** 视频详情页。 */
@Serializable
data class VideoDetailRoute(
    val aid: Long,
    val bvid: String = "",
    val epid: Long? = null,
)

// ── 播放器 ────────────────────────────────────────────────────────────

/** 视频播放器页面。 */
@Serializable
data class VideoPlayerRoute(
    val aid: Long,
    val cid: Long,
    val bvid: String = "",
    val epid: Long? = null,
    val title: String = "",
    val cover: String = "",
)

/** 番剧播放器页面。 */
@Serializable
data class SeasonPlayerRoute(
    val epid: Long,
    val sid: Long,
    val title: String = "",
    val cover: String = "",
)

// ── 搜索 ──────────────────────────────────────────────────────────────

/** 搜索结果页。 */
@Serializable
data class SearchResultRoute(
    val keyword: String,
)

// ── 直播 ──────────────────────────────────────────────────────────────

/** 直播播放器页面。 */
@Serializable
data class LivePlayerRoute(
    val roomId: Long,
    val title: String = "",
    val cover: String = "",
)

/** 直播分区列表页面。 */
@Serializable
data class LiveAreaRoute(
    val parentAreaId: Int,
    val areaId: Int = 0,
    val title: String,
)

/** 关注直播列表页面。 */
@Serializable
object LiveFollowRoute

// ── 用户 ──────────────────────────────────────────────────────────────

/** 用户空间页。 */
@Serializable
data class UserSpaceRoute(
    val mid: Long,
    val name: String? = null,
    val face: String? = null,
)

/** 关注列表页。 */
@Serializable
data class FollowRoute(
    val mid: Long,
)

// ── 番剧/PGC ──────────────────────────────────────────────────────────

/** 番剧详情页。 */
@Serializable
data class PgcFeatureRoute(
    val seasonId: Long = 0,
    val epid: Long? = null,
)

// ── 设置 ──────────────────────────────────────────────────────────────

/** 设置主页。 */
@Serializable
object SettingsRoute

/** 账号管理页。 */
@Serializable
object UserSwitchRoute

/** 编解码信息页。 */
@Serializable
object MediaCodecRoute

/** 日志查看页。 */
@Serializable
object LogViewerRoute

// ── 登录 ──────────────────────────────────────────────────────────────

/** 登录页面。 */
@Serializable
object LoginRoute
