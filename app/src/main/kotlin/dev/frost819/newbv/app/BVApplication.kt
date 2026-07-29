package dev.frost819.newbv.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import dev.frost819.newbv.app.network.HttpServer
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.InteractionLogger
import dev.frost819.newbv.core.log.LogCategory
import dev.frost819.newbv.data.datastore.BuvidGenerator
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * new BV 应用入口。
 *
 * 初始化顺序：
 * 1. Hilt 依赖注入（由 [HiltAndroidApp] 自动处理）
 * 2. [Prefs] 偏好设置初始化（阻塞读取 DataStore 首帧）
 * 3. [CrashHandler] 全局崩溃处理（通过 Hilt 注入，构造时自动 install）
 * 4. [InteractionLogger] 交互日志（通过 Hilt 注入）
 * 5. [HttpServer] 本地日志管理服务器（通过 Hilt 注入，按需启动）
 */
@HiltAndroidApp
class BVApplication : Application() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var interactionLogger: InteractionLogger

    @Inject
    lateinit var httpServer: HttpServer

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate() {
        super.onCreate()

        Prefs.init(dataStore)

        val buvid3 = BuvidGenerator.generateBuvid3()
        authRepository.buvid3 = buvid3
        BiliHttpApi.init(buvid3)

        @Suppress("UNUSED_EXPRESSION")
        crashHandler

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            interactionLogger.log(LogCategory.LIFECYCLE, "Application started")
        }

        coil3.SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .crossfade(true)
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .components {
                    add(OkHttpNetworkFetcherFactory(OkHttpClient()))
                }
                .build()
        }
    }
}
