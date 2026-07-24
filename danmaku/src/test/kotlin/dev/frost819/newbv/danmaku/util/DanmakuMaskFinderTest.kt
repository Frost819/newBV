package dev.frost819.newbv.danmaku.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskType
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuWebMaskFrame
import kotlinx.coroutines.test.runTest
import java.io.File
import org.junit.jupiter.api.Test

class DanmakuMaskFinderTest {

    private val webMaskFile = Any::class::class.java.getResource("/35496788838_30_0.webmask")!!
    private val mobMaskFile = Any::class::class.java.getResource("/35496788838_30_0.mobmask")!!

    private fun loadWebMask() = DanmakuMask.fromBinary(
        File(webMaskFile.toURI()).readBytes(),
        DanmakuMaskType.WebMask
    )

    private fun loadMobMask() = DanmakuMask.fromBinary(
        File(mobMaskFile.toURI()).readBytes(),
        DanmakuMaskType.MobMask
    )

    @Test
    fun `findFrame returns frame for time at start`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadWebMask()

        val frame = finder.findFrame(mask, 0L)

        assertThat(frame).isNotNull()
    }

    @Test
    fun `findFrame returns null for time after last segment`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadWebMask()

        val frame = finder.findFrame(mask, Long.MAX_VALUE)

        assertThat(frame).isNull()
    }

    @Test
    fun `findFrame returns correct frame for web mask`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadWebMask()

        val firstSegment = mask.getSegmentAt(0L)
        val targetTime = firstSegment?.range?.first?.plus(1000L) ?: 1000L
        val frame = finder.findFrame(mask, targetTime)

        assertThat(frame).isNotNull()
        assertThat(frame).isInstanceOf(DanmakuWebMaskFrame::class.java)
    }

    @Test
    fun `findFrame returns correct frame for mob mask`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadMobMask()

        val firstSegment = mask.getSegmentAt(0L)
        val targetTime = firstSegment?.range?.first?.plus(1000L) ?: 1000L
        val frame = finder.findFrame(mask, targetTime)

        assertThat(frame).isNotNull()
        assertThat(frame).isInstanceOf(DanmakuMobMaskFrame::class.java)
    }

    @Test
    fun `reset clears cached segment`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadWebMask()

        finder.findFrame(mask, 1000L)
        finder.reset()
        finder.findFrame(mask, 1000L)
    }

    @Test
    fun `findFrame caches segment and reuses on next call`() = runTest {
        val finder = DanmakuMaskFinder()
        val mask = loadWebMask()

        finder.findFrame(mask, 5000L)
        finder.findFrame(mask, 6000L)
        finder.findFrame(mask, 7000L)
    }
}

class CalculateMaskDelayTest {

    @Test
    fun `with frame and playing returns clamped delay`() {
        val frame = DanmakuWebMaskFrame(range = 1000L until 1500L, svg = "")
        val delay = calculateMaskDelay(frame, currentTime = 1100L, isPlaying = true)

        assertThat(delay).isEqualTo(300L)
    }

    @Test
    fun `with frame and playing clamps to minimum`() {
        val frame = DanmakuWebMaskFrame(range = 1000L until 1010L, svg = "")
        val delay = calculateMaskDelay(frame, currentTime = 1005L, isPlaying = true)

        assertThat(delay).isEqualTo(20L)
    }

    @Test
    fun `with frame and playing clamps to maximum`() {
        val frame = DanmakuWebMaskFrame(range = 1000L until 2000L, svg = "")
        val delay = calculateMaskDelay(frame, currentTime = 1000L, isPlaying = true)

        assertThat(delay).isEqualTo(300L)
    }

    @Test
    fun `without frame and playing returns 100ms`() {
        val delay = calculateMaskDelay(null, currentTime = 5000L, isPlaying = true)
        assertThat(delay).isEqualTo(100L)
    }

    @Test
    fun `without frame and paused returns 200ms`() {
        val delay = calculateMaskDelay(null, currentTime = 5000L, isPlaying = false)
        assertThat(delay).isEqualTo(200L)
    }

    @Test
    fun `with frame and paused returns 200ms`() {
        val frame = DanmakuMobMaskFrame(range = 1000L until 2000L, width = 10, height = 10, image = byteArrayOf())
        val delay = calculateMaskDelay(frame, currentTime = 1500L, isPlaying = false)
        assertThat(delay).isEqualTo(200L)
    }
}
