package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SponsorSkipTargetPolicy] 的单元测试。
 *
 * 验证自动跳转目标钳制：普通片段留 1s 尾部保护（防 Media3 直接进 STATE_ENDED）、
 * 片尾类允许顶到末尾、时长未就绪时原样返回、负值输入钳制为 0。
 */
class SponsorSkipTargetPolicyTest {
    @Test
    fun `clamps ordinary segment end to keep tail guard`() {
        // 片段尾 59.5s，时长 60s：目标钳制到 59s（留 1s 尾部保护）
        val target =
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(
                requestedMs = 59_500L,
                durationMs = 60_000L,
                category = "sponsor",
            )

        assertThat(target).isEqualTo(59_000L)
    }

    @Test
    fun `allows outro to end at video duration`() {
        val target =
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(
                requestedMs = 59_500L,
                durationMs = 60_000L,
                category = "outro",
            )

        assertThat(target).isEqualTo(59_500L)
    }

    @Test
    fun `returns requested value when duration unknown`() {
        assertThat(
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(59_500L, 0L, "sponsor"),
        ).isEqualTo(59_500L)
        assertThat(
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(59_500L, -1L, "sponsor"),
        ).isEqualTo(59_500L)
    }

    @Test
    fun `clamps negative request to zero`() {
        val target =
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(
                requestedMs = -100L,
                durationMs = 60_000L,
                category = "sponsor",
            )

        assertThat(target).isEqualTo(0L)
    }

    @Test
    fun `keeps target when segment end is far from duration`() {
        val target =
            SponsorSkipTargetPolicy.resolveSkipTargetPositionMs(
                requestedMs = 20_000L,
                durationMs = 60_000L,
                category = "intro",
            )

        assertThat(target).isEqualTo(20_000L)
    }
}
