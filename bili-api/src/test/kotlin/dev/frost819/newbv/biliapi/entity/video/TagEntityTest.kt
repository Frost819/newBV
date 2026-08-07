package dev.frost819.newbv.biliapi.entity.video

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [Tag] 实体 HTTP→Domain 转换方法的单元测试。
 */
class TagEntityTest {
    @Test
    fun `fromTag HTTP Tag maps tagId and tagName`() {
        val httpTag =
            dev.frost819.newbv.biliapi.http.entity.video.Tag(
                tagId = 42,
                tagName = "测试标签",
                cover = "",
                headCover = "",
                content = "",
                shortContent = "",
                type = 0,
                state = 0,
                ctime = 0,
                count = dev.frost819.newbv.biliapi.http.entity.video.Tag.Count(view = 0, use = 0, atten = 0),
                isAtten = 0,
                likes = 0,
                hates = 0,
                attribute = 0,
                liked = 0,
                hated = 0,
                extraAttr = 0,
            )

        val tag = Tag.fromTag(httpTag)

        assertThat(tag.id).isEqualTo(42)
        assertThat(tag.name).isEqualTo("测试标签")
    }

    @Test
    fun `fromTag VideoDetail Tag maps tagId and tagName`() {
        val httpTag =
            dev.frost819.newbv.biliapi.http.entity.video.VideoDetail.Tag(
                tagId = 99,
                tagName = "音乐",
                musicId = "",
                tagType = "",
                jumpUrl = "",
            )

        val tag = Tag.fromTag(httpTag)

        assertThat(tag.id).isEqualTo(99)
        assertThat(tag.name).isEqualTo("音乐")
    }
}
