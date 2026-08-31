package dev.frost819.newbv.app.entity.player.shortcut

import dev.frost819.newbv.data.datastore.PlaySpeed

/**
 * 自定义快捷键动作目录。
 *
 * 列出所有可绑定的快捷键动作，按分组组织。
 * 用于配置 UI（设置 → 音视频 → 自定义播放快捷键）展示可选项。
 */
object PlayerCustomShortcutCatalog {
    /** 单个动作条目。 */
    data class ActionEntry(
        val action: PlayerCustomShortcutAction,
        val displayName: String,
    )

    /** 动作分组。每个分组包含一组 [ActionEntry]，简单动作只有 1 个条目。 */
    data class ActionGroup(
        val id: String,
        val displayName: String,
        val values: List<ActionEntry>,
    )

    /** 获取所有可绑定的动作分组。 */
    fun groups(): List<ActionGroup> =
        buildList {
            // 简单动作
            add(simple("open_settings", "打开播放器设置菜单", PlayerCustomShortcutAction.OpenSettings))
            add(simple("open_related_videos", "打开相关视频", PlayerCustomShortcutAction.OpenRelatedVideos))
            add(simple("play_previous", "播放上一集", PlayerCustomShortcutAction.PlayPrevious))
            add(simple("play_next", "播放下一集", PlayerCustomShortcutAction.PlayNext))
            add(simple("open_video_detail", "打开视频详情", PlayerCustomShortcutAction.OpenVideoDetail))
            add(simple("open_up_page", "打开 UP 主页", PlayerCustomShortcutAction.OpenUpPage))
            add(simple("open_comments", "打开评论", PlayerCustomShortcutAction.OpenComments))
            add(simple("open_interaction", "打开视频交互", PlayerCustomShortcutAction.OpenInteraction))
            add(simple("toggle_loop", "单视频循环开关", PlayerCustomShortcutAction.ToggleLoop))
            add(simple("toggle_danmaku", "弹幕开关", PlayerCustomShortcutAction.ToggleDanmaku))
            add(simple("toggle_danmaku_mask", "弹幕防遮挡开关", PlayerCustomShortcutAction.ToggleDanmakuMask))
            add(simple("toggle_subtitle", "字幕开关", PlayerCustomShortcutAction.ToggleSubtitle))
            add(
                simple(
                    "toggle_persistent_bottom_progress",
                    "开关底部常驻迷你进度条",
                    PlayerCustomShortcutAction.TogglePersistentBottomProgress,
                ),
            )

            // 参数化动作
            add(
                valueGroup(
                    id = "toggle_playback_speed",
                    displayName = "倍速播放开关",
                    values =
                        PlaySpeed.entries.filter { it.speed != 1f }.map { speed ->
                            PlayerCustomShortcutAction.TogglePlaybackSpeed(speed.speed).entry(
                                displayName = "${speed.speed}x",
                            )
                        },
                ),
            )
        }

    /** 获取动作的显示名称。参数化动作返回分组名（不含参数值）。 */
    fun getActionDisplayName(action: PlayerCustomShortcutAction): String =
        groups()
            .flatMap { group -> group.values.map { ActionEntry(it.action, group.displayName) } }
            .firstOrNull { it.action == action }
            ?.displayName ?: action.javaClass.simpleName

    private fun simple(
        id: String,
        displayName: String,
        action: PlayerCustomShortcutAction,
    ): ActionGroup = ActionGroup(id = id, displayName = displayName, values = listOf(ActionEntry(action, displayName)))

    private fun valueGroup(
        id: String,
        displayName: String,
        values: List<ActionEntry>,
    ): ActionGroup = ActionGroup(id = id, displayName = displayName, values = values)

    private fun PlayerCustomShortcutAction.entry(displayName: String): ActionEntry =
        ActionEntry(action = this, displayName = displayName)
}
