package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.viewmodel.player.PlaybackStallMonitor.Decision
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PlaybackStallMonitor] 的单元测试。
 *
 * 验证起播 / 拖动 / 播放中卡顿的区分、单次超时与反复卡顿两种换线条件、换线次数上限与重置。
 * 时间由测试直接传入，不依赖真实时钟。
 */
class PlaybackStallMonitorTest {
    private lateinit var monitor: PlaybackStallMonitor

    @BeforeEach
    fun setUp() {
        monitor = PlaybackStallMonitor()
    }

    @Test
    fun `startup buffering is ignored`() {
        // When: 尚未进入播放中（起播 / 换线重播）
        val decision = monitor.onBufferingStarted(nowMs = 0L, wasPlaying = false)

        // Then
        assertThat(decision).isEqualTo(Decision.Ignore)
    }

    @Test
    fun `single stall while playing waits for stall timeout`() {
        val decision = monitor.onBufferingStarted(nowMs = 10_000L, wasPlaying = true)

        assertThat(decision).isEqualTo(Decision.SwitchIfStillStalled(STALL_SWITCH_TIMEOUT_MS))
    }

    @Test
    fun `third stall within window switches immediately`() {
        // Given: 窗口内已有两次播放中卡顿
        monitor.onBufferingStarted(nowMs = 10_000L, wasPlaying = true)
        monitor.onBufferingStarted(nowMs = 20_000L, wasPlaying = true)

        // When
        val decision = monitor.onBufferingStarted(nowMs = 30_000L, wasPlaying = true)

        // Then
        assertThat(decision).isEqualTo(Decision.SwitchNow)
    }

    @Test
    fun `stalls spread beyond window do not accumulate`() {
        monitor.onBufferingStarted(nowMs = 0L, wasPlaying = true)
        monitor.onBufferingStarted(nowMs = 40_000L, wasPlaying = true)

        // 第一次卡顿已滑出 60 秒窗口，窗口内只有两次
        val decision = monitor.onBufferingStarted(nowMs = 70_000L, wasPlaying = true)

        assertThat(decision).isInstanceOf(Decision.SwitchIfStillStalled::class.java)
    }

    @Test
    fun `buffering right after seek is not counted as repeated stall`() {
        // Given: 连续三次拖动，每次拖动都会触发一次缓冲
        repeat(3) { i ->
            val seekAt = 10_000L * (i + 1)
            monitor.onSeek(seekAt)

            // When
            val decision = monitor.onBufferingStarted(nowMs = seekAt + 100L, wasPlaying = true)

            // Then: 不计入反复卡顿，但长时间不恢复仍会换线
            assertThat(decision).isEqualTo(Decision.SwitchIfStillStalled(STALL_SWITCH_TIMEOUT_MS))
        }
    }

    @Test
    fun `buffering after seek grace is counted again`() {
        monitor.onSeek(0L)
        monitor.onBufferingStarted(nowMs = SEEK_GRACE_MS, wasPlaying = true)
        monitor.onBufferingStarted(nowMs = SEEK_GRACE_MS + 1_000L, wasPlaying = true)

        val decision = monitor.onBufferingStarted(nowMs = SEEK_GRACE_MS + 2_000L, wasPlaying = true)

        assertThat(decision).isEqualTo(Decision.SwitchNow)
    }

    @Test
    fun `switching restarts stall counting on the new line`() {
        monitor.onBufferingStarted(nowMs = 1_000L, wasPlaying = true)
        monitor.onBufferingStarted(nowMs = 2_000L, wasPlaying = true)

        // When: 换线后同一窗口内再卡两次
        monitor.onSwitched()
        monitor.onBufferingStarted(nowMs = 3_000L, wasPlaying = true)
        val decision = monitor.onBufferingStarted(nowMs = 4_000L, wasPlaying = true)

        // Then: 旧线路的卡顿不带到新线路
        assertThat(decision).isInstanceOf(Decision.SwitchIfStillStalled::class.java)
    }

    @Test
    fun `stops switching after max switches`() {
        repeat(MAX_STALL_SWITCHES) { monitor.onSwitched() }

        val decision = monitor.onBufferingStarted(nowMs = 1_000L, wasPlaying = true)

        assertThat(decision).isEqualTo(Decision.Ignore)
    }

    @Test
    fun `reset restores switch budget and clears history`() {
        repeat(MAX_STALL_SWITCHES) { monitor.onSwitched() }
        monitor.onSeek(0L)

        // When: 换片 / 换档后重新解析地址
        monitor.reset()

        // Then: 重新获得换线机会，拖动宽限也一并清除
        val decision = monitor.onBufferingStarted(nowMs = 100L, wasPlaying = true)
        assertThat(decision).isEqualTo(Decision.SwitchIfStillStalled(STALL_SWITCH_TIMEOUT_MS))
        monitor.onBufferingStarted(nowMs = 200L, wasPlaying = true)
        assertThat(monitor.onBufferingStarted(nowMs = 300L, wasPlaying = true)).isEqualTo(Decision.SwitchNow)
    }

    @Test
    fun `custom thresholds are respected`() {
        val strict =
            PlaybackStallMonitor(
                stallTimeoutMs = 1_000L,
                repeatWindowMs = 5_000L,
                repeatThreshold = 2,
                seekGraceMs = 500L,
                maxSwitches = 1,
            )

        assertThat(strict.onBufferingStarted(nowMs = 0L, wasPlaying = true))
            .isEqualTo(Decision.SwitchIfStillStalled(1_000L))
        assertThat(strict.onBufferingStarted(nowMs = 1_000L, wasPlaying = true)).isEqualTo(Decision.SwitchNow)

        strict.onSwitched()
        assertThat(strict.onBufferingStarted(nowMs = 2_000L, wasPlaying = true)).isEqualTo(Decision.Ignore)
    }
}
