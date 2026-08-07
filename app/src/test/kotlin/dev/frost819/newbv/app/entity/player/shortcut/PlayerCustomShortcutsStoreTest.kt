package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

/**
 * [PlayerCustomShortcutsStore] 的单元测试。
 *
 * 验证 CRUD 操作通过 Prefs 持久化的正确性。
 * Prefs 初始化一次，每个测试前 clear 重置。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerCustomShortcutsStoreTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_shortcuts_store", ".preferences_pb")
            file.deleteOnExit()
            val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
        }
    }

    @BeforeEach
    fun clearPrefs() {
        runBlocking { Prefs.clear() }
    }

    @AfterEach
    fun clearStore() {
        PlayerCustomShortcutsStore.clear()
    }

    @Test
    fun `get returns empty list when no shortcuts saved`() {
        assertThat(PlayerCustomShortcutsStore.get()).isEmpty()
    }

    @Test
    fun `save persists shortcuts and returns normalized list`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_DPAD_UP, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_DPAD_DOWN, PlayerCustomShortcutAction.ToggleDanmaku),
        )

        val saved = PlayerCustomShortcutsStore.save(shortcuts)

        assertThat(saved).hasSize(2)
        assertThat(Prefs.playerCustomShortcuts).isNotEmpty()
    }

    @Test
    fun `get returns saved shortcuts after save`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleLoop),
        )

        PlayerCustomShortcutsStore.save(shortcuts)
        val retrieved = PlayerCustomShortcutsStore.get()

        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].keyCode).isEqualTo(KeyEvent.KEYCODE_1)
        assertThat(retrieved[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleLoop)
    }

    @Test
    fun `getByKey returns map keyed by keyCode`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.ToggleDanmaku),
        )

        PlayerCustomShortcutsStore.save(shortcuts)
        val map = PlayerCustomShortcutsStore.getByKey()

        assertThat(map).hasSize(2)
        assertThat(map[KeyEvent.KEYCODE_1]?.action).isEqualTo(PlayerCustomShortcutAction.TogglePlayPause)
        assertThat(map[KeyEvent.KEYCODE_2]?.action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `upsert adds new shortcut`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)

        val shortcuts = PlayerCustomShortcutsStore.get()
        assertThat(shortcuts).hasSize(1)
        assertThat(shortcuts[0].keyCode).isEqualTo(KeyEvent.KEYCODE_1)
        assertThat(shortcuts[0].action).isEqualTo(PlayerCustomShortcutAction.TogglePlayPause)
    }

    @Test
    fun `upsert updates existing shortcut with same keyCode`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleDanmaku)

        val shortcuts = PlayerCustomShortcutsStore.get()
        assertThat(shortcuts).hasSize(1)
        assertThat(shortcuts[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `upsert preserves other shortcuts when updating one`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.ToggleDanmaku)
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleLoop)

        val shortcuts = PlayerCustomShortcutsStore.get()
        assertThat(shortcuts).hasSize(2)
        val key1 = shortcuts.find { it.keyCode == KeyEvent.KEYCODE_1 }
        assertThat(key1?.action).isEqualTo(PlayerCustomShortcutAction.ToggleLoop)
        val key2 = shortcuts.find { it.keyCode == KeyEvent.KEYCODE_2 }
        assertThat(key2?.action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }

    @Test
    fun `remove deletes shortcut by keyCode`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.ToggleDanmaku)

        PlayerCustomShortcutsStore.remove(KeyEvent.KEYCODE_1)

        val shortcuts = PlayerCustomShortcutsStore.get()
        assertThat(shortcuts).hasSize(1)
        assertThat(shortcuts[0].keyCode).isEqualTo(KeyEvent.KEYCODE_2)
    }

    @Test
    fun `remove non-existent keyCode does not throw`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)

        val result = PlayerCustomShortcutsStore.remove(KeyEvent.KEYCODE_0)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `clear removes all shortcuts`() {
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause)
        PlayerCustomShortcutsStore.upsert(KeyEvent.KEYCODE_2, PlayerCustomShortcutAction.ToggleDanmaku)

        val result = PlayerCustomShortcutsStore.clear()

        assertThat(result).isEmpty()
        assertThat(PlayerCustomShortcutsStore.get()).isEmpty()
    }

    @Test
    fun `save filters forbidden keyCodes`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_BACK, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleLoop),
        )

        val saved = PlayerCustomShortcutsStore.save(shortcuts)

        assertThat(saved).hasSize(1)
        assertThat(saved[0].keyCode).isEqualTo(KeyEvent.KEYCODE_1)
    }

    @Test
    fun `save deduplicates by keyCode keeping last`() {
        val shortcuts = listOf(
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.TogglePlayPause),
            PlayerCustomShortcut(KeyEvent.KEYCODE_1, PlayerCustomShortcutAction.ToggleDanmaku),
        )

        val saved = PlayerCustomShortcutsStore.save(shortcuts)

        assertThat(saved).hasSize(1)
        assertThat(saved[0].action).isEqualTo(PlayerCustomShortcutAction.ToggleDanmaku)
    }
}
