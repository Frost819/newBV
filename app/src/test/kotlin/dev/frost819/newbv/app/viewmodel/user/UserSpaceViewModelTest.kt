package dev.frost819.newbv.app.viewmodel.user

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.biliapi.entity.user.SpaceVideo
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoData
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.entity.user.UserSpaceInfo
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import app.cash.turbine.test
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

/**
 * [UserSpaceViewModel] 的单元测试。
 *
 * 验证用户信息加载、视频列表分页、关注/取关操作、错误与超时处理。
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

    private fun createViewModel(): UserSpaceViewModel =
        UserSpaceViewModel(userRepository = userRepository)

    private fun fakeUserInfo(
        mid: Long = 1L,
        isFollowed: Boolean = false,
    ) = UserSpaceInfo(
        mid = mid,
        name = "测试UP$mid",
        face = "https://example.com/face$mid.jpg",
        sign = "这是签名",
        level = 5,
        topPhoto = "https://example.com/top.jpg",
        isFollowed = isFollowed,
    )

    private fun fakeVideo(aid: Long) = SpaceVideo(
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
    fun `init loads user info and videos successfully`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        val videoData = fakeVideoData(videos = listOf(fakeVideo(1), fakeVideo(2)), hasNext = true)

        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery {
            userRepository.getSpaceVideos(
                mid = 1L,
                order = SpaceVideoOrder.PubDate,
                page = any(),
                preferApiType = BiliApiType.Web,
            )
        } returns videoData

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.mid).isEqualTo(1L)
        assertThat(state.userInfo).isNotNull()
        assertThat(state.userInfo?.name).isEqualTo("测试UP1")
        assertThat(state.isFollowing).isFalse()
        assertThat(state.videos).hasSize(2)
        assertThat(state.loading).isFalse()
        assertThat(state.error).isFalse()
        assertThat(state.userInfoLoading).isFalse()
        assertThat(state.userInfoError).isFalse()
        assertThat(state.hasMore).isTrue()
    }

    @Test
    fun `init sets isFollowing true when user is followed`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = true)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFollowing).isTrue()
    }

    @Test
    fun `init is idempotent when same mid and userInfo already loaded`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(1L) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.init(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.getUserInfo(1L) }
        coVerify(exactly = 1) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
    }

    @Test
    fun `init reloads when mid changes`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo(mid = 2L)
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.init(2L)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.getUserInfo(1L) }
        coVerify(exactly = 1) { userRepository.getUserInfo(2L) }
    }

    // ------------------------------------------------------------------
    // loadVideos pagination
    // ------------------------------------------------------------------

    @Test
    fun `loadVideos loads next page when hasMore is true`() = runTest(testDispatcher) {
        val firstPage = fakeVideoData(
            videos = listOf(fakeVideo(1)),
            hasNext = true,
        )
        val secondPage = fakeVideoData(
            videos = listOf(fakeVideo(2)),
            hasNext = false,
        )

        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returnsMany listOf(
            firstPage,
            secondPage,
        )

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.videos).hasSize(1)
        assertThat(viewModel.uiState.value.hasMore).isTrue()

        viewModel.loadVideos(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.videos).hasSize(2)
        assertThat(viewModel.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `loadVideos does not load when hasMore is false`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData(
            videos = listOf(fakeVideo(1)),
            hasNext = false,
        )

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.loadVideos(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
        assertThat(viewModel.uiState.value.videos).hasSize(1)
    }

    @Test
    fun `loadVideos does not load when already loading`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData(
            videos = listOf(fakeVideo(1)),
            hasNext = true,
        )

        viewModel = createViewModel()
        viewModel.init(1L)
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
    fun `refresh clears videos and reloads`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(1L) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData(
            videos = listOf(fakeVideo(1), fakeVideo(2)),
            hasNext = true,
        )

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.videos).hasSize(2)

        viewModel.refresh(1L)
        advanceUntilIdle()

        coVerify(exactly = 2) { userRepository.getUserInfo(1L) }
        coVerify(exactly = 2) { userRepository.getSpaceVideos(any(), any(), any(), any()) }
        assertThat(viewModel.uiState.value.videos).hasSize(2)
        assertThat(viewModel.uiState.value.error).isFalse()
        assertThat(viewModel.uiState.value.userInfoError).isFalse()
    }

    // ------------------------------------------------------------------
    // toggleFollow
    // ------------------------------------------------------------------

    @Test
    fun `toggleFollow follows user when not following`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.followUser(1L, BiliApiType.Web) } returns true

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFollowing).isFalse()

        viewModel.effect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isFollowing).isTrue()
            assertThat(viewModel.uiState.value.followLoading).isFalse()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(UserSpaceUiEffect.ShowToast::class.java)
            assertThat((effect as UserSpaceUiEffect.ShowToast).message).isEqualTo("关注成功")
        }
    }

    @Test
    fun `toggleFollow unfollows user when following`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = true)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.unfollowUser(1L, BiliApiType.Web) } returns true

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isFollowing).isTrue()

        viewModel.effect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isFollowing).isFalse()
            assertThat(viewModel.uiState.value.followLoading).isFalse()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(UserSpaceUiEffect.ShowToast::class.java)
            assertThat((effect as UserSpaceUiEffect.ShowToast).message).isEqualTo("已取消关注")
        }
    }

    @Test
    fun `toggleFollow emits failure toast when repository returns false`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.followUser(1L, BiliApiType.Web) } returns false

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isFollowing).isFalse()
            assertThat(viewModel.uiState.value.followLoading).isFalse()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(UserSpaceUiEffect.ShowToast::class.java)
            assertThat((effect as UserSpaceUiEffect.ShowToast).message).isEqualTo("操作失败")
        }
    }

    @Test
    fun `toggleFollow emits failure toast on exception`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.followUser(1L, BiliApiType.Web) } throws IOException("network error")

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isFollowing).isFalse()
            assertThat(viewModel.uiState.value.followLoading).isFalse()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(UserSpaceUiEffect.ShowToast::class.java)
            assertThat((effect as UserSpaceUiEffect.ShowToast).message).isEqualTo("操作失败")
        }
    }

    @Test
    fun `toggleFollow does nothing when mid is zero`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        viewModel.toggleFollow()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.followUser(any(), any()) }
        coVerify(exactly = 0) { userRepository.unfollowUser(any(), any()) }
    }

    @Test
    fun `toggleFollow does nothing when followLoading is true`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.followUser(1L, BiliApiType.Web) } coAnswers {
            delay(5_000)
            true
        }

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.toggleFollow()
        // followLoading is true, don't advance yet
        assertThat(viewModel.uiState.value.followLoading).isTrue()

        viewModel.toggleFollow()
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.followUser(1L, BiliApiType.Web) }
    }

    // ------------------------------------------------------------------
    // error states
    // ------------------------------------------------------------------

    @Test
    fun `loadUserInfo error sets userInfoError`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(1L) } throws IOException("network error")
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.userInfoError).isTrue()
        assertThat(state.userInfoLoading).isFalse()
        assertThat(state.userInfo).isNull()
    }

    @Test
    fun `loadVideos error sets error`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(1L) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } throws IOException("network error")

        viewModel = createViewModel()
        viewModel.init(1L)
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
    fun `loadUserInfo timeout sets userInfoError`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(any()) } coAnswers {
            delay(11_000)
            fakeUserInfo()
        }
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.userInfoError).isTrue()
        assertThat(state.userInfoLoading).isFalse()
    }

    @Test
    fun `loadVideos timeout sets error`() = runTest(testDispatcher) {
        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo()
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } coAnswers {
            delay(11_000)
            fakeVideoData()
        }

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isTrue()
        assertThat(state.loading).isFalse()
    }

    @Test
    fun `toggleFollow timeout emits failure toast`() = runTest(testDispatcher) {
        val info = fakeUserInfo(mid = 1L, isFollowed = false)
        coEvery { userRepository.getUserInfo(1L) } returns info
        coEvery { userRepository.getSpaceVideos(any(), any(), any(), any()) } returns fakeVideoData()
        coEvery { userRepository.followUser(1L, BiliApiType.Web) } coAnswers {
            delay(11_000)
            true
        }

        viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.toggleFollow()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isFollowing).isFalse()
            assertThat(viewModel.uiState.value.followLoading).isFalse()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(UserSpaceUiEffect.ShowToast::class.java)
            assertThat((effect as UserSpaceUiEffect.ShowToast).message).isEqualTo("操作失败")
        }
    }

    // ------------------------------------------------------------------
    // App apiType mapping
    // ------------------------------------------------------------------

    @Test
    fun `uses App apiType when Prefs apiType is App`() = runTest(testDispatcher) {
        every { Prefs.apiType } returns DataApiType.App

        coEvery { userRepository.getUserInfo(any()) } returns fakeUserInfo()
        coEvery {
            userRepository.getSpaceVideos(
                mid = any(),
                order = any(),
                page = any(),
                preferApiType = BiliApiType.App,
            )
        } returns fakeVideoData()

        viewModel = createViewModel()
        viewModel.init(1L)
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
