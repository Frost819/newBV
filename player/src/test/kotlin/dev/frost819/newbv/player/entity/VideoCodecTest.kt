package dev.frost819.newbv.player.entity

import dev.frost819.newbv.biliapi.entity.CodeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoCodecTest {

    @Test
    fun `fromCode returns correct codec by ordinal`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCode(0))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCode(1))
        assertEquals(VideoCodec.AV1, VideoCodec.fromCode(2))
    }

    @Test
    fun `fromCode returns AVC for null`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCode(null))
    }

    @Test
    fun `fromCode returns AVC for unknown code`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCode(99))
    }

    @Test
    fun `fromCodecString matches by prefix`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCodecString("avc1.640034"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecString("hev1.1.6.L1.40"))
        assertEquals(VideoCodec.AV1, VideoCodec.fromCodecString("av01.0.04M.08"))
        assertEquals(VideoCodec.DVH1, VideoCodec.fromCodecString("dvh1.05.06"))
        assertEquals(VideoCodec.HVC1, VideoCodec.fromCodecString("hvc1.1.6.L1.B0"))
    }

    @Test
    fun `fromCodecString returns null for unknown prefix`() {
        assertEquals(null, VideoCodec.fromCodecString("vp09.00.10.08"))
    }

    @Test
    fun `fromCodecId returns correct codec`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCodecId(7))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromCodecId(12))
        assertEquals(VideoCodec.AV1, VideoCodec.fromCodecId(13))
    }

    @Test
    fun `fromCodecId returns AVC for unknown id`() {
        assertEquals(VideoCodec.AVC, VideoCodec.fromCodecId(99))
    }

    @Test
    fun `toBiliApiCodeType returns correct CodeType`() {
        assertEquals(CodeType.Code264, VideoCodec.AVC.toBiliApiCodeType())
        assertEquals(CodeType.Code265, VideoCodec.HEVC.toBiliApiCodeType())
        assertEquals(CodeType.CodeAv1, VideoCodec.AV1.toBiliApiCodeType())
        assertEquals(CodeType.Code265, VideoCodec.DVH1.toBiliApiCodeType())
        assertEquals(CodeType.Code265, VideoCodec.HVC1.toBiliApiCodeType())
    }

    @Test
    fun `codec display names are non-empty`() {
        VideoCodec.entries.forEach { codec ->
            assert(codec.displayName.isNotBlank()) { "${codec.name} displayName is blank" }
        }
    }
}
