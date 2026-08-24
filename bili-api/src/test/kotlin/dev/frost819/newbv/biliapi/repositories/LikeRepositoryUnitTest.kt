package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [LikeRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证点赞查询与点赞/取消点赞操作的
 * 参数传递、成功路径与失败路径。不依赖真实网络。
 */
class LikeRepositoryUnitTest {
    private lateinit var repository: LikeRepository
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val AID = 993403941L
        private const val BVID = "BV1xx411c7mD"
        private const val CSRF = "test-bili-jct-token"
        private const val ACCESS_TOKEN = "test-access-token"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.biliJct = CSRF
        authRepository.accessToken = ACCESS_TOKEN
        repository = LikeRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // checkVideoLiked
    // ------------------------------------------------------------------

    @Test
    fun `checkVideoLiked returns true when API returns true`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoLiked(any(), any()) } returns true

            // When
            val result = repository.checkVideoLiked(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `checkVideoLiked returns false when API returns false`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoLiked(any(), any()) } returns false

            // When
            val result = repository.checkVideoLiked(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `checkVideoLiked passes aid and bvid to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoLiked(any(), any()) } returns true

            // When
            repository.checkVideoLiked(aid = AID, bvid = BVID)

            // Then
            coVerify { BiliHttpApi.checkVideoLiked(eq(AID), eq(BVID)) }
        }

    @Test
    fun `checkVideoLiked passes null bvid to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoLiked(any(), any()) } returns false

            // When
            repository.checkVideoLiked(aid = AID, bvid = null)

            // Then
            coVerify { BiliHttpApi.checkVideoLiked(eq(AID), isNull()) }
        }

    // ------------------------------------------------------------------
    // updateVideoLiked
    // ------------------------------------------------------------------

    @Test
    fun `updateVideoLiked succeeds when API returns success`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When — should not throw
            repository.updateVideoLiked(aid = AID, bvid = BVID, like = true)

            // Then — verify all parameters forwarded correctly
            coVerify { BiliHttpApi.sendVideoLike(eq(AID), eq(BVID), eq(true), eq(CSRF)) }
        }

    @Test
    fun `updateVideoLiked throws when API returns failure`() =
        runTest {
            // Given
            val errorMessage = "你已经点过赞了"
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(false, errorMessage)

            // When
            val exception =
                assertThrows(Exception::class.java) {
                    runBlocking { repository.updateVideoLiked(aid = AID, bvid = BVID, like = true) }
                }

            // Then
            assertThat(exception.message).isEqualTo(errorMessage)
        }

    @Test
    fun `updateVideoLiked passes like=true for liking`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.updateVideoLiked(aid = AID, bvid = BVID, like = true)

            // Then
            coVerify { BiliHttpApi.sendVideoLike(eq(AID), eq(BVID), eq(true), eq(CSRF)) }
        }

    @Test
    fun `updateVideoLiked passes like=false for unliking`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.updateVideoLiked(aid = AID, bvid = BVID, like = false)

            // Then
            coVerify { BiliHttpApi.sendVideoLike(eq(AID), eq(BVID), eq(false), eq(CSRF)) }
        }

    @Test
    fun `updateVideoLiked uses biliJct as csrf`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.updateVideoLiked(aid = AID, bvid = BVID, like = true)

            // Then
            coVerify { BiliHttpApi.sendVideoLike(any(), any(), any(), eq(CSRF)) }
        }

    @Test
    fun `updateVideoLiked uses empty string when biliJct is null`() =
        runTest {
            // Given
            authRepository.biliJct = null
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.updateVideoLiked(aid = AID, bvid = BVID, like = true)

            // Then
            coVerify { BiliHttpApi.sendVideoLike(eq(AID), eq(BVID), eq(true), eq("")) }
        }

    @Test
    fun `updateVideoLiked works with null bvid`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoLike(any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.updateVideoLiked(aid = AID, bvid = null, like = true)

            // Then
            coVerify { BiliHttpApi.sendVideoLike(eq(AID), isNull(), eq(true), eq(CSRF)) }
        }

    // ------------------------------------------------------------------
    // App 路径
    // ------------------------------------------------------------------

    @Test
    fun `checkVideoLiked App passes accessKey`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoLiked(any(), any(), any()) } returns true

            repository.checkVideoLiked(aid = AID, preferApiType = ApiType.App, bvid = BVID)

            coVerify { BiliHttpApi.checkVideoLiked(eq(AID), eq(BVID), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `updateVideoLiked App calls App API`() =
        runTest {
            coEvery { BiliHttpApi.sendVideoLikeApp(any(), any(), any()) } returns Pair(true, "")

            repository.updateVideoLiked(aid = AID, like = true, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.sendVideoLikeApp(eq(AID), eq(true), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `updateVideoLiked App throws on failure`() =
        runTest {
            coEvery { BiliHttpApi.sendVideoLikeApp(any(), any(), any()) } returns Pair(false, "app-error")

            val exception =
                assertThrows(Exception::class.java) {
                    runBlocking { repository.updateVideoLiked(aid = AID, like = true, preferApiType = ApiType.App) }
                }

            assertThat(exception.message).isEqualTo("app-error")
        }

    @Test
    fun `updateVideoLiked App uses empty accessKey when null`() =
        runTest {
            authRepository.accessToken = null
            coEvery { BiliHttpApi.sendVideoLikeApp(any(), any(), any()) } returns Pair(true, "")

            repository.updateVideoLiked(aid = AID, like = false, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.sendVideoLikeApp(eq(AID), eq(false), eq("")) }
        }
}
