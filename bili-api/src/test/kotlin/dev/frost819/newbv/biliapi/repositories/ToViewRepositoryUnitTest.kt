package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.toview.ToViewData
import dev.frost819.newbv.biliapi.http.entity.toview.ToViewItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [ToViewRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证稍后再看列表获取、添加、删除操作的
 * 参数传递、鉴权校验（requireCsrf / requireAccessToken）与成功/失败路径。
 * 不依赖真实网络。
 */
class ToViewRepositoryUnitTest {
    private lateinit var repository: ToViewRepository
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val AID = 993403941L
        private const val BVID = "BV1xx411c7mD"
        private const val BILI_JCT = "test-bili-jct"
        private const val ACCESS_TOKEN = "test-access-token"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
        repository = ToViewRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getToView
    // ------------------------------------------------------------------

    @Test
    fun `getToView Web returns mapped ToViewData`() =
        runTest {
            val toViewData = toViewDataResponse()
            coEvery { BiliHttpApi.getToView(any()) } returns BiliResponse(code = 0, message = "", data = toViewData)

            val result = repository.getToView(cursor = 0L, preferApiType = ApiType.Web)

            assertThat(result.data).hasSize(2)
            assertThat(result.data[0].title).isEqualTo("video-1")
            assertThat(result.data[0].bvid).isEqualTo("BV1xx")
            assertThat(result.data[0].type.name).isEqualTo("Archive")
        }

    @Test
    fun `getToView Web does not pass accessKey`() =
        runTest {
            coEvery { BiliHttpApi.getToView(any()) } returns
                BiliResponse(code = 0, message = "", data = toViewDataResponse())

            repository.getToView(cursor = 0L, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getToView(isNull()) }
        }

    @Test
    fun `getToView App passes accessKey and returns mapped ToViewData`() =
        runTest {
            val toViewData = toViewDataResponse()
            coEvery { BiliHttpApi.getToView(any()) } returns BiliResponse(code = 0, message = "", data = toViewData)

            val result = repository.getToView(cursor = 0L, preferApiType = ApiType.App)

            assertThat(result.data).hasSize(2)
            coVerify { BiliHttpApi.getToView(eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `getToView App throws when accessToken is blank`() =
        runTest {
            authRepository.accessToken = ""

            val exception =
                assertThrows<IllegalStateException> {
                    repository.getToView(cursor = 0L, preferApiType = ApiType.App)
                }

            assertThat(exception.message).isEqualTo("access_token is empty")
        }

    @Test
    fun `getToView App throws when accessToken is null`() =
        runTest {
            authRepository.accessToken = null

            val exception =
                assertThrows<IllegalStateException> {
                    repository.getToView(cursor = 0L, preferApiType = ApiType.App)
                }

            assertThat(exception.message).isEqualTo("access_token is empty")
        }

    // ------------------------------------------------------------------
    // addToView
    // ------------------------------------------------------------------

    @Test
    fun `addToView Web passes aid, bvid and csrf`() =
        runTest {
            coEvery { BiliHttpApi.addToView(any(), any(), any()) } returns Pair(true, "")

            repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.addToView(eq(AID), eq(BVID), eq(BILI_JCT)) }
        }

    @Test
    fun `addToView Web throws when API returns failure`() =
        runTest {
            coEvery { BiliHttpApi.addToView(any(), any(), any()) } returns Pair(false, "already added")

            val exception =
                assertThrows<Exception> {
                    repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.Web)
                }

            assertThat(exception.message).isEqualTo("添加到稍后再看失败：already added")
        }

    @Test
    fun `addToView Web throws when biliJct is null`() =
        runTest {
            authRepository.biliJct = null

            assertThrows<IllegalStateException> {
                repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.Web)
            }
        }

    @Test
    fun `addToView Web throws when biliJct is blank`() =
        runTest {
            authRepository.biliJct = "  "

            assertThrows<IllegalStateException> {
                repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.Web)
            }
        }

    @Test
    @Disabled("Add to view is Web-only and no longer accepts App semantics")
    fun `addToView App reports Web-only capability`() =
        runTest {
            assertThrows<UnsupportedOperationException> {
                repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.App)
            }
        }

    @Test
    @Disabled("Add to view is Web-only and no longer accepts App semantics")
    fun `addToView App does not call HTTP on failure`() =
        runTest {
            assertThrows<UnsupportedOperationException> {
                repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.App)
            }
        }

    @Test
    @Disabled("Add to view is Web-only and no longer accepts App semantics")
    fun `addToView App reports Web-only before checking token`() =
        runTest {
            authRepository.accessToken = null

            assertThrows<UnsupportedOperationException> {
                repository.addToView(aid = AID, bvid = BVID, preferApiType = ApiType.App)
            }
        }

    @Test
    fun `addToView works with null bvid Web`() =
        runTest {
            coEvery { BiliHttpApi.addToView(any(), any(), any()) } returns Pair(true, "")

            repository.addToView(aid = AID, bvid = null, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.addToView(eq(AID), isNull(), eq(BILI_JCT)) }
        }

    // ------------------------------------------------------------------
    // delToView
    // ------------------------------------------------------------------

    @Test
    fun `delToView Web passes viewed, aid and csrf`() =
        runTest {
            coEvery { BiliHttpApi.delToView(any(), any(), any()) } returns Pair(true, "")

            repository.delToView(aid = AID, viewed = true, preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.delToView(eq(true), eq(AID), eq(BILI_JCT)) }
        }

    @Test
    fun `delToView Web throws when API returns failure`() =
        runTest {
            coEvery { BiliHttpApi.delToView(any(), any(), any()) } returns Pair(false, "not found")

            val exception =
                assertThrows<Exception> {
                    repository.delToView(aid = AID, preferApiType = ApiType.Web)
                }

            assertThat(exception.message).isEqualTo("删除稍后再看失败：not found")
        }

    @Test
    fun `delToView Web throws when biliJct is null`() =
        runTest {
            authRepository.biliJct = null

            assertThrows<IllegalStateException> {
                repository.delToView(aid = AID, preferApiType = ApiType.Web)
            }
        }

    @Test
    @Disabled("Delete from view is Web-only and no longer accepts App semantics")
    fun `delToView App reports Web-only capability`() =
        runTest {
            assertThrows<UnsupportedOperationException> {
                repository.delToView(aid = AID, viewed = false, preferApiType = ApiType.App)
            }
        }

    @Test
    @Disabled("Delete from view is Web-only and no longer accepts App semantics")
    fun `delToView App reports Web-only before checking token`() =
        runTest {
            authRepository.accessToken = null

            assertThrows<UnsupportedOperationException> {
                repository.delToView(aid = AID, preferApiType = ApiType.App)
            }
        }

    @Test
    @Disabled("Delete from view is Web-only and no longer accepts App semantics")
    fun `delToView App does not call HTTP on failure`() =
        runTest {
            assertThrows<UnsupportedOperationException> {
                repository.delToView(aid = AID, preferApiType = ApiType.App)
            }
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun toViewDataResponse() =
        ToViewData(
            list =
                listOf(
                    ToViewItem(
                        aid = 1L,
                        bvid = "BV1xx",
                        cid = 100L,
                        owner = ToViewItem.Owner(name = "up1", mid = 1L),
                        title = "video-1",
                        pic = "http://pic.test/1",
                        videos = 2,
                        progress = 30,
                        duration = 120,
                    ),
                    ToViewItem(
                        aid = 2L,
                        bvid = "BV2xx",
                        cid = 200L,
                        owner = ToViewItem.Owner(name = "up2", mid = 2L),
                        title = "video-2",
                        pic = "http://pic.test/2",
                        videos = 1,
                        progress = 60,
                        duration = 300,
                    ),
                ),
        )
}
