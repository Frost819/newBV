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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * [ExoMediaPlayer] 真实流播放插桩测试。
 *
 * 使用本地 asset 视频（Sintel trailer，~4MB）验证完整播放生命周期：
 * 加载 → 缓冲 → 就绪 → 播放 → 暂停 → seek → 停止 → 释放。
 *
 * 由于模拟器网络不稳定，使用 asset:// URI 绕过网络依赖。
 */
@RunWith(AndroidJUnit4::class)
class ExoMediaPlayerStreamTest {
    private lateinit var context: Context
    private lateinit var player: ExoMediaPlayer

    private companion object {
        private const val TEST_VIDEO_URI = "asset:///test_video.mp4"
        private const val LATCH_TIMEOUT_SEC = 30L
    }

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

    private fun createTrackingListener(
        readyLatch: CountDownLatch,
        playLatch: CountDownLatch = CountDownLatch(1),
        pauseLatch: CountDownLatch? = null,
        errorRef: AtomicReference<Exception?>,
    ): VideoPlayerListener =
        object : VideoPlayerListener {
            override fun onError(error: Exception) {
                errorRef.set(error)
            }

            override fun onReady() {
                readyLatch.countDown()
            }

            override fun onPlay() {
                playLatch.countDown()
            }

            override fun onPause() {
                pauseLatch?.countDown()
            }

            override fun onBuffering() {}

            override fun onEnd() {}

            override fun onSeekBack(seekBackIncrementMs: Long) {}

            override fun onSeekForward(seekForwardIncrementMs: Long) {}
        }

    private fun awaitReady(
        readyLatch: CountDownLatch,
        errorRef: AtomicReference<Exception?>,
    ) {
        val ready = readyLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        if (!ready) {
            val error = errorRef.get()
            throw AssertionError(
                "onReady not received within ${LATCH_TIMEOUT_SEC}s. " +
                    "Error: ${error?.message ?: "none"}",
            )
        }
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
        runOnMainVoid { player.setOptions() }
    }

    @After
    fun teardown() {
        runOnMainVoid {
            if (this::player.isInitialized) player.release()
        }
    }

    @Test
    fun stream_prepare_and_play_returns_valid_duration() {
        val readyLatch = CountDownLatch(1)
        val playLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, playLatch, errorRef = errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
            player.start()
        }

        awaitReady(readyLatch, errorRef)

        val duration = runOnMain { player.duration }
        assertThat(duration).isNotEqualTo(androidx.media3.common.C.TIME_UNSET)
        assertThat(duration).isGreaterThan(0L)

        assertThat(playLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        assertThat(runOnMain { player.isPlaying }).isTrue()
    }

    @Test
    fun stream_playback_advances_position() {
        val readyLatch = CountDownLatch(1)
        val playLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, playLatch, errorRef = errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
            player.start()
        }

        awaitReady(readyLatch, errorRef)
        assertThat(playLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        Thread.sleep(3000)

        val position = runOnMain { player.currentPosition }
        assertThat(position).isGreaterThan(0L)
    }

    @Test
    fun stream_pause_stops_playback() {
        val readyLatch = CountDownLatch(1)
        val playLatch = CountDownLatch(1)
        val pauseLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, playLatch, pauseLatch, errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
            player.start()
        }

        awaitReady(readyLatch, errorRef)
        assertThat(playLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        Thread.sleep(2000)
        runOnMainVoid { player.pause() }

        assertThat(pauseLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(runOnMain { player.isPlaying }).isFalse()
    }

    @Test
    fun stream_seek_to_position() {
        val readyLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, errorRef = errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
        }

        awaitReady(readyLatch, errorRef)

        val duration = runOnMain { player.duration }
        assertThat(duration).isGreaterThan(10_000L)

        val targetPos = duration / 2
        runOnMainVoid { player.seekTo(targetPos) }
        Thread.sleep(1000)

        val actualPos = runOnMain { player.currentPosition }
        assertThat(actualPos).isAtLeast(targetPos - 5000L)
    }

    @Test
    fun stream_video_format_is_available_after_ready() {
        val readyLatch = CountDownLatch(1)
        val playLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, playLatch, errorRef = errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
            player.start()
        }

        awaitReady(readyLatch, errorRef)
        assertThat(playLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // videoWidth/Height 在无 surface 时为 0，但 videoFormat 的 mimeType 应该有值
        val debugInfo = runOnMain { player.debugInfo }
        assertThat(debugInfo).contains("video codec:")
        // MIME type 可能是 "video/av01" 或 "video/hevc" 等
        assertThat(debugInfo).contains("video/")
    }

    @Test
    fun stream_stop_and_restart() {
        val readyLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Exception?>(null)

        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch, errorRef = errorRef),
            )
            player.playUrl(videoUrl = TEST_VIDEO_URI, audioUrl = null)
            player.prepare()
        }

        awaitReady(readyLatch, errorRef)

        runOnMainVoid { player.start() }
        Thread.sleep(1000)
        runOnMainVoid { player.stop() }

        assertThat(runOnMain { player.isPlaying }).isFalse()

        val readyLatch2 = CountDownLatch(1)
        val errorRef2 = AtomicReference<Exception?>(null)
        runOnMainVoid {
            player.setPlayerEventListener(
                createTrackingListener(readyLatch2, errorRef = errorRef2),
            )
            player.prepare()
        }

        awaitReady(readyLatch2, errorRef2)
    }
}
