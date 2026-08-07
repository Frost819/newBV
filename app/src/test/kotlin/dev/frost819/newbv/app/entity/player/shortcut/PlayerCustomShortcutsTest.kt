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

    // ── All parameterized actions roundtrip ──────────────────────────

    @Test
    fun `serialize and parse roundtrip for SetDanmakuScale`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuScale(1.5f)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuScale
        assertThat(action.scale).isEqualTo(1.5f)
    }

    @Test
    fun `serialize and parse roundtrip for SetDanmakuOpacity`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuOpacity(0.5f)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuOpacity
        assertThat(action.opacity).isEqualTo(0.5f)
    }

    @Test
    fun `serialize and parse roundtrip for SetDanmakuSpeedFactor`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuSpeedFactor(1.0f)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuSpeedFactor
        assertThat(action.factor).isEqualTo(1.0f)
    }

    @Test
    fun `serialize and parse roundtrip for SetDanmakuArea`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuArea(0.75f)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuArea
        assertThat(action.area).isEqualTo(0.75f)
    }

    @Test
    fun `serialize and parse roundtrip for SetDanmakuMaskEnabled`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuMaskEnabled(true)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuMaskEnabled
        assertThat(action.enabled).isTrue()
    }

    @Test
    fun `serialize and parse roundtrip for SetSubtitleFontSize`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleFontSize(24)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetSubtitleFontSize
        assertThat(action.sp).isEqualTo(24)
    }

    @Test
    fun `serialize and parse roundtrip for SetSubtitleBackgroundOpacity`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(0.4f)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity
        assertThat(action.opacity).isEqualTo(0.4f)
    }

    @Test
    fun `serialize and parse roundtrip for SetSubtitleBottomPadding`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleBottomPadding(16)),
        )
        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetSubtitleBottomPadding
        assertThat(action.dp).isEqualTo(16)
    }

    @Test
    fun `serialize and parse roundtrip for all simple actions`() {
        val simpleActions = listOf(
            PlayerCustomShortcutAction.ShowInfo,
            PlayerCustomShortcutAction.OpenSettings,
            PlayerCustomShortcutAction.OpenVideoList,
            PlayerCustomShortcutAction.OpenRelatedVideos,
            PlayerCustomShortcutAction.PlayPrevious,
            PlayerCustomShortcutAction.PlayNext,
            PlayerCustomShortcutAction.OpenVideoDetail,
            PlayerCustomShortcutAction.OpenUpPage,
            PlayerCustomShortcutAction.ToggleLoop,
            PlayerCustomShortcutAction.TogglePersistentBottomProgress,
        )
        val shortcuts = simpleActions.mapIndexed { i, action ->
            PlayerCustomShortcut(KeyEvent.KEYCODE_1 + i, action)
        }

        val serialized = PlayerCustomShortcutsCodec.serialize(shortcuts)
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).hasSize(simpleActions.size)
        parsed.forEachIndexed { i, shortcut ->
            assertThat(shortcut.action).isEqualTo(simpleActions[i])
        }
    }

    // ── Normalize clamps ─────────────────────────────────────────────

    @Test
    fun `normalize clamps danmaku scale to 0_5 to 4`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuScale(10f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetDanmakuScale(0.01f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetDanmakuScale).scale).isEqualTo(4f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetDanmakuScale).scale).isEqualTo(0.5f)
    }

    @Test
    fun `normalize clamps danmaku speed factor to 0_5 to 1_5`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuSpeedFactor(5f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetDanmakuSpeedFactor(0.1f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetDanmakuSpeedFactor).factor).isEqualTo(1.5f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetDanmakuSpeedFactor).factor).isEqualTo(0.5f)
    }

    @Test
    fun `normalize clamps danmaku area to 0 to 1`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetDanmakuArea(5f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetDanmakuArea(-1f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetDanmakuArea).area).isEqualTo(1f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetDanmakuArea).area).isEqualTo(0f)
    }

    @Test
    fun `normalize clamps subtitle background opacity to 0 to 1`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(5f)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(-1f)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity).opacity).isEqualTo(1f)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity).opacity).isEqualTo(0f)
    }

    @Test
    fun `normalize clamps subtitle bottom padding to 0 to 48`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.SetSubtitleBottomPadding(100)),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.SetSubtitleBottomPadding(-10)),
        )

        val normalized = PlayerCustomShortcutsCodec.normalize(shortcuts)

        assertThat((normalized[0].action as PlayerCustomShortcutAction.SetSubtitleBottomPadding).dp).isEqualTo(48)
        assertThat((normalized[1].action as PlayerCustomShortcutAction.SetSubtitleBottomPadding).dp).isEqualTo(0)
    }

    @Test
    fun `normalize empty list returns empty`() {
        assertThat(PlayerCustomShortcutsCodec.normalize(emptyList())).isEmpty()
    }

    // ── Alias action names ───────────────────────────────────────────

    @Test
    fun `parse handles set_audio_id alias`() {
        val oldFormat = """[{"k":20,"a":"set_audio_id","p":{"audio_id":30280}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetAudio
        assertThat(action.audio).isEqualTo(Audio.A192K)
    }

    @Test
    fun `parse handles set_codec alias`() {
        val oldFormat = """[{"k":20,"a":"set_codec","p":{"codec":"HEVC"}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetVideoCodec
        assertThat(action.codec).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `parse handles set_danmaku_text_size alias`() {
        val oldFormat = """[{"k":20,"a":"set_danmaku_text_size","p":{"size":1.5}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuScale
        assertThat(action.scale).isEqualTo(1.5f)
    }

    @Test
    fun `parse handles set_danmaku_speed alias`() {
        val oldFormat = """[{"k":20,"a":"set_danmaku_speed","p":{"speed":1.0}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetDanmakuSpeedFactor
        assertThat(action.factor).isEqualTo(1.0f)
    }

    @Test
    fun `parse handles set_subtitle_text_size alias`() {
        val oldFormat = """[{"k":20,"a":"set_subtitle_text_size","p":{"size":24}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetSubtitleFontSize
        assertThat(action.sp).isEqualTo(24)
    }

    @Test
    fun `parse handles set_subtitle_bottom_padding alias with padding key`() {
        val oldFormat = """[{"k":20,"a":"set_subtitle_bottom_padding","p":{"padding":16}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).hasSize(1)
        val action = parsed[0].action as PlayerCustomShortcutAction.SetSubtitleBottomPadding
        assertThat(action.dp).isEqualTo(16)
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

    @Test
    fun `parse returns empty for invalid audio code`() {
        val oldFormat = """[{"k":20,"a":"set_audio","p":{"audio":99999}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).isEmpty()
    }

    @Test
    fun `parse returns empty for invalid codec name`() {
        val oldFormat = """[{"k":20,"a":"set_video_codec","p":{"codec":"INVALID"}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).isEmpty()
    }

    @Test
    fun `parse returns empty for invalid aspect ratio name`() {
        val oldFormat = """[{"k":20,"a":"set_aspect_ratio","p":{"aspect_ratio":"INVALID"}}]"""
        val parsed = PlayerCustomShortcutsCodec.parse(oldFormat)

        assertThat(parsed).isEmpty()
    }

    // ── Serialize edge cases ─────────────────────────────────────────

    @Test
    fun `serialize empty list produces valid JSON with version`() {
        val serialized = PlayerCustomShortcutsCodec.serialize(emptyList())
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).isEmpty()
        assertThat(serialized).isNotEmpty()
    }

    @Test
    fun `serialize and parse empty roundtrip`() {
        val serialized = PlayerCustomShortcutsCodec.serialize(emptyList())
        val parsed = PlayerCustomShortcutsCodec.parse(serialized)

        assertThat(parsed).isEmpty()
    }

    // ── getDisplayName for media keys ────────────────────────────────

    @Test
    fun `getDisplayName returns media play pause name`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            .isEqualTo("媒体播放/暂停")
    }

    @Test
    fun `getDisplayName returns media play name`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PLAY))
            .isEqualTo("媒体播放")
    }

    @Test
    fun `getDisplayName returns media pause name`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_PAUSE))
            .isEqualTo("媒体暂停")
    }

    @Test
    fun `getDisplayName returns media rewind name`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_REWIND))
            .isEqualTo("媒体快退")
    }

    @Test
    fun `getDisplayName returns media fast forward name`() {
        assertThat(PlayerCustomShortcutKeys.getDisplayName(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
            .isEqualTo("媒体快进")
    }

    // ── percentText ──────────────────────────────────────────────────

    @Test
    fun `percentText converts 0 to 0 percent`() {
        assertThat(0f.percentText()).isEqualTo("0%")
    }

    @Test
    fun `percentText converts 0_5 to 50 percent`() {
        assertThat(0.5f.percentText()).isEqualTo("50%")
    }

    @Test
    fun `percentText converts 1 to 100 percent`() {
        assertThat(1f.percentText()).isEqualTo("100%")
    }

    @Test
    fun `percentText converts 0_25 to 25 percent`() {
        assertThat(0.25f.percentText()).isEqualTo("25%")
    }

    @Test
    fun `percentText converts 0_85 to 85 percent`() {
        assertThat(0.85f.percentText()).isEqualTo("85%")
    }

    // ── Forbidden key codes ──────────────────────────────────────────

    @Test
    fun `isAllowedKeyCode returns false for KEYCODE_UNKNOWN`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_UNKNOWN)).isFalse()
    }

    @Test
    fun `isAllowedKeyCode returns false for KEYCODE_BUTTON_B`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_BUTTON_B)).isFalse()
    }

    @Test
    fun `isAllowedKeyCode returns false for KEYCODE_NUMPAD_ENTER`() {
        assertThat(PlayerCustomShortcutKeys.isAllowedKeyCode(KeyEvent.KEYCODE_NUMPAD_ENTER)).isFalse()
    }
}
