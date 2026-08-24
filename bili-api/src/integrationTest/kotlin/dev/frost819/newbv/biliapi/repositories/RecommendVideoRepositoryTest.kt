package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

class RecommendVideoRepositoryTest {
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
    private val recommendVideoRepository =
        RecommendVideoRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(
            ACCESS_TOKEN,
            BUVID,
        )
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)

        authRepository.sessionData = SeasonRepositoryTest.SESSDATA
        authRepository.accessToken = SeasonRepositoryTest.ACCESS_TOKEN
        authRepository.biliJct = SeasonRepositoryTest.BILI_JCT
        authRepository.mid = SeasonRepositoryTest.UID
    }

    @Test
    fun getPopularVideos() =
        runBlocking {
            // 查询类：断言返回热门视频数据
            val result =
                recommendVideoRepository.getPopularVideos(
                    page = PopularVideoPage(),
                    preferApiType = ApiType.App,
                )
            println("popular videos: ${result.list.size}")
            assertThat(result.list).isNotEmpty()
        }

    @Test
    fun `get popular videos with web api`() =
        runBlocking {
            `get popular videos`(5, ApiType.Web)
        }

    @Test
    fun `get popular videos with app api`() =
        runBlocking {
            `get popular videos`(5, ApiType.App)
        }

    private suspend fun `get popular videos`(
        pageCount: Int,
        preferApiType: ApiType,
    ) = runBlocking {
        // 查询类：断言每页返回热门视频数据
        var nextPage = PopularVideoPage()
        for (i in 0..pageCount) {
            val result =
                recommendVideoRepository.getPopularVideos(
                    page = nextPage,
                    preferApiType = preferApiType,
                )
            nextPage = result.nextPage
            println("popular page $i: ${result.list.size} items")
            assertThat(result.list).isNotEmpty()
        }
    }

    @Test
    fun `get recommend videos with web api`() =
        runBlocking {
            `get recommend videos`(5, ApiType.Web)
        }

    @Test
    fun `get recommend videos with app api`() =
        runBlocking {
            `get recommend videos`(5, ApiType.App)
        }

    private suspend fun `get recommend videos`(
        pageCount: Int,
        preferApiType: ApiType,
    ) = runBlocking {
        // 查询类：断言每页返回推荐视频数据
        var nextPage = RecommendPage()
        for (i in 0..pageCount) {
            println("page: $nextPage")
            val result =
                recommendVideoRepository.getRecommendVideos(
                    page = nextPage,
                    preferApiType = preferApiType,
                )
            nextPage = result.nextPage
            println("recommend page $i: ${result.items.size} items")
            assertThat(result.items).isNotEmpty()
        }
    }
}
