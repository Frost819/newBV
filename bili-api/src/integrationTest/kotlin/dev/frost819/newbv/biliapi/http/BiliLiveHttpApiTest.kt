package dev.frost819.newbv.biliapi.http

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

class BiliLiveHttpApiTest {
    companion object {
        private const val ROOM_ID = 1718159119

        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""

        @JvmStatic
        @BeforeAll
        fun setup() {
            BiliHttpApi.init(
                buvid3 = BUVID,
                sessData = SESSDATA,
                biliJct = BILI_JCT,
                mid = UID,
                accessToken = ACCESS_TOKEN,
            )
            runBlocking {
                runCatching { BiliHttpApi.fetchBuvid3FromSpi() }
            }
        }
    }

    @Test
    fun `get history live room danmaku`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                BiliLiveHttpApi.getLiveDanmuHistory(roomId = ROOM_ID)
            }
        }
    }

    @Test
    fun `get live event websocket connect url and token`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveDanmuInfo(roomId = ROOM_ID)
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertTrue(data.token.isNotBlank(), "token should not be blank")
            Assertions.assertTrue(data.hostList.isNotEmpty(), "host_list should not be empty")
        }
    }

    @Test
    fun `get live room info`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId = ROOM_ID)
                Assertions.assertEquals(0, response.code)
                Assertions.assertEquals(ROOM_ID, response.data?.roomId)
            }
        }
    }

    @Test
    fun `room init resolves the configured room`() {
        runBlocking {
            val response = BiliLiveHttpApi.getRoomInit(ROOM_ID)

            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            Assertions.assertEquals(ROOM_ID, response.data?.roomId)
        }
    }

    @Test
    fun `room play info returns a valid live state`() {
        runBlocking {
            val response = BiliLiveHttpApi.getRoomPlayInfoV2(ROOM_ID)

            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data)
            Assertions.assertEquals(ROOM_ID, data.roomId)
            Assertions.assertTrue(data.liveStatus in 0..2, "unexpected live_status=${data.liveStatus}")
        }
    }

    @Test
    fun `room stream endpoint returns response`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveStreamUrl(cid = ROOM_ID)

            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            Assertions.assertNotNull(response.data)
        }
    }

    @Test
    fun `get area live list returns 30 items from getRoomList`() {
        runBlocking {
            val response =
                BiliLiveHttpApi.getAreaLiveList(
                    parentAreaId = 2,
                    areaId = 0,
                    page = 1,
                    pageSize = 30,
                    sortType = "online",
                )
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertTrue(data.isNotEmpty(), "list should not be empty")
            Assertions.assertEquals(30, data.size, "should return exactly 30 items")
        }
    }

    @Test
    fun `get area live list page 2 returns different items`() {
        runBlocking {
            val page1 =
                BiliLiveHttpApi.getAreaLiveList(
                    parentAreaId = 2,
                    areaId = 0,
                    page = 1,
                    pageSize = 30,
                    sortType = "online",
                )
            val page2 =
                BiliLiveHttpApi.getAreaLiveList(
                    parentAreaId = 2,
                    areaId = 0,
                    page = 2,
                    pageSize = 30,
                    sortType = "online",
                )
            Assertions.assertEquals(0, page1.code)
            Assertions.assertEquals(0, page2.code)
            val page1Ids = page1.data!!.map { it.roomId }.toSet()
            val page2Ids = page2.data!!.map { it.roomId }.toSet()
            val overlap = page1Ids.intersect(page2Ids)
            Assertions.assertTrue(overlap.size < 5, "page1 and page2 should have minimal overlap, got: $overlap")
        }
    }

    @Test
    fun `get live list returns modules`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveList()
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
        }
    }

    @Test
    fun `get live recommend returns list`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveRecommend()
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertTrue(data.list.isNotEmpty(), "live recommend list should not be empty")
        }
    }

    @Test
    fun `get follow live returns response`() {
        runBlocking {
            val response = BiliLiveHttpApi.getFollowLive()
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
        }
    }

    @Test
    fun `get follow live paginates without duplicates`() {
        runBlocking {
            val firstResponse = BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10)
            Assertions.assertEquals(0, firstResponse.code, "API should return code=0, got: ${firstResponse.message}")
            val firstData = requireNotNull(firstResponse.data) { "data should not be null" }
            val firstRooms = firstData.rooms.ifEmpty { firstData.list }
            Assertions.assertTrue(firstRooms.size <= 10, "第 1 页不应超过 10 条，实际 ${firstRooms.size}")

            Assumptions.assumeTrue(
                firstData.count > 10,
                "关注中正在直播的主播不足 11 个（count=${firstData.count}），跳过翻页断言",
            )

            val secondResponse = BiliLiveHttpApi.getFollowLive(page = 2, pageSize = 10)
            Assertions.assertEquals(0, secondResponse.code, "API should return code=0, got: ${secondResponse.message}")
            val secondData = requireNotNull(secondResponse.data) { "data should not be null" }
            val secondRooms = secondData.rooms.ifEmpty { secondData.list }
            Assertions.assertTrue(secondRooms.isNotEmpty(), "还有第 2 页时不应返回空列表")

            val overlap = firstRooms.map { it.roomId }.intersect(secondRooms.map { it.roomId }.toSet())
            Assertions.assertTrue(overlap.isEmpty(), "第 1、2 页不应重复：$overlap")
        }
    }

    @Test
    fun `get follow live page past the end is empty`() {
        runBlocking {
            val firstResponse = BiliLiveHttpApi.getFollowLive(page = 1, pageSize = 10)
            val firstData = requireNotNull(firstResponse.data) { "data should not be null" }
            Assumptions.assumeTrue(firstData.count > 0, "关注中没有正在直播的主播，跳过")

            val totalPages = (firstData.count + 9) / 10
            val response = BiliLiveHttpApi.getFollowLive(page = totalPages + 1, pageSize = 10)
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertTrue(
                data.rooms.ifEmpty { data.list }.isEmpty(),
                "超出末页（第 $totalPages 页）应返回空列表",
            )
        }
    }

    @Test
    fun `get live area list returns categories`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveAreaList()
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertTrue(data.isNotEmpty(), "area list should not be empty")
        }
    }

    @Test
    fun `get room info returns details`() {
        runBlocking {
            val response = BiliLiveHttpApi.getRoomInfo(ROOM_ID)
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            val data = requireNotNull(response.data) { "data should not be null" }
            Assertions.assertEquals(ROOM_ID, data.roomId, "room_id should match")
            Assertions.assertTrue(data.uid > 0, "room info should expose anchor uid")
        }
    }

    @Test
    fun `anchor info resolves through user card endpoint`() {
        runBlocking {
            // /room/v1/Room/get_info 只返回 uid，不含 uname/face，直播间 UP 信息需再查一次用户卡片
            val roomInfo = BiliLiveHttpApi.getRoomInfo(ROOM_ID).getResponseData()
            println("room info anchor: uid=${roomInfo.uid}, uname='${roomInfo.uname}', face='${roomInfo.face}'")

            val cardResponse = BiliHttpApi.getUserCardInfo(uid = roomInfo.uid)
            Assertions.assertEquals(0, cardResponse.code, "card API should return code=0, got: ${cardResponse.message}")
            val card = cardResponse.getResponseData().card
            println("user card: name='${card.name}', face='${card.face}'")

            Assertions.assertTrue(card.name.isNotBlank(), "anchor name should not be blank")
            Assertions.assertTrue(card.face.isNotBlank(), "anchor face should not be blank")
        }
    }
}
