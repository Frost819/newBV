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
 * [CoinRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证投币查询与投币操作的参数传递、
 * 成功路径与失败路径。不依赖真实网络。
 */
class CoinRepositoryUnitTest {
    private lateinit var repository: CoinRepository
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
        repository = CoinRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // checkVideoCoined
    // ------------------------------------------------------------------

    @Test
    fun `checkVideoCoined returns true when API returns true`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoSentCoin(any(), any()) } returns true

            // When
            val result = repository.checkVideoCoined(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `checkVideoCoined returns false when API returns false`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoSentCoin(any(), any()) } returns false

            // When
            val result = repository.checkVideoCoined(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `checkVideoCoined passes aid and bvid to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoSentCoin(any(), any()) } returns true

            // When
            repository.checkVideoCoined(aid = AID, bvid = BVID)

            // Then
            coVerify { BiliHttpApi.checkVideoSentCoin(eq(AID), eq(BVID)) }
        }

    @Test
    fun `checkVideoCoined passes null bvid to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.checkVideoSentCoin(any(), any()) } returns false

            // When
            repository.checkVideoCoined(aid = AID, bvid = null)

            // Then
            coVerify { BiliHttpApi.checkVideoSentCoin(eq(AID), isNull()) }
        }

    // ------------------------------------------------------------------
    // sendVideoCoin
    // ------------------------------------------------------------------

    @Test
    fun `sendVideoCoin succeeds when API returns success`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When — should not throw
            repository.sendVideoCoin(aid = AID, bvid = BVID, multiply = 1)

            // Then — verify all parameters forwarded correctly.
            // BiliHttpApi.sendVideoCoin signature: (avid, bvid, multiply, like, csrf).
            // Repository does not pass `like`, so it defaults to false.
            coVerify {
                BiliHttpApi.sendVideoCoin(eq(AID), eq(BVID), eq(1), eq(false), eq(CSRF))
            }
        }

    @Test
    fun `sendVideoCoin throws when API returns failure`() =
        runTest {
            // Given
            val errorMessage = "硬币不足"
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(false, errorMessage)

            // When
            val exception =
                assertThrows(Exception::class.java) {
                    runBlocking { repository.sendVideoCoin(aid = AID, bvid = BVID, multiply = 1) }
                }

            // Then
            assertThat(exception.message).isEqualTo(errorMessage)
        }

    @Test
    fun `sendVideoCoin passes multiply value to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.sendVideoCoin(aid = AID, bvid = BVID, multiply = 2)

            // Then
            coVerify {
                BiliHttpApi.sendVideoCoin(eq(AID), eq(BVID), eq(2), any(), eq(CSRF))
            }
        }

    @Test
    fun `sendVideoCoin uses default multiply of 1`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When — multiply not specified, defaults to 1
            repository.sendVideoCoin(aid = AID, bvid = BVID)

            // Then
            coVerify {
                BiliHttpApi.sendVideoCoin(eq(AID), eq(BVID), eq(1), any(), eq(CSRF))
            }
        }

    @Test
    fun `sendVideoCoin uses biliJct as csrf`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.sendVideoCoin(aid = AID, bvid = BVID, multiply = 1)

            // Then
            coVerify {
                BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), eq(CSRF))
            }
        }

    @Test
    fun `sendVideoCoin uses empty string when biliJct is null`() =
        runTest {
            // Given
            authRepository.biliJct = null
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.sendVideoCoin(aid = AID, bvid = BVID, multiply = 1)

            // Then
            coVerify {
                BiliHttpApi.sendVideoCoin(eq(AID), eq(BVID), eq(1), any(), eq(""))
            }
        }

    @Test
    fun `sendVideoCoin works with null bvid`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoCoin(any(), any(), any(), any(), any()) } returns
                Pair(true, "")

            // When
            repository.sendVideoCoin(aid = AID, bvid = null, multiply = 1)

            // Then
            coVerify {
                BiliHttpApi.sendVideoCoin(eq(AID), isNull(), eq(1), any(), eq(CSRF))
            }
        }

    // ------------------------------------------------------------------
    // App 路径
    // ------------------------------------------------------------------

    @Test
    fun `checkVideoCoined App passes accessKey`() =
        runTest {
            coEvery { BiliHttpApi.checkVideoSentCoin(any(), any(), any()) } returns true

            repository.checkVideoCoined(aid = AID, preferApiType = ApiType.App, bvid = BVID)

            coVerify { BiliHttpApi.checkVideoSentCoin(eq(AID), eq(BVID), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `sendVideoCoin App calls App API`() =
        runTest {
            coEvery { BiliHttpApi.sendVideoCoinApp(any(), any(), any(), any()) } returns Pair(true, "")

            repository.sendVideoCoin(aid = AID, multiply = 2, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.sendVideoCoinApp(eq(AID), eq(2), eq(false), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `sendVideoCoin App throws on failure`() =
        runTest {
            coEvery { BiliHttpApi.sendVideoCoinApp(any(), any(), any(), any()) } returns Pair(false, "app-coin-error")

            val exception =
                assertThrows(Exception::class.java) {
                    runBlocking { repository.sendVideoCoin(aid = AID, preferApiType = ApiType.App) }
                }

            assertThat(exception.message).isEqualTo("app-coin-error")
        }

    @Test
    fun `sendVideoCoin App uses empty accessKey when null`() =
        runTest {
            authRepository.accessToken = null
            coEvery { BiliHttpApi.sendVideoCoinApp(any(), any(), any(), any()) } returns Pair(true, "")

            repository.sendVideoCoin(aid = AID, preferApiType = ApiType.App)

            coVerify { BiliHttpApi.sendVideoCoinApp(eq(AID), any(), any(), eq("")) }
        }
}
