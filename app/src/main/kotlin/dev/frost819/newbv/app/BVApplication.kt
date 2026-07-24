package dev.frost819.newbv.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.HiltAndroidApp
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.InteractionLogger
import dev.frost819.newbv.core.log.LogCategory
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * new BV 应用入口。
 *
 * 初始化顺序：
 * 1. Hilt 依赖注入（由 [HiltAndroidApp] 自动处理）
 * 2. [Prefs] 偏好设置初始化（阻塞读取 DataStore 首帧）
 * 3. [CrashHandler] 全局崩溃处理（通过 Hilt 注入，构造时自动 install）
 * 4. [InteractionLogger] 交互日志（通过 Hilt 注入）
 */
@HiltAndroidApp
class BVApplication : Application() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var interactionLogger: InteractionLogger

    override fun onCreate() {
        super.onCreate()

        // 初始化 Prefs（阻塞读取 DataStore 首帧数据到内存）
        Prefs.init(dataStore)

        // CrashHandler 在 Hilt 注入时已自动 install，此处引用确保它被创建
        @Suppress("UNUSED_EXPRESSION")
        crashHandler

        // 交互日志记录应用启动事件
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            interactionLogger.log(LogCategory.LIFECYCLE, "Application started")
        }
    }
}
