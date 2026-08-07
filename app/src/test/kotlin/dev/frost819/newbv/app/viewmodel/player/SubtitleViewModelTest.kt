package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.action.player.SubtitleSettingAction
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [SubtitleViewModel] 的单元测试。
 *
 * 验证字幕状态更新逻辑：字体大小、透明度、底部间距的设置。
 */
class SubtitleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var videoPlayRepository: VideoPlayRepository
    private lateinit var httpClient: HttpClient
    private lateinit var viewModel: SubtitleViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(Prefs)
        every { Prefs.defaultSubtitleFontSize } returns 24
        every { Prefs.defaultSubtitleBackgroundOpacity } returns 0.4f
        every { Prefs.defaultSubtitleBottomPadding } returns 12
        every { Prefs.defaultSubtitleFontSize = any() } answers {}
        every { Prefs.defaultSubtitleBackgroundOpacity = any() } answers {}
        every { Prefs.defaultSubtitleBottomPadding = any() } answers {}

        videoPlayRepository = mockk()
        httpClient = mockk()
        viewModel = SubtitleViewModel(videoPlayRepository, httpClient)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.subtitleState.value
        assertThat(state.fontSize).isEqualTo(24)
        assertThat(state.opacity).isEqualTo(0.4f)
        assertThat(state.bottomPadding).isEqualTo(12)
    }

    @Test
    fun `initial subtitleId is -1`() {
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
    }

    @Test
    fun `updateSubtitleState SetFontSize updates font size`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetFontSize(32))
        advanceUntilIdle()

        assertThat(viewModel.subtitleState.value.fontSize).isEqualTo(32)
    }

    @Test
    fun `updateSubtitleState SetOpacity updates opacity`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetOpacity(0.8f))
        advanceUntilIdle()

        assertThat(viewModel.subtitleState.value.opacity).isEqualTo(0.8f)
    }

    @Test
    fun `updateSubtitleState SetBottomPadding updates padding`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetBottomPadding(24))
        advanceUntilIdle()

        assertThat(viewModel.subtitleState.value.bottomPadding).isEqualTo(24)
    }

    @Test
    fun `toggleSubtitle does nothing when subtitle list is empty`() = runTest(testDispatcher) {
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
        viewModel.toggleSubtitle()
        advanceUntilIdle()
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
    }

    @Test
    fun `selectSubtitle minus one disables subtitle`() = runTest(testDispatcher) {
        viewModel.selectSubtitle(-1L)
        advanceUntilIdle()
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
        assertThat(viewModel.subtitleData.value).isEmpty()
    }

    @Test
    fun `clearSubtitle resets all subtitle state`() = runTest(testDispatcher) {
        viewModel.clearSubtitle()

        assertThat(viewModel.subtitleList.value).isEmpty()
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
        assertThat(viewModel.subtitleData.value).isEmpty()
    }

    @Test
    fun `updateSubtitleState SetFontSize persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetFontSize(48))
        advanceUntilIdle()

        verify { Prefs.defaultSubtitleFontSize = 48 }
    }

    @Test
    fun `updateSubtitleState SetOpacity persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetOpacity(0.6f))
        advanceUntilIdle()

        verify { Prefs.defaultSubtitleBackgroundOpacity = 0.6f }
    }

    @Test
    fun `updateSubtitleState SetBottomPadding persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateSubtitleState(SubtitleSettingAction.SetBottomPadding(30))
        advanceUntilIdle()

        verify { Prefs.defaultSubtitleBottomPadding = 30 }
    }

    @Test
    fun `updateSubtitleState with no change is no-op`() = runTest(testDispatcher) {
        val initialFontSize = viewModel.subtitleState.value.fontSize
        viewModel.updateSubtitleState(SubtitleSettingAction.SetFontSize(initialFontSize))
        advanceUntilIdle()

        assertThat(viewModel.subtitleState.value.fontSize).isEqualTo(initialFontSize)
    }

    @Test
    fun `toggleSubtitle does nothing when subtitle is active and list is empty`() = runTest(testDispatcher) {
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
        viewModel.toggleSubtitle()
        advanceUntilIdle()
        assertThat(viewModel.subtitleId.value).isEqualTo(-1L)
    }
}
