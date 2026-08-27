package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutsCodec.normalize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 自定义快捷键绑定。
 *
 * @property keyCode Android KeyEvent 键码
 * @property action 绑定的动作
 */
data class PlayerCustomShortcut(
    val keyCode: Int,
    val action: PlayerCustomShortcutAction,
)

/**
 * 快捷键键码校验工具。
 *
 * 禁止将系统关键键（BACK、ENTER 等）绑定为自定义快捷键。
 */
object PlayerCustomShortcutKeys {
    private val forbiddenKeyCodes = setOf(
        KeyEvent.KEYCODE_UNKNOWN,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
    )

    /** 检查键码是否允许绑定。 */
    fun isAllowedKeyCode(keyCode: Int): Boolean = keyCode > 0 && keyCode !in forbiddenKeyCodes

    /** 检查键码是否为取消键（用于配置 UI 中取消绑定）。 */
    fun isCancelKeyCode(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_ESCAPE ||
            keyCode == KeyEvent.KEYCODE_BUTTON_B

    /** 获取键码的可读名称。 */
    fun getDisplayName(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "方向上"
        KeyEvent.KEYCODE_DPAD_DOWN -> "方向下"
        KeyEvent.KEYCODE_DPAD_LEFT -> "方向左"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "方向右"
        KeyEvent.KEYCODE_MENU -> "菜单键"
        KeyEvent.KEYCODE_SPACE -> "空格"
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "媒体播放/暂停"
        KeyEvent.KEYCODE_MEDIA_PLAY -> "媒体播放"
        KeyEvent.KEYCODE_MEDIA_PAUSE -> "媒体暂停"
        KeyEvent.KEYCODE_MEDIA_REWIND -> "媒体快退"
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "媒体快进"
        else -> KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace('_', ' ')
    }
}

/**
 * 快捷键序列化/反序列化编解码器。
 *
 * 支持两种 JSON 格式：
 * - 数组格式（旧版兼容）：`[{"k":19,"a":"toggle_danmaku","p":{}}]`
 * - 版本化格式：`{"v":1,"items":[...]}`
 *
 * [normalize] 确保：键码去重、动作参数范围合法、禁用键过滤。
 * 已移除的历史动作（如 set_resolution）在解析时被静默丢弃。
 */
object PlayerCustomShortcutsCodec {
    private const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** 从 JSON 字符串解析快捷键列表。空字符串或解析失败返回空列表。 */
    fun parse(raw: String): List<PlayerCustomShortcut> {
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val items = if (raw.trimStart().startsWith("[")) {
                json.decodeFromString<List<PlayerCustomShortcutDto>>(raw)
            } else {
                json.decodeFromString<PlayerCustomShortcutsPayload>(raw).items
            }
            normalize(items.mapNotNull { it.toShortcutOrNull() })
        }.getOrDefault(emptyList())
    }

    /** 将快捷键列表序列化为 JSON 字符串。 */
    fun serialize(shortcuts: List<PlayerCustomShortcut>): String {
        val dto = PlayerCustomShortcutsPayload(
            version = VERSION,
            items = normalize(shortcuts).map { it.toDto() },
        )
        return json.encodeToString(dto)
    }

    /** 规范化快捷键列表：去重（同键码取最后）、参数 clamp、禁用键过滤。 */
    fun normalize(shortcuts: List<PlayerCustomShortcut>): List<PlayerCustomShortcut> {
        val seen = mutableSetOf<Int>()
        return shortcuts
            .asReversed()
            .mapNotNull { shortcut ->
                val normalizedAction = shortcut.action.normalized() ?: return@mapNotNull null
                if (!PlayerCustomShortcutKeys.isAllowedKeyCode(shortcut.keyCode)) return@mapNotNull null
                if (!seen.add(shortcut.keyCode)) return@mapNotNull null
                shortcut.copy(action = normalizedAction)
            }
            .asReversed()
    }

    private fun PlayerCustomShortcut.toDto(): PlayerCustomShortcutDto {
        val (actionId, params) = action.toStorage()
        return PlayerCustomShortcutDto(keyCode = keyCode, action = actionId, params = params)
    }

    private fun PlayerCustomShortcutDto.toShortcutOrNull(): PlayerCustomShortcut? {
        val decodedAction = actionFromStorage(action, params) ?: return null
        return PlayerCustomShortcut(keyCode = keyCode, action = decodedAction)
    }

    private fun PlayerCustomShortcutAction.normalized(): PlayerCustomShortcutAction? {
        return when (this) {
            is PlayerCustomShortcutAction.TogglePlaybackSpeed ->
                copy(speed = speed.coerceIn(0.25f, 4f))

            else -> this
        }
    }

    private fun PlayerCustomShortcutAction.toStorage(): Pair<String, JsonObject> {
        return when (this) {
            PlayerCustomShortcutAction.OpenSettings -> "open_settings" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenRelatedVideos -> "open_related_videos" to buildJsonObject { }
            PlayerCustomShortcutAction.PlayPrevious -> "play_previous" to buildJsonObject { }
            PlayerCustomShortcutAction.PlayNext -> "play_next" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenVideoDetail -> "open_video_detail" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenUpPage -> "open_up_page" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleLoop -> "toggle_loop" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleDanmaku -> "toggle_danmaku" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleDanmakuMask -> "toggle_danmaku_mask" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleSubtitle -> "toggle_subtitle" to buildJsonObject { }
            PlayerCustomShortcutAction.TogglePersistentBottomProgress ->
                "toggle_persistent_bottom_progress" to buildJsonObject { }

            is PlayerCustomShortcutAction.TogglePlaybackSpeed ->
                "set_playback_speed" to buildJsonObject { put("speed", speed) }
        }
    }

    private fun actionFromStorage(
        action: String,
        params: JsonObject,
    ): PlayerCustomShortcutAction? {
        return when (action) {
            "open_settings" -> PlayerCustomShortcutAction.OpenSettings
            "open_related_videos" -> PlayerCustomShortcutAction.OpenRelatedVideos
            "play_previous" -> PlayerCustomShortcutAction.PlayPrevious
            "play_next" -> PlayerCustomShortcutAction.PlayNext
            "open_video_detail" -> PlayerCustomShortcutAction.OpenVideoDetail
            "open_up_page" -> PlayerCustomShortcutAction.OpenUpPage
            "toggle_loop" -> PlayerCustomShortcutAction.ToggleLoop
            "toggle_danmaku" -> PlayerCustomShortcutAction.ToggleDanmaku
            "toggle_danmaku_mask" -> PlayerCustomShortcutAction.ToggleDanmakuMask
            "toggle_subtitle" -> PlayerCustomShortcutAction.ToggleSubtitle
            "toggle_persistent_bottom_progress" -> PlayerCustomShortcutAction.TogglePersistentBottomProgress

            "set_playback_speed" -> PlayerCustomShortcutAction.TogglePlaybackSpeed(
                params.float("speed") ?: return null,
            )

            // 已移除的历史动作返回 null，解析时被静默丢弃
            else -> null
        }?.normalized()
    }

    private fun JsonObject.float(name: String): Float? =
        this[name]?.jsonPrimitive?.floatOrNull

    @Serializable
    private data class PlayerCustomShortcutsPayload(
        @SerialName("v") val version: Int = VERSION,
        @SerialName("items") val items: List<PlayerCustomShortcutDto> = emptyList(),
    )

    @Serializable
    private data class PlayerCustomShortcutDto(
        @SerialName("k") val keyCode: Int,
        @SerialName("a") val action: String,
        @SerialName("p") val params: JsonObject = buildJsonObject { },
    )
}
