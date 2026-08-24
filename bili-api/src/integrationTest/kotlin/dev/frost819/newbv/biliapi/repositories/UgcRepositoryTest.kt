package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ugc.UgcType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.Properties
import kotlin.test.Test

class UgcRepositoryTest {
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
    private val ugcRepository: UgcRepository = UgcRepository(authRepository)

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `get region data`() =
        runBlocking {
            // 查询类：断言每个分区返回视频数据
            UgcType.entries
                .filter { it.locId != -1 }
                .forEach { ugcType ->
                    println("ugcType: $ugcType")
                    val result = ugcRepository.getRegionData(ugcType)
                    println("region items: ${result.items.size}")
                    assertThat(result.items).isNotEmpty()
                }
        }

    @Test
    fun `get region more data`() =
        runBlocking {
            UgcType.entries
                .filter { it.locId != -1 }
                .forEach { ugcType ->
                    println("ugcType: $ugcType")
                    val result = ugcRepository.getRegionMoreData(ugcType)
                    println("region list items: ${result.items.size}")
                    assertThat(result.items).isNotEmpty()
                }
        }
}
