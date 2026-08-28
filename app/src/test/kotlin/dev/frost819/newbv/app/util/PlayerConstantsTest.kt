package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import org.junit.jupiter.api.Test

/**
 * [PlayerConstants] 的单元测试。
 *
 * 验证 User-Agent 和 Referer 按接口类型正确返回。
 */
class PlayerConstantsTest {
    @Test
    fun `getUserAgent returns web UA for Web type`() {
        val ua = PlayerConstants.getUserAgent(ApiType.Web)
        assertThat(ua).isEqualTo(PlayerConstants.WEB_USER_AGENT)
        assertThat(ua).contains("Chrome")
    }

    @Test
    fun `getUserAgent returns app UA for App type`() {
        val ua = PlayerConstants.getUserAgent(ApiType.App)
        assertThat(ua).isEqualTo(PlayerConstants.APP_USER_AGENT)
        assertThat(ua).contains("Bilibili")
    }

    @Test
    fun `getReferer returns web referer for Web type`() {
        val referer = PlayerConstants.getReferer(ApiType.Web)
        assertThat(referer).isEqualTo(PlayerConstants.WEB_REFERER)
        assertThat(referer).contains("bilibili.com")
    }

    @Test
    fun `getReferer returns null for App type`() {
        val referer = PlayerConstants.getReferer(ApiType.App)
        assertThat(referer).isNull()
    }

    @Test
    fun `seek constants are positive`() {
        assertThat(PlayerConstants.SEEK_BASE_INCREMENT_MS).isGreaterThan(0L)
        assertThat(PlayerConstants.SEEK_STEP_INCREMENT_MS).isGreaterThan(0L)
        assertThat(PlayerConstants.SEEK_ACCELERATION_WINDOW_MS).isGreaterThan(0L)
    }

    @Test
    fun `heartbeat constants are positive`() {
        assertThat(PlayerConstants.HEARTBEAT_INTERVAL_MS).isGreaterThan(0L)
        assertThat(PlayerConstants.HEARTBEAT_INITIAL_DELAY_MS).isGreaterThan(0L)
    }

    @Test
    fun `countdown and auto hide constants are positive`() {
        assertThat(PlayerConstants.COUNTDOWN_DURATION_MS).isGreaterThan(0L)
        assertThat(PlayerConstants.CONTROLLER_AUTO_HIDE_MS).isGreaterThan(0L)
        assertThat(PlayerConstants.BACK_EXIT_WINDOW_MS).isGreaterThan(0L)
    }
}
