package dev.frost819.newbv.biliapi.entity.video

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [parseEpIdFromUri] 的单元测试。
 *
 * 覆盖 Web 相关视频 `redirect_url` 与 App gRPC `relate.uri` 的实际格式。
 */
class RelatedVideoEpIdParseTest {
    @Test
    fun `parses web redirect url with query`() {
        assertThat(parseEpIdFromUri("https://www.bilibili.com/bangumi/play/ep1364037?theme=movie"))
            .isEqualTo(1364037)
    }

    @Test
    fun `parses web redirect url without query`() {
        assertThat(parseEpIdFromUri("https://www.bilibili.com/bangumi/play/ep2192496"))
            .isEqualTo(2192496)
    }

    @Test
    fun `parses web redirect url with trailing slash`() {
        assertThat(parseEpIdFromUri("http://www.bilibili.com/bangumi/play/ep284272/"))
            .isEqualTo(284272)
    }

    @Test
    fun `parses bangumi uri`() {
        assertThat(parseEpIdFromUri("bilibili://bangumi/season/33495/ep123456"))
            .isEqualTo(123456)
    }

    @Test
    fun `returns null for url without ep`() {
        assertThat(parseEpIdFromUri("https://www.bilibili.com/bangumi/season/33495")).isNull()
    }

    @Test
    fun `returns null for null or blank input`() {
        assertThat(parseEpIdFromUri(null)).isNull()
        assertThat(parseEpIdFromUri("")).isNull()
    }
}
