package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.VideoCodec
import org.junit.jupiter.api.Test

/**
 * [PlayerCustomShortcutsCodec] 和 [PlayerCustomShortcutKeys] 的单元测试。
 *
 * 验证序列化/反序列化的正确性、normalize 的去重和参数 clamp、
 * 禁用键过滤逻辑。
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
    fun `serialize and parse roundtrip preserves simple actions`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_DPAD_UP, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_DPAD_DOWN, PlayerCustomShortcutAction.ToggleDanmaku),
            PlayerCustomShortcut(KeyEvent.KEYCODE_SPACE, PlayerCustomShortcutAction.ToggleSubtitle),
        )

        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(3)
        assertThat(parsed[0].keyCode).isEqualTo(KeyEvent.KEYCODE_DPAD_UP)
        assertThat(parsed[0].action).isEqualTo(PlayerCustomShortcutAction.TogglePlayPause)
        assertThat(parsed[1].keyCode).isEqualTo(KeyEvent.KEYCODE_DPAD_DOWN)
        assertThat(parsed[1].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `serialize and parse roundtrip preserves parameterized actions`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetPlaybackSpeed(2.0f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetResolution(80)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_3, PlayerCustomShortcutAction.SetAudio(Audio.A192K)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_4, PlayerCustomShortcutAction.SetVideoCodec(VideoCodec.HEVC)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_5, PlayerCustomShortcutAction.SetAspectRatio(VideoAspectRatio.FourToThree)),
        )

        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(5)
        assertThat(parsed[0].action).isInstanceOf(PlayerCustomShortcutAction.SetPlaybackSpeed::class.java)
        assertThat((parsed[0].action as PlayerCustomShortcutAction.SetPlaybackSpeed).speed).isEqualTo(2.0f)
        assertThat(parsed[1].action).isInstanceOf(PlayerCustomShortcutAction.SetResolution::class.java)
        assertThat((parsed[1].action as PlayerCustomShortcutAction.SetResolution).qualityId).isEqualTo(80)
        assertThat(parsed[2].action).isInstanceOf(PlayerCustomShortcutAction.SetAudio::class.java)
        assertThat((parsed[2].action as PlayerCustomShortcutAction.SetAudio).audio).isEqualTo(Audio.A192K)
        assertThat(parsed[3].action).isInstanceOf(PlayerCustomShortcutAction.SetVideoCodec::class.java)
        assertThat((parsed[3].action as PlayerCustomShortcutAction.SetVideoCodec).codec).isEqualTo(VideoCodec.HEVC)
        assertThat(parsed[4].action).isInstanceOf(PlayerCustomShortcutAction.SetAspectRatio::class.java)
        assertThat((parsed[4].action as PlayerCustomShortcutAction.SetAspectRatio).aspectRatio).isEqualTo(VideoAspectRatio.FourToThree)
    }

    @Test
    fun `normalize deduplicates by keyCode keeping last`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleDanmaku),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat(normalized).hasSize(1)
        assertThat(normalized[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `normalize filters forbidden keyCodes`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_BACK, PlayerCustomShortcutAction.TogglePlayPause),
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
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetPlaybackSpeed(10f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetPlaybackSpeed(0.01f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetPlaybackSpeed).speed).isEqualTo(4f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetPlaybackSpeed).speed).isEqualTo(0.25f)
    }

    @Test
    fun `normalize clamps danmaku opacity to 0-1`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuOpacity(5f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetDanmakuOpacity(-1f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetDanmakuOpacity).opacity).isEqualTo(1f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetDanmakuOpacity).opacity).isEqualTo(0f)
    }

    @Test
    fun `normalize clamps subtitle font size to 12-48`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleFontSize(100)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetSubtitleFontSize(0)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetSubtitleFontSize).sp).isEqualTo(48)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetSubtitleFontSize).sp).isEqualTo(12)
    }

    @Test
    fun `normalize filters invalid resolution codes`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetResolution(99999)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat(normalized).isEmpty()
    }

    @Test
    fun `isAllowedKeyCode returns false for forbidden keys`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_BACK)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_ENTER)).isFalse()
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_ESCAPE)).isFalse()
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
    }

    @Test
    fun `parse old array format still works`() {
        val oldFormat = """[{"k":19,"a":"toggle_play_pause","p":{}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].keyCode).isEqualTo(19)
        assertThat(parsed[0].action).isEqualTo(PlayerCustomShortcutAction.TogglePlayPause)
    }

    @Test
    fun `parse handles alias action names`() {
        val oldFormat = """[{"k":20,"a":"set_resolution_qn","p":{"qn":80}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].action).isInstanceOf(PlayerCustomShortcutAction.SetResolution::class.java)
        assertThat((parsed[0].action as PlayerCustomShortcutAction.SetResolution).qualityId).isEqualTo(80)
    }
}
