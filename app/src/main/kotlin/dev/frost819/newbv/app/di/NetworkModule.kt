package dev.frost819.newbv.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.network.HttpServer
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.biliapi.repositories.ChannelRepository
import dev.frost819.newbv.biliapi.repositories.CoinRepository
import dev.frost819.newbv.biliapi.repositories.CommentRepository
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.HistoryRepository
import dev.frost819.newbv.biliapi.repositories.LikeRepository
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.biliapi.repositories.LoginRepository
import dev.frost819.newbv.biliapi.repositories.OneClickTripleActionRepository
import dev.frost819.newbv.biliapi.repositories.PgcRepository
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.biliapi.repositories.SeasonRepository
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
import dev.frost819.newbv.biliapi.repositories.UgcRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.log.CrashHandler
import dev.frost819.newbv.core.log.CrashUploader
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.repository.AccountRepository
import dev.frost819.newbv.data.repository.SearchHistoryRepository
import dev.frost819.newbv.data.repository.SearchHistoryRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络与基础设施 Hilt 模块。
 *
 * 提供 BiliHttpApi 初始化、AuthRepository、崩溃处理和 HttpServer 的单例绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 [CrashUploader] 单例。
     *
     * 使用 [BuildConfig.CRASH_REPORT_TOKEN]（从 `local.properties` 的 `crashReport.token` 注入）。
     * token 为空时上传功能不可用，但实例仍会创建（调用时静默跳过）。
     */
    @Provides
    @Singleton
    fun provideCrashUploader(
        @ApplicationContext context: Context
    ): CrashUploader {
        return CrashUploader(context, BuildConfig.CRASH_REPORT_TOKEN)
    }

    /**
     * 提供 [CrashHandler] 单例。
     *
     * 安装全局未捕获异常处理器，崩溃日志写入 `filesDir/crash_logs`。
     * 同时绑定 [CrashUploader] 用于崩溃日志上传。
     */
    @Provides
    @Singleton
    fun provideCrashHandler(
        @ApplicationContext context: Context,
        crashUploader: CrashUploader,
    ): CrashHandler {
        return CrashHandler(context).apply {
            this.crashUploader = crashUploader
            install()
        }
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
     * 提供 [BiliHttpApi] 单例。
     *
     * BiliHttpApi 是 `object` 单例，在 [BVApplication.onCreate] 中完成 [BiliHttpApi.init]。
     * 此处仅返回引用，供需要依赖注入的组件使用。
     */
    @Provides
    @Singleton
    fun provideBiliHttpApi(): BiliHttpApi = BiliHttpApi

    /**
     * 提供 [InteractionTracker] 单例。
     *
     * 运行时追踪用户输入方式（Touch/DPad），驱动焦点视觉反馈的显示/隐藏。
     */
    @Provides
    @Singleton
    fun provideInteractionTracker(): InteractionTracker = InteractionTracker()

    /**
     * 提供 [LoginRepository] 单例。
     *
     * 封装 B 站登录接口（QR 登录、SMS 登录）。
     */
    @Provides
    @Singleton
    fun provideLoginRepository(): LoginRepository = LoginRepository()

    /**
     * 提供 [ChannelRepository] 单例。
     *
     * 管理 gRPC ManagedChannel，用于 App 接口调用。
     * 在用户登录后由 [dev.frost819.newbv.app.data.AccountRepositoryImpl] 初始化。
     */
    @Provides
    @Singleton
    fun provideChannelRepository(): ChannelRepository = ChannelRepository()

    /**
     * 提供 [CommentRepository] 单例。
     *
     * 封装主评论、楼中楼与评论点赞接口（Web/App HTTP）。
     */
    @Provides
    @Singleton
    fun provideCommentRepository(authRepository: AuthRepository): CommentRepository =
        CommentRepository(authRepository)

    /**
     * 提供 [RecommendVideoRepository] 单例。
     *
     * 封装推荐视频和热门视频接口（Web HTTP + App gRPC）。
     */
    @Provides
    @Singleton
    fun provideRecommendVideoRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): RecommendVideoRepository = RecommendVideoRepository(authRepository, channelRepository)

    /**
     * 提供 [UserRepository] 单例。
     *
     * 封装用户动态、用户空间等接口（Web HTTP + App gRPC）。
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): UserRepository = UserRepository(authRepository, channelRepository)

    /**
     * 提供 [UgcRepository] 单例。
     *
     * 封装 UGC 分区推荐信息流接口（Web HTTP）。
     */
    @Provides
    @Singleton
    fun provideUgcRepository(authRepository: AuthRepository): UgcRepository =
        UgcRepository(authRepository)

    /**
     * 提供 [PgcRepository] 单例。
     *
     * 封装 PGC 影视首页接口（轮播 + Feed + 索引）。
     */
    @Provides
    @Singleton
    fun providePgcRepository(): PgcRepository = PgcRepository()

    /**
     * 提供 [SeasonRepository] 单例。
     *
     * 封装追番列表与番剧时间表接口（Web HTTP + App gRPC）。
     */
    @Provides
    @Singleton
    fun provideSeasonRepository(authRepository: AuthRepository): SeasonRepository =
        SeasonRepository(authRepository)

    /**
     * 提供 [LikeRepository] 单例。
     *
     * 封装视频点赞状态查询与点赞/取消操作（Web HTTP）。
     */
    @Provides
    @Singleton
    fun provideLikeRepository(authRepository: AuthRepository): LikeRepository =
        LikeRepository(authRepository)

    /**
     * 提供 [CoinRepository] 单例。
     *
     * 封装视频投币状态查询与投币操作（Web HTTP）。
     */
    @Provides
    @Singleton
    fun provideCoinRepository(authRepository: AuthRepository): CoinRepository =
        CoinRepository(authRepository)

    /**
     * 提供 [FavoriteRepository] 单例。
     *
     * 封装视频收藏状态查询、收藏夹列表与收藏/取消操作（Web HTTP + App gRPC）。
     */
    @Provides
    @Singleton
    fun provideFavoriteRepository(authRepository: AuthRepository): FavoriteRepository =
        FavoriteRepository(authRepository)

    /**
     * 提供 [OneClickTripleActionRepository] 单例。
     *
     * 封装一键三连操作（Web HTTP）。
     */
    @Provides
    @Singleton
    fun provideOneClickTripleActionRepository(
        authRepository: AuthRepository,
    ): OneClickTripleActionRepository = OneClickTripleActionRepository(authRepository)

    /**
     * 提供 [ToViewRepository] 单例。
     *
     * 封装稍后再看列表查询、添加、删除（Web HTTP + App gRPC）。
     */
    @Provides
    @Singleton
    fun provideToViewRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): ToViewRepository = ToViewRepository(authRepository, channelRepository)

    /**
     * 提供 [HistoryRepository] 单例。
     *
     * 封装观看历史列表查询（Web HTTP + App gRPC，cursor 分页）。
     */
    @Provides
    @Singleton
    fun provideHistoryRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): HistoryRepository = HistoryRepository(authRepository, channelRepository)

    /**
     * 提供 [VideoDetailRepository] 单例。
     *
     * 封装视频详情数据获取（Web HTTP + App gRPC），
     * 聚合详情、用户操作状态（点赞/投币/收藏）、历史记录。
     */
    @Provides
    @Singleton
    fun provideVideoDetailRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
        favoriteRepository: FavoriteRepository,
        likeRepository: LikeRepository,
        coinRepository: CoinRepository,
    ): VideoDetailRepository = VideoDetailRepository(
        authRepository = authRepository,
        channelRepository = channelRepository,
        favoriteRepository = favoriteRepository,
        likeRepository = likeRepository,
        coinRepository = coinRepository,
    )

    /**
     * 提供 [VideoPlayRepository] 单例。
     *
     * 封装播放地址获取、弹幕、字幕、蒙版、心跳、缩略图等播放器相关接口。
     * 依赖 [AuthRepository]（会话凭证）和 [ChannelRepository]（gRPC channel）。
     */
    @Provides
    @Singleton
    fun provideVideoPlayRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): VideoPlayRepository = VideoPlayRepository(authRepository, channelRepository)

    /**
     * 提供 [AccountRepository] 单例。
     *
     * 绑定 [AccountRepositoryImpl] 实现，聚合 UserDao + Prefs + AuthRepository。
     */
    @Provides
    @Singleton
    fun provideAccountRepository(
        userDao: UserDao,
        authRepository: AuthRepository,
    ): AccountRepository = AccountRepositoryImpl(userDao, authRepository)

    /**
     * 提供 [SearchRepository] 单例。
     *
     * 封装 B 站搜索接口（热词/建议/搜索+筛选）。
     */
    @Provides
    @Singleton
    fun provideSearchRepository(
        authRepository: AuthRepository,
        channelRepository: ChannelRepository,
    ): SearchRepository = SearchRepository(authRepository, channelRepository)

    /**
     * 提供 [LiveRepository] 单例。
     *
     * 封装直播首页推荐、分区列表、直播间信息、流地址获取等接口（Web HTTP）。
     */
    @Provides
    @Singleton
    fun provideLiveRepository(): LiveRepository = LiveRepository()

    /**
     * 提供 [SearchHistoryRepository] 单例。
     *
     * 绑定 [SearchHistoryRepositoryImpl]，持久化搜索历史到 Room。
     */
    @Provides
    @Singleton
    fun provideSearchHistoryRepository(
        searchHistoryDao: dev.frost819.newbv.data.db.dao.SearchHistoryDao,
    ): SearchHistoryRepository = SearchHistoryRepositoryImpl(searchHistoryDao)

    /**
     * 提供共享的 [HttpClient] 单例。
     *
     * 使用 OkHttp 引擎，配置了连接/读取/写入超时。
     * 供需要直接 HTTP 请求的组件使用（如字幕下载），
     * 避免各组件各自创建 HttpClient 造成资源浪费。
     */
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout)
    }

    /**
     * 提供 [HttpServer] 单例。
     *
     * Ktor 本地日志管理服务器，在随机端口启动，提供 Web UI 和 REST API。
     * 服务于以下场景：
     * - 用户通过浏览器访问日志管理页面
     * - 下载日志文件（手动/崩溃日志）
     * - 创建手动日志
     *
     * 依赖 [CrashHandler]（日志文件读写）。
     * 由 [dev.frost819.newbv.app.BVApplication] 在应用启动时调用 [HttpServer.start] 启动。
     */
    @Provides
    @Singleton
    fun provideHttpServer(
        @ApplicationContext context: Context,
        crashHandler: CrashHandler,
    ): HttpServer {
        val assetProvider: (String) -> ByteArray? = { path ->
            runCatching {
                context.assets.open(path).use { it.readBytes() }
            }.recoverCatching { e ->
                if (e is FileNotFoundException) null else throw e
            }.getOrNull()
        }

        val logFileProvider: () -> List<File> = {
            crashHandler.listManualLogs() + crashHandler.listCrashLogs()
        }

        val manualLogCreator: () -> File? = { crashHandler.createManualLog() }

        return HttpServer(
            assetProvider = assetProvider,
            logFileProvider = logFileProvider,
            manualLogCreator = manualLogCreator
        )
    }
}
