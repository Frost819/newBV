package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.VideoCodec
import org.junit.jupiter.api.Test

/**
 * [PlayerCustomShortcutCatalog] 的单元测试。
 *
 * 验证动作分组结构、显示名称解析及分组 ID 唯一性。
 */
class PlayerCustomShortcutCatalogTest {

    @Test
    fun `groups returns non-empty list`() {
        val groups = PlayerCustomShortcutCatalog.groups()

        assertThat(groups).isNotEmpty()
    }

    @Test
    fun `groups contains all simple action groups`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val simpleGroupIds = groups.filter { it.action != null }.map { it.id }

        assertThat(simpleGroupIds).contains("show_info")
        assertThat(simpleGroupIds).contains("open_settings")
        assertThat(simpleGroupIds).contains("open_video_list")
        assertThat(simpleGroupIds).contains("open_related_videos")
        assertThat(simpleGroupIds).contains("toggle_play_pause")
        assertThat(simpleGroupIds).contains("play_previous")
        assertThat(simpleGroupIds).contains("play_next")
        assertThat(simpleGroupIds).contains("open_video_detail")
        assertThat(simpleGroupIds).contains("open_up_page")
        assertThat(simpleGroupIds).contains("toggle_loop")
        assertThat(simpleGroupIds).contains("toggle_danmaku")
        assertThat(simpleGroupIds).contains("toggle_subtitle")
        assertThat(simpleGroupIds).contains("toggle_persistent_bottom_progress")
    }

    @Test
    fun `simple action groups have action set and empty values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val simpleGroups = groups.filter { it.action != null }

        simpleGroups.forEach { group ->
            assertThat(group.action).isNotNull()
            assertThat(group.values).isEmpty()
        }
    }

    @Test
    fun `groups contains all value action groups`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val valueGroupIds = groups.filter { it.values.isNotEmpty() }.map { it.id }

        assertThat(valueGroupIds).contains("set_playback_speed")
        assertThat(valueGroupIds).contains("set_resolution")
        assertThat(valueGroupIds).contains("set_audio")
        assertThat(valueGroupIds).contains("set_video_codec")
        assertThat(valueGroupIds).contains("set_aspect_ratio")
        assertThat(valueGroupIds).contains("set_danmaku_scale")
        assertThat(valueGroupIds).contains("set_danmaku_opacity")
        assertThat(valueGroupIds).contains("set_danmaku_speed_factor")
        assertThat(valueGroupIds).contains("set_danmaku_area")
        assertThat(valueGroupIds).contains("set_danmaku_mask_enabled")
        assertThat(valueGroupIds).contains("set_subtitle_font_size")
        assertThat(valueGroupIds).contains("set_subtitle_background_opacity")
        assertThat(valueGroupIds).contains("set_subtitle_bottom_padding")
    }

    @Test
    fun `value action groups have null action and non-empty values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val valueGroups = groups.filter { it.values.isNotEmpty() }

        valueGroups.forEach { group ->
            assertThat(group.action).isNull()
            assertThat(group.values).isNotEmpty()
        }
    }

    @Test
    fun `all group ids are unique`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val ids = groups.map { it.id }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `set_playback_speed group has entries for all PlaySpeed values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val speedGroup = groups.find { it.id == "set_playback_speed" }!!

        assertThat(speedGroup.values).isNotEmpty()
        speedGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.SetPlaybackSpeed::class.java)
        }
    }

    @Test
    fun `set_resolution group has entries for all Resolution values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val resolutionGroup = groups.find { it.id == "set_resolution" }!!

        assertThat(resolutionGroup.values).isNotEmpty()
        resolutionGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.SetResolution::class.java)
        }
    }

    @Test
    fun `set_audio group has entries for all Audio values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val audioGroup = groups.find { it.id == "set_audio" }!!

        assertThat(audioGroup.values).isNotEmpty()
        audioGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.SetAudio::class.java)
        }
    }

    @Test
    fun `set_video_codec group has entries for all VideoCodec values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val codecGroup = groups.find { it.id == "set_video_codec" }!!

        assertThat(codecGroup.values).isNotEmpty()
        codecGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.SetVideoCodec::class.java)
        }
    }

    @Test
    fun `set_aspect_ratio group has entries for all VideoAspectRatio values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val ratioGroup = groups.find { it.id == "set_aspect_ratio" }!!

        assertThat(ratioGroup.values).hasSize(3)
        ratioGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.SetAspectRatio::class.java)
        }
    }

    @Test
    fun `set_danmaku_mask_enabled group has true and false entries`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val maskGroup = groups.find { it.id == "set_danmaku_mask_enabled" }!!

        assertThat(maskGroup.values).hasSize(2)
        val enabledValues = maskGroup.values.map { (it.action as PlayerCustomShortcutAction.SetDanmakuMaskEnabled).enabled }
        assertThat(enabledValues).containsExactly(false, true)
    }

    @Test
    fun `getActionDisplayName returns correct name for simple action`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.TogglePlayPause,
        )

        assertThat(name).isEqualTo("播放/暂停")
    }

    @Test
    fun `getActionDisplayName returns correct name for ToggleDanmaku`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.ToggleDanmaku,
        )

        assertThat(name).isEqualTo("弹幕开关")
    }

    @Test
    fun `getActionDisplayName returns correct name for parameterized action`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetPlaybackSpeed(2.0f),
        )

        assertThat(name).isEqualTo("设置播放速度：2.0x")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetResolution`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetResolution(80),
        )

        assertThat(name).contains("设置分辨率")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetAudio`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetAudio(Audio.A192K),
        )

        assertThat(name).isEqualTo("设置音频编码：A192K")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetVideoCodec`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetVideoCodec(VideoCodec.HEVC),
        )

        assertThat(name).isEqualTo("设置视频编码：HEVC")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetAspectRatio`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetAspectRatio(VideoAspectRatio.FourToThree),
        )

        assertThat(name).isEqualTo("设置画面比例：FourToThree")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetDanmakuMaskEnabled`() {
        val trueName = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetDanmakuMaskEnabled(true),
        )
        val falseName = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetDanmakuMaskEnabled(false),
        )

        assertThat(trueName).isEqualTo("设置弹幕防遮挡：开启")
        assertThat(falseName).isEqualTo("设置弹幕防遮挡：关闭")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetSubtitleFontSize`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetSubtitleFontSize(24),
        )

        assertThat(name).isEqualTo("设置字幕字号：24 SP")
    }

    @Test
    fun `getActionDisplayName returns correct name for SetSubtitleBottomPadding`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetSubtitleBottomPadding(16),
        )

        assertThat(name).isEqualTo("设置字幕底部间距：16 DP")
    }

    @Test
    fun `getActionDisplayName returns correct name for TogglePersistentBottomProgress`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.TogglePersistentBottomProgress,
        )

        assertThat(name).isEqualTo("开关底部常驻迷你进度条")
    }

    @Test
    fun `getActionDisplayName returns class simple name for unrecognized action`() {
        val name = PlayerCustomShortcutCatalog.getActionDisplayName(
            PlayerCustomShortcutAction.SetDanmakuScale(3f),
        )

        assertThat(name).isEqualTo("设置弹幕大小：300%")
    }

    @Test
    fun `ActionEntry valueDisplayName defaults to displayName when not specified`() {
        val entry = PlayerCustomShortcutCatalog.ActionEntry(
            action = PlayerCustomShortcutAction.TogglePlayPause,
            displayName = "播放/暂停",
        )

        assertThat(entry.valueDisplayName).isEqualTo("播放/暂停")
    }
}
