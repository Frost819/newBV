package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ugc.UgcType
import dev.frost819.newbv.biliapi.entity.ugc.UgcTypeV2
import dev.frost819.newbv.biliapi.entity.ugc.region.UgcFeedPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.region.RegionDynamic
import dev.frost819.newbv.biliapi.http.entity.region.RegionDynamicList
import dev.frost819.newbv.biliapi.http.entity.region.RegionFeedRcmd
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [UgcRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证分区推荐视频获取的
 * 参数传递、分页递增与数据转换。不依赖真实网络。
 */
class UgcRepositoryUnitTest {
    private lateinit var repository: UgcRepository
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        repository = UgcRepository(authRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getRegionFeedRcmd
    // ------------------------------------------------------------------

    @Test
    fun `getRegionFeedRcmd returns mapped UgcFeedData with items`() =
        runTest {
            val regionFeedRcmd =
                RegionFeedRcmd(
                    archives =
                        listOf(
                            fakeArchive(aid = 1L, title = "video-1"),
                            fakeArchive(aid = 2L, title = "video-2"),
                        ),
                )
            coEvery { BiliHttpApi.getRegionFeedRcmd(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = regionFeedRcmd)

            val result = repository.getRegionFeedRcmd(ugcType = UgcTypeV2.Douga, page = UgcFeedPage(nextPage = 1))

            assertThat(result.hasNext).isTrue()
            assertThat(result.items).hasSize(2)
            assertThat(result.items[0].aid).isEqualTo(1L)
            assertThat(result.items[0].title).isEqualTo("video-1")
            assertThat(result.items[1].aid).isEqualTo(2L)
        }

    @Test
    fun `getRegionFeedRcmd sets nextPage to page plus one`() =
        runTest {
            coEvery { BiliHttpApi.getRegionFeedRcmd(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = RegionFeedRcmd(archives = listOf(fakeArchive())))

            val result = repository.getRegionFeedRcmd(ugcType = UgcTypeV2.Game, page = UgcFeedPage(nextPage = 3))

            assertThat(result.nextPage.nextPage).isEqualTo(4)
        }

    @Test
    fun `getRegionFeedRcmd passes displayId and fromRegion tid`() =
        runTest {
            coEvery { BiliHttpApi.getRegionFeedRcmd(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = RegionFeedRcmd(archives = emptyList()))

            repository.getRegionFeedRcmd(ugcType = UgcTypeV2.Music, page = UgcFeedPage(nextPage = 5))

            coVerify {
                BiliHttpApi.getRegionFeedRcmd(
                    displayId = eq(5),
                    fromRegion = eq(UgcTypeV2.Music.tid),
                )
            }
        }

    @Test
    fun `getRegionFeedRcmd returns hasNext false when archives is empty`() =
        runTest {
            coEvery { BiliHttpApi.getRegionFeedRcmd(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = RegionFeedRcmd(archives = emptyList()))

            val result = repository.getRegionFeedRcmd(ugcType = UgcTypeV2.Douga, page = UgcFeedPage())

            assertThat(result.hasNext).isFalse()
            assertThat(result.items).isEmpty()
        }

    @Test
    fun `getRegionFeedRcmd maps author and stat fields`() =
        runTest {
            val archive =
                RegionFeedRcmd.Archive(
                    aid = 100L, bvid = "BV100", cid = 200L,
                    title = "test", cover = "http://cover.test", duration = 300, pubdate = 1000L,
                    stat = RegionFeedRcmd.Archive.Stat(view = 9999, like = 100, danmaku = 50),
                    author = RegionFeedRcmd.Archive.Author(mid = 555L, name = "up-name"),
                    trackid = "t1", goto = "av", recReason = "hot",
                )
            coEvery { BiliHttpApi.getRegionFeedRcmd(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = RegionFeedRcmd(archives = listOf(archive)))

            val result = repository.getRegionFeedRcmd(ugcType = UgcTypeV2.Douga, page = UgcFeedPage())

            val item = result.items[0]
            assertThat(item.author).isEqualTo("up-name")
            assertThat(item.authorMid).isEqualTo(555L)
            assertThat(item.play).isEqualTo(9999)
            assertThat(item.danmaku).isEqualTo(50)
            assertThat(item.duration).isEqualTo(300)
        }

    // ------------------------------------------------------------------
    // getRegionData (deprecated)
    // ------------------------------------------------------------------

    @Test
    fun `getRegionData returns mapped UgcRegionData with items`() =
        runTest {
            val regionDynamic =
                RegionDynamic(
                    banner = null,
                    card = emptyList(),
                    cBottom = 100L,
                    cTop = 0L,
                    new = listOf(fakeRegionDynamicListItem(param = "1", title = "video-1")),
                )
            coEvery { BiliHttpApi.getRegionDynamic(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = regionDynamic)

            @Suppress("DEPRECATION")
            val result = repository.getRegionData(ugcType = UgcType.Douga)

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].aid).isEqualTo(1L)
            assertThat(result.items[0].title).isEqualTo("video-1")
            assertThat(result.next.nextPage).isEqualTo(100L)
        }

    @Test
    fun `getRegionData returns empty items when new list is empty`() =
        runTest {
            val regionDynamic =
                RegionDynamic(
                    banner = null,
                    card = emptyList(),
                    cBottom = 0L,
                    cTop = 0L,
                    new = emptyList(),
                )
            coEvery { BiliHttpApi.getRegionDynamic(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = regionDynamic)

            @Suppress("DEPRECATION")
            val result = repository.getRegionData(ugcType = UgcType.Game)

            assertThat(result.items).isEmpty()
        }

    @Test
    fun `getRegionData passes rid and accessKey`() =
        runTest {
            coEvery { BiliHttpApi.getRegionDynamic(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RegionDynamic(
                            banner = null,
                            card = emptyList(),
                            cBottom = 0L,
                            cTop = 0L,
                            new = emptyList(),
                        ),
                )
            authRepository.accessToken = "test-token"

            @Suppress("DEPRECATION")
            repository.getRegionData(ugcType = UgcType.Music)

            coVerify { BiliHttpApi.getRegionDynamic(eq(UgcType.Music.rid), eq("test-token")) }
        }

    // ------------------------------------------------------------------
    // getRegionMoreData (deprecated)
    // ------------------------------------------------------------------

    @Test
    fun `getRegionMoreData returns mapped UgcRegionListData with items`() =
        runTest {
            val regionDynamicList =
                RegionDynamicList(
                    cBottom = 200L,
                    cTop = 100L,
                    new = listOf(fakeRegionDynamicListItem(param = "1", title = "video-1")),
                )
            coEvery { BiliHttpApi.getRegionDynamicList(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = regionDynamicList)

            @Suppress("DEPRECATION")
            val result = repository.getRegionMoreData(ugcType = UgcType.Douga)

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].aid).isEqualTo(1L)
            assertThat(result.items[0].title).isEqualTo("video-1")
            assertThat(result.next.nextPage).isEqualTo(200L)
        }

    @Test
    fun `getRegionMoreData returns empty items when new list is empty`() =
        runTest {
            coEvery { BiliHttpApi.getRegionDynamicList(any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RegionDynamicList(
                            cBottom = 0L,
                            cTop = 0L,
                            new = emptyList(),
                        ),
                )

            @Suppress("DEPRECATION")
            val result = repository.getRegionMoreData(ugcType = UgcType.Game)

            assertThat(result.items).isEmpty()
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeRegionDynamicListItem(
        param: String = "1",
        title: String = "title",
    ) = RegionDynamicList.Item(
        cover = "http://cover.test",
        coverLeftIcon1 = 0,
        coverLeftText1 = "",
        duration = 120,
        face = "",
        goto = "av",
        name = "UP",
        param = param,
        pubDate = 1700000000,
        rid = 1,
        rName = "综合",
        title = title,
        uri = "bilibili://video/$param",
    )

    private fun fakeArchive(
        aid: Long = 1L,
        title: String = "title",
    ) = RegionFeedRcmd.Archive(
        aid = aid, bvid = "BV$aid", cid = aid * 10,
        title = title, cover = "http://cover.test/$aid",
        duration = 120, pubdate = 1000L,
        stat = RegionFeedRcmd.Archive.Stat(view = 100, like = 10, danmaku = 5),
        author = RegionFeedRcmd.Archive.Author(mid = 1L, name = "up"),
        trackid = "t$aid", goto = "av", recReason = "",
    )
}
