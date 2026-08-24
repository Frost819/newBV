package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties
import java.util.UUID

class SearchRepositoryTest {
    companion object {
        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
    }

    private val authRepository = AuthRepository()
    private val channelRepository = ChannelRepository()
    private val searchRepository = SearchRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(
            ACCESS_TOKEN,
            BUVID,
        )
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)

        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
        authRepository.buvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
    }

    @Test
    fun `get search hot words with web api`() =
        runBlocking {
            // 查询类：断言返回热搜词
            val result =
                searchRepository.getSearchHotwords(
                    limit = 50,
                    preferApiType = ApiType.Web,
                )
            println("web hotwords: ${result.size}")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get search hot words with app api`() =
        runBlocking {
            val result =
                searchRepository.getSearchHotwords(
                    limit = 50,
                    preferApiType = ApiType.App,
                )
            println("app hotwords: ${result.size}")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get search suggest with web api`() =
        runBlocking {
            // 查询类：断言返回搜索建议
            val result =
                searchRepository.getSearchSuggest(
                    keyword = "00",
                    preferApiType = ApiType.Web,
                )
            println("web suggests: $result")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get search suggest with app api`() =
        runBlocking {
            val result =
                searchRepository.getSearchSuggest(
                    keyword = "00",
                    preferApiType = ApiType.App,
                )
            println("app suggests: $result")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `search type test`() =
        runBlocking {
            // 查询类：搜索视频断言返回视频结果（fate 有大量视频）
            val reply =
                searchRepository.searchType(
                    keyword = "fate",
                    type = SearchType.Video,
                    page = SearchTypePage(),
                    tid = 0,
                    order = SearchFilterOrderType.MostComment,
                    duration = SearchFilterDuration.All,
                    preferApiType = ApiType.App,
                )
            println("app video results: ${reply.videos.size}")
            assertThat(reply.videos).isNotEmpty()
        }

    @Test
    fun `search bangumi with web api`() =
        runBlocking {
            val reply =
                searchRepository.searchType(
                    keyword = "英雄联盟",
                    type = SearchType.MediaBangumi,
                    page = SearchTypePage(),
                    tid = null,
                    order = SearchFilterOrderType.ComprehensiveSort,
                    duration = SearchFilterDuration.All,
                    preferApiType = ApiType.Web,
                )
            assertThat(reply.pgcs).isNotEmpty()
        }

    @Test
    fun `search film and television with web api`() =
        runBlocking {
            val reply =
                searchRepository.searchType(
                    keyword = "英雄联盟",
                    type = SearchType.MediaFt,
                    page = SearchTypePage(),
                    tid = null,
                    order = SearchFilterOrderType.ComprehensiveSort,
                    duration = SearchFilterDuration.All,
                    preferApiType = ApiType.Web,
                )
            assertThat(reply.pgcs).isNotEmpty()
        }

    @Test
    fun `search all with web api`() =
        runBlocking {
            // 查询类：搜索"奥特曼"应返回视频结果（若为空说明接口异常或风控）
            val result =
                searchRepository.searchAll(
                    keyword = "奥特曼",
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            println("web searchAll videos: ${result.videos.size}, pgcs: ${result.pgcs.size}")
            assertThat(result.videos).isNotEmpty()
        }

    @Test
    fun `search all with gRPC api`() =
        runBlocking {
            // 查询类：gRPC SearchAll 应返回视频结果（若为空说明接口异常或风控）
            val result =
                searchRepository.searchAll(
                    keyword = "奥特曼",
                    page = 1,
                    preferApiType = ApiType.App,
                )
            println("gRPC searchAll videos: ${result.videos.size}, pgcs: ${result.pgcs.size}")
            assertThat(result.videos).isNotEmpty()
        }
}
