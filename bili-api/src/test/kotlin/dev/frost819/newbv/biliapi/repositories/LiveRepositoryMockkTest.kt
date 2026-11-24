package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.live.CodecSpec
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveRoom
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.http.entity.live.MediaInfo
import dev.frost819.newbv.biliapi.http.entity.live.PlayCodec
import dev.frost819.newbv.biliapi.http.entity.live.PlayFormat
import dev.frost819.newbv.biliapi.http.entity.live.PlayStream
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrl
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfo
import dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfoItem
import dev.frost819.newbv.biliapi.http.entity.live.QnDesc
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.http.entity.live.RoomInitData
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
import dev.frost819.newbv.biliapi.http.entity.live.SimpleDurl
import dev.frost819.newbv.biliapi.http.entity.live.SimplePlayUrlData
import dev.frost819.newbv.biliapi.http.entity.live.VideoColorInfo
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [LiveRepository] 的单元测试。
 *
 * 通过 MockK mock [BiliLiveHttpApi] 单例，验证 Repository 对直播列表、分区、
 * 房间信息、流地址、画质列表的调用逻辑与 fallback 策略。
 * 不依赖真实网络。
 */
class LiveRepositoryUnitTest {
    private val repository = LiveRepository()

    @BeforeEach
    fun setUp() {
        mockkObject(BiliLiveHttpApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliLiveHttpApi)
    }

    @Test
    fun `getLiveList returns response data`() =
        runBlocking {
            val fakeData = LiveListResponse()
            coEvery { BiliLiveHttpApi.getLiveList() } returns fakeResponse(fakeData)

            val result = repository.getLiveList()

            assertThat(result).isEqualTo(fakeData)
        }

    @Test
    fun `getLiveRecommend returns response data`() =
        runBlocking {
            val fakeData = LiveRecommendResponse()
            coEvery { BiliLiveHttpApi.getLiveRecommend() } returns fakeResponse(fakeData)

            val result = repository.getLiveRecommend()

            assertThat(result).isEqualTo(fakeData)
        }

    @Test
    fun `getFollowLive returns first page with hasMore when more live rooms remain`() =
        runBlocking {
            val firstPage = List(10) { fakeFollowLiveRoom(roomId = it + 1L) }
            coEvery { BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10) } returns
                fakeResponse(FollowLiveResponse(rooms = firstPage, count = 13))

            val result = repository.getFollowLive()

            // issue #337：关注了 13 个正在直播的主播，第 1 页只给 10 个但必须告知还有下一页
            assertThat(result.rooms).hasSize(10)
            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getFollowLive reports no more on last page`() =
        runBlocking {
            val lastPage = List(3) { fakeFollowLiveRoom(roomId = it + 11L) }
            coEvery { BiliLiveHttpApi.getFollowLive(page = 2, pageSize = 10) } returns
                fakeResponse(FollowLiveResponse(rooms = lastPage, count = 13))

            val result = repository.getFollowLive(page = 2)

            assertThat(result.rooms).hasSize(3)
            assertThat(result.hasMore).isFalse()
        }

    @Test
    fun `getFollowLive treats empty page as end`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10) } returns
                fakeResponse(FollowLiveResponse(rooms = emptyList()))

            val result = repository.getFollowLive()

            assertThat(result.rooms).isEmpty()
            assertThat(result.hasMore).isFalse()
        }

    @Test
    fun `getFollowLive falls back to list field when rooms is empty`() =
        runBlocking {
            val listField = List(2) { fakeFollowLiveRoom(roomId = it + 1L) }
            coEvery { BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10) } returns
                fakeResponse(FollowLiveResponse(rooms = emptyList(), list = listField, count = 2))

            val result = repository.getFollowLive()

            assertThat(result.rooms).hasSize(2)
            assertThat(result.hasMore).isFalse()
        }

    @Test
    fun `getFollowLive falls back to room count when server omits count`() =
        runBlocking {
            val fullPage = List(10) { fakeFollowLiveRoom(roomId = it + 1L) }
            coEvery { BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10) } returns
                fakeResponse(FollowLiveResponse(rooms = fullPage, count = 0))

            val result = repository.getFollowLive()

            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getLiveAreaList returns response data`() =
        runBlocking {
            val fakeData = listOf(LiveAreaParent(id = 1, name = "分区1", list = emptyList()))
            coEvery { BiliLiveHttpApi.getLiveAreaList() } returns fakeResponse(fakeData)

            val result = repository.getLiveAreaList()

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("分区1")
        }

    @Test
    fun `getAreaLiveList returns result with hasMore true when full page`() =
        runBlocking {
            val fakeRooms = List(30) { fakeLiveRoomItem(roomId = it + 1) }
            coEvery {
                BiliLiveHttpApi.getAreaLiveList(any(), any(), any(), any(), any())
            } returns fakeResponse(fakeRooms)

            val result = repository.getAreaLiveList(parentAreaId = 2, areaId = 0, page = 1)

            assertThat(result.list).hasSize(30)
            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getAreaLiveList returns result with hasMore false when partial page`() =
        runBlocking {
            val fakeRooms = List(10) { fakeLiveRoomItem(roomId = it + 1) }
            coEvery {
                BiliLiveHttpApi.getAreaLiveList(any(), any(), any(), any(), any())
            } returns fakeResponse(fakeRooms)

            val result = repository.getAreaLiveList(parentAreaId = 2, areaId = 0, page = 1)

            assertThat(result.list).hasSize(10)
            assertThat(result.hasMore).isFalse()
        }

    @Test
    fun `getRoomInit returns response data`() =
        runBlocking {
            val fakeData =
                RoomInitData(
                    roomId = 1718159119,
                    shortId = 0,
                    uid = 1,
                    needP2P = 0,
                    isHidden = false,
                    isLocked = false,
                    isPortrait = false,
                    liveStatus = 1,
                    hiddenTill = 0,
                    lockTill = 0,
                    encrypted = false,
                    pwdVerified = false,
                    liveTime = 0,
                    roomShield = 0,
                    isSp = 0,
                    specialType = 0,
                )
            coEvery { BiliLiveHttpApi.getRoomInit(any()) } returns fakeResponse(fakeData)

            val result = repository.getRoomInit(1718159119)

            assertThat(result.roomId).isEqualTo(1718159119)
        }

    @Test
    fun `getRoomInfo returns response data`() =
        runBlocking {
            val fakeData = RoomInfoData(roomId = 1718159119, shortId = 0, uid = 1, liveStatus = 1, title = "测试直播间")
            coEvery { BiliLiveHttpApi.getRoomInfo(any()) } returns fakeResponse(fakeData)

            val result = repository.getRoomInfo(1718159119)

            assertThat(result.roomId).isEqualTo(1718159119)
            assertThat(result.title).isEqualTo("测试直播间")
        }

    @Test
    fun `getAvailableQualities returns sorted qualities on success`() =
        runBlocking {
            val playInfo = fakeRoomPlayInfoV2Data()
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

            val result = repository.getAvailableQualities(1718159119)

            assertThat(result).isNotEmpty()
            assertThat(result[0].qn).isAtLeast(result[1].qn)
        }

    @Test
    fun `getAvailableQualities returns empty list on failure`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("network error")

            val result = repository.getAvailableQualities(1718159119)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getLiveStreamInfo returns url and qn on success`() =
        runBlocking {
            val playInfo = fakeRoomPlayInfoV2Data()
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

            val result = repository.getLiveStreamInfo(1718159119, qualityKey(10000))

            assertThat(result.url).isNotNull()
            assertThat(result.currentQn).isEqualTo(10000)
        }

    @Test
    fun `getLiveStreamInfo falls back to simple url on v2 failure`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
            coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } returns
                fakeResponse(
                    SimplePlayUrlData(
                        currentQuality = 0,
                        durl =
                            listOf(
                                SimpleDurl(
                                    url = "http://fallback.flv",
                                ),
                            ),
                    ),
                )
            val result = repository.getLiveStreamInfo(1718159119, 0)

            assertThat(result.url).isEqualTo("http://fallback.flv")
            assertThat(result.currentQn).isEqualTo(0)
        }

    @Test
    fun `getLiveStreamInfo returns null url when all methods fail`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
            coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } throws RuntimeException("simple error")

            val result = repository.getLiveStreamInfo(1718159119, 0)

            assertThat(result.url).isNull()
            assertThat(result.currentQn).isEqualTo(0)
        }

    @Test
    fun `getLiveStreamUrl delegates to getLiveStreamInfo url`() =
        runBlocking {
            val playInfo = fakeRoomPlayInfoV2Data()
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

            val result = repository.getLiveStreamUrl(1718159119, qualityKey(10000))

            assertThat(result).isNotNull()
        }

    @Test
    fun `getLivePlayInfo returns qualities and multiple lines`() =
        runBlocking {
            val playInfo =
                fakeRoomPlayInfoV2Data(
                    streams =
                        listOf(
                            fakeHttpStream(
                                urlInfos =
                                    listOf(
                                        PlayUrlInfoItem(host = "https://cdn-a.example", extra = "?a=1"),
                                        PlayUrlInfoItem(host = "https://cdn-b.example", extra = "?b=2"),
                                    ),
                            ),
                        ),
                )
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

            val result = repository.getLivePlayInfo(1718159119, qualityKey(10000))

            assertThat(result.qualities).hasSize(2)
            assertThat(result.qualities[0].qn).isEqualTo(10000)
            assertThat(result.lines).hasSize(2)
            assertThat(result.lines[0].url).isEqualTo("https://cdn-a.example/live.flv?a=1")
            assertThat(result.lines[1].url).isEqualTo("https://cdn-b.example/live.flv?b=2")
        }

    @Test
    fun `getLivePlayInfo falls back to single line on v2 failure`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
            coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } returns
                fakeResponse(
                    SimplePlayUrlData(
                        currentQuality = 150,
                        durl =
                            listOf(
                                SimpleDurl(url = "http://fallback.flv"),
                            ),
                    ),
                )

            val result = repository.getLivePlayInfo(1718159119, 0)

            assertThat(result.lines).hasSize(1)
            assertThat(result.lines[0].order).isEqualTo(1)
            assertThat(result.lines[0].url).isEqualTo("http://fallback.flv")
            assertThat(result.currentQn).isEqualTo(150)
        }

    @Test
    fun `getLivePlayInfo returns empty lines when all methods fail`() =
        runBlocking {
            coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
            coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } throws RuntimeException("simple error")

            val result = repository.getLivePlayInfo(1718159119, 0)

            assertThat(result.lines).isEmpty()
            assertThat(result.qualities).isEmpty()
            assertThat(result.currentQn).isEqualTo(0)
        }

    @Test
    fun `resolvePlayInfo deduplicates identical urls`() {
        val data =
            fakeRoomPlayInfoV2Data(
                streams =
                    listOf(
                        fakeHttpStream(
                            urlInfos =
                                listOf(
                                    PlayUrlInfoItem(host = "https://cdn.example", extra = "?a=1"),
                                    PlayUrlInfoItem(host = "https://cdn.example", extra = "?a=1"),
                                ),
                        ),
                    ),
            )

        val result = repository.resolvePlayInfo(data)

        assertThat(result.lines).hasSize(1)
        assertThat(result.lines[0].order).isEqualTo(1)
    }

    @Test
    fun `resolvePlayInfo prefers http_stream over http_hls`() {
        val data =
            fakeRoomPlayInfoV2Data(
                streams =
                    listOf(
                        fakeStream(
                            protocol = "http_hls",
                            formatName = "fmp4",
                            currentQn = 20000,
                            baseUrl = "/live.m3u8",
                        ),
                        fakeHttpStream(currentQn = 10000),
                    ),
            )

        val result = repository.resolvePlayInfo(data)

        assertThat(result.currentQn).isEqualTo(10000)
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/live.flv?query=1")
    }

    @Test
    fun `resolvePlayInfo returns empty qualities and lines when stream list is empty`() {
        val data = fakeRoomPlayInfoV2Data(streams = emptyList())

        val result = repository.resolvePlayInfo(data)

        assertThat(result.qualities).isEmpty()
        assertThat(result.lines).isEmpty()
        assertThat(result.currentQn).isEqualTo(0)
    }

    @Test
    fun `resolvePlayInfo includes quality that only appears in hdrQn`() {
        val data = fakeHdrRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data)

        // HDR 档 30000 不在任何 accept_qn 中，仅由 hevc 编码的 hdr_qn 指针引入；
        // HDR 编码节点的 accept_qn 与 SDR 重叠，同 qn 展开为 SDR/HDR 两个档位
        assertThat(result.qualities.map { it.qn }.distinct()).containsExactly(30000, 10000, 400).inOrder()
    }

    @Test
    fun `resolvePlayInfo marks HDR quality with decode hints`() {
        val data = fakeHdrRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data)

        val hdr = result.qualities.first { it.qn == 30000 }
        assertThat(hdr.isHdr).isTrue()
        assertThat(hdr.hdrType).isEqualTo(1)
        assertThat(hdr.codecName).isEqualTo("hevc")
        assertThat(hdr.codecs).isEqualTo("hvc1.1.6.L120")
        assertThat(hdr.width).isEqualTo(3840)
        assertThat(hdr.height).isEqualTo(2160)
        assertThat(hdr.displayDesc).contains("HDR")
        // SDR 档不携带 HDR 标记与解码提示
        val sdr = result.qualities.first { it.qn == 10000 }
        assertThat(sdr.isHdr).isFalse()
        assertThat(sdr.codecName).isNull()
        assertThat(sdr.codecs).isNull()
        assertThat(sdr.displayDesc).isEqualTo("原画")
    }

    @Test
    fun `resolvePlayInfo picks HDR codec when HDR quality requested`() {
        val data = fakeHdrRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(30000, hdrType = 1, eotf = 1))

        assertThat(result.currentQn).isEqualTo(30000)
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/hdr-live.flv?hdr=1")
    }

    @Test
    fun `resolvePlayInfo keeps avc preference when HDR quality not requested`() {
        val data = fakeHdrRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(10000))

        assertThat(result.currentQn).isEqualTo(10000)
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/live.flv?query=1")
    }

    @Test
    fun `resolvePlayInfo falls back to default codec when HDR stream absent`() {
        // qnDesc 标记 30000 为 HDR 档，但没有任何编码携带它（服务端降级）
        val data =
            fakeRoomPlayInfoV2Data(
                qnDesc =
                    listOf(
                        QnDesc(qn = 10000, desc = "原画"),
                        QnDesc(qn = 400, desc = "蓝光"),
                        QnDesc(qn = 30000, desc = "4K", hdrDesc = "HDR", hdrType = 1, eotf = 1),
                    ),
            )

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(30000, hdrType = 1, eotf = 1))

        // 回退到默认 avc 选流，不报错
        assertThat(result.currentQn).isEqualTo(10000)
        assertThat(result.lines).isNotEmpty()
    }

    @Test
    fun `resolvePlayInfo expands duplicate qn into SDR and HDR variants`() {
        val data = fakeDuplicateQnRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data)

        // 真实契约：HDR 编码节点 accept_qn 与 SDR 重叠，同 qn 展开为 SDR/HDR 双档位（SDR 在前）
        assertThat(result.qualities.map { it.key })
            .containsExactly(
                qualityKey(20000),
                qualityKey(20000, hdrType = 1, eotf = 1),
                qualityKey(10000),
                qualityKey(10000, hdrType = 1, eotf = 1),
                qualityKey(400),
            ).inOrder()
        val hdr = result.qualities.first { it.key == qualityKey(10000, hdrType = 1, eotf = 1) }
        assertThat(hdr.desc).isEqualTo("原画/HDR")
        assertThat(hdr.isHdr).isTrue()
        assertThat(hdr.codecName).isEqualTo("hevc")
        assertThat(hdr.codecs).isEqualTo("hvc1.2.4.L123")
        // 服务端描述已含 HDR 字样，不再追加徽标后缀
        assertThat(hdr.displayDesc).isEqualTo("原画/HDR")
        val sdr = result.qualities.first { it.key == qualityKey(10000) }
        assertThat(sdr.desc).isEqualTo("原画")
        assertThat(sdr.isHdr).isFalse()
        assertThat(sdr.codecName).isNull()
    }

    @Test
    fun `resolvePlayInfo SDR request never falls back to HDR node in flv`() {
        val hdrFlv =
            PlayStream(
                protocolName = "http_stream",
                format =
                    listOf(
                        PlayFormat(
                            formatName = "flv",
                            codec =
                                listOf(
                                    fakeHdrCodec(
                                        baseUrl = "/live.flv",
                                        hdrType = 1,
                                        eotf = 1,
                                        codecs = "hvc1.2.4.L123",
                                    ),
                                ),
                        ),
                    ),
            )
        val data =
            fakeRoomPlayInfoV2DataWith(
                qnDesc = listOf(QnDesc(qn = 10000, desc = "原画")),
                streams = listOf(hdrFlv),
            )

        val result = repository.resolvePlayInfo(data)

        assertThat(result.lines).isEmpty()
        assertThat(result.qualities).isEmpty()
    }

    @Test
    fun `resolvePlayInfo picks HLS codec node for duplicate-qn HDR request`() {
        val data = fakeDuplicateQnRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(10000, hdrType = 1, eotf = 1))

        // HDR 不走 flv（Media3 不支持 B 站 HEVC-in-FLV 私有扩展），优先 http_hls/fmp4
        assertThat(result.currentQn).isEqualTo(10000)
        assertThat(result.currentKey).isEqualTo(qualityKey(10000, hdrType = 1, eotf = 1))
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/prohdr.m3u8?hdr_type=1")
    }

    @Test
    fun `resolvePlayInfo omits HDR rows and falls back to SDR when HDR only exists in flv`() {
        // B 站 HEVC-in-FLV（codec_id=12）Media3 FlvExtractor 不支持：
        // HDR 仅存于 flv 时既不产出 HDR 档位行（避免"选中后静默播 SDR"），选流也回退 SDR
        val data = fakeDuplicateQnRoomPlayInfoV2Data(includeHlsHdr = false)

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(10000, hdrType = 1, eotf = 1))

        assertThat(result.qualities.none { it.isHdr }).isTrue()
        assertThat(result.currentKey).isEqualTo(qualityKey(400))
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/live.flv?query=1")
    }

    @Test
    fun `resolvePlayInfo keeps avc codec for duplicate-qn SDR request`() {
        val data = fakeDuplicateQnRoomPlayInfoV2Data()

        val result = repository.resolvePlayInfo(data, requestedKey = qualityKey(10000))

        // 同 qn 的 HDR 变体存在时，SDR 请求仍选 avc 编码节点
        assertThat(result.currentKey).isEqualTo(qualityKey(400))
        assertThat(result.lines[0].url).isEqualTo("https://cdn.example/live.flv?query=1")
    }

    @Test
    fun `resolvePlayInfo keeps per-variant decode hints for same-qn HDR10 and HLG rows`() {
        val result = repository.resolvePlayInfo(fakeMultiHdrVariantRoomPlayInfoV2Data())

        // 同 qn 的 HDR10 与 HLG 是两个独立编码节点，各自携带自己的 RFC 6381 串
        val hdr10 = result.qualities.first { it.key == qualityKey(10000, hdrType = 1, eotf = 1) }
        assertThat(hdr10.codecName).isEqualTo("hevc")
        assertThat(hdr10.codecs).isEqualTo("hvc1.2.4.L123")

        val hlg = result.qualities.first { it.key == qualityKey(10000, eotf = 2) }
        assertThat(hlg.codecName).isEqualTo("hevc")
        assertThat(hlg.codecs).isEqualTo("hvc1.2.4.L120")
    }

    @Test
    fun `resolvePlayInfo labels dolby only on HDR rows of dolby qn`() {
        val data =
            fakeRoomPlayInfoV2DataWith(
                qnDesc =
                    listOf(
                        QnDesc(qn = 10000, desc = "原画"),
                        QnDesc(qn = 10000, desc = "原画/杜比", hdrDesc = "HDR", hdrType = 1, eotf = 1),
                    ),
                streams =
                    listOf(
                        fakeHlsFmp4Stream(
                            listOf(
                                fakeSdrCodec(),
                                fakeHdrCodec("/prohdr.m3u8", hdrType = 1, eotf = 1, codecs = "hvc1.2.4.L123"),
                            ),
                        ),
                    ),
                dolbyQn = 10000,
            )

        val result = repository.resolvePlayInfo(data)

        // dolby_qn 是 qn 级指针，同 qn 的 SDR 行不得被标成杜比档（否则菜单多出一条幽灵档位行）
        val sdr = result.qualities.first { it.key == qualityKey(10000) }
        assertThat(sdr.isDolby).isFalse()
        assertThat(sdr.isHdr).isFalse()

        val hdr = result.qualities.first { it.key == qualityKey(10000, hdrType = 1, eotf = 1) }
        assertThat(hdr.isDolby).isTrue()
    }

    @Test
    fun `resolvePlayInfo drops phantom HDR row when hdr_qn set on non-HDR codec`() {
        val data =
            fakeRoomPlayInfoV2DataWith(
                qnDesc =
                    listOf(
                        QnDesc(qn = 10000, desc = "原画"),
                        QnDesc(qn = 30000, desc = "4K", hdrDesc = "HDR", hdrType = 1, eotf = 1),
                    ),
                streams = listOf(fakeHlsFmp4Stream(listOf(fakeSdrCodec().copy(hdrQn = 30000)))),
            )

        val result = repository.resolvePlayInfo(data)

        // 旧契约 hdr_qn 只对 HDR 载体节点有效：SDR 节点带 hdr_qn 产出的档位没有载体提示，
        // 选中后必然回退 SDR，只能算幽灵档位行
        assertThat(result.qualities.none { it.isHdr }).isTrue()
        assertThat(result.qualities.map { it.qn }).containsExactly(10000)
    }

    private fun <T> fakeResponse(data: T) =
        BiliResponse(
            code = 0,
            message = "0",
            ttl = 1,
            data = data,
        )

    private fun fakeFollowLiveRoom(roomId: Long) =
        FollowLiveRoom(
            roomId = roomId,
            uid = 1,
            title = "room $roomId",
            uname = "测试主播",
            liveStatus = 1,
            coverFromUser = "https://i0.hdslb.com/bfs/live/cover_$roomId.jpg",
            online = 100,
        )

    private fun fakeLiveRoomItem(roomId: Int) =
        LiveRoomItem(
            roomId = roomId,
            uid = 1,
            title = "room $roomId",
            uname = "测试主播",
            online = 100,
            liveStatus = 1,
        )

    private fun fakeRoomPlayInfoV2Data(
        streams: List<PlayStream> = listOf(fakeHttpStream()),
        qnDesc: List<QnDesc> =
            listOf(
                QnDesc(qn = 10000, desc = "原画"),
                QnDesc(qn = 400, desc = "蓝光"),
            ),
    ) = RoomPlayInfoV2Data(
        roomId = 1718159119,
        shortId = 0,
        uid = 1,
        liveStatus = 1,
        playUrlInfo =
            PlayUrlInfo(
                playUrl =
                    PlayUrl(
                        cid = 1718159119,
                        qnDesc = qnDesc,
                        stream = streams,
                    ),
            ),
    )

    private fun fakeHttpStream(
        formatName: String = "flv",
        codecName: String = "avc",
        currentQn: Int = 10000,
        acceptQn: List<Int> = listOf(10000, 400),
        urlInfos: List<PlayUrlInfoItem> = listOf(PlayUrlInfoItem(host = "https://cdn.example", extra = "?query=1")),
        baseUrl: String = "/live.flv",
    ) = fakeStream(
        protocol = "http_stream",
        formatName = formatName,
        codecName = codecName,
        currentQn = currentQn,
        acceptQn = acceptQn,
        urlInfos = urlInfos,
        baseUrl = baseUrl,
    )

    /**
     * 构造含 HDR 档的 v2 流数据：单个 http_stream 下 SDR（flv/avc）+ HDR（fmp4/hevc，hdr_qn=30000）。
     */
    private fun fakeHdrRoomPlayInfoV2Data() =
        RoomPlayInfoV2Data(
            roomId = 1718159119,
            shortId = 0,
            uid = 1,
            liveStatus = 1,
            playUrlInfo =
                PlayUrlInfo(
                    playUrl =
                        PlayUrl(
                            cid = 1718159119,
                            qnDesc =
                                listOf(
                                    QnDesc(qn = 10000, desc = "原画"),
                                    QnDesc(qn = 400, desc = "蓝光"),
                                    QnDesc(qn = 30000, desc = "4K", hdrDesc = "HDR", hdrType = 1, eotf = 1),
                                ),
                            stream =
                                listOf(
                                    PlayStream(
                                        protocolName = "http_stream",
                                        format =
                                            listOf(
                                                PlayFormat(
                                                    formatName = "flv",
                                                    codec =
                                                        listOf(
                                                            PlayCodec(
                                                                codecName = "avc",
                                                                currentQn = 10000,
                                                                acceptQn = listOf(10000, 400),
                                                                baseUrl = "/live.flv",
                                                                urlInfo =
                                                                    listOf(
                                                                        PlayUrlInfoItem(
                                                                            host = "https://cdn.example",
                                                                            extra = "?query=1",
                                                                        ),
                                                                    ),
                                                                mediaInfo = MediaInfo(),
                                                            ),
                                                        ),
                                                ),
                                                PlayFormat(
                                                    formatName = "fmp4",
                                                    codec =
                                                        listOf(
                                                            PlayCodec(
                                                                codecName = "hevc",
                                                                currentQn = 30000,
                                                                acceptQn = listOf(10000, 400),
                                                                baseUrl = "/hdr-live.flv",
                                                                urlInfo =
                                                                    listOf(
                                                                        PlayUrlInfoItem(
                                                                            host = "https://cdn.example",
                                                                            extra = "?hdr=1",
                                                                        ),
                                                                    ),
                                                                mediaInfo = MediaInfo(width = 3840, height = 2160),
                                                                hdrQn = 30000,
                                                                hdrType = 1,
                                                                videoCodecs = CodecSpec(base = "hvc1.1.6.L120"),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                ),
        )

    /** 档位标识编码，与 [LiveQuality.key] 一致。 */
    private fun qualityKey(
        qn: Int,
        hdrType: Int = 0,
        eotf: Int = 0,
    ) = qn * 100 + hdrType * 10 + eotf

    /**
     * 构造 S 赛 4K 直播间真实契约的 v2 流数据：
     * g_qn_desc 同 qn 重复（SDR/HDR 并存），HDR 流为同 format 下的额外 hevc 编码节点
     * （hdr_type=1、bt2020、eotf=1、prohdr 后缀 URL），accept_qn 与 SDR 重叠。
     *
     * @param includeHlsHdr 是否包含 http_hls/fmp4 的 HDR 编码节点（真实响应同时
     *   在 flv/ts/fmp4 下携带 HDR 节点；false 用于验证"HDR 仅存在于不可解析的
     *   flv 时回退 SDR"）
     */
    private fun fakeDuplicateQnRoomPlayInfoV2Data(includeHlsHdr: Boolean = true): RoomPlayInfoV2Data {
        val hdrCodec =
            PlayCodec(
                codecName = "hevc",
                currentQn = 10000,
                acceptQn = listOf(20000, 10000),
                baseUrl = "/prohdr.flv",
                urlInfo =
                    listOf(
                        PlayUrlInfoItem(
                            host = "https://cdn.example",
                            extra = "?hdr_type=1",
                        ),
                    ),
                mediaInfo = MediaInfo(width = 1920, height = 1080),
                hdrType = 1,
                videoCodecs = CodecSpec(base = "hvc1.2.4.L123"),
                videoColorInfo =
                    VideoColorInfo(
                        colorspace = "bt2020",
                        eotf = 1,
                        metadataTypes = listOf("smpte_st2086", "none"),
                    ),
            )
        val hlsHdrCodec = hdrCodec.copy(baseUrl = "/prohdr.m3u8")
        return RoomPlayInfoV2Data(
            roomId = 13242941,
            shortId = 0,
            uid = 1,
            liveStatus = 1,
            playUrlInfo =
                PlayUrlInfo(
                    playUrl =
                        PlayUrl(
                            cid = 13242941,
                            qnDesc =
                                listOf(
                                    QnDesc(qn = 20000, desc = "4K"),
                                    QnDesc(qn = 20000, desc = "4K/HDR", hdrType = 1, eotf = 1),
                                    QnDesc(qn = 10000, desc = "原画"),
                                    QnDesc(qn = 10000, desc = "原画/HDR", hdrType = 1, eotf = 1),
                                    QnDesc(qn = 400, desc = "蓝光"),
                                ),
                            stream =
                                buildList {
                                    add(
                                        PlayStream(
                                            protocolName = "http_stream",
                                            format =
                                                listOf(
                                                    PlayFormat(
                                                        formatName = "flv",
                                                        codec =
                                                            listOf(
                                                                PlayCodec(
                                                                    codecName = "avc",
                                                                    currentQn = 400,
                                                                    acceptQn = listOf(20000, 10000, 400),
                                                                    baseUrl = "/live.flv",
                                                                    urlInfo =
                                                                        listOf(
                                                                            PlayUrlInfoItem(
                                                                                host = "https://cdn.example",
                                                                                extra = "?query=1",
                                                                            ),
                                                                        ),
                                                                    mediaInfo = MediaInfo(),
                                                                ),
                                                                hdrCodec,
                                                            ),
                                                    ),
                                                ),
                                        ),
                                    )
                                    if (includeHlsHdr) {
                                        add(
                                            PlayStream(
                                                protocolName = "http_hls",
                                                format =
                                                    listOf(
                                                        PlayFormat(
                                                            formatName = "fmp4",
                                                            codec = listOf(hlsHdrCodec),
                                                        ),
                                                    ),
                                            ),
                                        )
                                    }
                                },
                        ),
                ),
        )
    }

    private fun fakeStream(
        protocol: String,
        formatName: String,
        codecName: String = "avc",
        currentQn: Int = 10000,
        acceptQn: List<Int> = listOf(10000, 400),
        urlInfos: List<PlayUrlInfoItem> = listOf(PlayUrlInfoItem(host = "https://cdn.example", extra = "?query=1")),
        baseUrl: String = "/live.flv",
    ) = PlayStream(
        protocolName = protocol,
        format =
            listOf(
                PlayFormat(
                    formatName = formatName,
                    codec =
                        listOf(
                            PlayCodec(
                                codecName = codecName,
                                currentQn = currentQn,
                                acceptQn = acceptQn,
                                baseUrl = baseUrl,
                                urlInfo = urlInfos,
                                mediaInfo = MediaInfo(),
                            ),
                        ),
                ),
            ),
    )

    /** 构造可自定义 qn_desc / stream / dolby_qn 的 v2 流数据。 */
    private fun fakeRoomPlayInfoV2DataWith(
        qnDesc: List<QnDesc>,
        streams: List<PlayStream>,
        dolbyQn: Int? = null,
    ) = RoomPlayInfoV2Data(
        roomId = 13242941,
        shortId = 0,
        uid = 1,
        liveStatus = 1,
        playUrlInfo =
            PlayUrlInfo(
                playUrl =
                    PlayUrl(
                        cid = 13242941,
                        qnDesc = qnDesc,
                        stream = streams,
                        dolbyQn = dolbyQn,
                    ),
            ),
    )

    /** 构造单 http_hls/fmp4 format 的 stream。 */
    private fun fakeHlsFmp4Stream(codecs: List<PlayCodec>) =
        PlayStream(
            protocolName = "http_hls",
            format = listOf(PlayFormat(formatName = "fmp4", codec = codecs)),
        )

    /** 构造 http_hls/fmp4 下的 SDR 编码节点（avc）。 */
    private fun fakeSdrCodec(acceptQn: List<Int> = listOf(10000)) =
        PlayCodec(
            codecName = "avc",
            currentQn = 10000,
            acceptQn = acceptQn,
            baseUrl = "/live.m3u8",
            urlInfo = listOf(PlayUrlInfoItem(host = "https://cdn.example", extra = "?qn=1")),
        )

    /** 构造 http_hls/fmp4 下的 HDR 载体编码节点（hevc + bt2020）。 */
    private fun fakeHdrCodec(
        baseUrl: String,
        hdrType: Int,
        eotf: Int,
        codecs: String,
        acceptQn: List<Int> = listOf(10000),
    ) = PlayCodec(
        codecName = "hevc",
        currentQn = 10000,
        acceptQn = acceptQn,
        baseUrl = baseUrl,
        urlInfo = listOf(PlayUrlInfoItem(host = "https://cdn.example", extra = "?hdr=1")),
        hdrType = hdrType,
        videoCodecs = CodecSpec(base = codecs),
        videoColorInfo = VideoColorInfo(colorspace = "bt2020", eotf = eotf),
    )

    /** 构造同 qn 并存 HDR10 与 HLG 两个编码节点的 v2 流数据（两节点 codecs 串不同）。 */
    private fun fakeMultiHdrVariantRoomPlayInfoV2Data() =
        fakeRoomPlayInfoV2DataWith(
            qnDesc =
                listOf(
                    QnDesc(qn = 10000, desc = "原画"),
                    QnDesc(qn = 10000, desc = "原画/HDR10", hdrDesc = "HDR", hdrType = 1, eotf = 1),
                    QnDesc(qn = 10000, desc = "原画/HLG", hdrDesc = "HDR", eotf = 2),
                ),
            streams =
                listOf(
                    fakeHlsFmp4Stream(
                        listOf(
                            fakeSdrCodec(),
                            fakeHdrCodec("/prohdr10.m3u8", hdrType = 1, eotf = 1, codecs = "hvc1.2.4.L123"),
                            fakeHdrCodec("/prohlg.m3u8", hdrType = 0, eotf = 2, codecs = "hvc1.2.4.L120"),
                        ),
                    ),
                ),
        )
}
