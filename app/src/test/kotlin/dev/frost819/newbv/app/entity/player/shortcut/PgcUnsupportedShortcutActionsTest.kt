package dev.frost819.newbv.app.entity.player.shortcut

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [pgcUnsupportedShortcutActions] 的单元测试。
 *
 * 验证番剧播放时被禁用的路由类快捷键集合：视频详情、UP 主页、相关视频。
 */
class PgcUnsupportedShortcutActionsTest {
    @Test
    fun `contains detail up and related video actions`() {
        assertThat(pgcUnsupportedShortcutActions)
            .containsExactly(
                PlayerCustomShortcutAction.OpenVideoDetail,
                PlayerCustomShortcutAction.OpenUpPage,
                PlayerCustomShortcutAction.OpenRelatedVideos,
            )
    }

    @Test
    fun `does not contain non routing actions`() {
        assertThat(pgcUnsupportedShortcutActions)
            .doesNotContain(PlayerCustomShortcutAction.OpenComments)
        assertThat(pgcUnsupportedShortcutActions)
            .doesNotContain(PlayerCustomShortcutAction.ToggleDanmaku)
        assertThat(pgcUnsupportedShortcutActions)
            .doesNotContain(PlayerCustomShortcutAction.OpenSettings)
    }
}
