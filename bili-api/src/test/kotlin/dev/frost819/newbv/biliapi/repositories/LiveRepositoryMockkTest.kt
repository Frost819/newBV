package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.live.AreaLiveListResult
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.http.entity.live.RoomInitData
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
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
    fun `getLiveList returns response data`() = runBlocking {
        val fakeData = LiveListResponse()
        coEvery { BiliLiveHttpApi.getLiveList() } returns fakeResponse(fakeData)

        val result = repository.getLiveList()

        assertThat(result).isEqualTo(fakeData)
    }

    @Test
    fun `getLiveRecommend returns response data`() = runBlocking {
        val fakeData = LiveRecommendResponse()
        coEvery { BiliLiveHttpApi.getLiveRecommend() } returns fakeResponse(fakeData)

        val result = repository.getLiveRecommend()

        assertThat(result).isEqualTo(fakeData)
    }

    @Test
    fun `getFollowLive returns response data`() = runBlocking {
        val fakeData = FollowLiveResponse()
        coEvery { BiliLiveHttpApi.getFollowLive() } returns fakeResponse(fakeData)

        val result = repository.getFollowLive()

        assertThat(result).isEqualTo(fakeData)
    }

    @Test
    fun `getLiveAreaList returns response data`() = runBlocking {
        val fakeData = listOf(LiveAreaParent(id = 1, name = "分区1", list = emptyList()))
        coEvery { BiliLiveHttpApi.getLiveAreaList() } returns fakeResponse(fakeData)

        val result = repository.getLiveAreaList()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("分区1")
    }

    @Test
    fun `getAreaLiveList returns result with hasMore true when full page`() = runBlocking {
        val fakeRooms = List(30) { fakeLiveRoomItem(roomId = it + 1) }
        coEvery {
            BiliLiveHttpApi.getAreaLiveList(any(), any(), any(), any(), any())
        } returns fakeResponse(fakeRooms)

        val result = repository.getAreaLiveList(parentAreaId = 2, areaId = 0, page = 1)

        assertThat(result.list).hasSize(30)
        assertThat(result.hasMore).isTrue()
    }

    @Test
    fun `getAreaLiveList returns result with hasMore false when partial page`() = runBlocking {
        val fakeRooms = List(10) { fakeLiveRoomItem(roomId = it + 1) }
        coEvery {
            BiliLiveHttpApi.getAreaLiveList(any(), any(), any(), any(), any())
        } returns fakeResponse(fakeRooms)

        val result = repository.getAreaLiveList(parentAreaId = 2, areaId = 0, page = 1)

        assertThat(result.list).hasSize(10)
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `getRoomInit returns response data`() = runBlocking {
        val fakeData = RoomInitData(roomId = 1718159119, shortId = 0, uid = 1, needP2P = 0, isHidden = false, isLocked = false, isPortrait = false, liveStatus = 1, hiddenTill = 0, lockTill = 0, encrypted = false, pwdVerified = false, liveTime = 0, roomShield = 0, isSp = 0, specialType = 0)
        coEvery { BiliLiveHttpApi.getRoomInit(any()) } returns fakeResponse(fakeData)

        val result = repository.getRoomInit(1718159119)

        assertThat(result.roomId).isEqualTo(1718159119)
    }

    @Test
    fun `getRoomInfo returns response data`() = runBlocking {
        val fakeData = RoomInfoData(roomId = 1718159119, shortId = 0, uid = 1, liveStatus = 1, title = "测试直播间")
        coEvery { BiliLiveHttpApi.getRoomInfo(any()) } returns fakeResponse(fakeData)

        val result = repository.getRoomInfo(1718159119)

        assertThat(result.roomId).isEqualTo(1718159119)
        assertThat(result.title).isEqualTo("测试直播间")
    }

    @Test
    fun `getAvailableQualities returns sorted qualities on success`() = runBlocking {
        val playInfo = fakeRoomPlayInfoV2Data()
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

        val result = repository.getAvailableQualities(1718159119)

        assertThat(result).isNotEmpty()
        assertThat(result[0].first).isAtLeast(result[1].first)
    }

    @Test
    fun `getAvailableQualities returns empty list on failure`() = runBlocking {
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("network error")

        val result = repository.getAvailableQualities(1718159119)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getLiveStreamInfo returns url and qn on success`() = runBlocking {
        val playInfo = fakeRoomPlayInfoV2Data()
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

        val result = repository.getLiveStreamInfo(1718159119, 10000)

        assertThat(result.url).isNotNull()
        assertThat(result.currentQn).isEqualTo(10000)
    }

    @Test
    fun `getLiveStreamInfo falls back to simple url on v2 failure`() = runBlocking {
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
        coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } returns
            fakeResponse(
                dev.frost819.newbv.biliapi.http.entity.live.SimplePlayUrlData(
                    currentQuality = 0,
                    durl = listOf(dev.frost819.newbv.biliapi.http.entity.live.SimpleDurl(url = "http://fallback.flv")),
                ),
            )
        val result = repository.getLiveStreamInfo(1718159119, 0)

        assertThat(result.url).isEqualTo("http://fallback.flv")
        assertThat(result.currentQn).isEqualTo(0)
    }

    @Test
    fun `getLiveStreamInfo returns null url when all methods fail`() = runBlocking {
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } throws RuntimeException("v2 error")
        coEvery { BiliLiveHttpApi.getLiveStreamUrl(any(), any()) } throws RuntimeException("simple error")

        val result = repository.getLiveStreamInfo(1718159119, 0)

        assertThat(result.url).isNull()
        assertThat(result.currentQn).isEqualTo(0)
    }

    @Test
    fun `getLiveStreamUrl delegates to getLiveStreamInfo url`() = runBlocking {
        val playInfo = fakeRoomPlayInfoV2Data()
        coEvery { BiliLiveHttpApi.getRoomPlayInfoV2(any(), any()) } returns fakeResponse(playInfo)

        val result = repository.getLiveStreamUrl(1718159119, 10000)

        assertThat(result).isNotNull()
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

    private fun fakeRoomPlayInfoV2Data() =
        RoomPlayInfoV2Data(
            roomId = 1718159119,
            shortId = 0,
            uid = 1,
            liveStatus = 1,
            playUrlInfo =
                dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfo(
                    playUrl =
                        dev.frost819.newbv.biliapi.http.entity.live.PlayUrl(
                            cid = 1718159119,
                            qnDesc =
                                listOf(
                                    dev.frost819.newbv.biliapi.http.entity.live.QnDesc(qn = 10000, desc = "原画"),
                                    dev.frost819.newbv.biliapi.http.entity.live.QnDesc(qn = 400, desc = "蓝光"),
                                ),
                            stream =
                                listOf(
                                    dev.frost819.newbv.biliapi.http.entity.live.PlayStream(
                                        protocolName = "http_stream",
                                        format =
                                            listOf(
                                                dev.frost819.newbv.biliapi.http.entity.live.PlayFormat(
                                                    formatName = "flv",
                                                    codec =
                                                        listOf(
                                                            dev.frost819.newbv.biliapi.http.entity.live.PlayCodec(
                                                                codecName = "avc",
                                                                currentQn = 10000,
                                                                acceptQn = listOf(10000, 400),
                                                                baseUrl = "/live.flv",
                                                                urlInfo =
                                                                    listOf(
                                                                        dev.frost819.newbv.biliapi.http.entity.live.PlayUrlInfoItem(
                                                                            host = "https://cdn.example",
                                                                            extra = "?query=1",
                                                                        ),
                                                                    ),
                                                                mediaInfo = dev.frost819.newbv.biliapi.http.entity.live.MediaInfo(),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                ),
        )
}
