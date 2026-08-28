package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.data.datastore.DanmakuType as DataDanmakuType
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.danmaku.entity.DanmakuType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
 * [DanmakuViewModel] 的单元测试。
 *
 * 验证弹幕状态更新逻辑：缩放、透明度、区域、速度因子、蒙版开关、类型过滤。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DanmakuViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var videoPlayRepository: VideoPlayRepository
    private lateinit var viewModel: DanmakuViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.d(any(), any(), any()) } returns 0
        mockkObject(Prefs)
        every { Prefs.defaultDanmakuScale } returns 1.75f
        every { Prefs.defaultDanmakuOpacity } returns 0.7f
        every { Prefs.defaultDanmakuArea } returns 0.5f
        every { Prefs.defaultDanmakuSpeedFactor } returns 1f
        every { Prefs.defaultDanmakuMask } returns false
        every { Prefs.defaultDanmakuTypes } returns listOf(
            DataDanmakuType.All, DataDanmakuType.Rolling, DataDanmakuType.Top, DataDanmakuType.Bottom,
        )
        every { Prefs.defaultDanmakuScale = any() } answers {}
        every { Prefs.defaultDanmakuOpacity = any() } answers {}
        every { Prefs.defaultDanmakuArea = any() } answers {}
        every { Prefs.defaultDanmakuSpeedFactor = any() } answers {}
        every { Prefs.defaultDanmakuMask = any() } answers {}
        every { Prefs.defaultDanmakuTypes = any() } answers {}

        videoPlayRepository = mockk()
        viewModel = DanmakuViewModel(videoPlayRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial danmakuState has default values`() {
        val state = viewModel.danmakuState.value
        assertThat(state.scale).isEqualTo(1.75f)
        assertThat(state.opacity).isEqualTo(0.7f)
        assertThat(state.area).isEqualTo(0.5f)
        assertThat(state.speedFactor).isEqualTo(1f)
        assertThat(state.maskEnabled).isFalse()
        assertThat(state.enabledTypes).isNotEmpty()
    }

    @Test
    fun `updateDanmakuState SetScale updates scale`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(2.5f))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.scale).isEqualTo(2.5f)
    }

    @Test
    fun `updateDanmakuState SetOpacity updates opacity`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetOpacity(0.9f))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.opacity).isEqualTo(0.9f)
    }

    @Test
    fun `updateDanmakuState SetArea updates area`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetArea(0.8f))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.area).isEqualTo(0.8f)
    }

    @Test
    fun `updateDanmakuState SetSpeedFactor updates speedFactor`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetSpeedFactor(1.5f))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.speedFactor).isEqualTo(1.5f)
    }

    @Test
    fun `updateDanmakuState SetMaskEnabled updates maskEnabled`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.maskEnabled).isTrue()

        viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(false))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.maskEnabled).isFalse()
    }

    @Test
    fun `updateDanmakuState SetEnabledTypes updates enabledTypes`() = runTest(testDispatcher) {
        val types = listOf(DanmakuType.Rolling, DanmakuType.Top)
        viewModel.updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(types))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.enabledTypes).contains(DanmakuType.Rolling)
        assertThat(viewModel.danmakuState.value.enabledTypes).contains(DanmakuType.Top)
    }

    @Test
    fun `toggleDanmaku clears enabledTypes when they are non-empty`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(
            DanmakuSettingAction.SetEnabledTypes(listOf(DanmakuType.Rolling, DanmakuType.Top, DanmakuType.Bottom)),
        )
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.enabledTypes).isNotEmpty()

        viewModel.toggleDanmaku()
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.enabledTypes).isEmpty()
    }

    @Test
    fun `toggleDanmaku re-enables default types when enabledTypes is empty`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(emptyList()))
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.enabledTypes).isEmpty()

        viewModel.toggleDanmaku()
        advanceUntilIdle()
        assertThat(viewModel.danmakuState.value.enabledTypes).isNotEmpty()
    }

    @Test
    fun `updateDanmakuState with no change is no-op`() = runTest(testDispatcher) {
        val initialScale = viewModel.danmakuState.value.scale
        viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(initialScale))
        advanceUntilIdle()

        assertThat(viewModel.danmakuState.value.scale).isEqualTo(initialScale)
    }

    @Test
    fun `updateDanmakuState SetScale persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(3.0f))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuScale = 3.0f }
    }

    @Test
    fun `updateDanmakuState SetOpacity persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetOpacity(0.5f))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuOpacity = 0.5f }
    }

    @Test
    fun `updateDanmakuState SetArea persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetArea(0.9f))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuArea = 0.9f }
    }

    @Test
    fun `updateDanmakuState SetSpeedFactor persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetSpeedFactor(2.0f))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuSpeedFactor = 2.0f }
    }

    @Test
    fun `updateDanmakuState SetMaskEnabled persists to Prefs`() = runTest(testDispatcher) {
        viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuMask = true }
    }

    @Test
    fun `updateDanmakuState SetEnabledTypes persists to Prefs`() = runTest(testDispatcher) {
        val types = listOf(DanmakuType.Rolling, DanmakuType.Top)
        viewModel.updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(types))
        advanceUntilIdle()

        verify { Prefs.defaultDanmakuTypes = any() }
    }

    @Test
    fun `danmakuMask initial value is null`() {
        assertThat(viewModel.danmakuMask.value).isNull()
    }

    @Test
    fun `danmakuPlayer initial value is null`() {
        assertThat(viewModel.danmakuPlayer).isNull()
    }
}
