package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.DashVideo
import dev.frost819.newbv.biliapi.entity.PlayData
import dev.frost819.newbv.data.datastore.VideoCodec
import org.junit.jupiter.api.Test

/**
 * [PlaybackSelection] 纯函数单测。
 *
 * 覆盖：编码表兜底（#263）、画质遍历顺序、能力感知选流（#288）。
 */
class PlaybackSelectionTest {
    private fun dashVideo(
        quality: Int,
        codecId: Int,
        codecs: String,
        width: Int = 1920,
        height: Int = 1080,
        frameRate: String = "30",
    ) = DashVideo(
        quality = quality,
        baseUrl = "https://example.com/$quality-$codecId.m4s",
        bandwidth = 1_000_000,
        codecId = codecId,
        width = width,
        height = height,
        frameRate = frameRate,
        backUrl = emptyList(),
        codecs = codecs,
    )

    private fun playData(
        videos: List<DashVideo>,
        codec: Map<Int, List<String>> = emptyMap(),
    ) = PlayData(dashVideos = videos, dashAudios = emptyList(), codec = codec)

    private fun capability(vararg decodable: VideoCodec): VideoCapabilityProvider =
        capabilityByProfile { it.codec in decodable }

    private fun capabilityByProfile(predicate: (VideoDecodeProfile) -> Boolean): VideoCapabilityProvider =
        object : VideoCapabilityProvider {
            override fun isDecodable(profile: VideoDecodeProfile): Boolean = predicate(profile)
        }

    @Test
    fun `collectCodecs falls back to dash tracks when codec table empty`() {
        // Given: Web codec 表为空（support_formats.codecs=null）
        val data = playData(listOf(dashVideo(80, 12, "hev1.1.6.L120.90")))

        // When
        val codecs = collectCodecs(data, 80)

        // Then
        assertThat(codecs).containsExactly(VideoCodec.HEVC)
    }

    @Test
    fun `collectCodecs merges table and tracks and sorts by ordinal`() {
        val data =
            playData(
                videos = listOf(dashVideo(80, 7, "avc1.640028"), dashVideo(80, 13, "av01.0.08M.08")),
                codec = mapOf(80 to listOf("hev1.1.6.L120.90")),
            )

        assertThat(collectCodecs(data, 80))
            .containsExactly(VideoCodec.AVC, VideoCodec.HEVC, VideoCodec.AV1)
            .inOrder()
    }

    @Test
    fun `orderQualities excludes higher qualities when lower ones available`() {
        // desired=80（1080P），可用 64/80/120 → 只用 <=80 的低档位，从高到低
        assertThat(orderQualities(listOf(120, 64, 80), 80)).containsExactly(80, 64).inOrder()
    }

    @Test
    fun `orderQualities all above desired picks lowest first`() {
        // 无 <=80 的档位，全部高于目标 → 从低到高
        assertThat(orderQualities(listOf(120, 127), 80)).containsExactly(120, 127).inOrder()
    }

    @Test
    fun `pickDecodableProfile prefers preferred codec at requested quality`() {
        val data =
            playData(
                listOf(
                    dashVideo(80, 7, "avc1.640028"),
                    dashVideo(80, 12, "hev1.1.6.L120.90"),
                ),
            )

        val candidate =
            pickDecodableProfile(
                data = data,
                requestedQualityId = 80,
                preferredCodec = VideoCodec.AVC,
                capabilityProvider = capability(VideoCodec.AVC, VideoCodec.HEVC),
            )

        assertThat(candidate).isEqualTo(PlaybackCandidate(80, VideoCodec.AVC))
    }

    @Test
    fun `pickDecodableProfile switches codec when preferred not decodable`() {
        // AVC 4K 超出能力，HEVC 4K 可解
        val data =
            playData(
                listOf(
                    dashVideo(120, 7, "avc1.640034", width = 2160, height = 3840, frameRate = "60"),
                    dashVideo(120, 12, "hev1.1.6.L153.90", width = 2160, height = 3840, frameRate = "60"),
                ),
            )

        val candidate =
            pickDecodableProfile(
                data = data,
                requestedQualityId = 120,
                preferredCodec = VideoCodec.AVC,
                capabilityProvider = capability(VideoCodec.HEVC),
            )

        assertThat(candidate).isEqualTo(PlaybackCandidate(120, VideoCodec.HEVC))
    }

    @Test
    fun `pickDecodableProfile lowers quality when no codec at requested quality decodable`() {
        // 4K 全部不可解，降到 1080P AVC
        val data =
            playData(
                listOf(
                    dashVideo(120, 7, "avc1.640034", width = 2160, height = 3840),
                    dashVideo(120, 12, "hev1.1.6.L153.90", width = 2160, height = 3840),
                    dashVideo(80, 7, "avc1.640028"),
                ),
            )

        // AVC 硬解只支持到 1080P，4K AVC 视为超能力
        val candidate =
            pickDecodableProfile(
                data,
                requestedQualityId = 120,
                preferredCodec = VideoCodec.AVC,
                capabilityProvider = capabilityByProfile { it.codec == VideoCodec.AVC && it.height <= 1080 },
            )

        assertThat(candidate).isEqualTo(PlaybackCandidate(80, VideoCodec.AVC))
    }

    @Test
    fun `pickDecodableProfile returns null when nothing decodable`() {
        val data = playData(listOf(dashVideo(120, 7, "avc1.640034", width = 2160, height = 3840)))

        val candidate =
            pickDecodableProfile(data, requestedQualityId = 120, preferredCodec = VideoCodec.AVC, capability())

        assertThat(candidate).isNull()
    }

    @Test
    fun `collectCodecs returns empty for unknown quality`() {
        val data = playData(listOf(dashVideo(80, 7, "avc1.640028")))

        assertThat(collectCodecs(data, 120)).isEmpty()
    }

    @Test
    fun `findTrack matches quality and codec`() {
        val target = dashVideo(120, 12, "hev1.1.6.L153.90", 2160, 3840)
        val data = playData(listOf(dashVideo(120, 7, "avc1.640034", 2160, 3840), target))

        assertThat(findTrack(data, 120, VideoCodec.HEVC)).isEqualTo(target)
        assertThat(findTrack(data, 80, VideoCodec.AVC)).isNull()
    }

    @Test
    fun `pickDecodableProfile all above requested picks lowest`() {
        // 目标 720P，但只提供 1080P/4K → 选最低的 1080P
        val data =
            playData(
                listOf(
                    dashVideo(120, 7, "avc1.640034", 2160, 3840),
                    dashVideo(80, 7, "avc1.640028"),
                ),
            )

        val candidate =
            pickDecodableProfile(
                data = data,
                requestedQualityId = 64,
                preferredCodec = VideoCodec.AVC,
                capabilityProvider = capability(VideoCodec.AVC),
            )

        assertThat(candidate).isEqualTo(PlaybackCandidate(80, VideoCodec.AVC))
    }

    @Test
    fun `trackMatchesCodec uses codecs string then codecId`() {
        assertThat(trackMatchesCodec(dashVideo(80, 7, "hvc1.1.6.L120.90"), VideoCodec.HEVC)).isTrue()
        assertThat(trackMatchesCodec(dashVideo(80, 7, "avc1.640028"), VideoCodec.HEVC)).isFalse()
        // codecs 为空时用 codecId 兜底
        assertThat(trackMatchesCodec(dashVideo(80, 12, ""), VideoCodec.HEVC)).isTrue()
    }
}
