package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.biliapi.entity.season.TimelineFilter
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [SeasonRepository] 的集成测试。
 *
 * 验证追番列表与放送时间表获取（Web + App HTTP）。查询类接口断言正常返回数据；
 * 追番列表可能为空（账号未追番），仅断言接口正常返回。
 * 依赖真实 B 站凭证和网络。
 */
class SeasonRepositoryTest {
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
    private val seasonRepository = SeasonRepository(authRepository)

    init {
        channelRepository.initDefaultChannel(
            ACCESS_TOKEN,
            BUVID,
        )
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)

        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
        authRepository.mid = UID
    }

    @Test
    fun `get following seasons with web api`() =
        runBlocking {
            // 查询类：追番列表可能为空（账号未追番），断言接口正常返回结构化数据
            val bangumiResult =
                seasonRepository.getFollowingSeasons(
                    type = FollowingSeasonType.Bangumi,
                    preferApiType = ApiType.Web,
                )
            val cinemaResult =
                seasonRepository.getFollowingSeasons(
                    type = FollowingSeasonType.Cinema,
                    preferApiType = ApiType.Web,
                )
            println("web bangumi: ${bangumiResult.list.size}, cinema: ${cinemaResult.list.size}")
            assertThat(bangumiResult.total).isAtLeast(0)
            assertThat(cinemaResult.total).isAtLeast(0)
        }

    @Test
    fun `get following seasons with app api`() =
        runBlocking {
            val bangumiResult =
                seasonRepository.getFollowingSeasons(
                    type = FollowingSeasonType.Bangumi,
                    preferApiType = ApiType.App,
                )
            val cinemaResult =
                seasonRepository.getFollowingSeasons(
                    type = FollowingSeasonType.Cinema,
                    preferApiType = ApiType.App,
                )
            println("app bangumi: ${bangumiResult.list.size}, cinema: ${cinemaResult.list.size}")
            assertThat(bangumiResult.total).isAtLeast(0)
            assertThat(cinemaResult.total).isAtLeast(0)
        }

    @Test
    fun `get timeline with web api`() =
        runBlocking {
            // 查询类：时间表固定返回一周数据，断言非空
            TimelineFilter.webFilters.forEach { filter ->
                val result =
                    seasonRepository.getTimeline(
                        filter = filter,
                        preferApiType = ApiType.Web,
                    )
                println("web filter: $filter, days: ${result.size}")
                assertThat(result).isNotEmpty()
            }
        }

    @Test
    fun `get timeline with app api`() =
        runBlocking {
            TimelineFilter.appFilters.forEach { filter ->
                val result =
                    seasonRepository.getTimeline(
                        filter = filter,
                        preferApiType = ApiType.App,
                    )
                println("app filter: $filter, days: ${result.size}")
                assertThat(result).isNotEmpty()
            }
        }
}
