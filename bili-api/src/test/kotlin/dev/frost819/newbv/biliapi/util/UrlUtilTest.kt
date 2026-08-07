package dev.frost819.newbv.biliapi.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UrlUtil] 的单元测试。
 *
 * 验证视频 URL 识别与 AV/BV 解析。
 */
class UrlUtilTest {
    @Test
    fun `isVideoUrl returns true for bilibili scheme`() {
        assertThat(UrlUtil.isVideoUrl("bilibili://video/12345")).isTrue()
    }

    @Test
    fun `isVideoUrl returns true for https www bilibili`() {
        assertThat(UrlUtil.isVideoUrl("https://www.bilibili.com/video/BV1xx411c7mD")).isTrue()
    }

    @Test
    fun `isVideoUrl returns false for non-video url`() {
        assertThat(UrlUtil.isVideoUrl("https://www.bilibili.com/bangumi/media/md2823")).isFalse()
        assertThat(UrlUtil.isVideoUrl("https://example.com")).isFalse()
    }

    @Test
    fun `parseAidFromUrl parses bilibili scheme with aid`() {
        assertThat(UrlUtil.parseAidFromUrl("bilibili://video/170001")).isEqualTo(170001L)
    }

    @Test
    fun `parseAidFromUrl parses https url with BV id`() {
        val url = "https://www.bilibili.com/video/BV17x411w7KC"
        assertThat(UrlUtil.parseAidFromUrl(url)).isEqualTo(170001L)
    }

    @Test
    fun `parseAidFromUrl parses https url with av id`() {
        val url = "https://www.bilibili.com/video/av170001"
        assertThat(UrlUtil.parseAidFromUrl(url)).isEqualTo(170001L)
    }

    @Test
    fun `parseBvidFromUrl returns bvid for bilibili scheme`() {
        val bvid = UrlUtil.parseBvidFromUrl("bilibili://video/170001")
        assertThat(bvid).isEqualTo("BV17x411w7KC")
    }

    @Test
    fun `parseBvidFromUrl returns bvid for https url`() {
        val url = "https://www.bilibili.com/video/BV17x411w7KC"
        val bvid = UrlUtil.parseBvidFromUrl(url)
        assertThat(bvid).isEqualTo("BV17x411w7KC")
    }
}
