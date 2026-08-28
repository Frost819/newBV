package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [PlayerCustomShortcutsCodec] 和 [PlayerCustomShortcutKeys] 的单元测试。
 *
 * 验证序列化/反序列化的正确性、normalize 的去重和参数 clamp、
 * 禁用键过滤逻辑，以及历史遗留动作的兼容性降级。
 */
class PlayerCustomShortcutsTest {
    @Test
    fun `parse empty string returns empty list`() {
        assertThat(PlayerCustomShortcutsCodec.parse("")).isEmpty()
        assertThat(PlayerCustomShortcutsCodec.parse("   ")).isEmpty()
    }

    @Test
    fun `parse invalid JSON returns empty list`() {
        assertThat(PlayerCustomShortcutsCodec.parse("{invalid}")).isEmpty()
        assertThat(PlayerCustomShortcutsCodec.parse("not json")).isEmpty()
    }

    @Test
    fun `serialize and parse roundtrip preserves all simple actions`() {
        val simpleActions =
            listOf(
                PlayerCustomShortcutAction.OpenSettings,
                PlayerCustomShortcutAction.OpenRelatedVideos,
                PlayerCustomShortcutAction.PlayPrevious,
                PlayerCustomShortcutAction.PlayNext,
                PlayerCustomShortcutAction.OpenVideoDetail,
                PlayerCustomShortcutAction.OpenUpPage,
                PlayerCustomShortcutAction.ToggleLoop,
                PlayerCustomShortcutAction.ToggleDanmaku,
                PlayerCustomShortcutAction.ToggleDanmakuMask,
                PlayerCustomShortcutAction.ToggleSubtitle,
                PlayerCustomShortcutAction.TogglePersistentBottomProgress,
            )
        val shortcuts =
            simpleActions.mapIndexed { i, action ->
                PlayerCustomShortcut(KeyEvent.KEYCODE_1 + i, action)
            }

        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(simpleActions.size)
        parsed.forEachIndexed { i, shortcut ->
            assertThat(shortcut.action).isEqualTo(simpleActions[i])
        }
    }

    @Test
    fun `serialize and parse roundtrip preserves parameterized action`() {
        val shortcuts =
            listOf(
                PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlaybackSpeed(2.0f)),
            )

        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.TogglePlaybackSpeed
        assertThat(action.speed).isEqualTo(2.0f)
    }

    @Test
    fun `normalize deduplicates by keyCode keeping last`() {
        val shortcuts =
            listOf(
                PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleLoop),
                PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleDanmaku),
            )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat(normalized).hasSize(1)
        assertThat(normalized[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `normalize filters forbidden keyCodes`() {
        val shortcuts =
            listOf(
                PlayerCustomShortcut(KeyEvent.KEYCODE_BACK, PlayerCustomShortcutAction.ToggleLoop),
                PlayerCustomShortcut(KeyEvent.KEYCODE_DPAD_CENTER, PlayerCustomShortcutAction.ToggleDanmaku),
                PlayerCustomShortcut(KeyEvent.KEYCODE_ENTER, PlayerCustomShortcutAction.ToggleSubtitle),
                PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleLoop),
            )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat(normalized).hasSize(1)
        assertThat(normalized[0].keyCode).isEqualTo(KeyEvent.KEYCODE_1)
    }

    @Test
    fun `normalize clamps playback speed to valid range`() {
        val shortcuts =
            listOf(
                PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlaybackSpeed(10f)),
                PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.TogglePlaybackSpeed(0.01f)),
            )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.TogglePlaybackSpeed).speed).isEqualTo(4f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.TogglePlaybackSpeed).speed).isEqualTo(0.25f)
    }

    @Test
    fun `normalize empty list returns empty`() {
        assertThat(PlayerCustomShortcutsCodec.normalize(emptyList())).isEmpty()
    }

    @Test
    fun `isAllowedKeyCode returns false for forbidden keys`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_BACK)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_ENTER)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_ESCAPE)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_UNKNOWN)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_BUTTON_B)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_NUMPAD_ENTER)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(0)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(-1)).isFalse()
    }

    @Test
    fun `isAllowedKeyCode returns true for valid keys`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_UP)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_SPACE)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_1)).isTrue()
    }

    @Test
    fun `isCancelKeyCode returns true for back and escape`() {
        assertThat(PlayerCustomShortcutKeys.isCancelKeyCode(KeyEvent.KEYCODE_BACK)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isCancelKeyCode(KeyEvent.KEYCODE_ESCAPE)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isCancelKeyCode(KeyEvent.KEYCODE_BUTTON_B)).isTrue()
        assertThat(PlayerCustomShortcutKeys.isCancelKeyCode(KeyEvent.KEYCODE_DPAD_UP)).isFalse()
    }

    @Test
    fun `getDisplayName returns readable name for common keys`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_DPAD_UP)).isEqualTo("方向上")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_DPAD_DOWN)).isEqualTo("方向下")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_DPAD_LEFT)).isEqualTo("方向左")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_DPAD_RIGHT)).isEqualTo("方向右")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_SPACE)).isEqualTo("空格")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MENU)).isEqualTo("菜单键")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            .isEqualTo("媒体播放/暂停")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PLAY))
            .isEqualTo("媒体播放")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PAUSE))
            .isEqualTo("媒体暂停")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_REWIND))
            .isEqualTo("媒体快退")
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
            .isEqualTo("媒体快进")
    }

    // ── 历史格式与遗留动作兼容 ────────────────────────────────────────

    @Test
    fun `parse old array format still works`() {
        val oldFormat = """[{"k":19,"a":"toggle_danmaku","p":{}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].keyCode).isEqualTo(19)
        assertThat(parsed[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `parse drops legacy removed actions`() {
        val legacy =
            """
            [
                {"k":20,"a":"show_info","p":{}},
                {"k":21,"a":"open_video_list","p":{}},
                {"k":22,"a":"toggle_play_pause","p":{}},
                {"k":23,"a":"set_resolution","p":{"quality_id":80}},
                {"k":24,"a":"set_danmaku_mask_enabled","p":{"enabled":true}},
                {"k":25,"a":"set_subtitle_font_size","p":{"sp":24}}
            ]
            """.trimIndent()

        assertThat(PlayerCustomShortcutsCodec.parse(legacy)).isEmpty()
    }

    @Test
    fun `parse keeps valid entries mixed with legacy removed ones`() {
        val mixed =
            """
            [
                {"k":20,"a":"set_danmaku_mask_enabled","p":{"enabled":true}},
                {"k":21,"a":"toggle_danmaku_mask","p":{}}
            ]
            """.trimIndent()
        val parsed = PlayerCustomShortcutsCodec.parse(mixed)

        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].keyCode).isEqualTo(21)
        assertThat(parsed[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmakuMask)
    }

    @Test
    fun `parse returns empty for unknown action name`() {
        val oldFormat = """[{"k":20,"a":"unknown_action","p":{}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).isEmpty()
    }

    @Test
    fun `parse returns empty for missing required params`() {
        val oldFormat = """[{"k":20,"a":"set_playback_speed","p":{}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).isEmpty()
    }

    // ── Serialize edge cases ─────────────────────────────────────────

    @Test
    fun `serialize empty list produces valid JSON with version`() {
        val serialized = PlayerCustomShortcutsCodec.serialize(emptyList())

        assertThat(serialized).isNotEmpty()
        assertThat(PlayerCustomShortcutsCodec.parse(serialized)).isEmpty()
    }
}
