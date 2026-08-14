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

@org.junit.jupiter.api.Tag("integration")
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
            FavoriteRepositoryTest.ACCESS_TOKEN,
            FavoriteRepositoryTest.BUVID,
        )
        BiliHttpApi.init(FavoriteRepositoryTest.BUVID)

        authRepository.sessionData = FavoriteRepositoryTest.SESSDATA
        authRepository.accessToken = FavoriteRepositoryTest.ACCESS_TOKEN
        authRepository.biliJct = FavoriteRepositoryTest.BILI_JCT
        authRepository.buvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
    }

    @Test
    fun `get search hot words with web api`() =
        runBlocking {
            val result =
                searchRepository.getSearchHotwords(
                    limit = 50,
                    preferApiType = ApiType.Web,
                )
            println(result)
        }

    @Test
    fun `get search hot words with app api`() =
        runBlocking {
            val result =
                searchRepository.getSearchHotwords(
                    limit = 50,
                    preferApiType = ApiType.App,
                )
            println(result)
        }

    @Test
    fun `get search suggest with web api`() =
        runBlocking {
            val result =
                searchRepository.getSearchSuggest(
                    keyword = "00",
                    preferApiType = ApiType.Web,
                )
            println(result)
        }

    @Test
    fun `get search suggest with app api`() =
        runBlocking {
            val result =
                searchRepository.getSearchSuggest(
                    keyword = "00",
                    preferApiType = ApiType.App,
                )
            println(result)
        }

    @Test
    fun `search type test`() =
        runBlocking {
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
            println(reply)
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
}
