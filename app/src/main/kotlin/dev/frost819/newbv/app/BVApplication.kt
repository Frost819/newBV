package dev.frost819.newbv.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import dev.frost819.newbv.app.data.danmaku.DanmakuFilterRepository
import dev.frost819.newbv.app.network.HttpServer
import dev.frost819.newbv.app.util.CacheManager
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.biliapi.repositories.ChannelRepository
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.CrashUploader
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

/**
 * new BV 应用入口。
 *
 * 初始化顺序：
 * 1. Hilt 依赖注入（由 [HiltAndroidApp] 自动处理）
 * 2. [Prefs] 偏好设置初始化（阻塞读取 DataStore 首帧）
 * 3. [CrashHandler] 全局崩溃处理（通过 Hilt 注入，构造时自动 install）
 * 4. [HttpServer] 本地日志管理服务器（通过 Hilt 注入，按需启动）
 */
@HiltAndroidApp
class BVApplication : Application() {
    private val logger = Loggers.get("BVApplication")

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var crashUploader: CrashUploader

    @Inject
    lateinit var httpServer: HttpServer

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var channelRepository: ChannelRepository

    @Inject
    lateinit var danmakuFilterRepository: DanmakuFilterRepository

    /**
     * 播放器与 CDN 测速共用的 OkHttpClient。
     *
     * 用 [Provider] 延迟解析：client 构建包含系统 CA 库读取与 SSLContext 初始化，
     * 放在启动时的 IO 线程预热，避免首次进播放器时在主线程做这件事。
     */
    @Inject
    lateinit var playerHttpClientProvider: Provider<OkHttpClient>

    override fun onCreate() {
        super.onCreate()

        Prefs.init(dataStore)

        val buvid3 = Prefs.buvid3
        val deviceCookies = Prefs.deviceCookies
        val sessData = Prefs.sessData
        val biliJct = Prefs.biliJct
        val mid = Prefs.uid.takeIf { it > 0 }
        val accessToken = Prefs.accessToken

        authRepository.buvid3 = buvid3
        authRepository.deviceCookies = deviceCookies
        authRepository.sessionData = sessData
        authRepository.biliJct = biliJct
        authRepository.mid = mid
        authRepository.accessToken = accessToken
        BiliHttpApi.init(
            buvid3 = buvid3,
            deviceCookies = deviceCookies,
            sessData = sessData,
            biliJct = biliJct,
            mid = mid,
            accessToken = accessToken,
        )

        // 账号云端弹幕屏蔽规则：启动时后台预热，播放器/直播只读缓存（登录态由仓库自行判断）
        danmakuFilterRepository.prefetch()

        if (accessToken.isNotBlank() && Prefs.buvid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                channelRepository.initDefaultChannel(accessToken, Prefs.buvid)
            }
        }

        if (!Prefs.buvid3FromSpi) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val spiResult = BiliHttpApi.fetchBuvid3FromSpi()
                if (spiResult != null) {
                    Prefs.buvid3 = spiResult.buvid3
                    Prefs.deviceCookies = spiResult.deviceCookies
                    Prefs.buvid3FromSpi = true
                    authRepository.buvid3 = spiResult.buvid3
                    authRepository.deviceCookies = spiResult.deviceCookies
                    BiliHttpApi.buvid3 = spiResult.buvid3
                    BiliHttpApi.deviceCookies = spiResult.deviceCookies
                }
            }
        }

        @Suppress("UNUSED_EXPRESSION")
        crashHandler

        // 已启用上传时，尝试上传上次崩溃未发送的日志；开关状态由 CrashUploader 实时读取
        if (crashUploader.canUpload()) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                crashUploader.uploadPendingCrashLogs()
            }
        }

        logger.info { "Application started" }

        // 图片磁盘缓存上限：开启自动清理且设置了阈值时按阈值限制，否则不限制。
        // Coil 在每次写入时自动 LRU 淘汰超限条目（缓存满阈值自动清理）。
        val diskCacheMaxBytes =
            if (Prefs.cacheAutoClean && Prefs.cacheThreshold > 0) {
                Prefs.cacheThreshold * CacheManager.BYTES_PER_MB
            } else {
                CacheManager.UNLIMITED_DISK_CACHE_BYTES
            }
        val imageDiskCache =
            DiskCache
                .Builder()
                .directory(
                    File(cacheDir, CacheManager.IMAGE_CACHE_DIR).absolutePath.toPath(),
                ).maxSizeBytes(diskCacheMaxBytes)
                .build()

        coil3.SingletonImageLoader.setSafe {
            ImageLoader
                .Builder(this)
                .crossfade(true)
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCache(imageDiskCache)
                .components {
                    add(OkHttpNetworkFetcherFactory(OkHttpClient()))
                }.build()
        }

        // 启动时检查缓存阈值，超限自动 LRU 清理（检查时机 = App 启动 + 缓存写入后）
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            CacheManager(this@BVApplication).checkCache()
        }

        // 预热播放器 OkHttpClient（构建 SSLContext/KeyStore 较慢，放到 IO 线程）。
        // 预热后单例已就绪，首次进播放器时不再阻塞主线程。
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            playerHttpClientProvider.get()
        }
    }
}
