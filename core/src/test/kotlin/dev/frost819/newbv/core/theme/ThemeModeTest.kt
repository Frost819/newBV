package dev.frost819.newbv.core.theme

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [ThemeMode] 的单元测试。
 *
 * 验证主题模式解析、[ThemeMode.isDark] 逻辑与 [ThemeMode.fromOrdinal] 安全恢复。
 */
class ThemeModeTest {
    @Test
    fun `FollowSystem isDark returns system value`() {
        assertThat(ThemeMode.FollowSystem.isDark(systemIsDark = true)).isTrue()
        assertThat(ThemeMode.FollowSystem.isDark(systemIsDark = false)).isFalse()
    }

    @Test
    fun `Dark isDark always true`() {
        assertThat(ThemeMode.Dark.isDark(systemIsDark = true)).isTrue()
        assertThat(ThemeMode.Dark.isDark(systemIsDark = false)).isTrue()
    }

    @Test
    fun `Light isDark always false`() {
        assertThat(ThemeMode.Light.isDark(systemIsDark = true)).isFalse()
        assertThat(ThemeMode.Light.isDark(systemIsDark = false)).isFalse()
    }

    @Test
    fun `fromOrdinal returns correct mode for valid ordinals`() {
        assertThat(ThemeMode.fromOrdinal(0)).isEqualTo(ThemeMode.FollowSystem)
        assertThat(ThemeMode.fromOrdinal(1)).isEqualTo(ThemeMode.Dark)
        assertThat(ThemeMode.fromOrdinal(2)).isEqualTo(ThemeMode.Light)
    }

    @Test
    fun `fromOrdinal returns FollowSystem for invalid ordinal`() {
        assertThat(ThemeMode.fromOrdinal(-1)).isEqualTo(ThemeMode.FollowSystem)
        assertThat(ThemeMode.fromOrdinal(999)).isEqualTo(ThemeMode.FollowSystem)
    }

    @Test
    fun `displayName is set correctly`() {
        assertThat(ThemeMode.FollowSystem.displayName).isEqualTo("跟随系统")
        assertThat(ThemeMode.Dark.displayName).isEqualTo("深色")
        assertThat(ThemeMode.Light.displayName).isEqualTo("浅色")
    }

    @Test
    fun `entries has exactly three modes`() {
        assertThat(ThemeMode.entries).hasSize(3)
    }
}
