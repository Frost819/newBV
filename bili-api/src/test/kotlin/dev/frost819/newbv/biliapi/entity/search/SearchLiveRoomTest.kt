package dev.frost819.newbv.biliapi.entity.search

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.search.SearchResultData
import dev.frost819.newbv.biliapi.repositories.SearchTypeResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * 直播间搜索结果解析测试。
 */
class SearchLiveRoomTest {
    @Test
    fun `search result parses and maps live room item`() {
        val data =
            SearchResultData(
                seid = "seid",
                page = 1,
                pageSize = 20,
                numResults = 1,
                numPages = 2,
                suggestKeyword = "",
                rqtType = "",
                eggHit = 0,
                result =
                    listOf(
                        Json.parseToJsonElement(
                            """
                            {
                              "type": "live_room",
                              "uid": 100,
                              "title": "直播标题",
                              "uname": "主播",
                              "roomid": 1718159119,
                              "live_status": 1,
                              "online": 1234,
                              "cover": "//cover.example/live.jpg",
                              "uface": "//face.example/avatar.jpg",
                              "cate_name": "测试分区"
                            }
                            """.trimIndent(),
                        ),
                    ),
            )

        val result = SearchTypeResult.fromSearchTypeResult(data)

        assertThat(result.liveRooms).hasSize(1)
        assertThat(result.liveRooms.single().roomId).isEqualTo(1718159119L)
        assertThat(result.liveRooms.single().cover).isEqualTo("https://cover.example/live.jpg")
        assertThat(result.liveRooms.single().areaName).isEqualTo("测试分区")
        assertThat(result.hasMore).isTrue()
    }
}
