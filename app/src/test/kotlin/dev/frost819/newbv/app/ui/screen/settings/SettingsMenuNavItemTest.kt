package dev.frost819.newbv.app.ui.screen.settings

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SettingsMenuNavItem] 枚举的单元测试。
 */
class SettingsMenuNavItemTest {

    @Test
    fun enum_hasSixEntries() {
        assertThat(SettingsMenuNavItem.entries).hasSize(6)
    }

    @Test
    fun enum_entriesInExpectedOrder() {
        assertThat(SettingsMenuNavItem.entries[0]).isEqualTo(SettingsMenuNavItem.AudioVideo)
        assertThat(SettingsMenuNavItem.entries[1]).isEqualTo(SettingsMenuNavItem.UI)
        assertThat(SettingsMenuNavItem.entries[2]).isEqualTo(SettingsMenuNavItem.Other)
        assertThat(SettingsMenuNavItem.entries[3]).isEqualTo(SettingsMenuNavItem.Storage)
        assertThat(SettingsMenuNavItem.entries[4]).isEqualTo(SettingsMenuNavItem.Info)
        assertThat(SettingsMenuNavItem.entries[5]).isEqualTo(SettingsMenuNavItem.About)
    }

    @Test
    fun displayNames_allNonEmpty() {
        SettingsMenuNavItem.entries.forEach { item ->
            assertThat(item.displayName).isNotEmpty()
        }
    }

    @Test
    fun displayNames_knownValues() {
        assertThat(SettingsMenuNavItem.AudioVideo.displayName).isEqualTo("音视频")
        assertThat(SettingsMenuNavItem.UI.displayName).isEqualTo("界面")
        assertThat(SettingsMenuNavItem.Other.displayName).isEqualTo("其他")
        assertThat(SettingsMenuNavItem.Storage.displayName).isEqualTo("存储")
        assertThat(SettingsMenuNavItem.Info.displayName).isEqualTo("设备信息")
        assertThat(SettingsMenuNavItem.About.displayName).isEqualTo("关于")
    }
}
