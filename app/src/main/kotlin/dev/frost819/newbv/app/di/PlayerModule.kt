package dev.frost819.newbv.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.app.util.MediaCodecVideoCapabilityProvider
import dev.frost819.newbv.app.util.VideoCapabilityProvider
import dev.frost819.newbv.player.CdnSelector
import dev.frost819.newbv.player.OkHttpCdnSelector
import dev.frost819.newbv.player.OkHttpUtil
import dev.frost819.newbv.player.impl.exo.ExoPlayerFactory
import javax.inject.Singleton

/**
 * 播放器相关 Hilt 模块。
 *
 * 提供播放器工厂等播放器相关的依赖绑定。
 * VideoInfoRepository 已通过 `@Inject constructor` + `@Singleton` 自动绑定，无需在此声明。
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    /**
     * 提供 [ExoPlayerFactory] 单例。
     *
     * 用于创建 Media3 ExoPlayer 实例。通过工厂模式抽象播放器创建，
     * 便于未来扩展其他播放器实现。
     */
    @Provides
    @Singleton
    fun provideExoPlayerFactory(): ExoPlayerFactory = ExoPlayerFactory()

    /**
     * 提供设备视频解码能力查询器。
     *
     * 基于 Media3 `MediaCodecVideoRenderer.supportsFormat` 实现，供播放选流时
     * 过滤超出本机解码能力的编码/画质组合。
     */
    @Provides
    @Singleton
    fun provideVideoCapabilityProvider(
        @ApplicationContext context: Context,
    ): VideoCapabilityProvider = MediaCodecVideoCapabilityProvider(context)

    /**
     * 提供 CDN 自动选择器。
     *
     * 基于带自定义 SSL 配置的 OkHttpClient 对候选播放地址测速，
     * 结果按 host 缓存，供 [dev.frost819.newbv.app.viewmodel.player.PlayerViewModel] 选流使用。
     */
    @Provides
    @Singleton
    fun provideCdnSelector(
        @ApplicationContext context: Context,
    ): CdnSelector = OkHttpCdnSelector(OkHttpUtil.generateCustomSslOkHttpClient(context))
}
