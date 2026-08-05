package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.action.player.SubtitleSettingAction
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
}
