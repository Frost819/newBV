package dev.frost819.newbv.data.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore 偏好设置键定义。
 *
 * 集中管理所有偏好项的 [Preferences.Key]，键名沿用原版 BV 的缩写以保持兼容。
 * 键名规则：基本类型用语义缩写，历史遗留键名不变。
 *
 * 已删除（相对原版）：
 * - 代理相关：`enable_proxy`、`proxy_http_server`、`proxy_grpc_server`、`prefer_official_cdn`
 * - 播放器类型：`pt`（仅 Media3，无需选择）
 * - FPS 显示：`sf`（调试用，非用户功能）
 */
internal object PrefKeys {
    // ===== 账号 & 认证 =====
    val isLogin = booleanPreferencesKey("il")
    val uid = longPreferencesKey("uid")
    val sid = stringPreferencesKey("sid")
    val sessData = stringPreferencesKey("sd")
    val biliJct = stringPreferencesKey("bj")
    val uidCkMd5 = stringPreferencesKey("ucm")
    val tokenExpiredDate = longPreferencesKey("ted")
    val accessToken = stringPreferencesKey("access_token")
    val refreshToken = stringPreferencesKey("refresh_token")
    val buvid = stringPreferencesKey("random_buvid")
    val buvid3 = stringPreferencesKey("random_buvid3")
    val incognitoMode = booleanPreferencesKey("im")

    // ===== 网络 & API =====
    val apiType = intPreferencesKey("api_type")
    val interactionLog = booleanPreferencesKey("interaction_log")
    val crashReportEndpoint = stringPreferencesKey("crash_report_endpoint")

    // ===== 播放器 - 视频 =====
    val defaultQuality = intPreferencesKey("dq")
    val defaultVideoCodec = intPreferencesKey("dvc")
    val enableSoftwareVideoDecoder = booleanPreferencesKey("enable_software_video_decoder")
    val actionAfterPlay = intPreferencesKey("action_after_play")
    val playerCustomShortcuts = stringPreferencesKey("player_custom_shortcuts")
    val shortcutToast = booleanPreferencesKey("shortcut_toast")

    // ===== 播放器 - 音频 =====
    val defaultAudio = intPreferencesKey("da")
    val enableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")

    // ===== 播放器 - 弹幕 =====
    val defaultDanmakuTypes = stringPreferencesKey("ddts")
    val defaultDanmakuScale = floatPreferencesKey("dds2")
    val defaultDanmakuOpacity = floatPreferencesKey("ddo")
    val defaultDanmakuSpeedFactor = floatPreferencesKey("ddsf")
    val defaultDanmakuArea = floatPreferencesKey("dda")
    val defaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val danmakuLevelFilter = intPreferencesKey("danmaku_level_filter")

    // ===== 播放器 - 字幕 =====
    val defaultSubtitleFontSize = intPreferencesKey("dsfs")
    val defaultSubtitleBackgroundOpacity = floatPreferencesKey("dsbo")
    val defaultSubtitleBottomPadding = intPreferencesKey("dsbp")

    // ===== 播放器 - 界面 =====
    val defaultPlaySpeed = intPreferencesKey("dps")
    val showVideoInfo = booleanPreferencesKey("show_video_info")
    val showPersistentSeek = booleanPreferencesKey("show_persistent_seek")

    // ===== 应用界面 =====
    val density = floatPreferencesKey("density")
    val homeLeftNavItem = intPreferencesKey("home_left_nav")
    val firstHomeTopNavItem = intPreferencesKey("first_home_top_nav")
    val firstPersonalTopNavItem = intPreferencesKey("first_personal_top_nav")
    val showHotword = booleanPreferencesKey("shw")
    val themeMode = intPreferencesKey("theme_mode")

    // ===== 存储设置 =====
    val imageCacheThreshold = intPreferencesKey("image_cache_threshold")
    val otherCacheThreshold = intPreferencesKey("other_cache_threshold")
}
