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
 * [FavoriteRepository] 的集成测试。
 *
 * 验证收藏状态查询、收藏/取消收藏、收藏夹列表与内容获取（Web + App HTTP）。
 * 查询类接口断言正常返回数据；收藏为互动类操作仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class FavoriteRepositoryTest {
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
    private val favoriteRepository = FavoriteRepository(authRepository)

    init {
        channelRepository.initDefaultChannel(ACCESS_TOKEN, BUVID)
        BiliHttpApi.init(
            buvid3 = BUVID,
            sessData = SESSDATA,
            biliJct = BILI_JCT,
            mid = UID,
            accessToken = ACCESS_TOKEN,
        )

        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `check video is favoured with cookies`() =
        runBlocking {
            // 查询类：正常返回布尔结果即视为数据有效
            val result =
                favoriteRepository.checkVideoFavoured(
                    aid = 170001,
                    preferApiType = ApiType.Web,
                )
            println("web favoured: $result")
        }

    @Test
    fun `check video is favoured with token`() =
        runBlocking {
            val result =
                favoriteRepository.checkVideoFavoured(
                    aid = 170001,
                    preferApiType = ApiType.App,
                )
            println("app favoured: $result")
        }

    @Test
    fun `add video to favorite folder with cookies`() =
        runBlocking {
            // 互动类：repository 失败会抛异常，正常返回即接口成功
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.Web)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.addVideoToFavoriteFolder(
                aid = 170001,
                addMediaIds = listOf(defaultMediaId),
                preferApiType = ApiType.Web,
            )
        }

    @Test
    fun `add video to favorite folder with token`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.App)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.addVideoToFavoriteFolder(
                aid = 170001,
                addMediaIds = listOf(defaultMediaId),
                preferApiType = ApiType.App,
            )
        }

    @Test
    fun `del video from favorite folder with cookies`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.Web)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.delVideoFromFavoriteFolder(
                aid = 170001,
                delMediaIds = listOf(defaultMediaId),
                preferApiType = ApiType.Web,
            )
        }

    @Test
    fun `del video from favorite folder with token`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.App)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.delVideoFromFavoriteFolder(
                aid = 170001,
                delMediaIds = listOf(defaultMediaId),
                preferApiType = ApiType.App,
            )
        }

    @Test
    fun `update video to favorite folder with cookies`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.App)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.updateVideoToFavoriteFolder(
                aid = 170001,
                addMediaIds = listOf(defaultMediaId),
                delMediaIds = listOf(),
                preferApiType = ApiType.Web,
            )
        }

    @Test
    fun `update video to favorite folder with token`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.App)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            favoriteRepository.updateVideoToFavoriteFolder(
                aid = 170001,
                addMediaIds = listOf(defaultMediaId),
                delMediaIds = listOf(),
                preferApiType = ApiType.App,
            )
        }

    @Test
    fun `get all favorite folders metadata with cookies`() =
        runBlocking {
            // 查询类：断言返回收藏夹列表数据
            val result =
                favoriteRepository.getAllFavoriteFolderMetadataList(
                    mid = UID,
                    rid = 170001,
                    preferApiType = ApiType.Web,
                )
            println("web folders: ${result.map { "${it.id}:${it.title}" }}")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get all favorite folders metadata with token`() =
        runBlocking {
            val result =
                favoriteRepository.getAllFavoriteFolderMetadataList(
                    mid = UID,
                    rid = 170001,
                    preferApiType = ApiType.App,
                )
            println("app folders: ${result.map { "${it.id}:${it.title}" }}")
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get favorite folder data with cookies`() =
        runBlocking {
            // 查询类：断言返回收藏夹内容数据
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.Web)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            val result =
                favoriteRepository.getFavoriteFolderData(
                    mediaId = defaultMediaId,
                    pageSize = 20,
                    pageNumber = 1,
                    preferApiType = ApiType.Web,
                )
            println("web folder medias: ${result.medias.size}")
            assertThat(result.medias).isNotEmpty()
        }

    @Test
    fun `get favorite folder data with token`() =
        runBlocking {
            val defaultMediaId = getDefaultFavoriteFolderId(ApiType.App)
            assertThat(defaultMediaId).isNotEqualTo(0L)
            val result =
                favoriteRepository.getFavoriteFolderData(
                    mediaId = defaultMediaId,
                    pageSize = 20,
                    pageNumber = 1,
                    preferApiType = ApiType.App,
                )
            println("app folder medias: ${result.medias.size}")
            assertThat(result.medias).isNotEmpty()
        }

    private suspend fun getDefaultFavoriteFolderId(preferApiType: ApiType): Long {
        val foldersInfoResult =
            favoriteRepository.getAllFavoriteFolderMetadataList(
                mid = UID,
                preferApiType = preferApiType,
            )
        val id = foldersInfoResult.find { it.title == "默认收藏夹" }?.id ?: 0
        println("default media id: $id")
        return id
    }
}
