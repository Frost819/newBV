package dev.frost819.newbv.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

/**
 * [PrefDelegate] 与 [Prefs] 辅助方法的单元测试。
 *
 * 测试 PrefDelegate 的读写、自定义 save/restore 映射、resetToDefault，
 * 以及 Prefs 的 flow 属性（themeModeFlow / densityFlow）
 * 和错误处理路径（未初始化时访问 dataStore / clear）。
 */
class PrefDelegateTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    private val dummy: String = ""

    @BeforeEach
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory(prefix = "delegate_test").toFile()
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(tempDir, "Test.preferences_pb") }
        )
        Prefs.resetForTesting()
        Prefs.init(dataStore)
    }

    @AfterEach
    fun tearDown() {
        Prefs.resetForTesting()
        tempDir.deleteRecursively()
    }

    // ===== PrefDelegate getValue / setValue =====

    @Test
    fun `getValue returns default when no value set`() {
        val key = stringPreferencesKey("test_str")
        val delegate = PrefDelegate(key, "default_value")
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo("default_value")
    }

    @Test
    fun `getValue returns value after setValue`() {
        val key = stringPreferencesKey("test_str2")
        val delegate = PrefDelegate(key, "default")
        delegate.setValue(null, ::dummy, "new_value")
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo("new_value")
    }

    @Test
    fun `setValue updates flow synchronously`() {
        val key = intPreferencesKey("test_int")
        val delegate = PrefDelegate(key, 0)
        delegate.setValue(null, ::dummy, 42)
        assertThat(delegate.flow.value).isEqualTo(42)
    }

    @Test
    fun `resetToDefault restores default value`() {
        val key = stringPreferencesKey("test_reset")
        val delegate = PrefDelegate(key, "default")
        delegate.setValue(null, ::dummy, "changed")
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo("changed")
        delegate.resetToDefault()
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo("default")
    }

    @Test
    fun `custom save and restore round trips correctly`() {
        val key = intPreferencesKey("test_enum_delegate")
        val delegate = PrefDelegate<ThemeMode, Int>(
            key = key,
            defaultValue = ThemeMode.FollowSystem,
            save = { it.ordinal },
            restore = { ThemeMode.fromOrdinal(it) }
        )
        delegate.setValue(null, ::dummy, ThemeMode.Dark)
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo(ThemeMode.Dark)
        assertThat(delegate.flow.value).isEqualTo(1)
    }

    @Test
    fun `custom save and restore with boolean type`() {
        val key = booleanPreferencesKey("test_bool_delegate")
        val delegate = PrefDelegate<Boolean, Boolean>(key, false)
        delegate.setValue(null, ::dummy, true)
        assertThat(delegate.getValue(null, ::dummy)).isTrue()
    }

    @Test
    fun `resetToDefault with custom save restores default persisted form`() {
        val key = intPreferencesKey("test_reset_custom")
        val delegate = PrefDelegate<ThemeMode, Int>(
            key = key,
            defaultValue = ThemeMode.Light,
            save = { it.ordinal },
            restore = { ThemeMode.fromOrdinal(it) }
        )
        delegate.setValue(null, ::dummy, ThemeMode.Dark)
        delegate.resetToDefault()
        assertThat(delegate.flow.value).isEqualTo(2)
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo(ThemeMode.Light)
    }

    // ===== Prefs flow 属性 =====

    @Test
    fun `themeModeFlow returns StateFlow with default value`() {
        val flow = Prefs.themeModeFlow
        assertThat(flow.value).isEqualTo(ThemeMode.FollowSystem)
    }

    @Test
    fun `themeModeFlow reflects changes`() = runBlocking {
        val flow = Prefs.themeModeFlow
        assertThat(flow.value).isEqualTo(ThemeMode.FollowSystem)

        Prefs.themeMode = ThemeMode.Dark
        delay(200)

        assertThat(flow.value).isEqualTo(ThemeMode.Dark)
    }

    @Test
    fun `densityFlow returns StateFlow with default value`() {
        val flow = Prefs.densityFlow
        assertThat(flow.value).isEqualTo(2f)
    }

    @Test
    fun `densityFlow reflects changes`() = runBlocking {
        val flow = Prefs.densityFlow
        assertThat(flow.value).isEqualTo(2f)

        Prefs.density = 3.5f
        delay(200)

        assertThat(flow.value).isEqualTo(3.5f)
    }

    // ===== Prefs 错误处理 =====

    @Test
    fun `dataStore throws when Prefs not initialized`() {
        Prefs.resetForTesting()
        assertThrows<IllegalStateException> {
            Prefs.dataStore
        }
        Prefs.init(dataStore)
    }

    @Test
    fun `clear throws when Prefs not initialized`() {
        Prefs.resetForTesting()
        assertThrows<IllegalStateException> {
            runBlocking { Prefs.clear() }
        }
        Prefs.init(dataStore)
    }

    @Test
    fun `launchPersist executes block`() = runBlocking {
        var executed = false
        Prefs.launchPersist { executed = true }
        delay(200)
        assertThat(executed).isTrue()
    }

    // ===== 覆盖全部 Prefs setter =====

    @Test
    fun `write to all remaining prefs covers setters`() = runBlocking {
        Prefs.sid = "test_sid"
        Prefs.uidCkMd5 = "test_md5"
        Prefs.accessToken = "test_token"
        Prefs.refreshToken = "test_refresh"
        Prefs.buvid3FromSpi = true
        Prefs.deviceCookies = "test_cookies"
        Prefs.crashReportEnabled = true
        Prefs.enableSoftwareVideoDecoder = true
        Prefs.enableFfmpegAudioRenderer = true
        Prefs.defaultDanmakuScale = 2.0f
        Prefs.defaultDanmakuOpacity = 0.5f
        Prefs.defaultDanmakuSpeedFactor = 2.0f
        Prefs.defaultDanmakuArea = 0.8f
        Prefs.defaultDanmakuMask = true
        Prefs.danmakuLevelFilter = 3
        Prefs.defaultSubtitleFontSize = 36
        Prefs.defaultSubtitleBackgroundOpacity = 0.6f
        Prefs.defaultSubtitleBottomPadding = 20
        Prefs.showVideoInfo = false
        Prefs.showPersistentSeek = true
        Prefs.showPlayerDebugInfo = true
        Prefs.firstHomeTopNavItem = HomeTopNavItem.Recommend
        Prefs.firstPersonalTopNavItem = PersonalTopNavItem.History
        Prefs.showHotword = false
        Prefs.cacheThreshold = 300
        Prefs.playerCustomShortcuts = "[{\"key\":\"test\"}]"

        delay(200)

        assertThat(Prefs.sid).isEqualTo("test_sid")
        assertThat(Prefs.enableSoftwareVideoDecoder).isTrue()
        assertThat(Prefs.defaultDanmakuScale).isEqualTo(2.0f)
        assertThat(Prefs.showPlayerDebugInfo).isTrue()
        assertThat(Prefs.firstHomeTopNavItem).isEqualTo(HomeTopNavItem.Recommend)
        assertThat(Prefs.defaultDanmakuSpeedFactor).isEqualTo(2.0f)
    }

    // ===== buvid3 preserved =====

    @Test
    fun `custom buvid3 is preserved across reinit`() = runBlocking {
        Prefs.buvid3 = "custom_buvid3_value"
        delay(200)

        Prefs.resetForTesting()
        Prefs.init(dataStore)

        assertThat(Prefs.buvid3).isEqualTo("custom_buvid3_value")
    }

    // ===== PrefDelegate null flow value =====

    @Test
    fun `getValue returns default when flow value is null`() {
        val key = stringPreferencesKey("test_null_flow")
        val delegate = PrefDelegate(key, "default")
        delegate.flow.value = null
        assertThat(delegate.getValue(null, ::dummy)).isEqualTo("default")
    }
}
