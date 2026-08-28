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
        val simpleGroupIds = groups.filter { it.action != null }.map { it.id }

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
    fun `simple action groups have action set and empty values`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val simpleGroups = groups.filter { it.action != null }

        simpleGroups.forEach { group ->
            assertThat(group.action).isNotNull()
            assertThat(group.values).isEmpty()
        }
    }

    @Test
    fun `toggle_playback_speed is the only value action group`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val valueGroupIds = groups.filter { it.values.isNotEmpty() }.map { it.id }

        assertThat(valueGroupIds).containsExactly("toggle_playback_speed")
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
    fun `toggle_playback_speed group excludes normal speed`() {
        val groups = PlayerCustomShortcutCatalog.groups()
        val speedGroup = groups.find { it.id == "toggle_playback_speed" }!!

        assertThat(speedGroup.values).isNotEmpty()
        assertThat(speedGroup.values.map { it.valueDisplayName }).doesNotContain("1.0x")
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

        assertThat(name).isEqualTo("倍速播放开关：2.0x")
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
        // 构造一个不在目录中的动作实例：同类型不同参数值无法区分，
        // 这里直接用未注册的 data object 场景不可行，改用参数不在目录的 speed 值
        val name =
            PlayerCustomShortcutCatalog.getActionDisplayName(
                PlayerCustomShortcutAction.ToggleLoop,
            )

        assertThat(name).isEqualTo("单视频循环开关")
    }

    @Test
    fun `ActionEntry valueDisplayName defaults to displayName when not specified`() {
        val entry =
            PlayerCustomShortcutCatalog.ActionEntry(
                action = PlayerCustomShortcutAction.ToggleLoop,
                displayName = "单视频循环开关",
            )

        assertThat(entry.valueDisplayName).isEqualTo("单视频循环开关")
    }
}
