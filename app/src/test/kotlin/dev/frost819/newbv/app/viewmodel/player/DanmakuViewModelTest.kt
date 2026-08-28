package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMeta
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuData
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.danmaku.entity.DanmakuType
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.io.IOException
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.DanmakuType as DataDanmakuType

/**
 * [DanmakuViewModel] 的单元测试。
 *
 * 覆盖两部分：
 * - 弹幕状态更新逻辑：缩放、透明度、区域、速度因子、蒙版开关、类型过滤
 * - 分段加载逻辑：元数据兜底、初始段定位、预取、去重、越界 clamp、
 *   弹幕关闭跳过、失败重试
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
        every { Prefs.defaultDanmakuTypes } returns
            listOf(
                DataDanmakuType.All,
                DataDanmakuType.Rolling,
                DataDanmakuType.Top,
                DataDanmakuType.Bottom,
            )
        every { Prefs.defaultDanmakuScale = any() } answers {}
        every { Prefs.defaultDanmakuOpacity = any() } answers {}
        every { Prefs.defaultDanmakuArea = any() } answers {}
        every { Prefs.defaultDanmakuSpeedFactor = any() } answers {}
        every { Prefs.defaultDanmakuMask = any() } answers {}
        every { Prefs.defaultDanmakuTypes = any() } answers {}
        every { Prefs.apiType } returns DataApiType.Web

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
    fun `updateDanmakuState SetScale updates scale`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(2.5f))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.scale).isEqualTo(2.5f)
        }

    @Test
    fun `updateDanmakuState SetOpacity updates opacity`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetOpacity(0.9f))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.opacity).isEqualTo(0.9f)
        }

    @Test
    fun `updateDanmakuState SetArea updates area`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetArea(0.8f))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.area).isEqualTo(0.8f)
        }

    @Test
    fun `updateDanmakuState SetSpeedFactor updates speedFactor`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetSpeedFactor(1.5f))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.speedFactor).isEqualTo(1.5f)
        }

    @Test
    fun `updateDanmakuState SetMaskEnabled updates maskEnabled`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.maskEnabled).isTrue()

            viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(false))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.maskEnabled).isFalse()
        }

    @Test
    fun `updateDanmakuState SetEnabledTypes updates enabledTypes`() =
        runTest(testDispatcher) {
            val types = listOf(DanmakuType.Rolling, DanmakuType.Top)
            viewModel.updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(types))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.enabledTypes).contains(DanmakuType.Rolling)
            assertThat(viewModel.danmakuState.value.enabledTypes).contains(DanmakuType.Top)
        }

    @Test
    fun `toggleDanmaku clears enabledTypes when they are non-empty`() =
        runTest(testDispatcher) {
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
    fun `toggleDanmaku re-enables default types when enabledTypes is empty`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(emptyList()))
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.enabledTypes).isEmpty()

            viewModel.toggleDanmaku()
            advanceUntilIdle()
            assertThat(viewModel.danmakuState.value.enabledTypes).isNotEmpty()
        }

    @Test
    fun `updateDanmakuState with no change is no-op`() =
        runTest(testDispatcher) {
            val initialScale = viewModel.danmakuState.value.scale
            viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(initialScale))
            advanceUntilIdle()

            assertThat(viewModel.danmakuState.value.scale).isEqualTo(initialScale)
        }

    @Test
    fun `updateDanmakuState SetScale persists to Prefs`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetScale(3.0f))
            advanceUntilIdle()

            verify { Prefs.defaultDanmakuScale = 3.0f }
        }

    @Test
    fun `updateDanmakuState SetOpacity persists to Prefs`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetOpacity(0.5f))
            advanceUntilIdle()

            verify { Prefs.defaultDanmakuOpacity = 0.5f }
        }

    @Test
    fun `updateDanmakuState SetArea persists to Prefs`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetArea(0.9f))
            advanceUntilIdle()

            verify { Prefs.defaultDanmakuArea = 0.9f }
        }

    @Test
    fun `updateDanmakuState SetSpeedFactor persists to Prefs`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetSpeedFactor(2.0f))
            advanceUntilIdle()

            verify { Prefs.defaultDanmakuSpeedFactor = 2.0f }
        }

    @Test
    fun `updateDanmakuState SetMaskEnabled persists to Prefs`() =
        runTest(testDispatcher) {
            viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
            advanceUntilIdle()

            verify { Prefs.defaultDanmakuMask = true }
        }

    @Test
    fun `updateDanmakuState SetEnabledTypes persists to Prefs`() =
        runTest(testDispatcher) {
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

    // === 分段加载 ===

    /** 构造一条 Web 分段接口返回的弹幕数据（time 为秒）。 */
    private fun fakeDanmakuData(
        dmid: Long,
        timeSec: Float,
    ) = DanmakuData(
        time = timeSec,
        type = 1,
        size = 25,
        color = 0xFFFFFF,
        timestamp = 0,
        pool = 0,
        midHash = "hash",
        dmid = dmid,
        level = 0,
        text = "dm-$dmid",
    )

    @Test
    fun `segmentIndexOf uses default 6min segment size`() {
        assertThat(viewModel.segmentIndexOf(0L)).isEqualTo(1)
        assertThat(viewModel.segmentIndexOf(359_999L)).isEqualTo(1)
        assertThat(viewModel.segmentIndexOf(360_000L)).isEqualTo(2)
        assertThat(viewModel.segmentIndexOf(720_001L)).isEqualTo(3)
    }

    @Test
    fun `loadDanmaku fetches initial segment and prefetches next one`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }
            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 2, preferApiType = any())
            }
            // 预取窗口只有 1 段
            coVerify(exactly = 0) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 3, preferApiType = any())
            }
        }

    @Test
    fun `loadDanmaku locates initial segment from initialPositionMs`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 5, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2, initialPositionMs = 720_000)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 3, preferApiType = any())
            }
            coVerify(exactly = 0) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }
        }

    @Test
    fun `loadDanmaku falls back to default segment size when meta fails`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } throws IOException("network down")
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            // 720_000ms 在默认 6 分钟分段下属于第 3 段
            viewModel.loadDanmaku(aid = 1, cid = 2, initialPositionMs = 720_000)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 3, preferApiType = any())
            }
        }

    @Test
    fun `loadDanmaku skips segment loading when danmaku is closed`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, closed = true, 50)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                videoPlayRepository.getDanmakuSegment(any(), any(), any(), any())
            }
        }

    @Test
    fun `segmentTotal clamps requests beyond the end`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 1, false, 10)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()
            // 已加载全部段后，进度推进到下一分段：目标段越界，不发起请求
            viewModel.onProgressChanged(360_000)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 2, preferApiType = any())
            }
        }

    @Test
    fun `onProgressChanged within loaded segment does not refetch`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()
            val callsAfterInit = 2 // 初始段 + 预取段

            // 同段内多次进度更新（10Hz 喂入），不产生新请求
            repeat(5) { viewModel.onProgressChanged(it * 1_000L) }
            advanceUntilIdle()

            coVerify(exactly = callsAfterInit) {
                videoPlayRepository.getDanmakuSegment(any(), any(), any(), any())
            }
        }

    @Test
    fun `onProgressChanged crossing boundary loads next segment`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 5, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            viewModel.onProgressChanged(360_000)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 3, preferApiType = any())
            }
        }

    @Test
    fun `segment fetch retries on failure and succeeds`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } throws
                IOException("flaky") andThen listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            coVerify(exactly = 2) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }
        }

    @Test
    fun `clearDanmaku resets segment state`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            viewModel.clearDanmaku()

            assertThat(viewModel.danmakuMask.value).isNull()
        }

    @Test
    fun `second loadDanmaku discards stale meta result of the first`() =
        runTest(testDispatcher) {
            // 第一次元数据请求慢（挂起），第二次立即返回：第一次的结果必须被丢弃
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } coAnswers
                { throw IOException("stale") } andThen DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2, initialPositionMs = 360_000)
            viewModel.loadDanmaku(aid = 1, cid = 2, initialPositionMs = 0)
            advanceUntilIdle()

            // 第二次加载从第 1 段开始且成功；陈旧结果不会覆盖分段状态
            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }
        }
}
