package dev.frost819.newbv.player.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoResolutionTest {

    @Test
    fun `fromCode returns correct resolution`() {
        assertEquals(VideoResolution.R240P, VideoResolution.fromCode(6))
        assertEquals(VideoResolution.R480P, VideoResolution.fromCode(32))
        assertEquals(VideoResolution.R720P, VideoResolution.fromCode(64))
        assertEquals(VideoResolution.R1080P, VideoResolution.fromCode(80))
        assertEquals(VideoResolution.R4K, VideoResolution.fromCode(120))
        assertEquals(VideoResolution.R8K, VideoResolution.fromCode(127))
    }

    @Test
    fun `fromCode returns R1080P for unknown code`() {
        assertEquals(VideoResolution.R1080P, VideoResolution.fromCode(0))
        assertEquals(VideoResolution.R1080P, VideoResolution.fromCode(999))
    }

    @Test
    fun `fromCode roundtrip with code`() {
        VideoResolution.entries.forEach { res ->
            assertEquals(res, VideoResolution.fromCode(res.code))
        }
    }

    @Test
    fun `R720P60 has correct code`() {
        assertEquals(74, VideoResolution.R720P60.code)
    }

    @Test
    fun `R1080P60 has correct code`() {
        assertEquals(116, VideoResolution.R1080P60.code)
    }

    @Test
    fun `RDolby has correct code`() {
        assertEquals(126, VideoResolution.RDolby.code)
    }

    @Test
    fun `RHdr has correct code`() {
        assertEquals(125, VideoResolution.RHdr.code)
    }

    @Test
    fun `display names and short names are non-empty`() {
        VideoResolution.entries.forEach { res ->
            assert(res.displayName.isNotBlank()) { "${res.name} displayName is blank" }
            assert(res.shortName.isNotBlank()) { "${res.name} shortName is blank" }
        }
    }
}
