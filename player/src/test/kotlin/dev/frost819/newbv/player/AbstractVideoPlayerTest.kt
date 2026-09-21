package dev.frost819.newbv.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [AbstractVideoPlayer] 的单元测试。
 *
 * 通过测试子类验证：
 * - [setPlayerEventListener] 的绑定与取消绑定行为
 * - 默认状态与空安全
 * - 播放器生命周期方法调用顺序（委托模式）
 * - 状态属性（isPlaying / currentPosition / duration 等）的读取
 * - 回调分发模式（模拟 ExoMediaPlayer 的 Player.Listener 回调）
 *
 * @see AbstractVideoPlayer
 * @see VideoPlayerListener
 */
class AbstractVideoPlayerTest {
    // ------------------------------------------------------------------
    //  Test doubles
    // ------------------------------------------------------------------

    /** 用于测试的抽象类具体实现，仅覆盖必要方法 */
    private class TestVideoPlayer : AbstractVideoPlayer() {
        override fun initPlayer() {}

        override fun setHeader(headers: Map<String, String>) {}

        override fun playUrl(
            videoUrl: String?,
            audioUrl: String?,
        ) {}

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
        override val debugInfo: String = "test"
        override val videoWidth: Int = 0
        override val videoHeight: Int = 0

        /** 暴露 mPlayerEventListener 供测试验证 */
        fun listener(): VideoPlayerListener? = mPlayerEventListener
    }

    /**
     * 用于测试的回调监听器实现，记录各回调是否被调用。
     */
    private class TestListener : VideoPlayerListener {
        var onErrorCalled = false
        var onReadyCalled = false
        var onPlayCalled = false
        var onPauseCalled = false
        var onBufferingCalled = false
        var onEndCalled = false
        var seekBackMs: Long? = null
        var seekForwardMs: Long? = null
        var lastError: Exception? = null
        var callbackOrder = mutableListOf<String>()

        override fun onError(error: Exception) {
            onErrorCalled = true
            lastError = error
            callbackOrder.add("onError")
        }

        override fun onReady() {
            onReadyCalled = true
            callbackOrder.add("onReady")
        }

        override fun onPlay() {
            onPlayCalled = true
            callbackOrder.add("onPlay")
        }

        override fun onPause() {
            onPauseCalled = true
            callbackOrder.add("onPause")
        }

        override fun onBuffering() {
            onBufferingCalled = true
            callbackOrder.add("onBuffering")
        }

        override fun onEnd() {
            onEndCalled = true
            callbackOrder.add("onEnd")
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
            seekBackMs = seekBackIncrementMs
            callbackOrder.add("onSeekBack")
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
            seekForwardMs = seekForwardIncrementMs
            callbackOrder.add("onSeekForward")
        }

        fun reset() {
            onErrorCalled = false
            onReadyCalled = false
            onPlayCalled = false
            onPauseCalled = false
            onBufferingCalled = false
            onEndCalled = false
            seekBackMs = null
            seekForwardMs = null
            lastError = null
            callbackOrder.clear()
        }
    }

    /**
     * Spy 子类，模拟 ExoMediaPlayer 的委托模式。
     *
     * 记录所有方法调用顺序，维护内部状态，
     * 并暴露 [mPlayerEventListener] 供回调分发模拟使用。
     */
    private class SpyVideoPlayer : AbstractVideoPlayer() {
        val calls = mutableListOf<String>()
        var lastSeekTime: Long? = null
        var lastVideoUrl: String? = null
        var lastAudioUrl: String? = null
        var lastHeaders: Map<String, String>? = null
        var lastSpeed: Float? = null

        private var _isPlaying = false
        private var _currentPosition = 0L
        private var _duration = 0L
        private var _bufferedPercentage = 0
        private var _speed = 1.0f
        private var _videoWidth = 0
        private var _videoHeight = 0

        override fun initPlayer() {
            calls.add("initPlayer")
        }

        override fun setHeader(headers: Map<String, String>) {
            calls.add("setHeader")
            lastHeaders = headers
        }

        override fun playUrl(
            videoUrl: String?,
            audioUrl: String?,
        ) {
            calls.add("playUrl")
            lastVideoUrl = videoUrl
            lastAudioUrl = audioUrl
        }

        override fun prepare() {
            calls.add("prepare")
        }

        override fun start() {
            calls.add("start")
            _isPlaying = true
        }

        override fun pause() {
            calls.add("pause")
            _isPlaying = false
        }

        override fun stop() {
            calls.add("stop")
            _isPlaying = false
        }

        override fun reset() {
            calls.add("reset")
            _isPlaying = false
            _currentPosition = 0
            _duration = 0
        }

        override val isPlaying: Boolean get() = _isPlaying

        override fun seekTo(time: Long) {
            calls.add("seekTo")
            lastSeekTime = time
            _currentPosition = time
        }

        override fun release() {
            calls.add("release")
        }

        override val currentPosition: Long get() = _currentPosition
        override val duration: Long get() = _duration
        override val bufferedPercentage: Int get() = _bufferedPercentage

        override fun setOptions() {
            calls.add("setOptions")
        }

        override var speed: Float
            get() = _speed
            set(value) {
                _speed = value
                lastSpeed = value
                calls.add("speed=$value")
            }
        override val tcpSpeed: Long = 0L
        override val debugInfo: String = "spy-debug"
        override val videoWidth: Int get() = _videoWidth
        override val videoHeight: Int get() = _videoHeight

        fun setTestDuration(d: Long) {
            _duration = d
        }

        fun setTestBufferedPercentage(p: Int) {
            _bufferedPercentage = p
        }

        fun setTestVideoSize(
            w: Int,
            h: Int,
        ) {
            _videoWidth = w
            _videoHeight = h
        }

        fun setTestCurrentPosition(p: Long) {
            _currentPosition = p
        }

        // --- Callback dispatch simulation (mirrors ExoMediaPlayer Player.Listener) ---

        fun simulateBuffering() {
            mPlayerEventListener?.onBuffering()
        }

        fun simulateReady() {
            mPlayerEventListener?.onReady()
        }

        fun simulatePlay() {
            mPlayerEventListener?.onPlay()
        }

        fun simulatePause() {
            mPlayerEventListener?.onPause()
        }

        fun simulateEnd() {
            mPlayerEventListener?.onEnd()
        }

        fun simulateError(e: Exception) {
            mPlayerEventListener?.onError(e)
        }

        fun simulateSeekBack(ms: Long) {
            mPlayerEventListener?.onSeekBack(ms)
        }

        fun simulateSeekForward(ms: Long) {
            mPlayerEventListener?.onSeekForward(ms)
        }

        fun listener(): VideoPlayerListener? = mPlayerEventListener
    }

    // ------------------------------------------------------------------
    //  Listener binding tests
    // ------------------------------------------------------------------

    @Test
    fun `setPlayerEventListener stores listener`() {
        val player = TestVideoPlayer()
        val listener = TestListener()

        player.setPlayerEventListener(listener)

        assertThat(player.listener()).isSameInstanceAs(listener)
    }

    @Test
    fun `setPlayerEventListener with null removes listener`() {
        val player = TestVideoPlayer()
        val listener = TestListener()

        player.setPlayerEventListener(listener)
        assertThat(player.listener()).isNotNull()

        player.setPlayerEventListener(null)
        assertThat(player.listener()).isNull()
    }

    @Test
    fun `replacing listener overwrites previous one`() {
        val player = TestVideoPlayer()
        val listener1 = TestListener()
        val listener2 = TestListener()

        player.setPlayerEventListener(listener1)
        player.setPlayerEventListener(listener2)

        assertThat(player.listener()).isSameInstanceAs(listener2)
        assertThat(player.listener()).isNotSameInstanceAs(listener1)
    }

    @Test
    fun `listener is null by default before any binding`() {
        val player = TestVideoPlayer()

        assertThat(player.listener()).isNull()
    }

    @Test
    fun `set then null then set again works correctly`() {
        val player = TestVideoPlayer()
        val listener1 = TestListener()
        val listener2 = TestListener()

        player.setPlayerEventListener(listener1)
        player.setPlayerEventListener(null)
        player.setPlayerEventListener(listener2)

        assertThat(player.listener()).isSameInstanceAs(listener2)
    }

    @Test
    fun `multiple setPlayerEventListener calls keep only the last`() {
        val player = TestVideoPlayer()
        val listeners = (1..5).map { TestListener() }

        listeners.forEach { player.setPlayerEventListener(it) }

        assertThat(player.listener()).isSameInstanceAs(listeners.last())
    }

    // ------------------------------------------------------------------
    //  Callback dispatch tests
    // ------------------------------------------------------------------

    @Test
    fun `listener receives error callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        val error = RuntimeException("test error")
        player.listener()?.onError(error)

        assertThat(listener.onErrorCalled).isTrue()
        assertThat(listener.lastError).isSameInstanceAs(error)
    }

    @Test
    fun `listener receives ready callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onReady()

        assertThat(listener.onReadyCalled).isTrue()
    }

    @Test
    fun `listener receives play callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onPlay()

        assertThat(listener.onPlayCalled).isTrue()
    }

    @Test
    fun `listener receives pause callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onPause()

        assertThat(listener.onPauseCalled).isTrue()
    }

    @Test
    fun `listener receives buffering callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onBuffering()

        assertThat(listener.onBufferingCalled).isTrue()
    }

    @Test
    fun `listener receives end callback`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onEnd()

        assertThat(listener.onEndCalled).isTrue()
    }

    @Test
    fun `listener receives seekBack with correct increment`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onSeekBack(5000L)

        assertThat(listener.seekBackMs).isEqualTo(5000L)
    }

    @Test
    fun `listener receives seekForward with correct increment`() {
        val player = TestVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.listener()?.onSeekForward(10000L)

        assertThat(listener.seekForwardMs).isEqualTo(10000L)
    }

    // ------------------------------------------------------------------
    //  Null safety tests — callbacks must not crash when listener is null
    // ------------------------------------------------------------------

    @Test
    fun `simulateBuffering does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateBuffering()
    }

    @Test
    fun `simulateReady does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateReady()
    }

    @Test
    fun `simulatePlay does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulatePlay()
    }

    @Test
    fun `simulatePause does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulatePause()
    }

    @Test
    fun `simulateEnd does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateEnd()
    }

    @Test
    fun `simulateError does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateError(RuntimeException("boom"))
    }

    @Test
    fun `simulateSeekBack does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateSeekBack(5000L)
    }

    @Test
    fun `simulateSeekForward does not crash when listener is null`() {
        val player = SpyVideoPlayer()

        player.simulateSeekForward(10000L)
    }

    // ------------------------------------------------------------------
    //  Callback dispatch via spy (mirrors ExoMediaPlayer Player.Listener pattern)
    // ------------------------------------------------------------------

    @Test
    fun `spy simulateBuffering dispatches onBuffering to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateBuffering()

        assertThat(listener.onBufferingCalled).isTrue()
    }

    @Test
    fun `spy simulateReady dispatches onReady to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateReady()

        assertThat(listener.onReadyCalled).isTrue()
    }

    @Test
    fun `spy simulatePlay dispatches onPlay to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulatePlay()

        assertThat(listener.onPlayCalled).isTrue()
    }

    @Test
    fun `spy simulatePause dispatches onPause to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulatePause()

        assertThat(listener.onPauseCalled).isTrue()
    }

    @Test
    fun `spy simulateEnd dispatches onEnd to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateEnd()

        assertThat(listener.onEndCalled).isTrue()
    }

    @Test
    fun `spy simulateError dispatches onError with exception to listener`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        val error = IllegalStateException("playback failed")
        player.simulateError(error)

        assertThat(listener.onErrorCalled).isTrue()
        assertThat(listener.lastError).isSameInstanceAs(error)
    }

    @Test
    fun `spy simulateSeekBack dispatches onSeekBack with increment`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateSeekBack(5000L)

        assertThat(listener.seekBackMs).isEqualTo(5000L)
    }

    @Test
    fun `spy simulateSeekForward dispatches onSeekForward with increment`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateSeekForward(10000L)

        assertThat(listener.seekForwardMs).isEqualTo(10000L)
    }

    @Test
    fun `callbacks fire in the order they are dispatched`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateBuffering()
        player.simulateReady()
        player.simulatePlay()
        player.simulatePause()
        player.simulateEnd()

        assertThat(listener.callbackOrder)
            .containsExactly(
                "onBuffering",
                "onReady",
                "onPlay",
                "onPause",
                "onEnd",
            ).inOrder()
    }

    @Test
    fun `replacing listener stops old listener from receiving callbacks`() {
        val player = SpyVideoPlayer()
        val listener1 = TestListener()
        val listener2 = TestListener()
        player.setPlayerEventListener(listener1)

        player.simulatePlay()
        assertThat(listener1.onPlayCalled).isTrue()

        player.setPlayerEventListener(listener2)
        player.simulatePause()

        assertThat(listener1.onPauseCalled).isFalse()
        assertThat(listener2.onPauseCalled).isTrue()
    }

    // ------------------------------------------------------------------
    //  Lifecycle / delegation tests
    // ------------------------------------------------------------------

    @Test
    fun `full lifecycle calls methods in correct order`() {
        val player = SpyVideoPlayer()

        player.initPlayer()
        player.setHeader(mapOf("User-Agent" to "TestUA"))
        player.playUrl("https://video.url", "https://audio.url")
        player.prepare()
        player.start()
        player.pause()
        player.seekTo(30000L)
        player.start()
        player.stop()
        player.reset()
        player.release()

        assertThat(player.calls)
            .containsExactly(
                "initPlayer",
                "setHeader",
                "playUrl",
                "prepare",
                "start",
                "pause",
                "seekTo",
                "start",
                "stop",
                "reset",
                "release",
            ).inOrder()
    }

    @Test
    fun `playUrl with video only stores video url and null audio`() {
        val player = SpyVideoPlayer()

        player.playUrl(videoUrl = "https://video.url", audioUrl = null)

        assertThat(player.lastVideoUrl).isEqualTo("https://video.url")
        assertThat(player.lastAudioUrl).isNull()
    }

    @Test
    fun `playUrl with video and audio stores both urls`() {
        val player = SpyVideoPlayer()

        player.playUrl("https://video.url", "https://audio.url")

        assertThat(player.lastVideoUrl).isEqualTo("https://video.url")
        assertThat(player.lastAudioUrl).isEqualTo("https://audio.url")
    }

    @Test
    fun `playUrl with both null stores null for both`() {
        val player = SpyVideoPlayer()

        player.playUrl(null, null)

        assertThat(player.lastVideoUrl).isNull()
        assertThat(player.lastAudioUrl).isNull()
    }

    @Test
    fun `playUrl uses default parameter values`() {
        val player = SpyVideoPlayer()

        player.playUrl(videoUrl = "https://video.url")

        assertThat(player.lastVideoUrl).isEqualTo("https://video.url")
        assertThat(player.lastAudioUrl).isNull()
    }

    @Test
    fun `setHeader stores headers map`() {
        val player = SpyVideoPlayer()
        val headers = mapOf("User-Agent" to "TestUA", "Referer" to "https://bilibili.com")

        player.setHeader(headers)

        assertThat(player.lastHeaders).isEqualTo(headers)
    }

    @Test
    fun `setHeader with empty map stores empty map`() {
        val player = SpyVideoPlayer()

        player.setHeader(emptyMap())

        assertThat(player.lastHeaders).isEmpty()
    }

    @Test
    fun `seekTo stores the target time`() {
        val player = SpyVideoPlayer()

        player.seekTo(45000L)

        assertThat(player.lastSeekTime).isEqualTo(45000L)
    }

    @Test
    fun `seekTo with zero`() {
        val player = SpyVideoPlayer()

        player.seekTo(0L)

        assertThat(player.lastSeekTime).isEqualTo(0L)
    }

    // ------------------------------------------------------------------
    //  State management tests
    // ------------------------------------------------------------------

    @Test
    fun `isPlaying is false before start`() {
        val player = SpyVideoPlayer()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun `isPlaying is true after start`() {
        val player = SpyVideoPlayer()

        player.start()

        assertThat(player.isPlaying).isTrue()
    }

    @Test
    fun `isPlaying is false after pause`() {
        val player = SpyVideoPlayer()
        player.start()

        player.pause()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun `isPlaying is false after stop`() {
        val player = SpyVideoPlayer()
        player.start()

        player.stop()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun `isPlaying is false after reset`() {
        val player = SpyVideoPlayer()
        player.start()

        player.reset()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun `currentPosition is zero by default`() {
        val player = SpyVideoPlayer()

        assertThat(player.currentPosition).isEqualTo(0L)
    }

    @Test
    fun `currentPosition updates after seekTo`() {
        val player = SpyVideoPlayer()

        player.seekTo(60000L)

        assertThat(player.currentPosition).isEqualTo(60000L)
    }

    @Test
    fun `currentPosition reflects setTestCurrentPosition`() {
        val player = SpyVideoPlayer()

        player.setTestCurrentPosition(120000L)

        assertThat(player.currentPosition).isEqualTo(120000L)
    }

    @Test
    fun `duration is zero by default`() {
        val player = SpyVideoPlayer()

        assertThat(player.duration).isEqualTo(0L)
    }

    @Test
    fun `duration reflects setTestDuration`() {
        val player = SpyVideoPlayer()

        player.setTestDuration(300000L)

        assertThat(player.duration).isEqualTo(300000L)
    }

    @Test
    fun `bufferedPercentage is zero by default`() {
        val player = SpyVideoPlayer()

        assertThat(player.bufferedPercentage).isEqualTo(0)
    }

    @Test
    fun `bufferedPercentage reflects setTestBufferedPercentage`() {
        val player = SpyVideoPlayer()

        player.setTestBufferedPercentage(75)

        assertThat(player.bufferedPercentage).isEqualTo(75)
    }

    @Test
    fun `speed defaults to 1_0`() {
        val player = SpyVideoPlayer()

        assertThat(player.speed).isEqualTo(1.0f)
    }

    @Test
    fun `speed setter updates getter`() {
        val player = SpyVideoPlayer()

        player.speed = 2.0f

        assertThat(player.speed).isEqualTo(2.0f)
    }

    @Test
    fun `speed setter records the value`() {
        val player = SpyVideoPlayer()

        player.speed = 1.5f

        assertThat(player.lastSpeed).isEqualTo(1.5f)
    }

    @Test
    fun `speed can be set to zero`() {
        val player = SpyVideoPlayer()

        player.speed = 0f

        assertThat(player.speed).isEqualTo(0f)
    }

    @Test
    fun `tcpSpeed is always zero`() {
        val player = SpyVideoPlayer()

        assertThat(player.tcpSpeed).isEqualTo(0L)
    }

    @Test
    fun `debugInfo is non-empty`() {
        val player = SpyVideoPlayer()

        assertThat(player.debugInfo).isNotEmpty()
    }

    @Test
    fun `videoWidth is zero by default`() {
        val player = SpyVideoPlayer()

        assertThat(player.videoWidth).isEqualTo(0)
    }

    @Test
    fun `videoHeight is zero by default`() {
        val player = SpyVideoPlayer()

        assertThat(player.videoHeight).isEqualTo(0)
    }

    @Test
    fun `videoWidth and videoHeight reflect setTestVideoSize`() {
        val player = SpyVideoPlayer()

        player.setTestVideoSize(1920, 1080)

        assertThat(player.videoWidth).isEqualTo(1920)
        assertThat(player.videoHeight).isEqualTo(1080)
    }

    // ------------------------------------------------------------------
    //  Combined lifecycle + callback dispatch tests
    // ------------------------------------------------------------------

    @Test
    fun `start then simulateBuffering then simulateReady triggers correct callbacks`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.start()
        player.simulateBuffering()
        player.simulateReady()

        assertThat(listener.callbackOrder).containsExactly("onBuffering", "onReady").inOrder()
    }

    @Test
    fun `play pause cycle dispatches onPlay then onPause`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulatePlay()
        player.simulatePause()

        assertThat(listener.onPlayCalled).isTrue()
        assertThat(listener.onPauseCalled).isTrue()
        assertThat(listener.callbackOrder).containsExactly("onPlay", "onPause").inOrder()
    }

    @Test
    fun `error during playback dispatches onError`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.start()
        player.simulatePlay()
        val error = RuntimeException("network error")
        player.simulateError(error)

        assertThat(listener.onPlayCalled).isTrue()
        assertThat(listener.onErrorCalled).isTrue()
        assertThat(listener.lastError).isSameInstanceAs(error)
    }

    @Test
    fun `end of video dispatches onEnd`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.start()
        player.simulatePlay()
        player.simulateEnd()

        assertThat(listener.onEndCalled).isTrue()
    }

    @Test
    fun `seekBack and seekForward dispatch with correct increments`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateSeekBack(5000L)
        player.simulateSeekForward(10000L)

        assertThat(listener.seekBackMs).isEqualTo(5000L)
        assertThat(listener.seekForwardMs).isEqualTo(10000L)
    }

    @Test
    fun `full playback lifecycle with callbacks`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.initPlayer()
        player.playUrl("https://video.url", "https://audio.url")
        player.prepare()
        player.simulateBuffering()
        player.simulateReady()
        player.start()
        player.simulatePlay()
        player.seekTo(30000L)
        player.simulatePause()
        player.simulatePlay()
        player.simulateEnd()

        assertThat(listener.callbackOrder)
            .containsExactly(
                "onBuffering",
                "onReady",
                "onPlay",
                "onPause",
                "onPlay",
                "onEnd",
            ).inOrder()
    }

    @Test
    fun `reset clears playback state`() {
        val player = SpyVideoPlayer()

        player.start()
        player.seekTo(60000L)
        player.setTestDuration(120000L)

        player.reset()

        assertThat(player.isPlaying).isFalse()
        assertThat(player.currentPosition).isEqualTo(0L)
        assertThat(player.duration).isEqualTo(0L)
    }

    @Test
    fun `release is callable without side effects on state queries`() {
        val player = SpyVideoPlayer()

        player.start()
        player.release()

        assertThat(player.calls).contains("release")
    }

    // ------------------------------------------------------------------
    //  VideoPlayerListener interface contract tests
    // ------------------------------------------------------------------

    @Test
    fun `VideoPlayerListener has all nine callback methods`() {
        // 过滤合成方法（默认实现会生成 access$...$jd 合成桥接）
        val methods = VideoPlayerListener::class.java.declaredMethods.filterNot { it.isSynthetic }

        assertThat(methods.map { it.name }).containsExactly(
            "onError",
            "onVideoDecodeUnsupported",
            "onReady",
            "onPlay",
            "onPause",
            "onBuffering",
            "onEnd",
            "onSeekBack",
            "onSeekForward",
        )
    }

    @Test
    fun `VideoPlayerListener onError accepts Exception parameter`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onError", Exception::class.java)

        assertThat(method.parameterCount).isEqualTo(1)
        assertThat(method.parameterTypes[0]).isEqualTo(Exception::class.java)
    }

    @Test
    fun `VideoPlayerListener onSeekBack accepts Long parameter`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onSeekBack", Long::class.javaPrimitiveType)

        assertThat(method.parameterCount).isEqualTo(1)
    }

    @Test
    fun `VideoPlayerListener onSeekForward accepts Long parameter`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onSeekForward", Long::class.javaPrimitiveType)

        assertThat(method.parameterCount).isEqualTo(1)
    }

    @Test
    fun `VideoPlayerListener onReady has no parameters`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onReady")

        assertThat(method.parameterCount).isEqualTo(0)
    }

    @Test
    fun `VideoPlayerListener onPlay has no parameters`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onPlay")

        assertThat(method.parameterCount).isEqualTo(0)
    }

    @Test
    fun `VideoPlayerListener onPause has no parameters`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onPause")

        assertThat(method.parameterCount).isEqualTo(0)
    }

    @Test
    fun `VideoPlayerListener onBuffering has no parameters`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onBuffering")

        assertThat(method.parameterCount).isEqualTo(0)
    }

    @Test
    fun `VideoPlayerListener onEnd has no parameters`() {
        val method = VideoPlayerListener::class.java.getDeclaredMethod("onEnd")

        assertThat(method.parameterCount).isEqualTo(0)
    }

    @Test
    fun `VideoPlayerListener is an interface`() {
        assertThat(VideoPlayerListener::class.java.isInterface).isTrue()
    }

    @Test
    fun `onError accepts any Exception subclass`() {
        val player = SpyVideoPlayer()
        val listener = TestListener()
        player.setPlayerEventListener(listener)

        player.simulateError(IllegalStateException("state"))
        player.simulateError(NullPointerException("npe"))

        assertThat(listener.onErrorCalled).isTrue()
    }
}
