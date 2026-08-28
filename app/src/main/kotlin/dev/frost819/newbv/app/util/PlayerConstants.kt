package dev.frost819.newbv.app.util

import dev.frost819.newbv.biliapi.entity.ApiType

/**
 * 播放器相关常量。
 *
 * 包含 User-Agent、Referer 等 HTTP 请求头，以及 seek 增量等播放器行为常量。
 */
object PlayerConstants {
    /** Web API 模式的 User-Agent（Desktop Chrome，B 站 CDN 需要此 UA 才能正常播放）。 */
    const val WEB_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/104.0.0.0 Safari/537.36"

    /** App API 模式的 User-Agent（B 站客户端 UA）。 */
    const val APP_USER_AGENT = "Bilibili Freedoooooom/MarkII"

    /** Web API 模式的 Referer，用于 B 站防盗链验证。 */
    const val WEB_REFERER = "https://www.bilibili.com"

    /** 根据接口类型获取 User-Agent。 */
    fun getUserAgent(apiType: ApiType): String =
        when (apiType) {
            ApiType.Web -> WEB_USER_AGENT
            ApiType.App -> APP_USER_AGENT
        }

    /** 根据接口类型获取 Referer，App 模式返回 null。 */
    fun getReferer(apiType: ApiType): String? =
        when (apiType) {
            ApiType.Web -> WEB_REFERER
            ApiType.App -> null
        }

    /** Seek 加速基础增量（毫秒）。 */
    const val SEEK_BASE_INCREMENT_MS = 10_000L

    /** Seek 加速每级增量（毫秒），每 5 次连按增加一级。 */
    const val SEEK_STEP_INCREMENT_MS = 5_000L

    /** Seek 加速判定窗口（毫秒），两次按键间隔在此范围内视为连按。 */
    const val SEEK_ACCELERATION_WINDOW_MS = 200L

    /** Seek 执行延迟（毫秒），停止按键后等待多久执行 seek。 */
    const val SEEK_EXECUTE_DELAY_MS = 1_000L

    /** 心跳上报间隔（毫秒）。 */
    const val HEARTBEAT_INTERVAL_MS = 15_000L

    /** 心跳上报初始延迟（毫秒）。 */
    const val HEARTBEAT_INITIAL_DELAY_MS = 5_000L

    /** 播放器 detach 时心跳上报超时（毫秒）。 */
    const val HEARTBEAT_DETACH_TIMEOUT_MS = 3_000L

    /** 进度条更新间隔（毫秒）。 */
    const val SEEKER_UPDATE_INTERVAL_MS = 100L

    /** 时钟更新间隔（毫秒）。 */
    const val CLOCK_UPDATE_INTERVAL_MS = 1_000L

    /** 倒计时默认时长（毫秒）：回到开头、跳下集、试看提示。 */
    const val COUNTDOWN_DURATION_MS = 5_000L

    /** Controller 信息栏自动隐藏延迟（毫秒）。 */
    const val CONTROLLER_AUTO_HIDE_MS = 5_000L

    /** 播放器快捷键提示显示时长（毫秒）。 */
    const val PLAYER_TIP_DURATION_MS = 1_500L

    /** 返回键退出确认窗口（毫秒）。 */
    const val BACK_EXIT_WINDOW_MS = 3_000L
}
