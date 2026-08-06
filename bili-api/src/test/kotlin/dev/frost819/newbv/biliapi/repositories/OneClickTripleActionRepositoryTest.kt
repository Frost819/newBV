package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.video.OneClickTripleAction
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
 * [OneClickTripleActionRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证一键三连操作的参数传递、
 * 成功路径与失败路径。不依赖真实网络。
 */
class OneClickTripleActionRepositoryTest {
    private lateinit var repository: OneClickTripleActionRepository
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val AID = 993403941L
        private const val BVID = "BV1xx411c7mD"
        private const val CSRF = "test-bili-jct-token"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.biliJct = CSRF
        repository = OneClickTripleActionRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    @Test
    fun `returns data when API returns success`() =
        runTest {
            // Given
            val expected = OneClickTripleAction(like = true, coin = true, fav = true)
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", expected)

            // When
            val result = repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isEqualTo(expected)
            assertThat(result?.like).isTrue()
            assertThat(result?.coin).isTrue()
            assertThat(result?.fav).isTrue()
        }

    @Test
    fun `throws when API returns failure`() =
        runTest {
            // Given
            val errorMessage = "操作太频繁"
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(false, errorMessage, null)

            // When
            val exception =
                assertThrows(Exception::class.java) {
                    runBlocking {
                        repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)
                    }
                }

            // Then
            assertThat(exception.message).isEqualTo(errorMessage)
        }

    @Test
    fun `passes correct aid and bvid to API`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", null)

            // When
            repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)

            // Then
            coVerify {
                BiliHttpApi.sendVideoOneClickTripleAction(eq(AID), eq(BVID), eq(CSRF))
            }
        }

    @Test
    fun `uses biliJct as csrf`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", null)

            // When
            repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)

            // Then
            coVerify {
                BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), eq(CSRF))
            }
        }

    @Test
    fun `uses empty string when biliJct is null`() =
        runTest {
            // Given
            authRepository.biliJct = null
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", null)

            // When
            repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)

            // Then
            coVerify {
                BiliHttpApi.sendVideoOneClickTripleAction(eq(AID), eq(BVID), eq(""))
            }
        }

    @Test
    fun `returns null when API returns success but data is null`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", null)

            // When
            val result = repository.sendVideoOneClickTripleAction(aid = AID, bvid = BVID)

            // Then
            assertThat(result).isNull()
        }

    @Test
    fun `works with null bvid`() =
        runTest {
            // Given
            coEvery { BiliHttpApi.sendVideoOneClickTripleAction(any(), any(), any()) } returns
                Triple(true, "", null)

            // When
            repository.sendVideoOneClickTripleAction(aid = AID, bvid = null)

            // Then
            coVerify {
                BiliHttpApi.sendVideoOneClickTripleAction(eq(AID), isNull(), eq(CSRF))
            }
        }
}
