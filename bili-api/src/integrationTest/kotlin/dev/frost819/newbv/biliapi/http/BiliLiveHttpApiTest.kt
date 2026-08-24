package dev.frost819.newbv.biliapi.http

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
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
        }
    }
}
