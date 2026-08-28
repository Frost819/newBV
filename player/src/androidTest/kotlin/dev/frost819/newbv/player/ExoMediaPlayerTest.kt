package dev.frost819.newbv.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.player.impl.exo.ExoMediaPlayer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ExoMediaPlayer] 的插桩测试。
 *
 * 验证播放器实例创建、状态流转、seek、变速等 API 行为。
 * 不依赖真实网络流，使用本地空 MediaSource 验证播放器生命周期。
 *
 * ExoPlayer 要求所有操作在主线程执行，因此所有播放器 API 调用都通过
 * [runOnMain] 辅助函数包装到主线程上。
 */
@RunWith(AndroidJUnit4::class)
class ExoMediaPlayerTest {
    private lateinit var context: Context
    private lateinit var player: ExoMediaPlayer

    private fun <T> runOnMain(block: () -> T): T {
        val result = arrayOfNulls<Any>(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result[0] = block()
        }
        @Suppress("UNCHECKED_CAST")
        return result[0] as T
    }

    private fun runOnMainVoid(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { block() }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        player =
            runOnMain {
                ExoMediaPlayer(
                    context = context,
                    options =
                        VideoPlayerOptions(
                            userAgent = "test-agent",
                            referer = "https://www.bilibili.com",
                            enableFfmpegAudioRenderer = false,
                            enableSoftwareVideoDecoder = false,
                        ),
                )
            }
    }

    @After
    fun teardown() {
        runOnMainVoid {
            if (this::player.isInitialized) player.release()
        }
    }

    @Test
    fun initPlayer_createsExoPlayerInstance() {
        val mPlayer = runOnMain { player.mPlayer }
        assertThat(mPlayer).isNotNull()
    }

    @Test
    fun play_doesNotCrashWhenIdle() {
        runOnMainVoid { player.start() }
        val isPlaying = runOnMain { player.isPlaying }
        assertThat(isPlaying).isFalse()
    }

    @Test
    fun seekTo_doesNotCrashWithoutMedia() {
        runOnMainVoid { player.seekTo(5000L) }
        val position = runOnMain { player.currentPosition }
        assertThat(position).isAtLeast(0L)
    }

    @Test
    fun speed_setterUpdatesPlaybackSpeed() {
        runOnMainVoid { player.speed = 2.0f }
        val speed1 = runOnMain { player.speed }
        assertThat(speed1).isWithin(0.001f).of(2.0f)

        runOnMainVoid { player.speed = 0.5f }
        val speed2 = runOnMain { player.speed }
        assertThat(speed2).isWithin(0.001f).of(0.5f)

        runOnMainVoid { player.speed = 1.0f }
        val speed3 = runOnMain { player.speed }
        assertThat(speed3).isWithin(0.001f).of(1.0f)
    }

    @Test
    fun duration_returnsTimeUnsetWhenIdle() {
        val duration = runOnMain { player.duration }
        assertThat(duration).isEqualTo(androidx.media3.common.C.TIME_UNSET)
    }

    @Test
    fun currentPosition_returnsZeroWhenIdle() {
        val position = runOnMain { player.currentPosition }
        assertThat(position).isEqualTo(0L)
    }

    @Test
    fun bufferedPercentage_returnsZeroWhenIdle() {
        val buffered = runOnMain { player.bufferedPercentage }
        assertThat(buffered).isEqualTo(0)
    }

    @Test
    fun videoDimensions_returnZeroWhenIdle() {
        val width = runOnMain { player.videoWidth }
        val height = runOnMain { player.videoHeight }
        assertThat(width).isEqualTo(0)
        assertThat(height).isEqualTo(0)
    }

    @Test
    fun tcpSpeed_returnsZero() {
        val speed = runOnMain { player.tcpSpeed }
        assertThat(speed).isEqualTo(0L)
    }

    @Test
    fun debugInfo_containsExpectedFields() {
        val info = runOnMain { player.debugInfo }
        assertThat(info).contains("player:")
        assertThat(info).contains("time:")
        assertThat(info).contains("resolution:")
        assertThat(info).contains("video codec:")
        assertThat(info).contains("audio codec:")
    }

    @Test
    fun playUrl_withVideoAndAudio_doesNotCrash() {
        runOnMainVoid {
            player.playUrl(
                videoUrl = "https://example.com/video.m4s",
                audioUrl = "https://example.com/audio.m4s",
            )
        }
    }

    @Test
    fun playUrl_withOnlyVideoUrl_doesNotCrash() {
        runOnMainVoid {
            player.playUrl(
                videoUrl = "https://example.com/video.flv",
                audioUrl = null,
            )
        }
    }

    @Test
    fun playUrl_withNullUrls_doesNotCrash() {
        runOnMainVoid { player.playUrl(videoUrl = null, audioUrl = null) }
    }

    @Test
    fun stopAndRelease_doNotCrash() {
        runOnMainVoid { player.stop() }
        runOnMainVoid { player.release() }
    }

    @Test
    fun setOptions_enablesPlayWhenReady() {
        runOnMainVoid { player.setOptions() }
        val playWhenReady = runOnMain { player.mPlayer?.playWhenReady }
        assertThat(playWhenReady).isTrue()
    }

    @Test
    fun softwareDecoderOption_createsPlayerWithoutCrash() {
        val softPlayer =
            runOnMain {
                ExoMediaPlayer(
                    context = context,
                    options =
                        VideoPlayerOptions(
                            enableFfmpegAudioRenderer = true,
                            enableSoftwareVideoDecoder = true,
                        ),
                )
            }
        val mPlayer = runOnMain { softPlayer.mPlayer }
        assertThat(mPlayer).isNotNull()
        runOnMainVoid { softPlayer.release() }
    }
}
