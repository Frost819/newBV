package dev.frost819.newbv.app.ui.state.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [PlayerUiState.isPgc] 的单元测试。
 *
 * 验证番剧（PGC）判定仅取决于 [PlayerUiState.epid]，
 * 与播放侧 [PlayerUiState.fromSeason] 解耦。
 */
class PlayerUiStateTest {
    @Test
    fun `isPgc is false when epid is null`() {
        assertThat(PlayerUiState(epid = null).isPgc).isFalse()
    }

    @Test
    fun `isPgc is false when epid is zero`() {
        assertThat(PlayerUiState(epid = 0).isPgc).isFalse()
    }

    @Test
    fun `isPgc is true when epid is positive`() {
        assertThat(PlayerUiState(epid = 12345).isPgc).isTrue()
    }

    @Test
    fun `isPgc is independent of fromSeason`() {
        assertThat(PlayerUiState(epid = null, fromSeason = true).isPgc).isFalse()
        assertThat(PlayerUiState(epid = 1, fromSeason = false).isPgc).isTrue()
    }
}
