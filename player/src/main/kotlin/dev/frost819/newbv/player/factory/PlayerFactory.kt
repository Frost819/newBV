package dev.frost819.newbv.player.factory

import android.content.Context
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerOptions

/**
 * 播放器工厂抽象基类。
 *
 * 用于解耦播放器的创建逻辑，方便后续扩展新的播放器引擎。
 *
 * @param T 具体的播放器类型
 */
abstract class PlayerFactory<T : AbstractVideoPlayer> {
    /**
     * 创建播放器实例。
     *
     * @param context Android Context
     * @param options 播放器配置
     * @return 播放器实例
     */
    abstract fun create(
        context: Context,
        options: VideoPlayerOptions,
    ): T
}
