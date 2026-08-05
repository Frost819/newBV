package dev.frost819.newbv.app.entity.player.shortcut

import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.danmaku.entity.DanmakuSpeedFactor

/**
 * 自定义快捷键动作目录。
 *
 * 列出所有可绑定的快捷键动作，按分组组织。
 * 用于配置 UI（P1-9 设置页）展示可选项。
 */
object PlayerCustomShortcutCatalog {

    /** 单个动作条目。 */
    data class ActionEntry(
        val action: PlayerCustomShortcutAction,
        val displayName: String,
        val valueDisplayName: String = displayName,
    )

    /** 动作分组。简单动作只有 [action]，参数化动作有 [values]。 */
    data class ActionGroup(
        val id: String,
        val displayName: String,
        val action: PlayerCustomShortcutAction? = null,
        val values: List<ActionEntry> = emptyList(),
    )

    /** 获取所有可绑定的动作分组。 */
    fun groups(): List<ActionGroup> = buildList {
        // 简单动作
        add(simple("show_info", "呼出播放信息层", PlayerCustomShortcutAction.ShowInfo))
        add(simple("open_settings", "打开播放器设置菜单", PlayerCustomShortcutAction.OpenSettings))
        add(simple("open_video_list", "打开视频列表", PlayerCustomShortcutAction.OpenVideoList))
        add(simple("open_related_videos", "打开相关视频", PlayerCustomShortcutAction.OpenRelatedVideos))
        add(simple("toggle_play_pause", "播放/暂停", PlayerCustomShortcutAction.TogglePlayPause))
        add(simple("play_previous", "上一个", PlayerCustomShortcutAction.PlayPrevious))
        add(simple("play_next", "下一个", PlayerCustomShortcutAction.PlayNext))
        add(simple("open_video_detail", "打开视频详情", PlayerCustomShortcutAction.OpenVideoDetail))
        add(simple("open_up_page", "打开 UP 主页", PlayerCustomShortcutAction.OpenUpPage))
        add(simple("toggle_loop", "单视频循环开关", PlayerCustomShortcutAction.ToggleLoop))
        add(simple("toggle_danmaku", "弹幕开关", PlayerCustomShortcutAction.ToggleDanmaku))
        add(simple("toggle_subtitle", "字幕开关", PlayerCustomShortcutAction.ToggleSubtitle))

        // 参数化动作
        add(
            valueGroup(
                id = "set_playback_speed",
                displayName = "设置播放速度",
                values = PlaySpeed.entries.map { speed ->
                    PlayerCustomShortcutAction.SetPlaybackSpeed(speed.speed).entry(
                        displayName = "设置播放速度：${speed.speed}x",
                        valueDisplayName = "${speed.speed}x",
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_resolution",
                displayName = "设置分辨率",
                values = Resolution.entries.map { resolution ->
                    PlayerCustomShortcutAction.SetResolution(resolution.code).entry(
                        displayName = "设置分辨率：${resolution.name}",
                        valueDisplayName = resolution.name,
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_audio",
                displayName = "设置音频编码",
                values = Audio.entries.map { audio ->
                    PlayerCustomShortcutAction.SetAudio(audio).entry(
                        displayName = "设置音频编码：${audio.name}",
                        valueDisplayName = audio.name,
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_video_codec",
                displayName = "设置视频编码",
                values = VideoCodec.entries.map { codec ->
                    PlayerCustomShortcutAction.SetVideoCodec(codec).entry(
                        displayName = "设置视频编码：${codec.name}",
                        valueDisplayName = codec.name,
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_aspect_ratio",
                displayName = "设置画面比例",
                values = VideoAspectRatio.entries.map { ratio ->
                    PlayerCustomShortcutAction.SetAspectRatio(ratio).entry(
                        displayName = "设置画面比例：${ratio.name}",
                        valueDisplayName = ratio.name,
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_danmaku_scale",
                displayName = "设置弹幕大小",
                values = listOf(0.5f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f).map { scale ->
                    PlayerCustomShortcutAction.SetDanmakuScale(scale).entry(
                        displayName = "设置弹幕大小：${scale.percentText()}",
                        valueDisplayName = scale.percentText(),
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_danmaku_opacity",
                displayName = "设置弹幕透明度",
                values = listOf(0f, 0.25f, 0.5f, 0.7f, 0.85f, 1f).map { opacity ->
                    PlayerCustomShortcutAction.SetDanmakuOpacity(opacity).entry(
                        displayName = "设置弹幕透明度：${opacity.percentText()}",
                        valueDisplayName = opacity.percentText(),
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_danmaku_speed_factor",
                displayName = "设置弹幕速度",
                values = DanmakuSpeedFactor.entries.map { factor ->
                    PlayerCustomShortcutAction.SetDanmakuSpeedFactor(factor.factor).entry(
                        displayName = "设置弹幕速度：${factor.factor}x",
                        valueDisplayName = "${factor.factor}x",
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_danmaku_area",
                displayName = "设置弹幕区域",
                values = listOf(0.25f, 0.5f, 0.75f, 1f).map { area ->
                    PlayerCustomShortcutAction.SetDanmakuArea(area).entry(
                        displayName = "设置弹幕区域：${area.percentText()}",
                        valueDisplayName = area.percentText(),
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_danmaku_mask_enabled",
                displayName = "设置弹幕防遮挡",
                values = listOf(false, true).map { enabled ->
                    PlayerCustomShortcutAction.SetDanmakuMaskEnabled(enabled).entry(
                        displayName = "设置弹幕防遮挡：${if (enabled) "开启" else "关闭"}",
                        valueDisplayName = if (enabled) "开启" else "关闭",
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_subtitle_font_size",
                displayName = "设置字幕字号",
                values = listOf(12, 16, 20, 24, 32, 40, 48).map { fontSize ->
                    PlayerCustomShortcutAction.SetSubtitleFontSize(fontSize).entry(
                        displayName = "设置字幕字号：$fontSize SP",
                        valueDisplayName = "$fontSize SP",
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_subtitle_background_opacity",
                displayName = "设置字幕背景透明度",
                values = listOf(0f, 0.25f, 0.4f, 0.5f, 0.75f, 1f).map { opacity ->
                    PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(opacity).entry(
                        displayName = "设置字幕背景透明度：${opacity.percentText()}",
                        valueDisplayName = opacity.percentText(),
                    )
                },
            ),
        )

        add(
            valueGroup(
                id = "set_subtitle_bottom_padding",
                displayName = "设置字幕底部间距",
                values = listOf(0, 8, 12, 16, 24, 32, 48).map { padding ->
                    PlayerCustomShortcutAction.SetSubtitleBottomPadding(padding).entry(
                        displayName = "设置字幕底部间距：$padding DP",
                        valueDisplayName = "$padding DP",
                    )
                },
            ),
        )

        add(
            simple(
                "toggle_persistent_bottom_progress",
                "开关底部常驻迷你进度条",
                PlayerCustomShortcutAction.TogglePersistentBottomProgress,
            ),
        )
    }

    /** 获取动作的显示名称。 */
    fun getActionDisplayName(action: PlayerCustomShortcutAction): String {
        return groups().flatMap { group ->
            group.action?.let { listOf(ActionEntry(it, group.displayName)) } ?: group.values
        }.firstOrNull { it.action == action }?.displayName ?: action.javaClass.simpleName
    }

    private fun simple(
        id: String,
        displayName: String,
        action: PlayerCustomShortcutAction,
    ): ActionGroup = ActionGroup(id = id, displayName = displayName, action = action)

    private fun valueGroup(
        id: String,
        displayName: String,
        values: List<ActionEntry>,
    ): ActionGroup = ActionGroup(id = id, displayName = displayName, values = values)

    private fun PlayerCustomShortcutAction.entry(
        displayName: String,
        valueDisplayName: String = displayName,
    ): ActionEntry = ActionEntry(action = this, displayName = displayName, valueDisplayName = valueDisplayName)
}
