package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.FavoriteItemType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfo
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfoListData
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteItem
import dev.frost819.newbv.biliapi.http.entity.user.favorite.Upper
import dev.frost819.newbv.biliapi.http.entity.user.favorite.UserFavoriteFoldersData
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [FavoriteRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证收藏查询、收藏/取消收藏操作、
 * 收藏夹元数据列表获取与收藏夹内容获取的参数传递与数据转换。
 * 不依赖真实网络。
 */
class FavoriteRepositoryUnitTest {
    private lateinit var repository: FavoriteRepository
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val AID = 993403941L
        private const val MID = 12345L
        private const val MEDIA_ID = 67890L
        private const val ACCESS_TOKEN = "test-access-token"
        private const val BILI_JCT = "test-bili-jct"
        private const val VIDEO_TYPE_VALUE = 2
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
        repository = FavoriteRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // checkVideoFavoured
    // ------------------------------------------------------------------

    @Test
    fun `checkVideoFavoured returns true when Web API returns true`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoFavoured(any(), any()) } returns true

            val result = repository.checkVideoFavoured(aid = AID, preferApiType = ApiType.Web)

            assertThat(result).isTrue()
        }

    @Test
    fun `checkVideoFavoured returns false when Web API returns false`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoFavoured(any(), any()) } returns false

            val result = repository.checkVideoFavoured(aid = AID, preferApiType = ApiType.Web)

            assertThat(result).isFalse()
        }

    @Test
    fun `checkVideoFavoured Web does not pass accessKey`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoFavoured(any(), any()) } returns true

            repository.checkVideoFavoured(aid = AID, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.checkVideoFavoured(eq(AID), isNull()) }
        }

    @Test
    fun `checkVideoFavoured App passes accessToken as accessKey`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoFavoured(any(), any()) } returns true

            repository.checkVideoFavoured(aid = AID, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.checkVideoFavoured(eq(AID), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `checkVideoFavoured App uses empty string when accessToken is null`() =
        runTest {
            authRepository.accessToken = null
            coEvery { BiliHttpApi.checkVideoFavoured(any(), any()) } returns false

            repository.checkVideoFavoured(aid = AID, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.checkVideoFavoured(eq(AID), eq("")) }
        }

    // ------------------------------------------------------------------
    // addVideoToFavoriteFolder
    // ------------------------------------------------------------------

    @Test
    fun `addVideoToFavoriteFolder Web passes aid, type, addMediaIds and csrf`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val addMediaIds = listOf(100L, 200L)

            repository.addVideoToFavoriteFolder(aid = AID, addMediaIds = addMediaIds, preferApiType = ApiType.Web)

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    addMediaIds = eq(addMediaIds),
                    delMediaIds = any(),
                    accessKey = isNull(),
                    csrf = eq(BILI_JCT),
                )
            }
        }

    @Test
    fun `addVideoToFavoriteFolder App passes accessToken instead of csrf`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val addMediaIds = listOf(100L)

            repository.addVideoToFavoriteFolder(aid = AID, addMediaIds = addMediaIds, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    addMediaIds = eq(addMediaIds),
                    delMediaIds = any(),
                    accessKey = eq(ACCESS_TOKEN),
                    csrf = isNull(),
                )
            }
        }

    // ------------------------------------------------------------------
    // delVideoFromFavoriteFolder
    // ------------------------------------------------------------------

    @Test
    fun `delVideoFromFavoriteFolder Web passes delMediaIds and csrf`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val delMediaIds = listOf(300L)

            repository.delVideoFromFavoriteFolder(aid = AID, delMediaIds = delMediaIds, preferApiType = ApiType.Web)

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    addMediaIds = any(),
                    delMediaIds = eq(delMediaIds),
                    accessKey = isNull(),
                    csrf = eq(BILI_JCT),
                )
            }
        }

    @Test
    fun `delVideoFromFavoriteFolder App passes accessToken`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val delMediaIds = listOf(300L)

            repository.delVideoFromFavoriteFolder(aid = AID, delMediaIds = delMediaIds, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    delMediaIds = eq(delMediaIds),
                    accessKey = eq(ACCESS_TOKEN),
                )
            }
        }

    // ------------------------------------------------------------------
    // updateVideoToFavoriteFolder
    // ------------------------------------------------------------------

    @Test
    fun `updateVideoToFavoriteFolder Web passes both add and del mediaIds with csrf`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val addMediaIds = listOf(100L)
            val delMediaIds = listOf(200L)

            repository.updateVideoToFavoriteFolder(
                aid = AID,
                addMediaIds = addMediaIds,
                delMediaIds = delMediaIds,
                preferApiType = ApiType.Web,
            )

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    addMediaIds = eq(addMediaIds),
                    delMediaIds = eq(delMediaIds),
                    accessKey = isNull(),
                    csrf = eq(BILI_JCT),
                )
            }
        }

    @Test
    fun `updateVideoToFavoriteFolder App passes both add and del mediaIds with accessToken`() =
        runTest {
            coJustRun { BiliHttpApi.setVideoToFavorite(any(), any(), any(), any(), any(), any()) }
            val addMediaIds = listOf(100L)
            val delMediaIds = listOf(200L)

            repository.updateVideoToFavoriteFolder(
                aid = AID,
                addMediaIds = addMediaIds,
                delMediaIds = delMediaIds,
                preferApiType = ApiType.App,
            )

            coVerify {
                BiliHttpApi.setVideoToFavorite(
                    avid = eq(AID),
                    type = eq(VIDEO_TYPE_VALUE),
                    addMediaIds = eq(addMediaIds),
                    delMediaIds = eq(delMediaIds),
                    accessKey = eq(ACCESS_TOKEN),
                    csrf = isNull(),
                )
            }
        }

    // ------------------------------------------------------------------
    // getAllFavoriteFolderMetadataList
    // ------------------------------------------------------------------

    @Test
    fun `getAllFavoriteFolderMetadataList Web maps folders and returns list`() =
        runTest {
            val folderData =
                UserFavoriteFoldersData(
                    count = 2,
                    list =
                        listOf(
                            UserFavoriteFoldersData.UserFavoriteFolder(
                                id = 1001L,
                                fid = 10L,
                                mid = MID,
                                attr = 0,
                                title = "folder-1",
                                favState = 1,
                                mediaCount = 5,
                            ),
                            UserFavoriteFoldersData.UserFavoriteFolder(
                                id = 1002L,
                                fid = 20L,
                                mid = MID,
                                attr = 0,
                                title = "folder-2",
                                favState = 0,
                                mediaCount = 0,
                            ),
                        ),
                )
            coEvery {
                BiliHttpApi.getAllFavoriteFoldersInfo(any(), any(), any(), any())
            } returns BiliResponse(code = 0, message = "", data = folderData)

            val result = repository.getAllFavoriteFolderMetadataList(mid = MID, preferApiType = ApiType.Web)

            assertThat(result).hasSize(2)
            assertThat(result[0].title).isEqualTo("folder-1")
            assertThat(result[0].videoInThisFav).isTrue()
            assertThat(result[0].mediaCount).isEqualTo(5)
            assertThat(result[1].videoInThisFav).isFalse()
        }

    @Test
    fun `getAllFavoriteFolderMetadataList Web passes mid, type and rid`() =
        runTest {
            coEvery {
                BiliHttpApi.getAllFavoriteFoldersInfo(any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = UserFavoriteFoldersData(count = 0, list = emptyList()),
                )

            repository.getAllFavoriteFolderMetadataList(
                mid = MID,
                type = FavoriteItemType.Video,
                rid = AID,
                preferApiType = ApiType.Web,
            )

            coVerify {
                BiliHttpApi.getAllFavoriteFoldersInfo(
                    mid = eq(MID),
                    type = eq(FavoriteItemType.Video.value),
                    rid = eq(AID),
                    accessKey = isNull(),
                )
            }
        }

    @Test
    fun `getAllFavoriteFolderMetadataList App passes accessToken`() =
        runTest {
            coEvery {
                BiliHttpApi.getAllFavoriteFoldersInfo(any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = UserFavoriteFoldersData(count = 0, list = emptyList()),
                )

            repository.getAllFavoriteFolderMetadataList(mid = MID, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.getAllFavoriteFoldersInfo(
                    mid = eq(MID),
                    type = any(),
                    rid = isNull(),
                    accessKey = eq(ACCESS_TOKEN),
                )
            }
        }

    @Test
    fun `getAllFavoriteFolderMetadataList App uses empty string when accessToken is null`() =
        runTest {
            authRepository.accessToken = null
            coEvery {
                BiliHttpApi.getAllFavoriteFoldersInfo(any(), any(), any(), any())
            } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = UserFavoriteFoldersData(count = 0, list = emptyList()),
                )

            repository.getAllFavoriteFolderMetadataList(mid = MID, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.getAllFavoriteFoldersInfo(
                    mid = eq(MID),
                    type = any(),
                    rid = isNull(),
                    accessKey = eq(""),
                )
            }
        }

    // ------------------------------------------------------------------
    // getFavoriteFolderData
    // ------------------------------------------------------------------

    @Test
    fun `getFavoriteFolderData Web returns mapped FavoriteFolderData`() =
        runTest {
            val listData =
                FavoriteFolderInfoListData(
                    info = favoriteFolderInfo(favState = 1, mediaCount = 3),
                    medias = listOf(fakeFavoriteItem(id = 1L), fakeFavoriteItem(id = 2L)),
                    hasMore = true,
                )
            coEvery {
                BiliHttpApi.getFavoriteList(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns BiliResponse(code = 0, message = "", data = listData)

            val result =
                repository.getFavoriteFolderData(
                    mediaId = MEDIA_ID,
                    pageSize = 20,
                    pageNumber = 1,
                    preferApiType = ApiType.Web,
                )

            assertThat(result.info.title).isEqualTo("test-folder")
            assertThat(result.info.videoInThisFav).isTrue()
            assertThat(result.medias).hasSize(2)
            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getFavoriteFolderData App passes accessToken`() =
        runTest {
            val listData =
                FavoriteFolderInfoListData(
                    info = favoriteFolderInfo(favState = 0, mediaCount = 0),
                    medias = emptyList(),
                    hasMore = false,
                )
            coEvery {
                BiliHttpApi.getFavoriteList(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns BiliResponse(code = 0, message = "", data = listData)

            repository.getFavoriteFolderData(
                mediaId = MEDIA_ID,
                pageSize = 10,
                pageNumber = 2,
                preferApiType = ApiType.App,
            )

            coVerify {
                BiliHttpApi.getFavoriteList(
                    mediaId = eq(MEDIA_ID),
                    pageSize = eq(10),
                    pageNumber = eq(2),
                    accessKey = eq(ACCESS_TOKEN),
                )
            }
        }

    @Test
    fun `getFavoriteFolderData App uses empty string when accessToken is null`() =
        runTest {
            authRepository.accessToken = null
            val listData =
                FavoriteFolderInfoListData(
                    info = favoriteFolderInfo(favState = 0, mediaCount = 0),
                    medias = emptyList(),
                    hasMore = false,
                )
            coEvery {
                BiliHttpApi.getFavoriteList(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns BiliResponse(code = 0, message = "", data = listData)

            repository.getFavoriteFolderData(mediaId = MEDIA_ID, preferApiType = ApiType.App)

            coVerify {
                BiliHttpApi.getFavoriteList(
                    mediaId = eq(MEDIA_ID),
                    pageSize = any(),
                    pageNumber = any(),
                    accessKey = eq(""),
                )
            }
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun favoriteFolderInfo(
        favState: Int,
        mediaCount: Int,
    ) = FavoriteFolderInfo(
        id = 1001L, fid = 10L, mid = MID, attr = 0,
        title = "test-folder", cover = "http://cover.test",
        upper = Upper(mid = MID, name = "up", face = "http://face.test"),
        coverType = 0, cntInfo = dev.frost819.newbv.biliapi.http.entity.user.favorite.CntInfo(collect = 0, play = 0),
        type = 11, intro = "", ctime = 0, mtime = 0, state = 0,
        favState = favState, likeState = 0, mediaCount = mediaCount,
    )

    private fun fakeFavoriteItem(id: Long) =
        FavoriteItem(
            id = id, type = 2, title = "video-$id", cover = "http://cover.test/$id",
            intro = "", page = 1, duration = 100,
            upper = Upper(mid = MID, name = "up", face = "http://face.test"),
            attr = 0, cntInfo = dev.frost819.newbv.biliapi.http.entity.user.favorite.CntInfo(collect = 0, play = 0),
            link = "http://link.test/$id", ctime = 0, pubtime = 0, favTime = 0,
            bvid = "BV$id",
        )
}
