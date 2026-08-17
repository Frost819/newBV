package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.live.MediaInfo
import dev.frost819.newbv.biliapi.http.entity.live.PlayCodec
import dev.frost819.newbv.biliapi.http.entity.live.PlayFormat
import dev.frost819.newbv.biliapi.http.entity.live.PlayStream
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrl
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfo
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfoItem
import dev.frost819.newbv.biliapi.http.entity.live.QnDesc
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
import org.junit.jupiter.api.Test

/**
 * [LiveRepository] 的单元测试。
 *
 * 验证直播流解析不依赖真实网络，并覆盖协议、格式和编码的优先级策略。
 */
class LiveRepositoryUnitTest {
    private val repository = LiveRepository()

    @Test
    fun `resolveStreamUrl prefers flv and avc from http stream`() {
        val data =
            playInfo(
                streams =
                    listOf(
                        stream(
                            "http_stream",
                            format("fmp4", codec("hevc", "hevc.m4s")),
                            format("flv", codec("hevc", "hevc.flv"), codec("avc", "avc.flv")),
                        ),
                        stream("http_hls", format("fmp4", codec("avc", "fallback.m3u8"))),
                    ),
            )

        assertThat(repository.resolveStreamUrl(data)).isEqualTo("https://cdn.example/avc.flv")
    }

    @Test
    fun `resolveStreamUrl falls back to hls when flv has no usable format`() {
        val data =
            playInfo(
                streams =
                    listOf(
                        stream("http_stream", format("flv")),
                        stream("http_hls", format("fmp4", codec("avc", "live.m3u8"))),
                    ),
            )

        assertThat(repository.resolveStreamUrl(data)).isEqualTo("https://cdn.example/live.m3u8")
    }

    @Test
    fun `resolveStreamUrl uses first available codec as final fallback`() {
        val data = playInfo(streams = listOf(stream("other", format("ts", codec("av1", "live.ts")))))

        assertThat(repository.resolveStreamUrl(data)).isEqualTo("https://cdn.example/live.ts")
    }

    private fun playInfo(streams: List<PlayStream>) =
        RoomPlayInfoV2Data(
            roomId = 1718159119,
            shortId = 0,
            uid = 1,
            liveStatus = 1,
            playUrlInfo = PlayUrlInfo(PlayUrl(cid = 1, qnDesc = listOf(QnDesc(10000, "原画")), stream = streams)),
        )

    private fun stream(
        protocol: String,
        vararg formats: PlayFormat,
    ) = PlayStream(protocol, formats.toList())

    private fun format(
        name: String,
        vararg codecs: PlayCodec,
    ) = PlayFormat(name, codecs.toList())

    private fun codec(
        name: String,
        path: String,
    ) = PlayCodec(
        codecName = name,
        currentQn = 10000,
        baseUrl = "/$path",
        urlInfo = listOf(PlayUrlInfoItem(host = "https://cdn.example", extra = "")),
        mediaInfo = MediaInfo(),
    )
}
