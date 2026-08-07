package dev.frost819.newbv.biliapi.entity.danmaku

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.jupiter.api.Test

/**
 * [DanmakuMobMaskFrame] equals/hashCode 及 [DanmakuMask] 解析的单元测试。
 */
class DanmakuMobMaskFrameTest {
    @Test
    fun `equals returns true for same reference`() {
        val frame = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame.equals(frame)).isTrue()
    }

    @Test
    fun `equals returns true for same values`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame1).isEqualTo(frame2)
    }

    @Test
    fun `equals returns false for different range`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 2000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame1).isNotEqualTo(frame2)
    }

    @Test
    fun `equals returns false for different width`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 20, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame1).isNotEqualTo(frame2)
    }

    @Test
    fun `equals returns false for different height`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 10, image = byteArrayOf(1, 2, 3))
        assertThat(frame1).isNotEqualTo(frame2)
    }

    @Test
    fun `equals returns false for different image`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(4, 5, 6))
        assertThat(frame1).isNotEqualTo(frame2)
    }

    @Test
    fun `equals returns false for non DanmakuMobMaskFrame type`() {
        val frame = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame.equals("string")).isFalse()
        assertThat(frame.equals(null)).isFalse()
    }

    @Test
    fun `hashCode is consistent for same values`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        assertThat(frame1.hashCode()).isEqualTo(frame2.hashCode())
    }

    @Test
    fun `hashCode differs for different values`() {
        val frame1 = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1, 2, 3))
        val frame2 = DanmakuMobMaskFrame(range = 0L until 2000L, width = 20, height = 10, image = byteArrayOf(4, 5, 6))
        assertThat(frame1.hashCode()).isNotEqualTo(frame2.hashCode())
    }

    // ------------------------------------------------------------------
    // DanmakuWebMaskFrame data class
    // ------------------------------------------------------------------

    @Test
    fun `DanmakuWebMaskFrame holds range and svg`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 5000L, svg = "<svg></svg>")
        assertThat(frame.range).isEqualTo(0L until 5000L)
        assertThat(frame.svg).isEqualTo("<svg></svg>")
    }

    @Test
    fun `DanmakuWebMaskFrame equals is reference equality`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 1000L, svg = "test")
        assertThat(frame.equals(frame)).isTrue()
    }

    // ------------------------------------------------------------------
    // DanmakuMaskSegment data class
    // ------------------------------------------------------------------

    @Test
    fun `DanmakuMaskSegment holds range and frames`() {
        val frame = DanmakuMobMaskFrame(range = 0L until 1000L, width = 10, height = 5, image = byteArrayOf(1))
        val segment = DanmakuMaskSegment(range = 0L until 10000L, frames = listOf(frame))
        assertThat(segment.range).isEqualTo(0L until 10000L)
        assertThat(segment.frames).hasSize(1)
    }

    // ------------------------------------------------------------------
    // DanmakuMask parsing from binary
    // ------------------------------------------------------------------

    /**
     * Builds a minimal MASK binary for [DanmakuMask.fromBinary].
     *
     * Format: magic("MASK") + version(int32) + skip(4 bytes) + size(int32) +
     * [time(int64) + offset(int64)] * size + gzip-compressed-segment-data
     */
    private fun buildMaskBinary(
        segmentTimes: List<Long>,
        segmentData: ByteArray,
    ): ByteArray {
        val buffer = Buffer()
        // magic
        buffer.writeUtf8("MASK")
        // version
        buffer.writeInt(1)
        // skip 4 bytes
        buffer.writeInt(0)
        // size
        buffer.writeInt(segmentTimes.size)
        // times and offsets
        var offset = 0L
        for ((index, time) in segmentTimes.withIndex()) {
            buffer.writeLong(time)
            buffer.writeLong(offset)
            // next offset would point past current segment data (we only have 1 segment)
            if (index == 0 && segmentTimes.size > 1) {
                offset = segmentData.size.toLong()
            }
        }
        // segment data (gzip compressed)
        buffer.write(segmentData)
        return buffer.readByteArray()
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val sink = Buffer()
        GzipSink(sink).buffer().use { it.write(data) }
        return sink.readByteArray()
    }

    @Test
    fun `DanmakuMask fromBinary with invalid magic throws exception`() {
        val invalidData = ByteArray(20) { 0 }

        try {
            DanmakuMask.fromBinary(invalidData, DanmakuMaskType.WebMask)
            assertThat(false).isTrue()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Not a mask file")
        }
    }

    @Test
    fun `DanmakuMask fromBinary with single segment returns correct segmentCount`() {
        val segmentData = gzipCompress(ByteArray(0))
        val binary = buildMaskBinary(listOf(60000L), segmentData)

        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        assertThat(mask.segmentCount).isEqualTo(1)
    }

    @Test
    fun `DanmakuMask getSegmentAt returns null for negative position`() {
        val segmentData = gzipCompress(ByteArray(0))
        val binary = buildMaskBinary(listOf(60000L), segmentData)

        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        // negative position is not in any segment range
        val segment = mask.getSegmentAt(-1L)
        assertThat(segment).isNull()
    }

    @Test
    fun `DanmakuMask getSegmentAt returns segment for position within range`() {
        val segmentData = gzipCompress(ByteArray(0))
        val binary = buildMaskBinary(listOf(60000L), segmentData)

        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        val segment = mask.getSegmentAt(30000L)
        assertThat(segment).isNotNull()
        assertThat(segment!!.frames).isEmpty()
    }

    @Test
    fun `DanmakuMask segments property returns all segments`() {
        val segmentData = gzipCompress(ByteArray(0))
        val binary = buildMaskBinary(listOf(60000L), segmentData)

        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        assertThat(mask.segments).hasSize(1)
    }

    @Test
    fun `DanmakuMaskType enum has WebMask and MobMask`() {
        assertThat(DanmakuMaskType.entries).containsExactly(DanmakuMaskType.WebMask, DanmakuMaskType.MobMask)
    }
}
