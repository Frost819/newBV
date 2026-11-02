package dev.frost819.newbv.player

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [isAudioRenderError] 与 [toPlaybackErrorMessage] 的单元测试。
 *
 * media3 会把音频输出链路上的断言失败归为 `ERROR_CODE_UNSPECIFIED`，
 * 此时只能靠堆栈识别，因此用例里用伪造的栈帧模拟 `DefaultAudioSink` 抛错。
 */
class PlaybackErrorsTest {
    @Test
    fun `audio track error codes are treated as audio errors`() {
        listOf(
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
        ).forEach { code ->
            assertThat(isAudioRenderError(errorCode = code, throwable = null)).isTrue()
        }
    }

    @Test
    fun `audio sink stack without error code is treated as audio error`() {
        // Given: media3 对该断言失败给出的错误码是 ERROR_CODE_UNSPECIFIED
        val error = audioSinkError()

        // Then
        assertThat(isAudioRenderError(errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED, throwable = error)).isTrue()
    }

    @Test
    fun `audio sink error nested as cause is detected`() {
        // Given: PlaybackException 包装了音频 sink 的断言失败
        val error = RuntimeException("Playback error", audioSinkError())

        // Then
        assertThat(error.isAudioRenderError()).isTrue()
        assertThat(error.toPlaybackErrorMessage()).isEqualTo("音频解码失败，请尝试切换音轨")
    }

    @Test
    fun `network error is not treated as audio error`() {
        // Given
        val error = IOException("HTTP 403")

        // Then
        assertThat(isAudioRenderError(errorCode = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, throwable = error))
            .isFalse()
        assertThat(error.isAudioRenderError()).isFalse()
    }

    @Test
    fun `network error code maps to network message`() {
        assertThat(playbackErrorMessage(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
            .isEqualTo("网络异常，请检查网络后重试")
        assertThat(playbackErrorMessage(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
            .isEqualTo("网络异常，请检查网络后重试")
    }

    @Test
    fun `network error codes are treated as retryable on another cdn`() {
        listOf(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_TIMEOUT,
        ).forEach { code ->
            assertThat(isNetworkError(errorCode = code, throwable = null)).isTrue()
        }
    }

    @Test
    fun `io exception without error code is treated as network error`() {
        assertThat(IOException("HTTP 403").isNetworkError()).isTrue()
        // 被包装的 IO 异常同样识别
        assertThat(RuntimeException("Playback error", IOException("timeout")).isNetworkError()).isTrue()
    }

    @Test
    fun `audio sink error is not retryable on another cdn`() {
        assertThat(isNetworkError(errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED, throwable = audioSinkError()))
            .isFalse()
    }

    @Test
    fun `unknown error is not retryable on another cdn`() {
        assertThat(IllegalArgumentException("boom").isNetworkError()).isFalse()
        assertThat(isNetworkError(errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED, throwable = null))
            .isFalse()
    }

    @Test
    fun `decoding error code maps to decode message`() {
        assertThat(playbackErrorMessage(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED))
            .isEqualTo("解码失败，请尝试切换画质或编码")
    }

    @Test
    fun `unknown or missing error code maps to fallback message`() {
        assertThat(playbackErrorMessage(null)).isEqualTo("播放失败，请稍后重试")
        assertThat(playbackErrorMessage(PlaybackException.ERROR_CODE_UNSPECIFIED)).isEqualTo("播放失败，请稍后重试")
    }

    @Test
    fun `plain exception maps to fallback message`() {
        assertThat(IllegalStateException("boom").toPlaybackErrorMessage()).isEqualTo("播放失败，请稍后重试")
    }

    /** 构造一个栈帧落在 media3 音频输出包内的异常。 */
    private fun audioSinkError(): IllegalArgumentException =
        IllegalArgumentException("Buffer too small").apply {
            stackTrace =
                arrayOf(
                    StackTraceElement(
                        "androidx.media3.exoplayer.audio.DefaultAudioSink",
                        "handleBuffer",
                        "DefaultAudioSink.java",
                        954,
                    ),
                    StackTraceElement(
                        "androidx.media3.exoplayer.audio.MediaCodecAudioRenderer",
                        "processOutputBuffer",
                        "MediaCodecAudioRenderer.java",
                        832,
                    ),
                )
        }
}
