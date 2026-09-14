package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
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
    fun `getFollowLive returns response data`() =
        runBlocking {
            val fakeData = FollowLiveResponse()
            coEvery { BiliLiveHttpApi.getFollowLive() } returns fakeResponse(fakeData)

            val result = repository.getFollowLive()

            assertThat(result).isEqualTo(fakeData)
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
            assertThat(result[0].first).isAtLeast(result[1].first)
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

            val result = repository.getLiveStreamInfo(1718159119, 10000)

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

            val result = repository.getLiveStreamUrl(1718159119, 10000)

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

            val result = repository.getLivePlayInfo(1718159119, 10000)

            assertThat(result.qualities).hasSize(2)
            assertThat(result.qualities[0].first).isEqualTo(10000)
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

    private fun <T> fakeResponse(data: T) =
        BiliResponse(
            code = 0,
            message = "0",
            ttl = 1,
            data = data,
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

    private fun fakeRoomPlayInfoV2Data(streams: List<PlayStream> = listOf(fakeHttpStream())) =
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
                                ),
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
}
