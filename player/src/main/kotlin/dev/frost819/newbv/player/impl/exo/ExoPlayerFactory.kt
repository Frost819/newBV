package dev.frost819.newbv.player.impl.exo

import android.content.Context
import dev.frost819.newbv.player.VideoPlayerOptions
import dev.frost819.newbv.player.factory.PlayerFactory

/**
 * ExoPlayer 播放器工厂。
 *
 * 创建 [ExoMediaPlayer] 实例，供 Hilt Module 注入或直接使用。
 */
class ExoPlayerFactory : PlayerFactory<ExoMediaPlayer>() {
    override fun create(context: Context, options: VideoPlayerOptions): ExoMediaPlayer {
        return ExoMediaPlayer(context, options)
    }
}
