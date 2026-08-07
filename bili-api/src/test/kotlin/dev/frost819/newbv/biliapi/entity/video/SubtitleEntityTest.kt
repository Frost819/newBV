package dev.frost819.newbv.biliapi.entity.video

import bilibili.community.service.dm.v1.subtitleItem
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import bilibili.community.service.dm.v1.SubtitleAiStatus as GrpcSubtitleAiStatus
import bilibili.community.service.dm.v1.SubtitleAiType as GrpcSubtitleAiType
import bilibili.community.service.dm.v1.SubtitleType as GrpcSubtitleType

/**
 * [Subtitle] 实体 HTTP→Domain 转换方法的单元测试。
 */
class SubtitleEntityTest {
    @Test
    fun `fromSubtitleItem maps CC type and Normal aiType and None aiStatus`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.SubtitleItem(
                id = 100L,
                lan = "zh-Hans",
                lanDoc = "中文（简体）",
                isLock = false,
                subtitleUrl = "http://subtitle.test/100.json",
                type = 0,
                idStr = "100",
                aiType = 0,
                aiStatus = 0,
            )

        val subtitle = Subtitle.fromSubtitleItem(httpItem)

        assertThat(subtitle.id).isEqualTo(100L)
        assertThat(subtitle.lang).isEqualTo("zh-Hans")
        assertThat(subtitle.langDoc).isEqualTo("中文（简体）")
        assertThat(subtitle.url).isEqualTo("http://subtitle.test/100.json")
        assertThat(subtitle.type).isEqualTo(SubtitleType.CC)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Normal)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.None)
    }

    @Test
    fun `fromSubtitleItem maps AI type and Translate aiType and Exposure aiStatus`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.SubtitleItem(
                id = 200L,
                lan = "ai-en",
                lanDoc = "AI English",
                isLock = false,
                subtitleUrl = "http://subtitle.test/200.json",
                type = 1,
                idStr = "200",
                aiType = 1,
                aiStatus = 1,
            )

        val subtitle = Subtitle.fromSubtitleItem(httpItem)

        assertThat(subtitle.type).isEqualTo(SubtitleType.AI)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Translate)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.Exposure)
    }

    @Test
    fun `fromSubtitleItem maps aiStatus 2 to Assist`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.SubtitleItem(
                id = 300L,
                lan = "ai-zh",
                lanDoc = "AI 中文",
                isLock = false,
                subtitleUrl = "",
                type = 1,
                idStr = "300",
                aiType = 0,
                aiStatus = 2,
            )

        val subtitle = Subtitle.fromSubtitleItem(httpItem)

        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.Assist)
    }

    @Test
    fun `fromSubtitleItem unknown type defaults to CC`() {
        val httpItem =
            dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo.SubtitleItem(
                id = 400L,
                lan = "",
                lanDoc = "",
                isLock = false,
                subtitleUrl = "",
                type = 99,
                idStr = "400",
                aiType = 99,
                aiStatus = 99,
            )

        val subtitle = Subtitle.fromSubtitleItem(httpItem)

        assertThat(subtitle.type).isEqualTo(SubtitleType.CC)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Normal)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.None)
    }

    // ------------------------------------------------------------------
    // Subtitle.fromSubtitleItem(gRPC SubtitleItem)
    // ------------------------------------------------------------------

    @Test
    fun `fromSubtitleItem gRPC maps CC type and Normal aiType and None aiStatus`() {
        val grpcItem =
            subtitleItem {
                id = 100L
                lan = "zh-Hans"
                lanDoc = "中文（简体）"
                subtitleUrl = "http://subtitle.test/100.json"
                type = GrpcSubtitleType.CC
                aiType = GrpcSubtitleAiType.Normal
                aiStatus = GrpcSubtitleAiStatus.None
            }

        val subtitle = Subtitle.fromSubtitleItem(grpcItem)

        assertThat(subtitle.id).isEqualTo(100L)
        assertThat(subtitle.lang).isEqualTo("zh-Hans")
        assertThat(subtitle.langDoc).isEqualTo("中文（简体）")
        assertThat(subtitle.url).isEqualTo("http://subtitle.test/100.json")
        assertThat(subtitle.type).isEqualTo(SubtitleType.CC)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Normal)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.None)
    }

    @Test
    fun `fromSubtitleItem gRPC maps AI type and Translate aiType and Exposure aiStatus`() {
        val grpcItem =
            subtitleItem {
                id = 200L
                lan = "ai-en"
                lanDoc = "AI English"
                subtitleUrl = "http://subtitle.test/200.json"
                type = GrpcSubtitleType.AI
                aiType = GrpcSubtitleAiType.Translate
                aiStatus = GrpcSubtitleAiStatus.Exposure
            }

        val subtitle = Subtitle.fromSubtitleItem(grpcItem)

        assertThat(subtitle.type).isEqualTo(SubtitleType.AI)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Translate)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.Exposure)
    }

    @Test
    fun `fromSubtitleItem gRPC maps Assist aiStatus`() {
        val grpcItem =
            subtitleItem {
                id = 300L
                lan = "ai-zh"
                lanDoc = "AI 中文"
                subtitleUrl = ""
                aiStatus = GrpcSubtitleAiStatus.Assist
            }

        val subtitle = Subtitle.fromSubtitleItem(grpcItem)

        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.Assist)
    }

    @Test
    fun `fromSubtitleItem gRPC defaults type to CC when not set`() {
        val grpcItem =
            subtitleItem {
                id = 400L
                lan = ""
                lanDoc = ""
                subtitleUrl = ""
            }

        val subtitle = Subtitle.fromSubtitleItem(grpcItem)

        assertThat(subtitle.type).isEqualTo(SubtitleType.CC)
        assertThat(subtitle.aiType).isEqualTo(SubtitleAiType.Normal)
        assertThat(subtitle.aiStatus).isEqualTo(SubtitleAiStatus.None)
    }
}
