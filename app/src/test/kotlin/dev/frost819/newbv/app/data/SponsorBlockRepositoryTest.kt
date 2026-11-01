package dev.frost819.newbv.app.data

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.network.SponsorBlockApi
import dev.frost819.newbv.app.network.entity.SponsorHashVideo
import dev.frost819.newbv.app.network.entity.SponsorSegmentDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [SponsorBlockRepository] 的单元测试。
 *
 * Mock [SponsorBlockApi]，验证：bvid 筛选、CID 过滤、归一化去重、
 * 空分类拦截请求、内存缓存 TTL、网络失败不写缓存。
 */
class SponsorBlockRepositoryTest {
    private val repository = SponsorBlockRepository()

    private fun dto(
        uuid: String,
        startSec: Double,
        endSec: Double,
        category: String = "sponsor",
        actionType: String = "skip",
        cid: String = "100",
        votes: Int = 0,
        locked: Int = 0,
    ) = SponsorSegmentDto(
        segment = listOf(startSec, endSec),
        uuid = uuid,
        category = category,
        actionType = actionType,
        cid = cid,
        votes = votes,
        locked = locked,
    )

    @BeforeEach
    fun setUp() {
        mockkObject(SponsorBlockApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SponsorBlockApi)
    }

    @Test
    fun `returns empty without request when categories empty`() =
        runTest {
            val result = repository.getSkipSegments("BV1xx", 100L, emptyList())

            assertThat(result).isEmpty()
            coVerify(exactly = 0) { SponsorBlockApi.getSkipSegments(any(), any()) }
        }

    @Test
    fun `filters response by videoID and cid`() =
        runTest {
            val videos =
                listOf(
                    SponsorHashVideo(
                        videoID = "BV1other",
                        segments = listOf(dto("wrong", 1.0, 2.0)),
                    ),
                    SponsorHashVideo(
                        videoID = "BV1xx",
                        segments =
                            listOf(
                                dto("a", 1.0, 2.0, cid = "100"),
                                dto("b", 3.0, 4.0, cid = "200"),
                            ),
                    ),
                )
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns videos

            val result = repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))

            assertThat(result.map { it.uuid }).containsExactly("a")
        }

    @Test
    fun `returns all cid segments when cid invalid`() =
        runTest {
            val videos =
                listOf(
                    SponsorHashVideo(
                        videoID = "BV1xx",
                        segments =
                            listOf(
                                dto("a", 1.0, 2.0, cid = "100"),
                                dto("b", 3.0, 4.0, cid = "200"),
                            ),
                    ),
                )
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns videos

            val result = repository.getSkipSegments("BV1xx", 0L, listOf("sponsor"))

            assertThat(result).hasSize(2)
        }

    @Test
    fun `normalizes unknown actionType and invalid range away and dedupes by uuid`() =
        runTest {
            val videos =
                listOf(
                    SponsorHashVideo(
                        videoID = "BV1xx",
                        segments =
                            listOf(
                                dto("a", 1.0, 2.0),
                                dto("bad-action", 1.0, 2.0, actionType = "full"),
                                dto("bad-range", 2.0, 1.0),
                                // UUID 重复：高票者胜出
                                dto("dup", 5.0, 6.0, votes = 1),
                                dto("dup", 5.0, 6.0, votes = 10),
                            ),
                    ),
                )
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns videos

            val result = repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))

            assertThat(result.map { it.uuid }).containsExactly("a", "dup").inOrder()
            assertThat(result[1].votes).isEqualTo(10)
        }

    @Test
    fun `caches result within ttl and skips second request`() =
        runTest {
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns
                listOf(SponsorHashVideo(videoID = "BV1xx", segments = listOf(dto("a", 1.0, 2.0))))

            repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))
            repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))

            coVerify(exactly = 1) { SponsorBlockApi.getSkipSegments(any(), any()) }
        }

    @Test
    fun `cache key distinguishes category selections`() =
        runTest {
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns emptyList()

            repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))
            repository.getSkipSegments("BV1xx", 100L, listOf("sponsor", "intro"))

            coVerify(exactly = 2) { SponsorBlockApi.getSkipSegments(any(), any()) }
        }

    @Test
    fun `does not cache on network failure and retries next call`() =
        runTest {
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } throws IOException("offline")

            runCatching { repository.getSkipSegments("BV1xx", 100L, listOf("sponsor")) }
            val second =
                runCatching { repository.getSkipSegments("BV1xx", 100L, listOf("sponsor")) }

            coVerify(exactly = 2) { SponsorBlockApi.getSkipSegments(any(), any()) }
            assertThat(second.exceptionOrNull()).isInstanceOf(IOException::class.java)
        }

    @Test
    fun `maps dto to domain with ms action type and locked flag`() =
        runTest {
            coEvery { SponsorBlockApi.getSkipSegments(any(), any()) } returns
                listOf(
                    SponsorHashVideo(
                        videoID = "BV1xx",
                        segments = listOf(dto("a", 1.5, 2.5, actionType = "mute", locked = 1)),
                    ),
                )

            val result = repository.getSkipSegments("BV1xx", 100L, listOf("sponsor"))

            assertThat(result).hasSize(1)
            assertThat(result[0].startMs).isEqualTo(1_500L)
            assertThat(result[0].endMs).isEqualTo(2_500L)
            assertThat(result[0].actionType).isEqualTo(SponsorActionType.MUTE)
            assertThat(result[0].locked).isTrue()
        }
}
