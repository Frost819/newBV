package dev.frost819.newbv.biliapi.http

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

@org.junit.jupiter.api.Tag("integration")
class BiliLiveHttpApiTest {
    companion object {
        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2

        @JvmStatic
        @BeforeAll
        fun setup() {
            BiliHttpApi.init(BUVID)
            BiliHttpApi.sessData = SESSDATA
            BiliHttpApi.mid = UID
            runBlocking {
                runCatching { BiliHttpApi.fetchBuvid3FromSpi() }
            }
        }
    }

    @Test
    fun `get history live room danmaku`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                BiliLiveHttpApi.getLiveDanmuHistory(roomId = 22739471)
            }
        }
    }

    @Test
    fun `get live event websocket connect url and token`() {
        runBlocking {
            val response = BiliLiveHttpApi.getLiveDanmuInfo(roomId = 22739471)
            Assertions.assertEquals(0, response.code, "API should return code=0, got: ${response.message}")
            Assertions.assertNotNull(response.data, "data should not be null")
            Assertions.assertTrue(response.data!!.token.isNotBlank(), "token should not be blank")
            Assertions.assertTrue(response.data!!.hostList.isNotEmpty(), "host_list should not be empty")
        }
    }

    @Test
    fun `get live room info`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                BiliLiveHttpApi.getLiveRoomPlayInfo(roomId = 22739471)
            }
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
            Assertions.assertNotNull(response.data, "data should not be null")
            Assertions.assertTrue(response.data!!.isNotEmpty(), "list should not be empty")
            Assertions.assertEquals(30, response.data!!.size, "should return exactly 30 items")
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
}
