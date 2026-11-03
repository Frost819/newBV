package dev.frost819.newbv.app.ui.component.player.menu

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiStatus
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiType
import dev.frost819.newbv.biliapi.entity.video.SubtitleType
import org.junit.jupiter.api.Test

/**
 * [Subtitle.toMenuLabel] 的单元测试。
 *
 * 验证字幕菜单文案与 BV 原版一致：人工字幕显示语言名，
 * AI 字幕去掉「（自动生成）/（自动翻译）」后缀并补 `(AI)` 标记。
 */
class SubtitleTrackLabelTest {
    private fun subtitle(
        id: Long = 1L,
        lang: String = "zh-Hans",
        langDoc: String = "中文（简体）",
        type: SubtitleType = SubtitleType.CC,
    ) = Subtitle(
        id = id,
        lang = lang,
        langDoc = langDoc,
        url = "",
        type = type,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None,
    )

    @Test
    fun `manual subtitle keeps language name without marker`() {
        val label = subtitle(langDoc = "中文（简体）").toMenuLabel()

        assertThat(label).isEqualTo("中文（简体）")
    }

    @Test
    fun `AI generated subtitle strips hint and adds marker`() {
        val label =
            subtitle(
                lang = "ai-zh",
                langDoc = "中文（自动生成）",
                type = SubtitleType.AI,
            ).toMenuLabel()

        assertThat(label).isEqualTo("中文(AI)")
    }

    @Test
    fun `AI translated subtitle strips hint and adds marker`() {
        val label =
            subtitle(
                lang = "ai-en",
                langDoc = "英语（自动翻译）",
                type = SubtitleType.AI,
            ).toMenuLabel()

        assertThat(label).isEqualTo("英语(AI)")
    }

    @Test
    fun `AI subtitle without hint still adds marker`() {
        val label =
            subtitle(
                lang = "ai-zh",
                langDoc = "中文",
                type = SubtitleType.AI,
            ).toMenuLabel()

        assertThat(label).isEqualTo("中文(AI)")
    }

    @Test
    fun `close placeholder renders close label`() {
        val label = subtitle(id = -1L, langDoc = "关闭").toMenuLabel()

        assertThat(label).isEqualTo("关闭")
    }
}
