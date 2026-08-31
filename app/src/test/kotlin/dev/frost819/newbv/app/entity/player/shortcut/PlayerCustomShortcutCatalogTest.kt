package dev.frost819.newbv.app.entity.player.shortcut

import com.google.common.truth.Truth.assertThat
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
        val simpleGroupIds = groups.filter { it.values.size == 1 }.map { it.id }

        assertThat(simpleGroupIds).containsExactly(
            "open_settings",
            "open_related_videos",
            "play_previous",
            "play_next",
            "open_video_detail",
            "open_up_page",
            "toggle_loop",
            "toggle_danmaku",
            "toggle_danmaku_mask",
            "toggle_subtitle",
            "toggle_persistent_bottom_progress",
        )
    }

    @Test
    fun `simple action groups have exactly one value`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val simpleGroups = groups.filter { it.values.size == 1 }

        simpleGroups.forEach { group ->
            assertThat(group.values).hasSize(1)
        }
    }

    @Test
    fun `toggle_playback_speed is the only parameterized action group`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val paramGroupIds = groups.filter { it.values.size > 1 }.map { it.id }

        assertThat(paramGroupIds).containsExactly("toggle_playback_speed")
    }

    @Test
    fun `parameterized action groups have multiple values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val paramGroups = groups.filter { it.values.size > 1 }

        paramGroups.forEach { group ->
            assertThat(group.values.size).isGreaterThan(1)
        }
    }

    @Test
    fun `all group ids are unique`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val ids = groups.map { it.id }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `toggle_playback_speed group excludes normal speed`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val speedGroup = groups.find { it.id == "toggle_playback_speed" }!!

        assertThat(speedGroup.values).isNotEmpty()
        assertThat(speedGroup.values.map { it.displayName }).doesNotContain("1.0x")
        speedGroup.values.forEach { entry ->
            assertThat(entry.action).isInstanceOf(PlayerCustomShortcutAction.TogglePlaybackSpeed::class.java)
        }
    }

    @Test
    fun `getActionDisplayName returns correct name for OpenSettings`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.OpenSettings,
            )

        assertThat(name).isEqualTo("打开播放器设置菜单")
    }

    @Test
    fun `getActionDisplayName returns correct name for ToggleDanmaku`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.ToggleDanmaku,
            )

        assertThat(name).isEqualTo("弹幕开关")
    }

    @Test
    fun `getActionDisplayName returns correct name for ToggleDanmakuMask`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.ToggleDanmakuMask,
            )

        assertThat(name).isEqualTo("弹幕防遮挡开关")
    }

    @Test
    fun `getActionDisplayName returns correct name for parameterized action`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.TogglePlaybackSpeed(2.0f),
            )

        assertThat(name).isEqualTo("倍速播放开关")
    }

    @Test
    fun `getActionDisplayName returns correct name for TogglePersistentBottomProgress`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.TogglePersistentBottomProgress,
            )

        assertThat(name).isEqualTo("开关底部常驻迷你进度条")
    }

    @Test
    fun `getActionDisplayName returns class simple name for unrecognized action`() {
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.ToggleLoop,
            )

        assertThat(name).isEqualTo("单视频循环开关")
    }
}
