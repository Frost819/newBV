@file:Suppress("UNCHECKED_CAST")

package dev.frost819.newbv.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 偏好设置委托。
 *
 * 提供三级读写机制，兼顾性能与持久化：
 * 1. 内存缓存：[flow]（[MutableStateFlow]），读操作直接命中内存，同步无锁。
 * 2. 写：立即更新内存（UI 瞬间响应），再异步写 DataStore（不阻塞 UI）。
 * 3. 初始化：[Prefs.init] 阻塞读取 DataStore 首帧数据填充内存，之后长连接 collect 同步。
 *
 * 支持两种委托工厂：
 * - 基本类型（String/Int/Boolean/Float/Long）：直接存储。
 * - 对象类型（Enum/Date/列表等）：通过 [save]/[restore] 映射到基本类型。
 *
 * @param T 业务层类型（如枚举、Date）。
 * @param P DataStore 持久化类型（String/Int/Boolean/Float/Long）。
 * @param key DataStore 键。
 * @param defaultValue 默认值（业务类型）。
 * @param save 业务类型 → 持久化类型 转换函数，默认直接强转。
 * @param restore 持久化类型 → 业务类型 转换函数，默认直接强转。
 */
class PrefDelegate<T, P>(
    private val key: Preferences.Key<P>,
    private val defaultValue: T,
    private val save: (T) -> P = { it as P },
    private val restore: (P) -> T = { it as T },
) : ReadWriteProperty<Any?, T> {

    /** 内存缓存流，初始值为默认值的持久化形式。 */
    internal val flow: MutableStateFlow<Any?> = MutableStateFlow(save(defaultValue))

    /**
     * 读取当前值。
     *
     * 直接读内存缓存，若无持久化值则返回 [defaultValue]。
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val rawValue = flow.value as? P
        return if (rawValue != null) restore(rawValue) else defaultValue
    }

    /**
     * 写入新值。
     *
     * 1. 立即更新内存缓存，UI 瞬间响应。
     * 2. 异步写入 DataStore，不阻塞调用方。
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val persistValue = save(value)
        flow.value = persistValue
        Prefs.launchPersist {
            Prefs.dataStore.edit { prefs -> prefs[key] = persistValue }
        }
    }

    /** 重置内存缓存为默认值的持久化形式（用于 [Prefs.clear]）。 */
    internal fun resetToDefault() {
        flow.value = save(defaultValue)
    }
}

/**
 * 偏好设置管理器。
 *
 * 集中管理全部应用偏好项，通过 [PrefDelegate] 暴露读写属性。
 * 使用前必须在 Application.onCreate 调用 [init] 完成内存缓存初始化。
 *
 * 设计要点：
 * - [dataStore] 由 Hilt 注入（见 di 模块的 DataStoreModule）。
 * - 读操作同步无锁（命中内存），写操作异步持久化。
 * - [init] 阻塞读取首帧 DataStore 数据，再启动 collect 保持内存与磁盘同步。
 * - [init] 自动检查并生成 buvid / buvid3（若缺失）。
 *
 * 已删除（相对原版 BV）的偏好项：
 * - 代理相关：`enableProxy`、`proxyHttpServer`、`proxyGRPCServer`、`preferOfficialCdn`
 * - 播放器类型：`playerType`（仅 Media3，无需选择）
 * - FPS 显示：`showFps`（调试用，非用户功能）
 *
 * 使用示例：
 * ```
 * Prefs.init(dataStore)      // Application.onCreate
 * val isLogin = Prefs.isLogin // 任意位置读取
 * Prefs.isLogin = true        // 任意位置写入
 * ```
 *
 * @see PrefDelegate
 * @see PrefKeys
 * @see BuvidGenerator
 */
object Prefs {

    /** 持久化写入的协程作用域，使用 IO 调度器 + SupervisorJob（单次失败不影响后续）。 */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 所有委托的内存缓存流映射表，由 [registerDelegate] 填充。 */
    private val delegateMap = ConcurrentHashMap<Preferences.Key<*>, PrefDelegate<*, *>>()

    /** DataStore 实例，由 [init] 注入。 */
    @Volatile
    private var dataStoreRef: DataStore<Preferences>? = null

    /** DataStore 实例（仅供 [PrefDelegate] 内部异步写入使用）。 */
    internal val dataStore: DataStore<Preferences>
        get() = dataStoreRef ?: error("Prefs 未初始化，请先调用 Prefs.init(dataStore)")

    /** 初始化状态标志，防止重复初始化。 */
    @Volatile
    private var initialized = false

    /** 持久化写入互斥锁，避免并发 edit 冲突。 */
    private val persistMutex = Mutex()

    // ===== 委托工厂 =====

    /**
     * 创建基本类型委托。
     *
     * @param T 基本类型（String/Int/Boolean/Float/Long）。
     * @param key DataStore 键。
     * @param default 默认值。
     */
    private fun <T> pref(key: Preferences.Key<T>, default: T): PrefDelegate<T, T> =
        PrefDelegate(key, default).also { registerDelegate(key, it) }

    /**
     * 创建对象映射委托。
     *
     * @param T 业务类型。
     * @param P 持久化类型。
     * @param key DataStore 键。
     * @param default 默认值（业务类型）。
     * @param save 业务 → 持久化 转换。
     * @param restore 持久化 → 业务 转换。
     */
    private fun <T, P> pref(
        key: Preferences.Key<P>,
        default: T,
        save: (T) -> P,
        restore: (P) -> T,
    ): PrefDelegate<T, P> = PrefDelegate(key, default, save, restore).also { registerDelegate(key, it) }

    /** 注册委托到映射表，供 [init] 同步内存缓存使用。 */
    private fun <T, P> registerDelegate(key: Preferences.Key<P>, delegate: PrefDelegate<T, P>) {
        delegateMap[key] = delegate
    }

    // ===== 偏好项定义 =====

    // --- 账号 & 认证（PRD 7.8） ---

    /** 是否已登录。 */
    var isLogin by pref(PrefKeys.isLogin, false)
    /** 当前登录用户 UID。 */
    var uid by pref(PrefKeys.uid, 0L)
    /** SID（会话标识）。 */
    var sid by pref(PrefKeys.sid, "")
    /** SESSDATA Cookie 值。 */
    var sessData by pref(PrefKeys.sessData, "")
    /** bili_jct（csrf token）。 */
    var biliJct by pref(PrefKeys.biliJct, "")
    /** uid_ck_md5（登录校验值）。 */
    var uidCkMd5 by pref(PrefKeys.uidCkMd5, "")
    /** Token 过期时间。 */
    var tokenExpiredDate by pref(
        PrefKeys.tokenExpiredDate,
        Date(0),
        save = { it.time },
        restore = { Date(it) }
    )
    /** Access Token。 */
    var accessToken by pref(PrefKeys.accessToken, "")
    /** Refresh Token。 */
    var refreshToken by pref(PrefKeys.refreshToken, "")
    /** buvid（设备标识），首次启动自动生成。 */
    var buvid by pref(PrefKeys.buvid, "")
    /** buvid3（Web 端设备标识），首次启动自动生成。 */
    var buvid3 by pref(PrefKeys.buvid3, "")
    /** 无痕模式（不记录历史）。 */
    var incognitoMode by pref(PrefKeys.incognitoMode, false)

    // --- 网络 & API（PRD 7.5） ---

    /** 接口类型（Web/App）。 */
    var apiType by pref(
        PrefKeys.apiType,
        ApiType.Web,
        save = { it.ordinal },
        restore = { ApiType.fromOrdinal(it) }
    )
    /** 交互日志记录开关。 */
    var interactionLog by pref(PrefKeys.interactionLog, true)
    /** 崩溃上报端点（空字符串表示关闭）。 */
    var crashReportEndpoint by pref(PrefKeys.crashReportEndpoint, "")

    // --- 播放器 - 视频（PRD 7.1） ---

    /** 默认画质。 */
    var defaultQuality by pref(
        PrefKeys.defaultQuality,
        Resolution.R1080P,
        save = { it.code },
        restore = { Resolution.fromCode(it) }
    )
    /** 默认视频编码。 */
    var defaultVideoCodec by pref(
        PrefKeys.defaultVideoCodec,
        VideoCodec.AVC,
        save = { it.ordinal },
        restore = { VideoCodec.fromCode(it) }
    )
    /** 启用视频软解。 */
    var enableSoftwareVideoDecoder by pref(PrefKeys.enableSoftwareVideoDecoder, false)
    /** 播放结束动作。 */
    var actionAfterPlay by pref(
        PrefKeys.actionAfterPlay,
        ActionAfterPlay.PlayNext,
        save = { it.code },
        restore = { ActionAfterPlay.fromCode(it) }
    )
    /** 自定义播放快捷键（JSON 字符串）。 */
    var playerCustomShortcuts by pref(PrefKeys.playerCustomShortcuts, "")
    /** 快捷键触发 Toast 提示开关。 */
    var shortcutToast by pref(PrefKeys.shortcutToast, true)

    // --- 播放器 - 音频（PRD 7.1） ---

    /** 默认音频编码。 */
    var defaultAudio by pref(
        PrefKeys.defaultAudio,
        Audio.A192K,
        save = { it.code },
        restore = { Audio.fromCode(it) }
    )
    /** 启用 FFmpeg 音频软解。 */
    var enableFfmpegAudioRenderer by pref(PrefKeys.enableFfmpegAudioRenderer, false)

    // --- 播放器 - 弹幕（PRD 7.3） ---

    /** 默认弹幕类型（多选，逗号分隔的序号字符串）。 */
    var defaultDanmakuTypes by pref(
        PrefKeys.defaultDanmakuTypes,
        listOf(DanmakuType.All, DanmakuType.Rolling, DanmakuType.Top, DanmakuType.Bottom),
        save = { list -> list.joinToString(",") { it.ordinal.toString() } },
        restore = { str ->
            if (str.isEmpty()) emptyList()
            else str.split(",")
                .mapNotNull { runCatching { DanmakuType.entries[it.toInt()] }.getOrNull() }
        }
    )
    /** 默认弹幕大小。 */
    var defaultDanmakuScale by pref(PrefKeys.defaultDanmakuScale, 1.75f)
    /** 默认弹幕透明度。 */
    var defaultDanmakuOpacity by pref(PrefKeys.defaultDanmakuOpacity, 0.7f)
    /** 默认弹幕速度因子。 */
    var defaultDanmakuSpeedFactor by pref(PrefKeys.defaultDanmakuSpeedFactor, 1f)
    /** 默认弹幕显示区域。 */
    var defaultDanmakuArea by pref(PrefKeys.defaultDanmakuArea, 0.5f)
    /** 默认防遮挡蒙版开关。 */
    var defaultDanmakuMask by pref(PrefKeys.defaultDanmakuMask, false)
    /** 弹幕用户等级屏蔽（0-6，0 表示不屏蔽）。 */
    var danmakuLevelFilter by pref(PrefKeys.danmakuLevelFilter, 0)

    // --- 播放器 - 字幕（PRD 7.4） ---

    /** 默认字幕字号（SP）。 */
    var defaultSubtitleFontSize by pref(PrefKeys.defaultSubtitleFontSize, 24)
    /** 默认字幕背景透明度。 */
    var defaultSubtitleBackgroundOpacity by pref(PrefKeys.defaultSubtitleBackgroundOpacity, 0.4f)
    /** 默认字幕底部边距（DP）。 */
    var defaultSubtitleBottomPadding by pref(PrefKeys.defaultSubtitleBottomPadding, 12)

    // --- 播放器 - 界面（PRD 7.1/7.2） ---

    /** 默认播放速度。 */
    var defaultPlaySpeed by pref(
        PrefKeys.defaultPlaySpeed,
        PlaySpeed.X1,
        save = { it.code },
        restore = { PlaySpeed.fromCode(it) }
    )
    /** 显示视频详情页（关闭后点击直接播放）。 */
    var showVideoInfo by pref(PrefKeys.showVideoInfo, true)
    /** 显示常显进度条。 */
    var showPersistentSeek by pref(PrefKeys.showPersistentSeek, false)

    // --- 应用界面（PRD 7.2） ---

    /** 界面缩放密度（默认 1f，app 层 init 时按屏幕宽度重新计算）。 */
    var density by pref(PrefKeys.density, 1f)
    /** 启动页（左侧导航项）。 */
    var homeLeftNavItem by pref(
        PrefKeys.homeLeftNavItem,
        LeftNaviItem.Home,
        save = { it.ordinal },
        restore = { LeftNaviItem.fromOrdinal(it) }
    )
    /** 首页置顶 Tab。 */
    var firstHomeTopNavItem by pref(
        PrefKeys.firstHomeTopNavItem,
        HomeTopNavItem.Dynamics,
        save = { it.code },
        restore = { HomeTopNavItem.fromCode(it) }
    )
    /** 个人页置顶 Tab。 */
    var firstPersonalTopNavItem by pref(
        PrefKeys.firstPersonalTopNavItem,
        PersonalTopNavItem.ToView,
        save = { it.ordinal },
        restore = { PersonalTopNavItem.fromOrdinal(it) }
    )
    /** 显示搜索热词。 */
    var showHotword by pref(PrefKeys.showHotword, true)
    /** 主题模式（跟随系统/深色/浅色）。 */
    var themeMode by pref(
        PrefKeys.themeMode,
        ThemeMode.FollowSystem,
        save = { it.ordinal },
        restore = { ThemeMode.fromOrdinal(it) }
    )

    // --- 存储设置（PRD 7.6） ---

    /** 图片缓存阈值（MB）。 */
    var imageCacheThreshold by pref(PrefKeys.imageCacheThreshold, 500)
    /** 其他缓存阈值（MB）。 */
    var otherCacheThreshold by pref(PrefKeys.otherCacheThreshold, 200)

    // ===== 初始化 =====

    /**
     * 初始化偏好设置。
     *
     * **必须在 Application.onCreate 调用**，且在依赖 Prefs 的模块初始化之前。
     *
     * 执行步骤：
     * 1. 阻塞读取 DataStore 首帧数据，填充所有委托的内存缓存。
     * 2. 检查并生成缺失的 buvid / buvid3。
     * 3. 启动长连接 collect，DataStore 变化时同步内存缓存。
     *
     * @param dataStore DataStore 实例（由 Hilt 注入）。
     */
    fun init(dataStore: DataStore<Preferences>) {
        check(!initialized) { "Prefs 已经初始化，禁止重复调用 init()" }
        dataStoreRef = dataStore
        initialized = true

        val initialPrefs = runBlocking { dataStore.data.first() }
        updateMemoryCache(initialPrefs)
        checkAndInitBuvid(initialPrefs)

        scope.launch {
            dataStore.data.collect { preferences -> updateMemoryCache(preferences) }
        }
    }

    /** 将 DataStore 数据同步到所有委托的内存缓存。 */
    private fun updateMemoryCache(preferences: Preferences) {
        delegateMap.forEach { (key, delegate) ->
            if (preferences.contains(key)) {
                val value = preferences[key]
                (delegate as PrefDelegate<Any?, Any?>).flow.value = value
            }
        }
    }

    /** 检查 buvid / buvid3 是否缺失，缺失则自动生成并持久化。 */
    private fun checkAndInitBuvid(prefs: Preferences) {
        if (!prefs.contains(PrefKeys.buvid) || prefs[PrefKeys.buvid].isNullOrEmpty()) {
            buvid = BuvidGenerator.generateBuvid()
        }
        if (!prefs.contains(PrefKeys.buvid3) || prefs[PrefKeys.buvid3].isNullOrEmpty()) {
            buvid3 = BuvidGenerator.generateBuvid3()
        }
    }

    /**
     * 在持久化协程作用域中执行写操作。
     *
     * 使用互斥锁保证写入串行（DataStore 内部已串行，此处做二次保护避免并发 edit）。
     */
    internal fun launchPersist(block: suspend () -> Unit) {
        scope.launch {
            persistMutex.withLock { block() }
        }
    }

    /**
     * 重置所有偏好为默认值（测试用）。
     *
     * 清空 DataStore 并重置所有委托的内存缓存为默认值。
     */
    suspend fun clear() {
        check(initialized) { "Prefs 未初始化，无法 clear" }
        dataStore.edit { it.clear() }
        delegateMap.forEach { (_, delegate) -> delegate.resetToDefault() }
    }

    /** 获取指定偏好键对应的 [MutableStateFlow]（用于 Compose 观察偏好变化）。 */
    @Suppress("UNCHECKED_CAST")
    fun <T> flowOf(key: Preferences.Key<T>): MutableStateFlow<Any?>? =
        delegateMap[key]?.flow

    // ===== 测试辅助 =====

    /**
     * 重置初始化状态（仅测试用）。
     *
     * 允许在单元测试中重复 [init]：
     * 1. 重置初始化标志与 DataStore 引用。
     * 2. 将所有委托的内存缓存重置为默认值。
     * 3. 保留 [delegateMap] 不清空（委托本身在 object 加载时创建，无法重建）。
     *
     * 生产代码禁止调用。
     */
    internal fun resetForTesting() {
        initialized = false
        dataStoreRef = null
        delegateMap.forEach { (_, delegate) -> delegate.resetToDefault() }
    }
}
