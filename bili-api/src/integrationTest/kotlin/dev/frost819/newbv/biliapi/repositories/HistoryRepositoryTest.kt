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
 * [HistoryRepository] 的集成测试。
 *
 * 验证历史记录获取（Web HTTP + App gRPC）。查询类接口断言正常返回数据。
 * 依赖真实 B 站凭证和网络。
 */
class HistoryRepositoryTest {
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
    private val historyRepository = HistoryRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(
            ACCESS_TOKEN,
            BUVID,
        )
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)

        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `get histories with web api`() =
        runBlocking {
            // 查询类：历史记录不应为空（测试账号有观看历史；若为空说明接口异常或风控）
            val result =
                historyRepository.getHistories(
                    cursor = 0,
                    preferApiType = ApiType.Web,
                )
            println("web histories: ${result.data.size}")
            assertThat(result.data).isNotEmpty()
        }

    @Test
    fun `get histories with app api`() =
        runBlocking {
            val result =
                historyRepository.getHistories(
                    cursor = 1688955898,
                    preferApiType = ApiType.App,
                )
            println("app histories: ${result.data.size}")
            assertThat(result.data).isNotEmpty()
        }
}
