package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [LikeRepository] 的集成测试。
 *
 * 验证点赞状态查询与点赞/取消点赞操作（Web HTTP + App HTTP）。
 * 查询类接口断言正常返回数据；点赞为互动类操作，仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class LikeRepositoryTest {
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

        private const val TEST_AID = 993403941L
        private const val TEST_BVID = "BV1iw411c7mD"
    }

    private val authRepository = AuthRepository()
    private val likeRepository = LikeRepository(authRepository)

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
    }

    @Test
    fun `check video liked`() =
        runBlocking {
            // 查询类：正常返回布尔结果即视为数据有效
            val result = likeRepository.checkVideoLiked(aid = TEST_AID, bvid = TEST_BVID, preferApiType = ApiType.Web)
            println("video $TEST_BVID liked: $result")
        }

    @Test
    fun `update video liked`() =
        runBlocking {
            // 互动类：先赞后取消还原状态，repository 失败会抛异常
            likeRepository.updateVideoLiked(aid = TEST_AID, bvid = TEST_BVID, like = true, preferApiType = ApiType.Web)
            println("liked $TEST_BVID successfully")

            val afterLike =
                likeRepository.checkVideoLiked(
                    aid = TEST_AID,
                    bvid = TEST_BVID,
                    preferApiType = ApiType.Web,
                )
            println("after like, liked state: $afterLike")

            likeRepository.updateVideoLiked(aid = TEST_AID, bvid = TEST_BVID, like = false, preferApiType = ApiType.Web)
            println("unliked $TEST_BVID successfully")
        }

    @Test
    fun `check video liked with app api`() =
        runBlocking {
            val result = likeRepository.checkVideoLiked(aid = TEST_AID, bvid = TEST_BVID, preferApiType = ApiType.App)
            println("app video $TEST_BVID liked: $result")
        }

    @Test
    fun `update video liked with app api`() =
        runBlocking {
            likeRepository.updateVideoLiked(aid = TEST_AID, bvid = TEST_BVID, like = true, preferApiType = ApiType.App)
            println("app liked $TEST_BVID successfully")

            val afterLike =
                likeRepository.checkVideoLiked(
                    aid = TEST_AID,
                    bvid = TEST_BVID,
                    preferApiType = ApiType.App,
                )
            println("app after like, liked state: $afterLike")

            likeRepository.updateVideoLiked(aid = TEST_AID, bvid = TEST_BVID, like = false, preferApiType = ApiType.App)
            println("app unliked $TEST_BVID successfully")
        }
}
