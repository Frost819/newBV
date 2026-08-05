package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

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
 * - 数组格式（旧版兼容）：`[{"k":19,"a":"toggle_play_pause","p":{}}]`
 * - 版本化格式：`{"v":1,"items":[...]}`
 *
 * [normalize] 确保：键码去重、动作参数范围合法、禁用键过滤。
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
            is PlayerCustomShortcutAction.SetPlaybackSpeed ->
                copy(speed = speed.coerceIn(0.25f, 4f))

            is PlayerCustomShortcutAction.SetResolution ->
                takeIf { Resolution.entries.any { resolution -> resolution.code == qualityId } }

            is PlayerCustomShortcutAction.SetDanmakuScale ->
                copy(scale = scale.coerceIn(0.5f, 4f))

            is PlayerCustomShortcutAction.SetDanmakuOpacity ->
                copy(opacity = opacity.coerceIn(0f, 1f))

            is PlayerCustomShortcutAction.SetDanmakuSpeedFactor ->
                copy(factor = factor.coerceIn(0.5f, 1.5f))

            is PlayerCustomShortcutAction.SetDanmakuArea ->
                copy(area = area.coerceIn(0f, 1f))

            is PlayerCustomShortcutAction.SetSubtitleFontSize ->
                copy(sp = sp.coerceIn(12, 48))

            is PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity ->
                copy(opacity = opacity.coerceIn(0f, 1f))

            is PlayerCustomShortcutAction.SetSubtitleBottomPadding ->
                copy(dp = dp.coerceIn(0, 48))

            else -> this
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun PlayerCustomShortcutAction.toStorage(): Pair<String, JsonObject> {
        return when (this) {
            PlayerCustomShortcutAction.ShowInfo -> "show_info" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenSettings -> "open_settings" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenVideoList -> "open_video_list" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenRelatedVideos -> "open_related_videos" to buildJsonObject { }
            PlayerCustomShortcutAction.TogglePlayPause -> "toggle_play_pause" to buildJsonObject { }
            PlayerCustomShortcutAction.PlayPrevious -> "play_previous" to buildJsonObject { }
            PlayerCustomShortcutAction.PlayNext -> "play_next" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenVideoDetail -> "open_video_detail" to buildJsonObject { }
            PlayerCustomShortcutAction.OpenUpPage -> "open_up_page" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleLoop -> "toggle_loop" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleDanmaku -> "toggle_danmaku" to buildJsonObject { }
            PlayerCustomShortcutAction.ToggleSubtitle -> "toggle_subtitle" to buildJsonObject { }
            PlayerCustomShortcutAction.TogglePersistentBottomProgress ->
                "toggle_persistent_bottom_progress" to buildJsonObject { }

            is PlayerCustomShortcutAction.SetPlaybackSpeed ->
                "set_playback_speed" to buildJsonObject { put("speed", speed) }

            is PlayerCustomShortcutAction.SetResolution ->
                "set_resolution" to buildJsonObject { put("quality_id", qualityId) }

            is PlayerCustomShortcutAction.SetAudio ->
                "set_audio" to buildJsonObject { put("audio", audio.code) }

            is PlayerCustomShortcutAction.SetVideoCodec ->
                "set_video_codec" to buildJsonObject { put("codec", codec.name) }

            is PlayerCustomShortcutAction.SetAspectRatio ->
                "set_aspect_ratio" to buildJsonObject { put("aspect_ratio", aspectRatio.name) }

            is PlayerCustomShortcutAction.SetDanmakuScale ->
                "set_danmaku_scale" to buildJsonObject { put("scale", scale) }

            is PlayerCustomShortcutAction.SetDanmakuOpacity ->
                "set_danmaku_opacity" to buildJsonObject { put("opacity", opacity) }

            is PlayerCustomShortcutAction.SetDanmakuSpeedFactor ->
                "set_danmaku_speed_factor" to buildJsonObject { put("factor", factor) }

            is PlayerCustomShortcutAction.SetDanmakuArea ->
                "set_danmaku_area" to buildJsonObject { put("area", area) }

            is PlayerCustomShortcutAction.SetDanmakuMaskEnabled ->
                "set_danmaku_mask_enabled" to buildJsonObject { put("enabled", enabled) }

            is PlayerCustomShortcutAction.SetSubtitleFontSize ->
                "set_subtitle_font_size" to buildJsonObject { put("sp", sp) }

            is PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity ->
                "set_subtitle_background_opacity" to buildJsonObject { put("opacity", opacity) }

            is PlayerCustomShortcutAction.SetSubtitleBottomPadding ->
                "set_subtitle_bottom_padding" to buildJsonObject { put("dp", dp) }
        }
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    private fun actionFromStorage(
        action: String,
        params: JsonObject,
    ): PlayerCustomShortcutAction? {
        return when (action) {
            "show_info" -> PlayerCustomShortcutAction.ShowInfo
            "open_settings" -> PlayerCustomShortcutAction.OpenSettings
            "open_video_list" -> PlayerCustomShortcutAction.OpenVideoList
            "open_related_videos" -> PlayerCustomShortcutAction.OpenRelatedVideos
            "toggle_play_pause" -> PlayerCustomShortcutAction.TogglePlayPause
            "play_previous" -> PlayerCustomShortcutAction.PlayPrevious
            "play_next" -> PlayerCustomShortcutAction.PlayNext
            "open_video_detail" -> PlayerCustomShortcutAction.OpenVideoDetail
            "open_up_page" -> PlayerCustomShortcutAction.OpenUpPage
            "toggle_loop" -> PlayerCustomShortcutAction.ToggleLoop
            "toggle_danmaku" -> PlayerCustomShortcutAction.ToggleDanmaku
            "toggle_subtitle" -> PlayerCustomShortcutAction.ToggleSubtitle
            "toggle_persistent_bottom_progress" -> PlayerCustomShortcutAction.TogglePersistentBottomProgress

            "set_playback_speed" -> PlayerCustomShortcutAction.SetPlaybackSpeed(
                params.float("speed") ?: return null,
            )

            "set_resolution", "set_resolution_qn" -> PlayerCustomShortcutAction.SetResolution(
                params.int("quality_id") ?: params.int("qn") ?: return null,
            )

            "set_audio", "set_audio_id" -> {
                val audioCode = params.int("audio") ?: params.int("audio_id") ?: return null
                val audio = Audio.entries.find { it.code == audioCode } ?: return null
                PlayerCustomShortcutAction.SetAudio(audio)
            }

            "set_video_codec", "set_codec" -> {
                val codecName = params.string("codec") ?: return null
                val codec = VideoCodec.entries.find { it.name == codecName } ?: return null
                PlayerCustomShortcutAction.SetVideoCodec(codec)
            }

            "set_aspect_ratio" -> {
                val aspectRatioName = params.string("aspect_ratio") ?: return null
                val aspectRatio = VideoAspectRatio.entries.find { it.name == aspectRatioName } ?: return null
                PlayerCustomShortcutAction.SetAspectRatio(aspectRatio)
            }

            "set_danmaku_scale", "set_danmaku_text_size" -> PlayerCustomShortcutAction.SetDanmakuScale(
                params.float("scale") ?: params.float("size") ?: return null,
            )

            "set_danmaku_opacity" -> PlayerCustomShortcutAction.SetDanmakuOpacity(
                params.float("opacity") ?: return null,
            )

            "set_danmaku_speed_factor", "set_danmaku_speed" -> PlayerCustomShortcutAction.SetDanmakuSpeedFactor(
                params.float("factor") ?: params.float("speed") ?: return null,
            )

            "set_danmaku_area" -> PlayerCustomShortcutAction.SetDanmakuArea(
                params.float("area") ?: return null,
            )

            "set_danmaku_mask_enabled" -> PlayerCustomShortcutAction.SetDanmakuMaskEnabled(
                params.boolean("enabled") ?: return null,
            )

            "set_subtitle_font_size", "set_subtitle_text_size" -> PlayerCustomShortcutAction.SetSubtitleFontSize(
                params.int("sp") ?: params.int("size") ?: return null,
            )

            "set_subtitle_background_opacity" -> PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(
                params.float("opacity") ?: return null,
            )

            "set_subtitle_bottom_padding" -> PlayerCustomShortcutAction.SetSubtitleBottomPadding(
                params.int("dp") ?: params.int("padding") ?: return null,
            )

            else -> null
        }?.normalized()
    }

    private fun JsonObject.int(name: String): Int? = this[name]?.jsonPrimitive?.intOrNull
    private fun JsonObject.float(name: String): Float? = this[name]?.jsonPrimitive?.floatOrNull
    private fun JsonObject.boolean(name: String): Boolean? = this[name]?.jsonPrimitive?.booleanOrNull
    private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.content

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

/**
 * Float 转百分比显示文本。
 */
internal fun Float.percentText(): String = "${(this * 100).roundToInt()}%"
