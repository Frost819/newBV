package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [VideoDetailRepository] 的集成测试。
 *
 * 验证视频详情获取（Web HTTP + App gRPC）与 PGC 番剧详情（Web + App HTTP）。
 * 查询类接口断言正常返回数据（标题非空、aid 匹配）。
 * 依赖真实 B 站凭证和网络。
 */
class VideoDetailRepositoryTest {
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
    private val videoDetailRepository =
        VideoDetailRepository(
            authRepository,
            channelRepository,
            FavoriteRepository(authRepository),
            LikeRepository(authRepository),
            CoinRepository(authRepository),
        )

    init {
        channelRepository.initDefaultChannel(ACCESS_TOKEN, BUVID)
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    /** 查询类断言：详情标题非空且 aid 匹配。 */
    private suspend fun assertVideoDetail(
        aid: Long,
        preferApiType: ApiType,
    ): VideoDetail {
        val result =
            videoDetailRepository.getVideoDetail(
                aid = aid,
                preferApiType = preferApiType,
            )
        println("$preferApiType detail aid=$aid title=${result.title}")
        assertThat(result.title).isNotEmpty()
        assertThat(result.aid).isEqualTo(aid)
        return result
    }

    @Test
    fun `get video info with http`() =
        runBlocking {
            assertVideoDetail(170001, ApiType.Web)
        }

    @Test
    fun `get video info with grpc`() =
        runBlocking {
            assertVideoDetail(170001, ApiType.App)
        }

    @Test
    fun `get multi part video info with http`() =
        runBlocking {
            val result = assertVideoDetail(836207, ApiType.Web)
            // 多 P 视频：断言分 P 列表非空
            assertThat(result.pages).isNotEmpty()
        }

    @Test
    fun `get multi part video info with grpc`() =
        runBlocking {
            val result = assertVideoDetail(836207, ApiType.App)
            assertThat(result.pages).isNotEmpty()
        }

    @Test
    fun `get ugc season video info with http`() =
        runBlocking {
            assertVideoDetail(954251211, ApiType.Web)
        }

    @Test
    fun `get ugc season video info with grpc`() =
        runBlocking {
            assertVideoDetail(954251211, ApiType.App)
        }

    @Test
    fun `get anime video info with http`() =
        runBlocking {
            assertVideoDetail(314583081, ApiType.Web)
        }

    @Test
    fun `get anime video info with grpc`() =
        runBlocking {
            assertVideoDetail(314583081, ApiType.App)
        }

    @Test
    fun `get argue video info with http`() =
        runBlocking {
            assertVideoDetail(996965888, ApiType.Web)
        }

    @Test
    fun `get argue video info with grpc`() =
        runBlocking {
            assertVideoDetail(996965888, ApiType.App)
        }

    @Test
    fun `get pgc season video prefer web api`() =
        runBlocking {
            // 查询类：断言返回番剧详情数据
            val result =
                videoDetailRepository.getPgcVideoDetail(
                    epid = 752900,
                    preferApiType = ApiType.Web,
                )
            println("web season title=${result.title}")
            assertThat(result.title).isNotEmpty()
        }

    @Test
    fun `get pgc season video prefer app api`() =
        runBlocking {
            val result =
                videoDetailRepository.getPgcVideoDetail(
                    epid = 752900,
                    preferApiType = ApiType.App,
                )
            println("app season title=${result.title}")
            assertThat(result.title).isNotEmpty()
        }
}
