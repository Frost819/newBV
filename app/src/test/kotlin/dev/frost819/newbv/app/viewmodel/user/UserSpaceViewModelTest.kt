package dev.frost819.newbv.app.viewmodel.user

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.user.SpaceVideo
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoData
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/**
 * [UserSpaceViewModel] 的单元测试。
 *
 * 验证用户投稿视频列表分页加载、错误与超时处理。
 * 用户名和头像由路由参数传入，不调用 getUserInfo API。
 * 使用 MockK mock [UserRepository]，mockkObject mock [Prefs]。
 */
class UserSpaceViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: UserSpaceViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()

        mockkObject(Prefs)
        every { Prefs.apiType } returns DataApiType.Web
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Prefs)
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UserSpaceViewModel = UserSpaceViewModel(userRepository = userRepository)

    private fun fakeVideo(aid: Long) =
        SpaceVideo(
            aid = aid,
            bvid = "BV$aid",
            title = "视频$aid",
            cover = "https://example.com/cover$aid.jpg",
            author = "测试UP",
            duration = 600,
            play = 10000,
            danmaku = 500,
            pubTime = "2024-01-01",
        )

    private fun fakeVideoData(
        videos: List<SpaceVideo> = listOf(fakeVideo(1), fakeVideo(2)),
        hasNext: Boolean = true,
    ) = SpaceVideoData(
        videos = videos,
        page = SpaceVideoPage(hasNext = hasNext),
    )

    // ------------------------------------------------------------------
    // init
    // ------------------------------------------------------------------

    @Test
    fun `init loads videos successfully`() =
        runTest(testDispatcher) {
            val videoData = fakeVideoData(videos = listOf(fakeVideo(1), fakeVideo(2)), hasNext = true)

            coEvery {
                userRepository.getSpaceVideos(
                    mid = 1L,
                    order = SpaceVideoOrder.PubDate,
                    page = any(),
                    preferApiType = BiliApiType.Web,
                )
            } returns videoData

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP", "https://example.com/face.jpg")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.mid).isEqualTo(1L)
            assertThat(state.name).isEqualTo("测试UP")
            assertThat(state.face).isEqualTo("https://example.com/face.jpg")
            assertThat(state.videos).hasSize(2)
            assertThat(state.loading).isFalse()
            assertThat(state.error).isFalse()
            assertThat(state.hasMore).isTrue()
        }

    @Test
    fun `init is idempotent when same mid and videos already loaded`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            coVerify(exactly = 1) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
        }

    @Test
    fun `init reloads when mid changes`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

            viewModel = createViewModel()
            viewModel.init(1L, "UP1")
            advanceUntilIdle()

            viewModel.init(2L, "UP2")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                userRepository.getSpaceVideos(mid = 1L, order = any(), page = any(), preferApiType = any())
            }
            coVerify(exactly = 1) {
                userRepository.getSpaceVideos(mid = 2L, order = any(), page = any(), preferApiType = any())
            }
        }

    // ------------------------------------------------------------------
    // loadVideos pagination
    // ------------------------------------------------------------------

    @Test
    fun `loadVideos loads next page when hasMore is true`() =
        runTest(testDispatcher) {
            val firstPage =
                fakeVideoData(
                    videos = listOf(fakeVideo(1)),
                    hasNext = true,
                )
            val secondPage =
                fakeVideoData(
                    videos = listOf(fakeVideo(2)),
                    hasNext = false,
                )

            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returnsMany
                listOf(
                    firstPage,
                    secondPage,
                )

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.videos).hasSize(1)
            assertThat(viewModel.uiState.value.hasMore).isTrue()

            viewModel.loadVideos(1L)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.videos).hasSize(2)
            assertThat(viewModel.uiState.value.hasMore).isFalse()
        }

    @Test
    fun `loadVideos does not load when hasMore is false`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns
                fakeVideoData(
                    videos = listOf(fakeVideo(1)),
                    hasNext = false,
                )

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            viewModel.loadVideos(1L)
            advanceUntilIdle()

            coVerify(exactly = 1) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
            assertThat(viewModel.uiState.value.videos).hasSize(1)
        }

    @Test
    fun `loadVideos does not load when already loading`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns
                fakeVideoData(
                    videos = listOf(fakeVideo(1)),
                    hasNext = true,
                )

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            // loading is true (set synchronously), don't advance yet
            assertThat(viewModel.uiState.value.loading).isTrue()

            viewModel.loadVideos(1L)
            advanceUntilIdle()

            coVerify(exactly = 1) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
        }

    // ------------------------------------------------------------------
    // refresh
    // ------------------------------------------------------------------

    @Test
    fun `refresh clears videos and reloads`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns
                fakeVideoData(
                    videos = listOf(fakeVideo(1), fakeVideo(2)),
                    hasNext = true,
                )

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.videos).hasSize(2)

            viewModel.refresh(1L)
            advanceUntilIdle()

            coVerify(exactly = 2) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
            assertThat(viewModel.uiState.value.videos).hasSize(2)
            assertThat(viewModel.uiState.value.error).isFalse()
        }

    // ------------------------------------------------------------------
    // error states
    // ------------------------------------------------------------------

    @Test
    fun `loadVideos error sets error`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } throws IOException("network error")

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.error).isTrue()
            assertThat(state.loading).isFalse()
            assertThat(state.videos).isEmpty()
        }

    // ------------------------------------------------------------------
    // timeout handling
    // ------------------------------------------------------------------

    @Test
    fun `loadVideos timeout sets error`() =
        runTest(testDispatcher) {
            coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } coAnswers {
                delay(11_000)
                fakeVideoData()
            }

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.error).isTrue()
            assertThat(state.loading).isFalse()
        }

    // ------------------------------------------------------------------
    // App apiType mapping
    // ------------------------------------------------------------------

    @Test
    fun `uses App apiType when Prefs apiType is App`() =
        runTest(testDispatcher) {
            every { Prefs.apiType } returns DataApiType.App

            coEvery {
                userRepository.getSpaceVideos(
                    mid = any(),
                    order = any(),
                    page = any(),
                    preferApiType = BiliApiType.App,
                )
            } returns fakeVideoData()

            viewModel = createViewModel()
            viewModel.init(1L, "测试UP")
            advanceUntilIdle()

            coVerify {
                userRepository.getSpaceVideos(
                    mid = any(),
                    order = any(),
                    page = any(),
                    preferApiType = BiliApiType.App,
                )
            }
        }
}
