package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [OneClickTripleActionRepository] 的集成测试。
 *
 * 验证一键三连操作（Web HTTP + App HTTP）。
 * 一键三连为互动类操作（不可重复），仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class OneClickTripleActionRepositoryTest {
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
    private val tripleActionRepository = OneClickTripleActionRepository(authRepository)

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
    }

    @Test
    fun `send video one click triple action`() =
        runBlocking {
            // 互动类：repository 失败会抛异常，正常返回即接口成功
            val result =
                tripleActionRepository.sendVideoOneClickTripleAction(
                    aid = TEST_AID,
                    bvid = TEST_BVID,
                )
            println("triple action result: like=${result?.like}, coin=${result?.coin}, fav=${result?.fav}")
            assertThat(result).isNotNull()
        }

    @Test
    fun `send video one click triple action with app api`() =
        runBlocking {
            val result =
                tripleActionRepository.sendVideoOneClickTripleAction(
                    aid = TEST_AID,
                    bvid = TEST_BVID,
                    preferApiType = ApiType.App,
                )
            println("app triple action result: like=${result?.like}, coin=${result?.coin}, fav=${result?.fav}")
            assertThat(result).isNotNull()
        }
}
