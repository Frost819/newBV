package dev.frost819.newbv.app.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.VideoCodec
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [MediaCodecVideoCapabilityProvider] 的插桩测试。
 *
 * 该实现依赖系统 `MediaCodecList` / `MediaCodecVideoRenderer.supportsFormat`，
 * 只能在真实设备/模拟器上验证。仅断言与设备无关的稳定行为：
 * - 普通低分辨率 AVC 一定可解码；
 * - 荒谬的超大分辨率一定不可解码；
 * - 查询稳定、不抛异常。
 */
@RunWith(AndroidJUnit4::class)
class MediaCodecVideoCapabilityProviderTest {
    private lateinit var context: Context
    private lateinit var provider: VideoCapabilityProvider

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // 注入固定软解开关，避免依赖 Prefs 初始化
        provider = MediaCodecVideoCapabilityProvider(context) { false }
    }

    private fun avcProfile(
        width: Int,
        height: Int,
        frameRate: Float?,
        codecs: String?,
    ) = VideoDecodeProfile(VideoCodec.AVC, width, height, frameRate, codecs)

    @Test
    fun standardAvc480pIsDecodable() {
        val profile = avcProfile(640, 480, 30f, "avc1.42E01E")

        assertThat(provider.isDecodable(profile)).isTrue()
    }

    @Test
    fun absurdResolutionIsNotDecodable() {
        val profile = avcProfile(20000, 20000, 60f, "avc1.640034")

        assertThat(provider.isDecodable(profile)).isFalse()
    }

    @Test
    fun queryIsStableAndCached() {
        val profile = avcProfile(640, 480, 30f, "avc1.42E01E")

        val first = provider.isDecodable(profile)
        val second = provider.isDecodable(profile)

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun dolbyVisionMimeDoesNotThrow() {
        // 结果随设备而异（有无 DV 解码器），只验证查询不抛异常
        val profile = VideoDecodeProfile(VideoCodec.DVH1, 1920, 1080, 24f, null)

        provider.isDecodable(profile)
    }

    @Test
    fun softwareSelectorAlsoReturnsResult() {
        val softwareProvider = MediaCodecVideoCapabilityProvider(context) { true }
        val profile = avcProfile(640, 480, 30f, "avc1.42E01E")

        assertThat(softwareProvider.isDecodable(profile)).isTrue()
    }
}
