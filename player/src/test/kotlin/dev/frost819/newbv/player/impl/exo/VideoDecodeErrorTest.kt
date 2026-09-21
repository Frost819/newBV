package dev.frost819.newbv.player.impl.exo

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [isVideoDecodeError] 的错误码分类单测。
 *
 * 保证解码能力相关错误被识别（触发回退），其它错误不被误判（走普通 onError）。
 */
class VideoDecodeErrorTest {
    @Test
    fun `decode related error codes are recognized`() {
        val decodeCodes =
            listOf(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            )

        decodeCodes.forEach { code ->
            assertThat(isVideoDecodeError(code)).isTrue()
        }
    }

    @Test
    fun `non decode error codes are not recognized`() {
        val otherCodes =
            listOf(
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_UNSPECIFIED,
            )

        otherCodes.forEach { code ->
            assertThat(isVideoDecodeError(code)).isFalse()
        }
    }
}
