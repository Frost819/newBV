package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.biliapi.entity.danmaku.BiliDanmakuFilterConfig
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMeta
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuData
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuFilterRule
import dev.frost819.newbv.biliapi.repositories.AuthRepository
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
    private lateinit var authRepository: AuthRepository
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
        every { Prefs.defaultDanmakuBlockColorful } returns false
        every { Prefs.defaultDanmakuBlockRepeat } returns false
        every { Prefs.defaultDanmakuBlockSpecial } returns false
        every { Prefs.defaultDanmakuBlockByUserFilter } returns true
        every { Prefs.defaultDanmakuAiCloudLevel } returns 0
        every { Prefs.danmakuBiliFilterSynced } returns false
        every { Prefs.danmakuBiliFilterSyncedMid } returns 0L
        every { Prefs.defaultDanmakuScale = any() } answers {}
        every { Prefs.defaultDanmakuOpacity = any() } answers {}
        every { Prefs.defaultDanmakuArea = any() } answers {}
        every { Prefs.defaultDanmakuSpeedFactor = any() } answers {}
        every { Prefs.defaultDanmakuMask = any() } answers {}
        every { Prefs.defaultDanmakuTypes = any() } answers {}
        every { Prefs.defaultDanmakuBlockColorful = any() } answers {}
        every { Prefs.defaultDanmakuBlockRepeat = any() } answers {}
        every { Prefs.defaultDanmakuBlockSpecial = any() } answers {}
        every { Prefs.defaultDanmakuBlockByUserFilter = any() } answers {}
        every { Prefs.defaultDanmakuAiCloudLevel = any() } answers {}
        every { Prefs.danmakuBiliFilterSynced = any() } answers {}
        every { Prefs.danmakuBiliFilterSyncedMid = any() } answers {}
        every { Prefs.apiType } returns DataApiType.Web

        videoPlayRepository = mockk()
        // loadDanmaku/prepareForLive 都会拉取云端屏蔽规则；默认空规则，个案测试再覆盖
        coEvery { videoPlayRepository.getDanmakuFilterRules() } returns emptyList()
        authRepository = AuthRepository()
        authRepository.mid = 12345L
        viewModel = DanmakuViewModel(videoPlayRepository, authRepository)
        // 分段过滤/合并默认跑在 Dispatchers.Default，测试需换到测试调度器才能被 advanceUntilIdle 驱动
        viewModel.filterDispatcher = testDispatcher
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

    // === 蒙版按需加载 ===

    @Test
    fun `loadDanmakuMask skips request when mask disabled`() =
        runTest(testDispatcher) {
            stubDanmakuLoading()

            // 防遮挡默认关闭：进入播放器/切集不应发起蒙版请求
            viewModel.loadDanmaku(aid = 1L, cid = 2L)
            viewModel.loadDanmakuMask()
            advanceUntilIdle()

            coVerify(exactly = 0) { videoPlayRepository.getDanmakuMask(any(), any(), any()) }
            assertThat(viewModel.danmakuMask.value).isNull()
        }

    @Test
    fun `loadDanmakuMask requests mask when mask enabled`() =
        runTest(testDispatcher) {
            every { Prefs.defaultDanmakuMask } returns true
            stubDanmakuLoading()
            coEvery { videoPlayRepository.getDanmakuMask(any(), any(), any()) } returns null
            val enabledViewModel = DanmakuViewModel(videoPlayRepository, authRepository)
            enabledViewModel.filterDispatcher = testDispatcher

            enabledViewModel.loadDanmaku(aid = 1L, cid = 2L)
            enabledViewModel.loadDanmakuMask()
            advanceUntilIdle()

            coVerify(exactly = 1) { videoPlayRepository.getDanmakuMask(1L, 2L, any()) }
        }

    @Test
    fun `enabling mask after video loaded loads mask lazily`() =
        runTest(testDispatcher) {
            stubDanmakuLoading()
            coEvery { videoPlayRepository.getDanmakuMask(any(), any(), any()) } returns null
            // 关闭状态下加载视频+蒙版：不请求
            viewModel.loadDanmaku(aid = 11L, cid = 22L)
            viewModel.loadDanmakuMask()
            advanceUntilIdle()

            viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
            advanceUntilIdle()

            coVerify(exactly = 1) { videoPlayRepository.getDanmakuMask(11L, 22L, any()) }
        }

    @Test
    fun `enabling mask without video does not request`() =
        runTest(testDispatcher) {
            // 直播等场景从不调用 loadDanmaku，打开开关也不应发起请求
            viewModel.updateDanmakuState(DanmakuSettingAction.SetMaskEnabled(true))
            advanceUntilIdle()

            coVerify(exactly = 0) { videoPlayRepository.getDanmakuMask(any(), any(), any()) }
        }

    /** 为 [DanmakuViewModel.loadDanmaku] 提供元数据与分段桩，避免无关的 MockK 未桩化异常。 */
    private fun stubDanmakuLoading() {
        coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
            DanmakuMeta(360_000, 3, false, 100)
        coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `danmakuPlayer initial value is null`() {
        assertThat(viewModel.danmakuPlayer).isNull()
    }

    // === 播放倍速同步 ===

    @Test
    fun `updateSpeed applies speed to engine`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        viewModel.danmakuPlayer = player

        viewModel.updateSpeed(2f)

        verify(exactly = 1) { player.updatePlaySpeed(2f) }
    }

    @Test
    fun `updateSpeed before engine init does not throw`() {
        // 回归 #298：起播默认倍速与手动切换都需同步到弹幕引擎；引擎未创建时忽略即可
        viewModel.updateSpeed(2f)

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
            viewModel.onVideoPositionChanged(360_000)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 2, preferApiType = any())
            }
        }

    @Test
    fun `onVideoPositionChanged within loaded segment does not refetch`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()
            val callsAfterInit = 2 // 初始段 + 预取段

            // 同段内多次进度更新（10Hz 喂入），不产生新请求
            repeat(5) { viewModel.onVideoPositionChanged(it * 1_000L) }
            advanceUntilIdle()

            coVerify(exactly = callsAfterInit) {
                videoPlayRepository.getDanmakuSegment(any(), any(), any(), any())
            }
        }

    @Test
    fun `onVideoPositionChanged crossing boundary loads next segment`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns DanmakuMeta(360_000, 5, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            viewModel.onVideoPositionChanged(360_000)
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

    // === 引擎时钟对齐 ===

    @Test
    fun `onVideoPositionChanged seeks engine when position jumps beyond threshold`() {
        // 回归：断点续播/切集使视频位置跳变（如恢复到 20 分钟）而引擎时钟仍在 0，
        // 引擎按自身时钟渲染会长时间无弹幕，喂入位置时必须自动对齐引擎
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.getCurrentTimeMs() } returns 0L
        viewModel.danmakuPlayer = player

        viewModel.onVideoPositionChanged(1_200_000)

        verify(exactly = 1) { player.seekTo(1_200_000) }
    }

    @Test
    fun `onVideoPositionChanged does not seek engine within threshold`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.getCurrentTimeMs() } returns 1_000_000L
        viewModel.danmakuPlayer = player

        // 正常播放的采样抖动（< 500ms）不应触发引擎 seek
        viewModel.onVideoPositionChanged(1_000_300)

        verify(exactly = 0) { player.seekTo(any()) }
    }

    @Test
    fun `onVideoPositionChanged is safe before engine is initialized`() {
        // 引擎尚未 init（danmakuPlayer 为 null）时喂入位置不应抛异常
        viewModel.onVideoPositionChanged(1_200_000)
    }

    @Test
    fun `onVideoPositionChanged keeps engine paused after jump when not playing`() {
        // DanmakuPlayer.seekTo 会内部解暂停；暂停/缓冲期间喂入跳变位置后必须还原暂停态，
        // 否则弹幕会在视频暂停时继续滚动
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.getCurrentTimeMs() } returns 0L
        viewModel.danmakuPlayer = player

        viewModel.onVideoPositionChanged(1_200_000)

        verify { player.seekTo(1_200_000) }
        verify { player.pause() }
    }

    @Test
    fun `onVideoPositionChanged after jump does not pause engine when playing`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.getCurrentTimeMs() } returns 0L
        viewModel.danmakuPlayer = player
        viewModel.play()

        viewModel.onVideoPositionChanged(1_200_000)

        verify { player.seekTo(1_200_000) }
        verify(exactly = 0) { player.pause() }
    }

    // === AI 云屏蔽阈值 ===

    @Test
    fun `shouldDropByAi drops when score below raw server level`() {
        // 服务端等级（1-10）直接作为阈值：weight 低于等级即丢弃
        // 实测官方端"强"档 aiLevel=3 → 阈值 7，weight 6 丢弃、7 保留
        val score1 = DanmakuData(0f, 1, 25, 0xFFFFFF, 0, 0, "h", 1, 1, "text")
        assertThat(viewModel.run { score1.shouldDropByAi(1) }).isFalse()
        assertThat(viewModel.run { score1.shouldDropByAi(2) }).isTrue()

        val score6 = DanmakuData(0f, 1, 25, 0xFFFFFF, 0, 0, "h", 1, 6, "text")
        assertThat(viewModel.run { score6.shouldDropByAi(7) }).isTrue()

        val score7 = DanmakuData(0f, 1, 25, 0xFFFFFF, 0, 0, "h", 1, 7, "text")
        assertThat(viewModel.run { score7.shouldDropByAi(7) }).isFalse()

        // 实测 weight 可达 11，阈值 10 时保留
        val score11 = DanmakuData(0f, 1, 25, 0xFFFFFF, 0, 0, "h", 1, 11, "text")
        assertThat(viewModel.run { score11.shouldDropByAi(10) }).isFalse()
    }

    @Test
    fun `shouldDropByAi returns false when no score available`() {
        val data = DanmakuData(0f, 1, 25, 0xFFFFFF, 0, 0, "h", 1, 0, "text")
        assertThat(viewModel.run { data.shouldDropByAi(3) }).isFalse()
    }

    // === 重复弹幕合并 ===

    @Test
    fun `mergeDuplicates groups by text and marks merged type`() {
        val nowSec = (System.currentTimeMillis() / 1000).toInt()
        val list =
            listOf(
                DanmakuData(1f, 1, 25, 0xFFFFFF, nowSec, 0, "h1", 1, 0, "重复内容"),
                DanmakuData(2f, 1, 25, 0xFFFFFF, nowSec, 0, "h2", 2, 0, "重复内容"),
                DanmakuData(3f, 1, 25, 0xFFFFFF, nowSec, 0, "h3", 3, 0, "重复内容"),
                DanmakuData(4f, 1, 25, 0xFFFFFF, nowSec, 0, "h4", 4, 0, "正常弹幕"),
            )

        val merged = viewModel.run { list.mergeDuplicates() }

        // 3 条重复弹幕：1 条 MERGED + 2 条 ORIGINAL；1 条正常弹幕：NORMAL
        assertThat(merged).hasSize(4)
        val mergedItems = merged.filter { it.mergedType == DanmakuItemData.MERGED_TYPE_MERGED }
        val originalItems = merged.filter { it.mergedType == DanmakuItemData.MERGED_TYPE_ORIGINAL }
        val normalItems = merged.filter { it.mergedType == DanmakuItemData.MERGED_TYPE_NORMAL }
        assertThat(mergedItems).hasSize(1)
        assertThat(originalItems).hasSize(2)
        assertThat(normalItems).hasSize(1)
        assertThat(mergedItems[0].content).isEqualTo("重复内容 ×3")
    }

    @Test
    fun `mergeDuplicates does not merge across time windows`() {
        val nowSec = (System.currentTimeMillis() / 1000).toInt()
        val list =
            listOf(
                DanmakuData(1f, 1, 25, 0xFFFFFF, nowSec, 0, "h1", 1, 0, "重复内容"),
                DanmakuData(7f, 1, 25, 0xFFFFFF, nowSec, 0, "h2", 2, 0, "重复内容"),
            )

        val merged = viewModel.run { list.mergeDuplicates() }

        // time 差 6 秒超出 5 秒窗口，不合并
        assertThat(merged).hasSize(2)
        assertThat(merged.all { it.mergedType == DanmakuItemData.MERGED_TYPE_NORMAL }).isTrue()
    }

    // === B 站过滤配置同步门控 ===

    @Test
    fun `applyBiliFilterConfig skips when hasValidConfig is false`() =
        runTest(testDispatcher) {
            // 未登录时 dm/view 不返回 playerConfig/flag，config 为默认值
            val defaultConfig = BiliDanmakuFilterConfig.DEFAULT

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, defaultConfig)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            // 不应置位 synced
            verify(exactly = 0) { Prefs.danmakuBiliFilterSynced = true }
        }

    @Test
    fun `applyBiliFilterConfig syncs when hasValidConfig is true`() =
        runTest(testDispatcher) {
            val config =
                BiliDanmakuFilterConfig(
                    blockTop = true,
                    blockColor = true,
                    aiSwitch = true,
                    aiLevel = 3,
                    hasValidConfig = true,
                    hasPlayerConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            // 应置位 synced 并记录 mid
            verify { Prefs.danmakuBiliFilterSynced = true }
            verify { Prefs.danmakuBiliFilterSyncedMid = 12345L }
        }

    @Test
    fun `applyBiliFilterConfig skips when already synced for same mid`() =
        runTest(testDispatcher) {
            every { Prefs.danmakuBiliFilterSynced } returns true
            every { Prefs.danmakuBiliFilterSyncedMid } returns 12345L

            val config =
                BiliDanmakuFilterConfig(
                    blockColor = true,
                    hasValidConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            // 已同步过同 mid，不再重复同步
            verify(exactly = 0) { Prefs.danmakuBiliFilterSynced = true }
        }

    @Test
    fun `applyBiliFilterConfig re-syncs when mid changes`() =
        runTest(testDispatcher) {
            every { Prefs.danmakuBiliFilterSynced } returns true
            every { Prefs.danmakuBiliFilterSyncedMid } returns 99999L // 不同 mid

            val config =
                BiliDanmakuFilterConfig(
                    blockColor = true,
                    hasValidConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            // mid 变化，重新同步
            verify { Prefs.danmakuBiliFilterSyncedMid = 12345L }
        }

    @Test
    fun `applyBiliFilterConfig keeps all types when server block flags are true`() =
        runTest(testDispatcher) {
            // 真机实测：官方端未开启屏蔽时服务端仍下发 block*=true，此时不应清空类型
            val config =
                BiliDanmakuFilterConfig(
                    blockTop = true,
                    blockScroll = true,
                    blockBottom = true,
                    blockColor = true,
                    blockSpecial = true,
                    hasValidConfig = true,
                    hasPlayerConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            val state = viewModel.danmakuState.value
            assertThat(state.enabledTypes).isNotEmpty()
            assertThat(state.blockColorful).isFalse()
            assertThat(state.blockSpecial).isFalse()
        }

    @Test
    fun `applyBiliFilterConfig removes type only when server block flag is false`() =
        runTest(testDispatcher) {
            val config =
                BiliDanmakuFilterConfig(
                    blockTop = true,
                    blockScroll = false,
                    blockBottom = true,
                    hasValidConfig = true,
                    hasPlayerConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            val state = viewModel.danmakuState.value
            assertThat(state.enabledTypes).doesNotContain(DanmakuType.Rolling)
            assertThat(state.enabledTypes).contains(DanmakuType.Top)
        }

    @Test
    fun `applyBiliFilterConfig stores raw server ai level as threshold`() =
        runTest(testDispatcher) {
            val config =
                BiliDanmakuFilterConfig(
                    aiSwitch = true,
                    aiLevel = 3,
                    hasValidConfig = true,
                    hasPlayerConfig = true,
                )

            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100, config)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            // 服务端等级直接作为丢弃阈值，不再映射五档预设（避免弱档映射后零效果）
            assertThat(viewModel.danmakuState.value.aiCloudLevel).isEqualTo(3)
        }

    // === 直播云端屏蔽规则链路 ===

    @Test
    fun `prepareForLive loads cloud rules and drops live danmaku by keyword`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuFilterRules() } returns
                listOf(
                    DanmakuFilterRule(id = 1, mid = 12345L, type = 0, filter = "广告", opened = true),
                    DanmakuFilterRule(id = 2, mid = 12345L, type = 0, filter = "已停用", opened = false),
                )

            viewModel.prepareForLive()
            advanceUntilIdle()

            assertThat(viewModel.shouldDropLiveDanmaku("这是一条广告弹幕", mid = 1L)).isTrue()
            assertThat(viewModel.shouldDropLiveDanmaku("正常弹幕", mid = 1L)).isFalse()
            // 停用规则不生效
            assertThat(viewModel.shouldDropLiveDanmaku("已停用词", mid = 1L)).isFalse()
            // 仅启用规则计入菜单展示条数
            assertThat(viewModel.danmakuState.value.userFilterRuleCount).isEqualTo(1)
        }

    @Test
    fun `prepareForLive drops live danmaku by blocked user uid`() =
        runTest(testDispatcher) {
            // 用户规则（type=2）下发原始 UID 时按 crc32 归一化，与 WS mid 现算哈希匹配
            coEvery { videoPlayRepository.getDanmakuFilterRules() } returns
                listOf(DanmakuFilterRule(id = 3, mid = 12345L, type = 2, filter = "54321", opened = true))

            viewModel.prepareForLive()
            advanceUntilIdle()

            assertThat(viewModel.shouldDropLiveDanmaku("任意内容", mid = 54321L)).isTrue()
            assertThat(viewModel.shouldDropLiveDanmaku("任意内容", mid = 99999L)).isFalse()
        }

    @Test
    fun `user filter load failure degrades and retries on next entry`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuFilterRules() } throws IOException("network down")

            viewModel.prepareForLive()
            advanceUntilIdle()

            // 失败降级：不过滤，也不置位 loaded
            assertThat(viewModel.shouldDropLiveDanmaku("广告", mid = 1L)).isFalse()

            coEvery { videoPlayRepository.getDanmakuFilterRules() } returns
                listOf(DanmakuFilterRule(id = 4, mid = 12345L, type = 0, filter = "广告", opened = true))

            // 再次进入直播间时重试成功
            viewModel.prepareForLive()
            advanceUntilIdle()

            assertThat(viewModel.shouldDropLiveDanmaku("这是广告", mid = 1L)).isTrue()
            coVerify(exactly = 2) { videoPlayRepository.getDanmakuFilterRules() }
        }

    @Test
    fun `blockByUserFilter off skips live filtering`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuFilterRules() } returns
                listOf(DanmakuFilterRule(id = 5, mid = 12345L, type = 0, filter = "广告", opened = true))
            viewModel.prepareForLive()
            advanceUntilIdle()

            viewModel.updateDanmakuState(DanmakuSettingAction.SetBlockByUserFilter(false))
            advanceUntilIdle()

            assertThat(viewModel.shouldDropLiveDanmaku("这是广告", mid = 1L)).isFalse()
        }

    // === 未登录与重复弹幕热切换 ===

    @Test
    fun `unlogged user skips cloud filter request`() =
        runTest(testDispatcher) {
            // 未登录时接口必返回 -101，不应发起无效请求
            authRepository.mid = null
            stubDanmakuLoading()

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()

            coVerify(exactly = 0) { videoPlayRepository.getDanmakuFilterRules() }
            assertThat(viewModel.danmakuState.value.userFilterRuleCount).isEqualTo(0)
        }

    @Test
    fun `toggle blockRepeat does not refetch segments`() =
        runTest(testDispatcher) {
            coEvery { videoPlayRepository.getDanmakuMeta(any(), any()) } returns
                DanmakuMeta(360_000, 3, false, 100)
            coEvery { videoPlayRepository.getDanmakuSegment(any(), any(), any(), any()) } returns
                listOf(fakeDanmakuData(1, 0f))
            viewModel.danmakuPlayer = mockk(relaxed = true)

            viewModel.loadDanmaku(aid = 1, cid = 2)
            advanceUntilIdle()
            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }

            // 合并项/原件随分段始终成对生成，切换开关靠 DuplicateMergedFilter 热切换，无需重新下载
            viewModel.updateDanmakuState(DanmakuSettingAction.SetBlockRepeat(true))
            advanceUntilIdle()

            coVerify(exactly = 1) {
                videoPlayRepository.getDanmakuSegment(aid = 1, cid = 2, segmentIndex = 1, preferApiType = any())
            }
        }
}
