package dev.frost819.newbv.app.network

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SponsorBlockApi] 的单元测试。
 *
 * 验证 SHA-256 哈希前缀计算与 SponsorBlock 接口协议对齐。
 */
class SponsorBlockApiTest {
    @Test
    fun `sha256Prefix returns 4 lowercase hex characters matching SHA-256 digest`() {
        assertThat(SponsorBlockApi.sha256Prefix("BV1GJ411x7h7")).isEqualTo("d5c2")
    }

    @Test
    fun `sha256Prefix always produces 4 lowercase hex chars`() {
        val prefix = SponsorBlockApi.sha256Prefix("BV1xx411c7mD")

        assertThat(prefix).hasLength(4)
        assertThat(prefix).matches("[0-9a-f]{4}")
    }
}
