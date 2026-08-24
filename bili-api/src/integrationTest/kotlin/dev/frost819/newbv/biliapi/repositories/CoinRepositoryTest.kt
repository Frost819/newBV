package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [CoinRepository] 的集成测试。
 *
 * 验证投币状态查询与投币操作（Web HTTP + App HTTP）。
 * 查询类接口断言正常返回数据；投币为互动类操作（不可重复），仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class CoinRepositoryTest {
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
    private val coinRepository = CoinRepository(authRepository)

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
    }

    @Test
    fun `check video coined`() =
        runBlocking {
            // 查询类：正常返回布尔结果即视为数据有效
            val result = coinRepository.checkVideoCoined(aid = TEST_AID, bvid = TEST_BVID, preferApiType = ApiType.Web)
            println("video $TEST_BVID coined: $result")
        }

    @Test
    fun `send video coin`() =
        runBlocking {
            // 互动类：repository 失败会抛异常，走到这里即接口返回正常
            coinRepository.sendVideoCoin(aid = TEST_AID, bvid = TEST_BVID, multiply = 1, preferApiType = ApiType.Web)
            println("coined $TEST_BVID successfully")
        }

    @Test
    fun `check video coined with app api`() =
        runBlocking {
            val result =
                coinRepository.checkVideoCoined(
                    aid = TEST_AID,
                    bvid = TEST_BVID,
                    preferApiType = ApiType.App,
                )
            println("app video $TEST_BVID coined: $result")
        }

    @Test
    fun `send video coin with app api`() =
        runBlocking {
            coinRepository.sendVideoCoin(
                aid = TEST_AID,
                bvid = TEST_BVID,
                multiply = 1,
                preferApiType = ApiType.App,
            )
            println("app coined $TEST_BVID successfully")
        }
}
