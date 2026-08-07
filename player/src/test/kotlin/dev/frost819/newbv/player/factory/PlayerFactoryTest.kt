package dev.frost819.newbv.player.factory

import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.player.AbstractVideoPlayer
import dev.frost819.newbv.player.VideoPlayerOptions
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * [PlayerFactory] 的单元测试。
 *
 * 通过具体实现验证工厂模式的创建行为。
 */
class PlayerFactoryTest {

    /** 用于测试的具体播放器实现 */
    private class DummyPlayer : AbstractVideoPlayer() {
        override fun initPlayer() {}
        override fun setHeader(headers: Map<String, String>) {}
        override fun playUrl(videoUrl: String?, audioUrl: String?) {}
        override fun prepare() {}
        override fun start() {}
        override fun pause() {}
        override fun stop() {}
        override fun reset() {}
        override val isPlaying: Boolean = false
        override fun seekTo(time: Long) {}
        override fun release() {}
        override val currentPosition: Long = 0L
        override val duration: Long = 0L
        override val bufferedPercentage: Int = 0
        override fun setOptions() {}
        override var speed: Float = 1.0f
        override val tcpSpeed: Long = 0L
        override val debugInfo: String = "dummy"
        override val videoWidth: Int = 1920
        override val videoHeight: Int = 1080
    }

    /** 用于测试的具体工厂实现 */
    private class DummyPlayerFactory : PlayerFactory<DummyPlayer>() {
        var lastOptions: VideoPlayerOptions? = null

        override fun create(context: Context, options: VideoPlayerOptions): DummyPlayer {
            lastOptions = options
            return DummyPlayer()
        }
    }

    @Test
    fun `create returns instance of correct type`() {
        val factory = DummyPlayerFactory()
        val context = mockk<Context>()
        val options = VideoPlayerOptions()

        val player = factory.create(context, options)

        assertThat(player).isInstanceOf(DummyPlayer::class.java)
        assertThat(player).isInstanceOf(AbstractVideoPlayer::class.java)
    }

    @Test
    fun `create passes options to implementation`() {
        val factory = DummyPlayerFactory()
        val context = mockk<Context>()
        val options = VideoPlayerOptions(userAgent = "TestUA", enableFfmpegAudioRenderer = true)

        factory.create(context, options)

        assertThat(factory.lastOptions).isEqualTo(options)
        assertThat(factory.lastOptions?.userAgent).isEqualTo("TestUA")
        assertThat(factory.lastOptions?.enableFfmpegAudioRenderer).isTrue()
    }

    @Test
    fun `create returns new instance each call`() {
        val factory = DummyPlayerFactory()
        val context = mockk<Context>()
        val options = VideoPlayerOptions()

        val player1 = factory.create(context, options)
        val player2 = factory.create(context, options)

        assertThat(player1).isNotSameInstanceAs(player2)
    }

    @Test
    fun `create with default options works correctly`() {
        val factory = DummyPlayerFactory()
        val context = mockk<Context>()

        val player = factory.create(context, VideoPlayerOptions())

        assertThat(player).isNotNull()
        assertThat(factory.lastOptions?.userAgent).isNull()
    }

    @Test
    fun `factory is subclass of PlayerFactory`() {
        val factory = DummyPlayerFactory()

        assertThat(factory).isInstanceOf(PlayerFactory::class.java)
    }
}
