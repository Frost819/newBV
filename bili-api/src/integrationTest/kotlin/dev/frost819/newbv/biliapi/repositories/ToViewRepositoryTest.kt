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
 * [ToViewRepository] 的集成测试。
 *
 * 验证稍后再看列表获取（Web HTTP + App HTTP access_key）以及添加/删除操作。
 * 查询类接口断言正常返回数据；添加/删除为互动类操作仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class ToViewRepositoryTest {
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

        private const val TEST_AID = 170001L
        private const val TEST_BVID = "BV1xx411c7mD"
    }

    private val authRepository = AuthRepository()
    private val toViewRepository = ToViewRepository(authRepository)

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `get toview with web api`() {
        runBlocking {
            // 查询类：断言返回稍后再看列表数据（可能为空，仅验证结构）
            val result =
                toViewRepository.getToView(
                    cursor = 0L,
                    preferApiType = ApiType.Web,
                )
            println("web toview count: ${result.data.size}")
            result.data.take(3).forEach { println("  - ${it.title}") }
            assertThat(result.data).isNotNull()
        }
    }

    @Test
    fun `get toview with grpc`() {
        runBlocking {
            val result =
                toViewRepository.getToView(
                    cursor = 0L,
                    preferApiType = ApiType.App,
                )
            println("grpc toview count: ${result.data.size}")
            result.data.take(3).forEach { println("  - ${it.title}") }
            assertThat(result.data).isNotNull()
        }
    }

    @Test
    fun `add and del toview with web api`() {
        runBlocking {
            // 互动类：先加后删还原状态，repository 失败会抛异常
            toViewRepository.addToView(
                aid = TEST_AID,
                bvid = TEST_BVID,
                preferApiType = ApiType.Web,
            )
            println("added $TEST_BVID to toview")

            toViewRepository.delToView(
                aid = TEST_AID,
                viewed = false,
                preferApiType = ApiType.Web,
            )
            println("deleted $TEST_BVID from toview")
        }
    }
}
