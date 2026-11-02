package dev.frost819.newbv.player

import androidx.media3.common.PlaybackException
import java.io.IOException

/*
 * 播放错误分类与提示文案。
 *
 * 播放器实现用它判断是否需要切换音轨 / 编码，ViewModel 用它给出面向用户的文案，
 * 避免把 media3 的错误码散落到各处。
 */

/** media3 明确标记的音频管线错误码（AudioTrack 初始化 / 写入 / 直通失败）。 */
private val AUDIO_ERROR_CODES =
    setOf(
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
    )

/**
 * 换 CDN 重试可能有帮助的错误码（网络 / HTTP / 拉流超时）。
 *
 * 只收「换一个源地址可能自愈」的错误：权限与明文流量限制属客户端配置问题，
 * 解码与音频管线错误与源地址无关，都不在此列。
 */
private val NETWORK_ERROR_CODES =
    setOf(
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_TIMEOUT,
    )

/** 视频解码能力相关错误码（可尝试换编码 / 降画质重试）。 */
private val VIDEO_DECODE_ERROR_CODES =
    setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    )

/**
 * 音频输出链路所在的包名，用于从堆栈识别 media3 未能归类的音频错误。
 *
 * 依赖 release 构建的 `-keepnames class androidx.media3.exoplayer.audio.**`
 * （`app/proguard-rules.pro`）：否则类名被混淆，栈帧不再匹配此前缀。
 */
private const val AUDIO_PACKAGE_PREFIX = "androidx.media3.exoplayer.audio."

/** 异常链最多回溯层数（防异常链成环）。 */
private const val MAX_CAUSE_DEPTH = 8

/** 无法归类的播放错误的兜底提示。 */
private const val FALLBACK_MESSAGE = "播放失败，请稍后重试"

/**
 * 是否为视频解码能力相关错误（可尝试换编码 / 降画质重试）。
 *
 * 覆盖解码器初始化失败、查询失败、解码失败、格式超出能力、格式不支持；
 * 其余错误（网络、解析容器等）交由普通 [VideoPlayerListener.onError] 处理。
 */
fun PlaybackException.isVideoDecodeError(): Boolean = isVideoDecodeError(errorCode)

/**
 * 是否为视频解码能力相关错误（参数化版本，便于单测）。
 *
 * @param errorCode media3 错误码
 */
internal fun isVideoDecodeError(errorCode: Int): Boolean = errorCode in VIDEO_DECODE_ERROR_CODES

/**
 * 是否为音频管线错误。
 *
 * 两类来源：
 * - media3 明确标记的音频错误码（AudioTrack 初始化 / 写入 / 直通失败）；
 * - 音频输出链路上的运行时断言。典型场景：单个压缩音频帧大于解码器输入缓冲
 *   （例如 32KB 输入缓冲的 Dolby 帧），media3 在该分支会 flush 解码器，
 *   随后在 `DefaultAudioSink.handleBuffer` 触发 `checkArgument` 失败，
 *   并被归为 `ERROR_CODE_UNSPECIFIED`——此时代码无法区分，只能靠堆栈识别。
 */
fun Throwable.isAudioRenderError(): Boolean =
    isAudioRenderError(
        errorCode = (this as? PlaybackException)?.errorCode,
        throwable = this,
    )

/**
 * 是否为音频管线错误（参数化版本，便于单测）。
 *
 * @param errorCode media3 错误码；非 [PlaybackException] 时传 `null`
 * @param throwable 待检查的异常，会连同原因链一起判定
 */
internal fun isAudioRenderError(
    errorCode: Int?,
    throwable: Throwable?,
): Boolean {
    if (errorCode != null && errorCode in AUDIO_ERROR_CODES) return true
    return throwable?.causeChain()?.any { it.isInAudioPackage() } == true
}

/**
 * 是否为换 CDN 重试可能有帮助的错误。
 *
 * 调用方（如播放器 ViewModel 的自动选源）据此决定是否切换到下一个候选地址：
 * 解码、音频管线、容器解析等错误换地址不会改善，重试只是白白拉长用户等待。
 *
 * 判定顺序：media3 网络类错误码优先；非 [PlaybackException] 或其错误码未归类时，
 * 回溯原因链中的 [IOException]（media3 的 `HttpDataSourceException` 等均继承自它）。
 */
fun Throwable.isNetworkError(): Boolean =
    isNetworkError(
        errorCode = (this as? PlaybackException)?.errorCode,
        throwable = this,
    )

/**
 * 是否为换 CDN 重试可能有帮助的错误（参数化版本，便于单测）。
 *
 * @param errorCode media3 错误码；非 [PlaybackException] 时传 `null`
 * @param throwable 待检查的异常，会连同原因链一起判定
 */
internal fun isNetworkError(
    errorCode: Int?,
    throwable: Throwable?,
): Boolean {
    if (errorCode != null && errorCode in NETWORK_ERROR_CODES) return true
    return throwable?.causeChain()?.any { it is IOException } == true
}

/**
 * 转换为面向用户的中文提示。
 *
 * @see playbackErrorMessage
 */
fun Throwable.toPlaybackErrorMessage(): String =
    if (isAudioRenderError()) {
        "音频解码失败，请尝试切换音轨"
    } else {
        playbackErrorMessage((this as? PlaybackException)?.errorCode)
    }

/**
 * 按 media3 错误码生成提示文案（纯函数，便于单测）。
 *
 * @param errorCode media3 错误码；非 [PlaybackException] 时传 `null`
 */
internal fun playbackErrorMessage(errorCode: Int?): String =
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> "网络异常，请检查网络后重试"

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        -> "视频源解析失败，请稍后重试"

        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
        -> "解码失败，请尝试切换画质或编码"

        else -> FALLBACK_MESSAGE
    }

/** 从当前异常开始回溯原因链。 */
private fun Throwable.causeChain(): List<Throwable> {
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && chain.size < MAX_CAUSE_DEPTH && current !in chain) {
        chain.add(current)
        current = current.cause
    }
    return chain
}

/** 是否有栈帧落在 media3 的音频输出包内。 */
private fun Throwable.isInAudioPackage(): Boolean = stackTrace.any { it.className.startsWith(AUDIO_PACKAGE_PREFIX) }
