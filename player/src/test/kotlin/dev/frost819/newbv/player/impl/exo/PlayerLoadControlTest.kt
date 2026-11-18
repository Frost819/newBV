package dev.frost819.newbv.player.impl.exo

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PlayerLoadControl] 的单元测试。
 *
 * 分两类：
 * - 缓冲参数推导（[PlayerLoadControl.policyFor]）：验证字节预算与时间上限随堆规模变化，并夹在合理区间；
 * - 实际加载行为：直接驱动 media3 的 [LoadControl.shouldContinueLoading]，验证时间上限真正生效，
 *   防止有人把参数改回 media3 默认的 50 秒窗 / 137MiB 字节阈值（低内存电视上会吃满堆）。
 */
@OptIn(UnstableApi::class)
class PlayerLoadControlTest {
    private val playerId = PlayerId("test")
    private lateinit var loadControl: LoadControl

    @BeforeEach
    fun setUp() {
        loadControl = PlayerLoadControl.create(heapLimitBytes = HEAP_LIMIT_192_MB)
        loadControl.onPrepared(playerId)
        // 未调用 onTracksSelected 时目标字节数为 0，会被判定为「已达上限」而停止加载，
        // 这里显式提供一次轨道选择结果，让字节阈值取到配置值。
        loadControl.onTracksSelected(
            parameters(bufferedDurationUs = 0),
            TrackGroupArray(),
            emptyArray<ExoTrackSelection>(),
        )
    }

    @Test
    fun `policy reserves one sixth of heap for buffering`() {
        // When: 192MB 堆（日志中 TCL MT9655 实测上限）
        val policy = PlayerLoadControl.policyFor(HEAP_LIMIT_192_MB)

        // Then: 32MiB 预算，其余空间留给图片 / 弹幕 / Compose
        assertThat(policy.targetBufferBytes).isEqualTo(32 * MIB)
    }

    @Test
    fun `policy uses flat max buffer window across heap sizes`() {
        // Then: 时间上限不再按堆分档——真正的内存闸门是字节预算，
        // 时间上限只负责吸收 CDN 的间歇性停顿，因此统一放宽到 60 秒
        assertThat(PlayerLoadControl.policyFor(HEAP_LIMIT_192_MB).maxBufferMs).isEqualTo(60_000)
        assertThat(PlayerLoadControl.policyFor(256L * MIB).maxBufferMs).isEqualTo(60_000)
        assertThat(PlayerLoadControl.policyFor(512L * MIB).maxBufferMs).isEqualTo(60_000)
    }

    @Test
    fun `policy clamps target buffer bytes between floor and cap`() {
        // Then: 极小堆取 32MiB 下限（低于此值连 1080P 的 60 秒窗都放不下），极大堆取 96MiB 上限
        assertThat(PlayerLoadControl.policyFor(48L * MIB).targetBufferBytes).isEqualTo(32 * MIB)
        assertThat(PlayerLoadControl.policyFor(4L * 1024 * MIB).targetBufferBytes).isEqualTo(96 * MIB)
    }

    @Test
    fun `policy keeps media3 buffer duration invariants`() {
        // Given
        val policy = PlayerLoadControl.policyFor(HEAP_LIMIT_192_MB)

        // Then: 单调约束不满足时 DefaultLoadControl.Builder.build 会抛异常
        assertThat(policy.maxBufferMs).isAtLeast(policy.minBufferMs)
        assertThat(policy.minBufferMs).isAtLeast(policy.bufferForPlaybackMs)
        assertThat(policy.minBufferMs).isAtLeast(policy.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun `continues loading below max buffer window`() {
        // When: 缓冲不足最小值 → 继续加载
        val loadingWhenEmpty = loadControl.shouldContinueLoading(parameters(bufferedDurationUs = 1_000_000))

        // When: 缓冲介于最小值与上限之间 → 保持继续加载
        val loadingWhenPartial = loadControl.shouldContinueLoading(parameters(bufferedDurationUs = 10_000_000))

        // Then
        assertThat(loadingWhenEmpty).isTrue()
        assertThat(loadingWhenPartial).isTrue()
    }

    @Test
    fun `stops loading at max buffer window`() {
        // Given: 先进入加载状态（缓冲未达最小值）
        loadControl.shouldContinueLoading(parameters(bufferedDurationUs = 1_000_000))

        // When: 缓冲超过 60 秒上限
        val loadingWhenFull = loadControl.shouldContinueLoading(parameters(bufferedDurationUs = 65_000_000))

        // Then
        assertThat(loadingWhenFull).isFalse()
    }

    @Test
    fun `back buffer is disabled`() {
        // Then: 不回看缓冲，避免高码率流长期持有已播样本
        assertThat(loadControl.getBackBufferDurationUs(playerId)).isEqualTo(0L)
    }

    /** 构造 media3 的加载参数：除缓冲时长外全部取固定值（非直播、1 倍速、未卡顿）。 */
    private fun parameters(bufferedDurationUs: Long) =
        LoadControl.Parameters(
            playerId, // 与 onPrepared 使用同一实例
            Timeline.EMPTY, // 不依赖播放列表内容
            MediaSource.MediaPeriodId("test"), // 当前媒体周期
            0L, // playbackPositionUs
            bufferedDurationUs, // 唯一随用例变化的参数
            1f, // playbackSpeed
            true, // playWhenReady
            false, // rebuffering
            C.TIME_UNSET, // targetLiveOffsetUs（非直播）
            C.TIME_UNSET, // lastRebufferRealtimeMs（无卡顿记录）
        )

    private companion object {
        /** 1MiB 字节数。 */
        const val MIB = 1024 * 1024

        /** 日志中 TCL MT9655 的进程堆上限（192MB）。 */
        const val HEAP_LIMIT_192_MB = 192L * MIB
    }
}
