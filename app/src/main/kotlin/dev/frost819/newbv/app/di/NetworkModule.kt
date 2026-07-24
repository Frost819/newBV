package dev.frost819.newbv.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.InteractionLogger
import dev.frost819.newbv.data.datastore.BuvidGenerator
import java.io.File
import javax.inject.Singleton

/**
 * 网络与基础设施 Hilt 模块。
 *
 * 提供 BiliHttpApi 初始化、AuthRepository、日志组件的单例绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 [InteractionLogger] 单例。
     *
     * 日志目录：`filesDir/interaction_logs`
     */
    @Provides
    @Singleton
    fun provideInteractionLogger(@ApplicationContext context: Context): InteractionLogger {
        val logDir = File(context.filesDir, InteractionLogger.FILE_PREFIX)
        logDir.mkdirs()
        return InteractionLogger(logDir = logDir)
    }

    /**
     * 提供 [CrashHandler] 单例。
     *
     * 安装全局未捕获异常处理器，崩溃日志写入 `filesDir/crash_logs`。
     */
    @Provides
    @Singleton
    fun provideCrashHandler(
        @ApplicationContext context: Context,
        interactionLogger: InteractionLogger
    ): CrashHandler {
        return CrashHandler(context, interactionLogger).also { it.install() }
    }

    /**
     * 提供 [AuthRepository] 单例。
     *
     * 管理当前登录用户的凭证（SESSDATA、bili_jct、access_token 等）。
     */
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = AuthRepository()

    /**
     * 初始化 [BiliHttpApi]。
     *
     * BiliHttpApi 是 `object` 单例，需要先调用 [BiliHttpApi.init] 传入 buvid3。
     * buvid3 由 [BuvidGenerator.generateBuvid3] 生成（每次启动重新生成，
     * 与 Prefs 中的 buvid3 一致性由应用层保证）。
     */
    @Provides
    @Singleton
    fun provideBiliHttpApi(authRepository: AuthRepository): BiliHttpApi {
        val buvid3 = BuvidGenerator.generateBuvid3()
        authRepository.buvid3 = buvid3
        BiliHttpApi.init(buvid3)
        return BiliHttpApi
    }

    /**
     * 提供 [InteractionTracker] 单例。
     *
     * 运行时追踪用户输入方式（Touch/DPad），驱动焦点视觉反馈的显示/隐藏。
     */
    @Provides
    @Singleton
    fun provideInteractionTracker(): InteractionTracker = InteractionTracker()
}
