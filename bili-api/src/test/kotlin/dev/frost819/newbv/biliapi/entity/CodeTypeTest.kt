package dev.frost819.newbv.biliapi.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [CodeType] 的单元测试。
 *
 * 验证编解码器枚举的 codecId 映射与 gRPC 类型转换。
 */
class CodeTypeTest {
    @Test
    fun `fromCodecId returns Code264 for 7`() {
        assertThat(CodeType.fromCodecId(7)).isEqualTo(CodeType.Code264)
    }

    @Test
    fun `fromCodecId returns Code265 for 12`() {
        assertThat(CodeType.fromCodecId(12)).isEqualTo(CodeType.Code265)
    }

    @Test
    fun `fromCodecId returns CodeAv1 for 13`() {
        assertThat(CodeType.fromCodecId(13)).isEqualTo(CodeType.CodeAv1)
    }

    @Test
    fun `fromCodecId returns NoCode for 0`() {
        assertThat(CodeType.fromCodecId(0)).isEqualTo(CodeType.NoCode)
    }

    @Test
    fun `fromCodecId returns NoCode for unknown id`() {
        assertThat(CodeType.fromCodecId(999)).isEqualTo(CodeType.NoCode)
    }

    @Test
    fun `fromCodecId returns NoCode for null`() {
        assertThat(CodeType.fromCodecId(null)).isEqualTo(CodeType.NoCode)
    }

    @Test
    fun `toPlayerSharedCodeType maps all values`() {
        assertThat(CodeType.NoCode.toPlayerSharedCodeType()).isEqualTo(
            bilibili.playershared.CodeType.NOCODE,
        )
        assertThat(CodeType.Code264.toPlayerSharedCodeType()).isEqualTo(
            bilibili.playershared.CodeType.CODE264,
        )
        assertThat(CodeType.Code265.toPlayerSharedCodeType()).isEqualTo(
            bilibili.playershared.CodeType.CODE265,
        )
        assertThat(CodeType.CodeAv1.toPlayerSharedCodeType()).isEqualTo(
            bilibili.playershared.CodeType.CODEAV1,
        )
        assertThat(CodeType.Unrecognized.toPlayerSharedCodeType()).isEqualTo(
            bilibili.playershared.CodeType.UNRECOGNIZED,
        )
    }

    @Test
    fun `toPgcPlayUrlCodeType maps CodeAv1 to NoCode`() {
        assertThat(CodeType.CodeAv1.toPgcPlayUrlCodeType()).isEqualTo(
            bilibili.pgc.gateway.player.v2.CodeType.NOCODE,
        )
    }

    @Test
    fun `toPgcPlayUrlCodeType maps all values`() {
        assertThat(CodeType.NoCode.toPgcPlayUrlCodeType()).isEqualTo(
            bilibili.pgc.gateway.player.v2.CodeType.NOCODE,
        )
        assertThat(CodeType.Code264.toPgcPlayUrlCodeType()).isEqualTo(
            bilibili.pgc.gateway.player.v2.CodeType.CODE264,
        )
        assertThat(CodeType.Code265.toPgcPlayUrlCodeType()).isEqualTo(
            bilibili.pgc.gateway.player.v2.CodeType.CODE265,
        )
        assertThat(CodeType.Unrecognized.toPgcPlayUrlCodeType()).isEqualTo(
            bilibili.pgc.gateway.player.v2.CodeType.UNRECOGNIZED,
        )
    }

    @Test
    fun `str and codecId properties have correct values`() {
        assertThat(CodeType.Code264.str).isEqualTo("avc1")
        assertThat(CodeType.Code264.codecId).isEqualTo(7)
        assertThat(CodeType.Code265.str).isEqualTo("hev1")
        assertThat(CodeType.Code265.codecId).isEqualTo(12)
        assertThat(CodeType.CodeAv1.str).isEqualTo("av01")
        assertThat(CodeType.CodeAv1.codecId).isEqualTo(13)
    }
}
