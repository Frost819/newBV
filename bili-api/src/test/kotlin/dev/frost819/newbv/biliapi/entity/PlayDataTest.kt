package dev.frost819.newbv.biliapi.entity

import bilibili.app.playerunite.v1.playViewUniteReply
import bilibili.pgc.gateway.player.v2.dashItem
import bilibili.pgc.gateway.player.v2.dashVideo
import bilibili.pgc.gateway.player.v2.dolbyItem
import bilibili.pgc.gateway.player.v2.playViewBusinessInfo
import bilibili.pgc.gateway.player.v2.playViewReply
import bilibili.pgc.gateway.player.v2.responseUrl
import bilibili.pgc.gateway.player.v2.segmentVideo
import bilibili.pgc.gateway.player.v2.stream
import bilibili.pgc.gateway.player.v2.streamInfo
import bilibili.pgc.gateway.player.v2.videoInfo
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.video.Dash
import dev.frost819.newbv.biliapi.http.entity.video.DashData
import dev.frost819.newbv.biliapi.http.entity.video.DashDolby
import dev.frost819.newbv.biliapi.http.entity.video.DashFlac
import dev.frost819.newbv.biliapi.http.entity.video.Durl
import dev.frost819.newbv.biliapi.http.entity.video.PlayUrlData
import dev.frost819.newbv.biliapi.http.entity.video.PlayUrlV2Data
import dev.frost819.newbv.biliapi.http.entity.video.SegmentBase
import dev.frost819.newbv.biliapi.http.entity.video.SupportFormat
import org.junit.jupiter.api.Test
import bilibili.playershared.dashItem as sharedDashItem
import bilibili.playershared.dashVideo as sharedDashVideo
import bilibili.playershared.dolbyItem as sharedDolbyItem
import bilibili.playershared.lossLessItem as sharedLossLessItem
import bilibili.playershared.responseUrl as sharedResponseUrl
import bilibili.playershared.segmentVideo as sharedSegmentVideo
import bilibili.playershared.stream as sharedStream
import bilibili.playershared.streamInfo as sharedStreamInfo
import bilibili.playershared.vodInfo as sharedVodInfo

/**
 * [PlayData] 实体的单元测试。
 *
 * 覆盖 [PlayData.fromPlayUrlData] 的 Web API 响应解析（DASH 视频/音频、杜比/FLAC、
 * 试看流 durl、codec 映射、空值处理）以及 [PlayData.plus] 合并算子的
 * 去重、排序、优先级与 needPay 聚合逻辑。所有测试为纯数据变换，不依赖网络。
 */
class PlayDataTest {
    // region ---- 测试夹具构造辅助 ----

    private fun segmentBase() =
        SegmentBase(
            initialization = "init-range",
            indexRange = "0-999",
        )

    /** 构造视频流 [DashData]，id 对应画质、codecId 对应编码。 */
    private fun videoDashData(
        id: Int,
        codecId: Int = 7,
        baseUrl: String = "http://cdn.test/video-$id.m4s",
        bandwidth: Int = 1_000_000 + id,
        codecs: String = "avc1.640028",
        width: Int = 1920,
        height: Int = 1080,
        frameRate: String = "30",
        backupUrl: List<String> = listOf("http://cdn2.test/video-$id.m4s"),
    ) = DashData(
        id = id,
        baseUrl = baseUrl,
        backupUrl = backupUrl,
        bandwidth = bandwidth,
        mimeType = "video/mp4",
        codecs = codecs,
        width = width,
        height = height,
        frameRate = frameRate,
        sar = "1:1",
        startWithSap = 1,
        segmentBase = segmentBase(),
        codecId = codecId,
    )

    /** 构造音频流 [DashData]，id 对应编码标识。 */
    private fun audioDashData(
        id: Int,
        baseUrl: String = "http://cdn.test/audio-$id.m4s",
        bandwidth: Int = 200_000 + id,
        backupUrl: List<String> = listOf("http://cdn2.test/audio-$id.m4s"),
        codecId: Int = 0,
    ) = DashData(
        id = id,
        baseUrl = baseUrl,
        backupUrl = backupUrl,
        bandwidth = bandwidth,
        mimeType = "audio/mp4",
        codecs = "mp4a.40.2",
        width = 0,
        height = 0,
        frameRate = "",
        sar = "1:1",
        startWithSap = 1,
        segmentBase = segmentBase(),
        codecId = codecId,
    )

    private fun dash(
        video: List<DashData> = emptyList(),
        audio: List<DashData>? = null,
        dolby: DashDolby = DashDolby(),
        flac: DashFlac? = null,
    ) = Dash(
        duration = 120_000,
        minBufferTime = 1.5f,
        video = video,
        audio = audio,
        dolby = dolby,
        flac = flac,
    )

    /** 构造 [PlayUrlData]，仅填充 [PlayData.fromPlayUrlData] 关心的字段。 */
    private fun playUrlData(
        dash: Dash? = null,
        durl: List<Durl> = emptyList(),
        quality: Int = 80,
        videoCodecId: Int = 7,
        supportFormats: List<SupportFormat> = emptyList(),
    ) = PlayUrlData(
        from = "server",
        result = "suee",
        message = "",
        quality = quality,
        format = "dash",
        timeLength = 120_000,
        acceptFormat = "hdflv2,flv,flv720,mp4",
        videoCodecId = videoCodecId,
        seekParam = "start",
        seekType = "offset",
        dash = dash,
        durl = durl,
        supportFormats = supportFormats,
    )

    private fun durl(
        url: String = "http://cdn.test/preview.flv",
        backupUrl: List<String> = listOf("http://cdn2.test/preview.flv"),
        order: Int = 1,
    ) = Durl(
        order = order,
        length = 120_000,
        size = 5_000_000,
        ahead = "",
        vhead = "",
        url = url,
        backupUrl = backupUrl,
    )

    private fun supportFormat(
        quality: Int,
        codecs: List<String>? = listOf("avc1"),
        description: String = "1080P",
    ) = SupportFormat(
        quality = quality,
        format = "flv",
        newDescription = description,
        description = description,
        displayDesc = description,
        superScript = "",
        codecs = codecs,
    )

    /** 构造 [PlayData]，为 [PlayData.plus] 测试提供带默认值的便捷构造。 */
    private fun playData(
        dashVideos: List<DashVideo> = emptyList(),
        dashAudios: List<DashAudio> = emptyList(),
        dolby: DashAudio? = null,
        flac: DashAudio? = null,
        codec: Map<Int, List<String>> = emptyMap(),
        needPay: Boolean = false,
    ) = PlayData(dashVideos, dashAudios, dolby, flac, codec, needPay)

    // endregion

    // region ---- fromPlayUrlData: DASH 视频解析 ----

    @Test
    fun `fromPlayUrlData parses dash video with correct field mapping`() {
        // Given: dash 含两条视频流（不同画质、不同编码）
        val v1 = videoDashData(id = 80, codecId = 7, codecs = "avc1.640028", bandwidth = 1_000_080)
        val v2 = videoDashData(id = 120, codecId = 12, codecs = "hev1.1.6.L120", bandwidth = 2_000_120)
        val data =
            playUrlData(
                dash = dash(video = listOf(v1, v2)),
                supportFormats =
                    listOf(
                        supportFormat(80, listOf("avc1")),
                        supportFormat(120, listOf("hev1")),
                    ),
            )

        // When
        val playData = PlayData.fromPlayUrlData(data)

        // Then: 每个字段逐一映射
        assertThat(playData.dashVideos).hasSize(2)

        val first = playData.dashVideos[0]
        assertThat(first.quality).isEqualTo(80)
        assertThat(first.codecId).isEqualTo(7)
        assertThat(first.baseUrl).isEqualTo("http://cdn.test/video-80.m4s")
        assertThat(first.bandwidth).isEqualTo(1_000_080)
        assertThat(first.codecs).isEqualTo("avc1.640028")
        assertThat(first.width).isEqualTo(1920)
        assertThat(first.height).isEqualTo(1080)
        assertThat(first.frameRate).isEqualTo("30")
        assertThat(first.backUrl).containsExactly("http://cdn2.test/video-80.m4s")

        val second = playData.dashVideos[1]
        assertThat(second.quality).isEqualTo(120)
        assertThat(second.codecId).isEqualTo(12)
        assertThat(second.codecs).isEqualTo("hev1.1.6.L120")
        assertThat(second.bandwidth).isEqualTo(2_000_120)
    }

    @Test
    fun `fromPlayUrlData uses codecId not id for DashVideo codecId - regression test`() {
        // 回归测试：曾出现 codecId = it.id 的 bug（把画质 id 当成编码 id）。
        // 当 id(画质) 与 codecId(编码) 不同时，DashVideo.codecId 必须等于 DashData.codecId。
        // Given: id=80(画质), codecId=7(avc1) —— 两者不同
        val v = videoDashData(id = 80, codecId = 7)
        val data = playUrlData(dash = dash(video = listOf(v)))

        // When
        val playData = PlayData.fromPlayUrlData(data)

        // Then
        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].quality).isEqualTo(80)
        assertThat(playData.dashVideos[0].codecId).isEqualTo(7)
    }

    @Test
    fun `fromPlayUrlData keeps multiple videos with same quality but different codecId`() {
        // 同一画质下有多种编码（avc1 + hev1 + av01），应全部保留
        val v1 = videoDashData(id = 80, codecId = 7, baseUrl = "http://c/avc.m4s")
        val v2 = videoDashData(id = 80, codecId = 12, baseUrl = "http://c/hev.m4s")
        val v3 = videoDashData(id = 80, codecId = 13, baseUrl = "http://c/av1.m4s")
        val data = playUrlData(dash = dash(video = listOf(v1, v2, v3)))

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dashVideos).hasSize(3)
        assertThat(playData.dashVideos.map { it.codecId }).containsExactly(7, 12, 13)
        assertThat(playData.dashVideos.map { it.baseUrl })
            .containsExactly("http://c/avc.m4s", "http://c/hev.m4s", "http://c/av1.m4s")
    }

    // endregion

    // region ---- fromPlayUrlData: 音频解析 ----

    @Test
    fun `fromPlayUrlData parses dash audio streams with correct field mapping`() {
        val a1 = audioDashData(id = 30216, baseUrl = "http://c/a1.m4s", bandwidth = 200_016)
        val a2 = audioDashData(id = 30232, baseUrl = "http://c/a2.m4s", bandwidth = 300_032)
        val data = playUrlData(dash = dash(audio = listOf(a1, a2)))

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dashAudios).hasSize(2)

        val first = playData.dashAudios[0]
        assertThat(first.baseUrl).isEqualTo("http://c/a1.m4s")
        assertThat(first.bandwidth).isEqualTo(200_016)
        assertThat(first.codecId).isEqualTo(30216)
        assertThat(first.backUrl).containsExactly("http://cdn2.test/audio-30216.m4s")

        val second = playData.dashAudios[1]
        assertThat(second.codecId).isEqualTo(30232)
        assertThat(second.bandwidth).isEqualTo(300_032)
    }

    @Test
    fun `fromPlayUrlData returns empty audio list when dash audio is null`() {
        val data =
            playUrlData(
                dash = dash(video = listOf(videoDashData(id = 80)), audio = null),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dashAudios).isEmpty()
    }

    // endregion

    // region ---- fromPlayUrlData: 杜比音频解析 ----

    @Test
    fun `fromPlayUrlData parses dolby audio from first dolby item`() {
        val dolbyAudio = audioDashData(id = 30250, baseUrl = "http://c/dolby.m4s", bandwidth = 500_000)
        val data =
            playUrlData(
                dash =
                    dash(
                        video = listOf(videoDashData(id = 126)),
                        dolby = DashDolby(audio = listOf(dolbyAudio), type = 2),
                    ),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNotNull()
        assertThat(playData.dolby!!.baseUrl).isEqualTo("http://c/dolby.m4s")
        assertThat(playData.dolby!!.bandwidth).isEqualTo(500_000)
        assertThat(playData.dolby!!.codecId).isEqualTo(30250)
        assertThat(playData.dolby!!.backUrl).containsExactly("http://cdn2.test/audio-30250.m4s")
    }

    @Test
    fun `fromPlayUrlData picks first dolby item when multiple exist`() {
        val a1 = audioDashData(id = 30250, baseUrl = "http://c/d1.m4s")
        val a2 = audioDashData(id = 30251, baseUrl = "http://c/d2.m4s")
        val data =
            playUrlData(
                dash = dash(dolby = DashDolby(audio = listOf(a1, a2))),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNotNull()
        assertThat(playData.dolby!!.baseUrl).isEqualTo("http://c/d1.m4s")
        assertThat(playData.dolby!!.codecId).isEqualTo(30250)
    }

    @Test
    fun `fromPlayUrlData returns null dolby when dolby audio is null`() {
        val data =
            playUrlData(
                dash = dash(dolby = DashDolby(audio = null)),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNull()
    }

    @Test
    fun `fromPlayUrlData returns null dolby when dolby audio list is empty`() {
        val data =
            playUrlData(
                dash = dash(dolby = DashDolby(audio = emptyList())),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNull()
    }

    @Test
    fun `fromPlayUrlData returns null dolby when dash is null`() {
        val data = playUrlData(dash = null)

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNull()
    }

    // endregion

    // region ---- fromPlayUrlData: FLAC 音频解析 ----

    @Test
    fun `fromPlayUrlData parses flac audio with correct field mapping`() {
        val flacAudio = audioDashData(id = 30251, baseUrl = "http://c/flac.m4s", bandwidth = 800_000)
        val data =
            playUrlData(
                dash =
                    dash(
                        flac = DashFlac(display = true, audio = flacAudio),
                    ),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.flac).isNotNull()
        assertThat(playData.flac!!.baseUrl).isEqualTo("http://c/flac.m4s")
        assertThat(playData.flac!!.bandwidth).isEqualTo(800_000)
        assertThat(playData.flac!!.codecId).isEqualTo(30251)
        assertThat(playData.flac!!.backUrl).containsExactly("http://cdn2.test/audio-30251.m4s")
    }

    @Test
    fun `fromPlayUrlData returns null flac when flac is null`() {
        val data = playUrlData(dash = dash(flac = null))

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.flac).isNull()
    }

    @Test
    fun `fromPlayUrlData returns null flac when flac audio is null`() {
        val data =
            playUrlData(
                dash = dash(flac = DashFlac(display = true, audio = null)),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.flac).isNull()
    }

    // endregion

    // region ---- fromPlayUrlData: needPay / 试看流 durl ----

    @Test
    fun `fromPlayUrlData sets needPay true when only durl present without dash`() {
        // 付费视频未付费：无 dash，仅有试看流 durl
        val data =
            playUrlData(
                dash = null,
                durl = listOf(durl(url = "http://c/preview1.flv")),
                quality = 80,
                videoCodecId = 7,
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.needPay).isTrue()
    }

    @Test
    fun `fromPlayUrlData sets needPay false when dash present even if durl also present`() {
        // 有 dash 时 hasDash=true，即使 durl 也存在，needPay 仍为 false
        val data =
            playUrlData(
                dash = dash(video = listOf(videoDashData(id = 80))),
                durl = listOf(durl()),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.needPay).isFalse()
        // 有 dash 时视频来自 dash.video，而非 durl
        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].baseUrl).isEqualTo("http://cdn.test/video-80.m4s")
    }

    @Test
    fun `fromPlayUrlData sets needPay false when neither dash nor durl present`() {
        val data = playUrlData(dash = null, durl = emptyList())

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.needPay).isFalse()
        assertThat(playData.dashVideos).isEmpty()
    }

    @Test
    fun `fromPlayUrlData converts durl to DashVideo with correct field mapping`() {
        val data =
            playUrlData(
                dash = null,
                durl =
                    listOf(
                        durl(url = "http://c/p1.flv", backupUrl = listOf("http://c2/p1.flv"), order = 1),
                        durl(url = "http://c/p2.flv", backupUrl = listOf("http://c2/p2.flv"), order = 2),
                    ),
                quality = 64,
                videoCodecId = 7,
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dashVideos).hasSize(2)

        val v0 = playData.dashVideos[0]
        // durl 模式下 quality = playUrlData.quality
        assertThat(v0.quality).isEqualTo(64)
        assertThat(v0.baseUrl).isEqualTo("http://c/p1.flv")
        assertThat(v0.backUrl).containsExactly("http://c2/p1.flv")
        // codecId = playUrlData.videoCodecId
        assertThat(v0.codecId).isEqualTo(7)
        // durl 模式下这些字段为默认值
        assertThat(v0.bandwidth).isEqualTo(0)
        assertThat(v0.width).isEqualTo(0)
        assertThat(v0.height).isEqualTo(0)
        assertThat(v0.frameRate).isEqualTo("")
        assertThat(v0.codecs).isEqualTo("")

        val v1 = playData.dashVideos[1]
        assertThat(v1.baseUrl).isEqualTo("http://c/p2.flv")
    }

    // endregion

    // region ---- fromPlayUrlData: codec 映射 ----

    @Test
    fun `fromPlayUrlData builds codec map from supportFormats`() {
        val data =
            playUrlData(
                dash = dash(video = listOf(videoDashData(id = 80))),
                supportFormats =
                    listOf(
                        supportFormat(80, listOf("avc1", "hev1")),
                        supportFormat(120, listOf("avc1", "hev1", "av01")),
                        supportFormat(126, listOf("avc1")),
                    ),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.codec).hasSize(3)
        assertThat(playData.codec[80]).containsExactly("avc1", "hev1")
        assertThat(playData.codec[120]).containsExactly("avc1", "hev1", "av01")
        assertThat(playData.codec[126]).containsExactly("avc1")
    }

    @Test
    fun `fromPlayUrlData skips supportFormats with null codecs`() {
        val data =
            playUrlData(
                supportFormats =
                    listOf(
                        supportFormat(80, listOf("avc1")),
                        supportFormat(120, codecs = null),
                    ),
            )

        val playData = PlayData.fromPlayUrlData(data)

        // null codecs 的项被 mapNotNull 过滤
        assertThat(playData.codec).hasSize(1)
        assertThat(playData.codec).containsKey(80)
        assertThat(playData.codec).doesNotContainKey(120)
    }

    @Test
    fun `fromPlayUrlData includes supportFormat with empty codecs list`() {
        // codecs 非空但为空列表时，仍被包含（仅 null 被过滤）
        val data =
            playUrlData(
                supportFormats = listOf(supportFormat(80, codecs = emptyList())),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.codec).hasSize(1)
        assertThat(playData.codec[80]).isEmpty()
    }

    @Test
    fun `fromPlayUrlData returns empty codec map when supportFormats is empty`() {
        val data =
            playUrlData(
                dash = dash(video = listOf(videoDashData(id = 80))),
            )

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.codec).isEmpty()
    }

    // endregion

    // region ---- fromPlayUrlData: 边界情况 ----

    @Test
    fun `fromPlayUrlData with empty dash returns empty lists and needPay false`() {
        // dash != null 但 video/audio 为空 → hasDash=true, isPreview=false
        val data = playUrlData(dash = dash(video = emptyList(), audio = null))

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dashVideos).isEmpty()
        assertThat(playData.dashAudios).isEmpty()
        assertThat(playData.dolby).isNull()
        assertThat(playData.flac).isNull()
        assertThat(playData.needPay).isFalse()
    }

    @Test
    fun `fromPlayUrlData with default DashDolby returns null dolby`() {
        // DashDolby() 默认 audio=null
        val data = playUrlData(dash = dash(dolby = DashDolby()))

        val playData = PlayData.fromPlayUrlData(data)

        assertThat(playData.dolby).isNull()
    }

    // endregion

    // region ---- plus: 视频合并、去重、排序 ----

    @Test
    fun `plus merges dashVideos and deduplicates by codecId_quality`() {
        // this 与 other 有相同 codecId+quality 的视频 → 去重（保留 this 的）
        val shared =
            DashVideo(
                quality = 80,
                baseUrl = "http://this/v.m4s",
                bandwidth = 100,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val dup =
            DashVideo(
                quality = 80,
                baseUrl = "http://other/v.m4s",
                bandwidth = 200,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val left = playData(dashVideos = listOf(shared))
        val other = playData(dashVideos = listOf(dup))

        val result = left + other

        // 去重后只剩一个，且是 this 的（baseUrl 指向 this）
        assertThat(result.dashVideos).hasSize(1)
        assertThat(result.dashVideos[0].baseUrl).isEqualTo("http://this/v.m4s")
    }

    @Test
    fun `plus keeps videos with same quality but different codecId`() {
        val v1 =
            DashVideo(
                quality = 80,
                baseUrl = "u1",
                bandwidth = 1,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val v2 =
            DashVideo(
                quality = 80,
                baseUrl = "u2",
                bandwidth = 2,
                codecId = 12,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "hev1",
            )
        val left = playData(dashVideos = listOf(v1))
        val other = playData(dashVideos = listOf(v2))

        val result = left + other

        assertThat(result.dashVideos).hasSize(2)
        assertThat(result.dashVideos.map { it.codecId }).containsExactly(7, 12)
    }

    @Test
    fun `plus keeps videos with same codecId but different quality`() {
        val v1 =
            DashVideo(
                quality = 80,
                baseUrl = "u1",
                bandwidth = 1,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val v2 =
            DashVideo(
                quality = 120,
                baseUrl = "u2",
                bandwidth = 2,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val left = playData(dashVideos = listOf(v1))
        val other = playData(dashVideos = listOf(v2))

        val result = left + other

        assertThat(result.dashVideos).hasSize(2)
    }

    @Test
    fun `plus sorts merged videos by quality descending`() {
        val low =
            DashVideo(
                quality = 16,
                baseUrl = "lo",
                bandwidth = 1,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val mid =
            DashVideo(
                quality = 80,
                baseUrl = "mid",
                bandwidth = 2,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val high =
            DashVideo(
                quality = 120,
                baseUrl = "hi",
                bandwidth = 3,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        // this 提供 low+high, other 提供 mid
        val left = playData(dashVideos = listOf(low, high))
        val other = playData(dashVideos = listOf(mid))

        val result = left + other

        // 注意 low 与 high codecId 相同但 quality 不同，不会被去重
        assertThat(result.dashVideos.map { it.quality }).containsExactly(120, 80, 16).inOrder()
    }

    @Test
    fun `plus stable-sorts videos with equal quality preserving first occurrence order`() {
        // quality 相同、codecId 不同 → 都保留；稳定排序保持原始相对顺序
        val v1 =
            DashVideo(
                quality = 80,
                baseUrl = "first",
                bandwidth = 1,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "avc1",
            )
        val v2 =
            DashVideo(
                quality = 80,
                baseUrl = "second",
                bandwidth = 2,
                codecId = 12,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "hev1",
            )
        val v3 =
            DashVideo(
                quality = 80,
                baseUrl = "third",
                bandwidth = 3,
                codecId = 13,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = "av01",
            )
        val left = playData(dashVideos = listOf(v1, v2))
        val other = playData(dashVideos = listOf(v3))

        val result = left + other

        assertThat(result.dashVideos.map { it.baseUrl })
            .containsExactly("first", "second", "third")
            .inOrder()
    }

    // endregion

    // region ---- plus: 音频合并、去重、排序 ----

    @Test
    fun `plus merges dashAudios and deduplicates by codecId`() {
        val a1 = DashAudio(baseUrl = "http://this/a.m4s", bandwidth = 100, codecId = 30216, backUrl = emptyList())
        val a2 = DashAudio(baseUrl = "http://other/a.m4s", bandwidth = 200, codecId = 30216, backUrl = emptyList())
        val left = playData(dashAudios = listOf(a1))
        val other = playData(dashAudios = listOf(a2))

        val result = left + other

        // 去重后保留 this 的（first wins）
        assertThat(result.dashAudios).hasSize(1)
        assertThat(result.dashAudios[0].baseUrl).isEqualTo("http://this/a.m4s")
    }

    @Test
    fun `plus keeps audios with different codecId`() {
        val a1 = DashAudio(baseUrl = "u1", bandwidth = 100, codecId = 30216, backUrl = emptyList())
        val a2 = DashAudio(baseUrl = "u2", bandwidth = 200, codecId = 30232, backUrl = emptyList())
        val left = playData(dashAudios = listOf(a1))
        val other = playData(dashAudios = listOf(a2))

        val result = left + other

        assertThat(result.dashAudios).hasSize(2)
        assertThat(result.dashAudios.map { it.codecId }).containsExactly(30232, 30216).inOrder()
    }

    @Test
    fun `plus sorts merged audios by codecId descending`() {
        val a1 = DashAudio(baseUrl = "lo", bandwidth = 1, codecId = 30216, backUrl = emptyList())
        val a2 = DashAudio(baseUrl = "hi", bandwidth = 2, codecId = 30280, backUrl = emptyList())
        val a3 = DashAudio(baseUrl = "mid", bandwidth = 3, codecId = 30232, backUrl = emptyList())
        val left = playData(dashAudios = listOf(a1, a2))
        val other = playData(dashAudios = listOf(a3))

        val result = left + other

        assertThat(result.dashAudios.map { it.codecId }).containsExactly(30280, 30232, 30216).inOrder()
    }

    // endregion

    // region ---- plus: dolby / flac 优先级 ----

    @Test
    fun `plus takes this dolby when both have dolby`() {
        val d1 = DashAudio(baseUrl = "this-dolby", bandwidth = 1, codecId = 30250, backUrl = emptyList())
        val d2 = DashAudio(baseUrl = "other-dolby", bandwidth = 2, codecId = 30251, backUrl = emptyList())
        val left = playData(dolby = d1)
        val other = playData(dolby = d2)

        val result = left + other

        assertThat(result.dolby).isNotNull()
        assertThat(result.dolby!!.baseUrl).isEqualTo("this-dolby")
    }

    @Test
    fun `plus takes other dolby when this dolby is null`() {
        val d2 = DashAudio(baseUrl = "other-dolby", bandwidth = 2, codecId = 30251, backUrl = emptyList())
        val left = playData(dolby = null)
        val other = playData(dolby = d2)

        val result = left + other

        assertThat(result.dolby).isNotNull()
        assertThat(result.dolby!!.baseUrl).isEqualTo("other-dolby")
    }

    @Test
    fun `plus returns null dolby when both are null`() {
        val left = playData(dolby = null)
        val other = playData(dolby = null)

        val result = left + other

        assertThat(result.dolby).isNull()
    }

    @Test
    fun `plus takes this flac when both have flac`() {
        val f1 = DashAudio(baseUrl = "this-flac", bandwidth = 1, codecId = 30251, backUrl = emptyList())
        val f2 = DashAudio(baseUrl = "other-flac", bandwidth = 2, codecId = 30252, backUrl = emptyList())
        val left = playData(flac = f1)
        val other = playData(flac = f2)

        val result = left + other

        assertThat(result.flac).isNotNull()
        assertThat(result.flac!!.baseUrl).isEqualTo("this-flac")
    }

    @Test
    fun `plus takes other flac when this flac is null`() {
        val f2 = DashAudio(baseUrl = "other-flac", bandwidth = 2, codecId = 30252, backUrl = emptyList())
        val left = playData(flac = null)
        val other = playData(flac = f2)

        val result = left + other

        assertThat(result.flac).isNotNull()
        assertThat(result.flac!!.baseUrl).isEqualTo("other-flac")
    }

    @Test
    fun `plus returns null flac when both are null`() {
        val left = playData(flac = null)
        val other = playData(flac = null)

        val result = left + other

        assertThat(result.flac).isNull()
    }

    // endregion

    // region ---- plus: codec 映射合并 ----

    @Test
    fun `plus merges codec maps with distinct values for shared keys`() {
        val left = playData(codec = mapOf(80 to listOf("avc1"), 120 to listOf("hev1")))
        val other = playData(codec = mapOf(80 to listOf("hev1", "av01")))

        val result = left + other

        // key 80: ["avc1"] + ["hev1", "av01"] 去重 → ["avc1", "hev1", "av01"]
        assertThat(result.codec[80]).containsExactly("avc1", "hev1", "av01")
        // key 120 仅 this 有 → 保留
        assertThat(result.codec[120]).containsExactly("hev1")
    }

    @Test
    fun `plus deduplicates overlapping codec values`() {
        val left = playData(codec = mapOf(80 to listOf("avc1", "hev1")))
        val other = playData(codec = mapOf(80 to listOf("hev1", "av01")))

        val result = left + other

        // ["avc1", "hev1", "hev1", "av01"] 去重 → ["avc1", "hev1", "av01"]
        assertThat(result.codec[80]).containsExactly("avc1", "hev1", "av01")
    }

    @Test
    fun `plus filters out none string from merged codec values`() {
        val left = playData(codec = mapOf(80 to listOf("none", "avc1")))
        val other = playData(codec = mapOf(80 to listOf("none", "hev1")))

        val result = left + other

        // 合并后 ["none", "avc1", "none", "hev1"] → 去重 ["none", "avc1", "hev1"] → 过滤 none → ["avc1", "hev1"]
        assertThat(result.codec[80]).containsExactly("avc1", "hev1")
        assertThat(result.codec[80]).doesNotContain("none")
    }

    @Test
    fun `plus drops codec keys that exist only in other`() {
        // plus 仅遍历 this.codec 的 key，other 独有的 key 会被丢弃（当前实现行为）
        val left = playData(codec = mapOf(80 to listOf("avc1")))
        val other = playData(codec = mapOf(120 to listOf("hev1")))

        val result = left + other

        assertThat(result.codec).containsKey(80)
        assertThat(result.codec).doesNotContainKey(120)
    }

    @Test
    fun `plus preserves codec key when other has no entry for it`() {
        val left = playData(codec = mapOf(80 to listOf("avc1")))
        val other = playData(codec = emptyMap())

        val result = left + other

        assertThat(result.codec[80]).containsExactly("avc1")
    }

    // endregion

    // region ---- plus: needPay 聚合 ----

    @Test
    fun `plus needPay is true when this is true`() {
        val result = playData(needPay = true) + playData(needPay = false)
        assertThat(result.needPay).isTrue()
    }

    @Test
    fun `plus needPay is true when other is true`() {
        val result = playData(needPay = false) + playData(needPay = true)
        assertThat(result.needPay).isTrue()
    }

    @Test
    fun `plus needPay is true when both are true`() {
        val result = playData(needPay = true) + playData(needPay = true)
        assertThat(result.needPay).isTrue()
    }

    @Test
    fun `plus needPay is false when both are false`() {
        val result = playData(needPay = false) + playData(needPay = false)
        assertThat(result.needPay).isFalse()
    }

    // endregion

    // region ---- plus: 端到端 / 综合 ----

    @Test
    fun `plus end-to-end merge of two complete PlayData`() {
        val left =
            playData(
                dashVideos =
                    listOf(
                        DashVideo(
                            quality = 80,
                            baseUrl = "t-v80",
                            bandwidth = 1,
                            codecId = 7,
                            width = 1920,
                            height = 1080,
                            frameRate = "30",
                            backUrl = emptyList(),
                            codecs = "avc1",
                        ),
                        DashVideo(
                            quality = 120,
                            baseUrl = "t-v120",
                            bandwidth = 2,
                            codecId = 12,
                            width = 3840,
                            height = 2160,
                            frameRate = "60",
                            backUrl = emptyList(),
                            codecs = "hev1",
                        ),
                    ),
                dashAudios =
                    listOf(
                        DashAudio(baseUrl = "t-a1", bandwidth = 100, codecId = 30216, backUrl = emptyList()),
                    ),
                dolby = DashAudio(baseUrl = "t-dolby", bandwidth = 500, codecId = 30250, backUrl = emptyList()),
                flac = null,
                codec = mapOf(80 to listOf("avc1"), 120 to listOf("hev1")),
                needPay = false,
            )
        val other =
            playData(
                dashVideos =
                    listOf(
                        DashVideo(
                            quality = 80,
                            baseUrl = "o-v80",
                            bandwidth = 3,
                            codecId = 13,
                            width = 1920,
                            height = 1080,
                            frameRate = "30",
                            backUrl = emptyList(),
                            codecs = "av01",
                        ),
                        DashVideo(
                            quality = 120,
                            baseUrl = "o-v120-dup",
                            bandwidth = 4,
                            codecId = 12,
                            width = 3840,
                            height = 2160,
                            frameRate = "60",
                            backUrl = emptyList(),
                            codecs = "hev1",
                        ),
                    ),
                dashAudios =
                    listOf(
                        DashAudio(baseUrl = "o-a1", bandwidth = 200, codecId = 30232, backUrl = emptyList()),
                        DashAudio(baseUrl = "o-a2-dup", bandwidth = 300, codecId = 30216, backUrl = emptyList()),
                    ),
                dolby = DashAudio(baseUrl = "o-dolby", bandwidth = 600, codecId = 30251, backUrl = emptyList()),
                flac = DashAudio(baseUrl = "o-flac", bandwidth = 800, codecId = 30251, backUrl = emptyList()),
                codec = mapOf(80 to listOf("av01"), 126 to listOf("avc1")),
                needPay = true,
            )

        val result = left + other

        // 视频：4 条合并 → t-v80(7,80) + o-v80(13,80) + t-v120(12,120) + o-v120-dup(12,120被去重)
        // 去重后 3 条，按 quality 降序：120, 80, 80
        assertThat(result.dashVideos).hasSize(3)
        assertThat(result.dashVideos[0].quality).isEqualTo(120)
        assertThat(result.dashVideos[0].baseUrl).isEqualTo("t-v120") // this 的优先
        assertThat(result.dashVideos[1].quality).isEqualTo(80)
        assertThat(result.dashVideos[1].baseUrl).isEqualTo("t-v80") // this 的优先
        assertThat(result.dashVideos[2].quality).isEqualTo(80)
        assertThat(result.dashVideos[2].baseUrl).isEqualTo("o-v80") // other 的（codecId 不同，保留）

        // 音频：3 条合并 → t-a1(30216) + o-a1(30232) + o-a2-dup(30216 被去重)
        assertThat(result.dashAudios).hasSize(2)
        // 按 codecId 降序
        assertThat(result.dashAudios[0].codecId).isEqualTo(30232)
        assertThat(result.dashAudios[1].codecId).isEqualTo(30216)
        assertThat(result.dashAudios[1].baseUrl).isEqualTo("t-a1") // this 优先

        // dolby: this 优先
        assertThat(result.dolby).isNotNull()
        assertThat(result.dolby!!.baseUrl).isEqualTo("t-dolby")
        // flac: this 为 null，取 other
        assertThat(result.flac).isNotNull()
        assertThat(result.flac!!.baseUrl).isEqualTo("o-flac")

        // codec: key 80 合并, key 120 仅 this, key 126 仅 other → 丢弃
        assertThat(result.codec).hasSize(2)
        assertThat(result.codec[80]).containsExactly("avc1", "av01")
        assertThat(result.codec[120]).containsExactly("hev1")
        assertThat(result.codec).doesNotContainKey(126)

        // needPay: OR
        assertThat(result.needPay).isTrue()
    }

    @Test
    fun `plus of two empty PlayData returns empty PlayData`() {
        val result = playData() + playData()

        assertThat(result.dashVideos).isEmpty()
        assertThat(result.dashAudios).isEmpty()
        assertThat(result.dolby).isNull()
        assertThat(result.flac).isNull()
        assertThat(result.codec).isEmpty()
        assertThat(result.needPay).isFalse()
    }

    // endregion

    // region ---- DashVideo / DashAudio 数据类 ----

    @Test
    fun `DashVideo data class holds all fields correctly`() {
        val video =
            DashVideo(
                quality = 116,
                baseUrl = "http://c/v.m4s",
                bandwidth = 5_000_000,
                codecId = 13,
                width = 3840,
                height = 2160,
                frameRate = "120",
                backUrl = listOf("http://c2/v.m4s", "http://c3/v.m4s"),
                codecs = "av01.0.12M.08",
            )

        assertThat(video.quality).isEqualTo(116)
        assertThat(video.baseUrl).isEqualTo("http://c/v.m4s")
        assertThat(video.bandwidth).isEqualTo(5_000_000)
        assertThat(video.codecId).isEqualTo(13)
        assertThat(video.width).isEqualTo(3840)
        assertThat(video.height).isEqualTo(2160)
        assertThat(video.frameRate).isEqualTo("120")
        assertThat(video.backUrl).hasSize(2)
        assertThat(video.codecs).isEqualTo("av01.0.12M.08")
    }

    @Test
    fun `DashVideo codecs defaults to null when not provided`() {
        val video =
            DashVideo(
                quality = 80,
                baseUrl = "u",
                bandwidth = 1,
                codecId = 7,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
            )

        assertThat(video.codecs).isNull()
    }

    @Test
    fun `DashAudio data class holds all fields correctly`() {
        val audio =
            DashAudio(
                baseUrl = "http://c/a.m4s",
                bandwidth = 320_000,
                codecId = 30232,
                backUrl = listOf("http://c2/a.m4s"),
            )

        assertThat(audio.baseUrl).isEqualTo("http://c/a.m4s")
        assertThat(audio.bandwidth).isEqualTo(320_000)
        assertThat(audio.codecId).isEqualTo(30232)
        assertThat(audio.backUrl).containsExactly("http://c2/a.m4s")
    }

    @Test
    fun `DashVideo equality is based on all fields`() {
        val v1 =
            DashVideo(
                quality = 80,
                baseUrl = "u",
                bandwidth = 1,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl =
                    listOf(
                        "b",
                    ),
                codecs = "avc1",
            )
        val v2 =
            DashVideo(
                quality = 80,
                baseUrl = "u",
                bandwidth = 1,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl =
                    listOf(
                        "b",
                    ),
                codecs = "avc1",
            )
        val v3 =
            DashVideo(
                quality = 80,
                baseUrl = "u",
                bandwidth = 1,
                codecId = 12,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl =
                    listOf(
                        "b",
                    ),
                codecs = "avc1",
            )

        assertThat(v1).isEqualTo(v2)
        assertThat(v1).isNotEqualTo(v3)
    }

    @Test
    fun `DashAudio equality is based on all fields`() {
        val a1 = DashAudio(baseUrl = "u", bandwidth = 1, codecId = 30216, backUrl = listOf("b"))
        val a2 = DashAudio(baseUrl = "u", bandwidth = 1, codecId = 30216, backUrl = listOf("b"))
        val a3 = DashAudio(baseUrl = "u", bandwidth = 1, codecId = 30232, backUrl = listOf("b"))

        assertThat(a1).isEqualTo(a2)
        assertThat(a1).isNotEqualTo(a3)
    }

    // endregion

    // region ---- fromPlayUrlV2Data ----

    @Test
    fun `fromPlayUrlV2Data delegates to fromPlayUrlData with videoInfo`() {
        val v1 = videoDashData(id = 80, codecId = 7, codecs = "avc1.640028")
        val data =
            PlayUrlV2Data(
                expInfo = PlayUrlV2Data.ExpInfo(buyVipDonatedSeason = 0),
                playCheck = PlayUrlV2Data.PlayCheck(playDetail = ""),
                playViewBusinessInfo =
                    PlayUrlV2Data.PlayViewBusinessInfo(
                        episodeInfo =
                            PlayUrlV2Data.PlayViewBusinessInfo.EpisodeInfo(
                                aid = 1L,
                                bvid = "BV1",
                                cid = 1L,
                                deliveryBusinessFragmentVideo = false,
                                deliveryFragmentVideo = false,
                                epId = 1,
                                epStatus = 0,
                                interaction =
                                    PlayUrlV2Data.PlayViewBusinessInfo.EpisodeInfo.Interaction(interaction = false),
                                longTitle = "",
                                title = "",
                            ),
                        seasonInfo = PlayUrlV2Data.PlayViewBusinessInfo.SeasonInfo(seasonId = 1, seasonType = 1),
                        userStatus =
                            PlayUrlV2Data.PlayViewBusinessInfo.UserStatus(
                                followInfo =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.FollowInfo(
                                        follow = 0,
                                        followStatus = 0,
                                    ),
                                isLogin = 1,
                                payInfo =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.PayInfo(
                                        payCheck = 0,
                                        payPackPaid = 0,
                                        sponsor = 0,
                                    ),
                                vipInfo = PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.VipInfo(realVip = false),
                                watchProgress =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.WatchProgress(
                                        currentWatchProgress = 0,
                                        lastEpId = 0,
                                        lastTime = 0,
                                    ),
                            ),
                    ),
                videoInfo =
                    playUrlData(
                        dash = dash(video = listOf(v1)),
                        supportFormats = listOf(supportFormat(80, listOf("avc1"))),
                    ),
                viewInfo =
                    PlayUrlV2Data.ViewInfo(
                        aiRepairQnTrialInfo = PlayUrlV2Data.ViewInfo.AiRepairQnTrialInfo(trialAble = false),
                        endPage = PlayUrlV2Data.ViewInfo.EndPage(hide = false),
                        extToast = kotlinx.serialization.json.JsonNull,
                        qnTrialInfo = PlayUrlV2Data.ViewInfo.QnTrialInfo(trialAble = false),
                        report =
                            PlayUrlV2Data.ViewInfo.Report(
                                epId = "",
                                epStatus = "",
                                seasonId = "",
                                seasonStatus = "",
                                seasonType = "",
                                vipStatus = "",
                                vipType = "",
                            ),
                    ),
            )

        val playData = PlayData.fromPlayUrlV2Data(data)

        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].quality).isEqualTo(80)
        assertThat(playData.dashVideos[0].codecs).isEqualTo("avc1.640028")
        assertThat(playData.codec).hasSize(1)
        assertThat(playData.codec[80]).containsExactly("avc1")
        assertThat(playData.needPay).isFalse()
    }

    @Test
    fun `fromPlayUrlV2Data with durl-only videoInfo sets needPay true`() {
        val data =
            PlayUrlV2Data(
                expInfo = PlayUrlV2Data.ExpInfo(buyVipDonatedSeason = 0),
                playCheck = PlayUrlV2Data.PlayCheck(playDetail = ""),
                playViewBusinessInfo =
                    PlayUrlV2Data.PlayViewBusinessInfo(
                        episodeInfo =
                            PlayUrlV2Data.PlayViewBusinessInfo.EpisodeInfo(
                                aid = 1L,
                                bvid = "BV1",
                                cid = 1L,
                                deliveryBusinessFragmentVideo = false,
                                deliveryFragmentVideo = false,
                                epId = 1,
                                epStatus = 0,
                                interaction =
                                    PlayUrlV2Data.PlayViewBusinessInfo.EpisodeInfo.Interaction(interaction = false),
                                longTitle = "",
                                title = "",
                            ),
                        seasonInfo = PlayUrlV2Data.PlayViewBusinessInfo.SeasonInfo(seasonId = 1, seasonType = 1),
                        userStatus =
                            PlayUrlV2Data.PlayViewBusinessInfo.UserStatus(
                                followInfo =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.FollowInfo(
                                        follow = 0,
                                        followStatus = 0,
                                    ),
                                isLogin = 1,
                                payInfo =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.PayInfo(
                                        payCheck = 0,
                                        payPackPaid = 0,
                                        sponsor = 0,
                                    ),
                                vipInfo = PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.VipInfo(realVip = false),
                                watchProgress =
                                    PlayUrlV2Data.PlayViewBusinessInfo.UserStatus.WatchProgress(
                                        currentWatchProgress = 0,
                                        lastEpId = 0,
                                        lastTime = 0,
                                    ),
                            ),
                    ),
                videoInfo = playUrlData(dash = null, durl = listOf(durl())),
                viewInfo =
                    PlayUrlV2Data.ViewInfo(
                        aiRepairQnTrialInfo = PlayUrlV2Data.ViewInfo.AiRepairQnTrialInfo(trialAble = false),
                        endPage = PlayUrlV2Data.ViewInfo.EndPage(hide = false),
                        extToast = kotlinx.serialization.json.JsonNull,
                        qnTrialInfo = PlayUrlV2Data.ViewInfo.QnTrialInfo(trialAble = false),
                        report =
                            PlayUrlV2Data.ViewInfo.Report(
                                epId = "",
                                epStatus = "",
                                seasonId = "",
                                seasonStatus = "",
                                seasonType = "",
                                vipStatus = "",
                                vipType = "",
                            ),
                    ),
            )

        val playData = PlayData.fromPlayUrlV2Data(data)

        assertThat(playData.needPay).isTrue()
        assertThat(playData.dashVideos).hasSize(1)
    }

    // endregion

    // region ---- fromPlayViewUniteReply (gRPC) ----

    @Test
    fun `fromPlayViewUniteReply maps dashVideo streams with audio dolby and flac`() {
        val reply =
            playViewUniteReply {
                vodInfo =
                    sharedVodInfo {
                        streamList +=
                            sharedStream {
                                streamInfo =
                                    sharedStreamInfo {
                                        quality = 80
                                    }
                                dashVideo =
                                    sharedDashVideo {
                                        baseUrl = "http://cdn.test/video-80.m4s"
                                        bandwidth = 1_000_000
                                        codecid = 7
                                        width = 1920
                                        height = 1080
                                        frameRate = "30"
                                    }
                            }
                        dashAudio +=
                            sharedDashItem {
                                id = 30280
                                baseUrl = "http://cdn.test/audio.m4s"
                                bandwidth = 500_000
                            }
                        dolby =
                            sharedDolbyItem {
                                audio +=
                                    sharedDashItem {
                                        id = 30250
                                        baseUrl = "http://cdn.test/dolby.m4s"
                                        bandwidth = 320_000
                                    }
                            }
                        lossLessItem =
                            sharedLossLessItem {
                                audio =
                                    sharedDashItem {
                                        id = 30251
                                        baseUrl = "http://cdn.test/flac.m4s"
                                        bandwidth = 400_000
                                    }
                            }
                    }
            }

        val playData = PlayData.fromPlayViewUniteReply(reply)

        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].quality).isEqualTo(80)
        assertThat(playData.dashVideos[0].baseUrl).isEqualTo("http://cdn.test/video-80.m4s")
        assertThat(playData.dashVideos[0].codecId).isEqualTo(7)
        assertThat(playData.dashAudios).hasSize(1)
        assertThat(playData.dashAudios[0].baseUrl).isEqualTo("http://cdn.test/audio.m4s")
        assertThat(playData.dolby).isNotNull()
        assertThat(playData.dolby?.baseUrl).isEqualTo("http://cdn.test/dolby.m4s")
        assertThat(playData.flac).isNotNull()
        assertThat(playData.flac?.baseUrl).isEqualTo("http://cdn.test/flac.m4s")
        assertThat(playData.needPay).isFalse()
        assertThat(playData.codec).hasSize(1)
        assertThat(playData.codec[80]).isNotNull()
    }

    @Test
    fun `fromPlayViewUniteReply with segmentVideo only sets needPay true`() {
        val reply =
            playViewUniteReply {
                vodInfo =
                    sharedVodInfo {
                        streamList +=
                            sharedStream {
                                streamInfo =
                                    sharedStreamInfo {
                                        quality = 64
                                    }
                                segmentVideo =
                                    sharedSegmentVideo {
                                        segment +=
                                            sharedResponseUrl {
                                                url = "http://cdn.test/preview.m4s"
                                            }
                                    }
                            }
                    }
            }

        val playData = PlayData.fromPlayViewUniteReply(reply)

        assertThat(playData.needPay).isTrue()
        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].baseUrl).isEqualTo("http://cdn.test/preview.m4s")
        assertThat(playData.dashVideos[0].quality).isEqualTo(64)
    }

    @Test
    fun `fromPlayViewUniteReply with empty streams returns empty lists`() {
        val reply =
            playViewUniteReply {
                vodInfo = sharedVodInfo {}
            }

        val playData = PlayData.fromPlayViewUniteReply(reply)

        assertThat(playData.dashVideos).isEmpty()
        assertThat(playData.dashAudios).isEmpty()
        assertThat(playData.dolby).isNull()
        assertThat(playData.flac).isNull()
        assertThat(playData.needPay).isFalse()
    }

    @Test
    fun `fromPlayViewUniteReply with lossLessItem id zero returns null flac`() {
        val reply =
            playViewUniteReply {
                vodInfo =
                    sharedVodInfo {
                        streamList +=
                            sharedStream {
                                streamInfo = sharedStreamInfo { quality = 80 }
                                dashVideo =
                                    sharedDashVideo {
                                        baseUrl = "http://cdn.test/video.m4s"
                                        codecid = 7
                                    }
                            }
                        lossLessItem =
                            sharedLossLessItem {
                                audio =
                                    sharedDashItem {
                                        id = 0
                                        baseUrl = ""
                                    }
                            }
                    }
            }

        val playData = PlayData.fromPlayViewUniteReply(reply)

        assertThat(playData.flac).isNull()
    }

    // endregion

    // region ---- fromPgcPlayViewReply (gRPC) ----

    @Test
    fun `fromPgcPlayViewReply maps dashVideo streams with audio and dolby`() {
        val reply =
            playViewReply {
                videoInfo =
                    videoInfo {
                        streamList +=
                            stream {
                                info = streamInfo { quality = 80 }
                                dashVideo =
                                    dashVideo {
                                        baseUrl = "http://cdn.test/pgc-video.m4s"
                                        bandwidth = 2_000_000
                                        codecid = 7
                                        width = 1920
                                        height = 1080
                                        frameRate = "60"
                                    }
                            }
                        dashAudio +=
                            dashItem {
                                id = 30280
                                baseUrl = "http://cdn.test/pgc-audio.m4s"
                                bandwidth = 500_000
                            }
                        dolby =
                            dolbyItem {
                                audio =
                                    dashItem {
                                        id = 30250
                                        baseUrl = "http://cdn.test/pgc-dolby.m4s"
                                        bandwidth = 320_000
                                    }
                            }
                    }
                business = playViewBusinessInfo { isPreview = false }
            }

        val playData = PlayData.fromPgcPlayViewReply(reply)

        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].quality).isEqualTo(80)
        assertThat(playData.dashVideos[0].baseUrl).isEqualTo("http://cdn.test/pgc-video.m4s")
        assertThat(playData.dashAudios).hasSize(1)
        assertThat(playData.dolby).isNotNull()
        assertThat(playData.dolby?.baseUrl).isEqualTo("http://cdn.test/pgc-dolby.m4s")
        assertThat(playData.flac).isNull()
        assertThat(playData.needPay).isFalse()
        assertThat(playData.codec).hasSize(1)
    }

    @Test
    fun `fromPgcPlayViewReply with isPreview true sets needPay true`() {
        val reply =
            playViewReply {
                videoInfo =
                    videoInfo {
                        streamList +=
                            stream {
                                info = streamInfo { quality = 64 }
                                segmentVideo =
                                    segmentVideo {
                                        segment +=
                                            responseUrl {
                                                url = "http://cdn.test/pgc-preview.m4s"
                                            }
                                    }
                            }
                    }
                business = playViewBusinessInfo { isPreview = true }
            }

        val playData = PlayData.fromPgcPlayViewReply(reply)

        assertThat(playData.needPay).isTrue()
        assertThat(playData.dashVideos).hasSize(1)
        assertThat(playData.dashVideos[0].baseUrl).isEqualTo("http://cdn.test/pgc-preview.m4s")
    }

    @Test
    fun `fromPgcPlayViewReply with empty streams returns empty lists`() {
        val reply =
            playViewReply {
                videoInfo = videoInfo {}
                business = playViewBusinessInfo { isPreview = false }
            }

        val playData = PlayData.fromPgcPlayViewReply(reply)

        assertThat(playData.dashVideos).isEmpty()
        assertThat(playData.dashAudios).isEmpty()
        assertThat(playData.needPay).isFalse()
    }

    // endregion
}
