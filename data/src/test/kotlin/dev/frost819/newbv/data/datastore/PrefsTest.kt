package dev.frost819.newbv.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * [Prefs] 的单元测试。
 *
 * 验证偏好设置的默认值、读写、枚举映射、buvid 自动生成。
 * 使用临时文件创建真实 DataStore，不依赖 Android 环境。
 */
class PrefsTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory(prefix = "prefs_test").toFile()
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

    // ===== 默认值测试 =====

    @Test
    fun `default isLogin is false`() {
        assertThat(Prefs.isLogin).isFalse()
    }

    @Test
    fun `default uid is zero`() {
        assertThat(Prefs.uid).isEqualTo(0L)
    }

    @Test
    fun `default sessData is empty`() {
        assertThat(Prefs.sessData).isEmpty()
    }

    @Test
    fun `default biliJct is empty`() {
        assertThat(Prefs.biliJct).isEmpty()
    }

    @Test
    fun `default incognitoMode is false`() {
        assertThat(Prefs.incognitoMode).isFalse()
    }

    @Test
    fun `default apiType is Web`() {
        assertThat(Prefs.apiType).isEqualTo(ApiType.Web)
    }

    @Test
    fun `default quality is R1080P`() {
        assertThat(Prefs.defaultQuality).isEqualTo(Resolution.R1080P)
    }

    @Test
    fun `default videoCodec is AVC`() {
        assertThat(Prefs.defaultVideoCodec).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `default audio is A192K`() {
        assertThat(Prefs.defaultAudio).isEqualTo(Audio.A192K)
    }

    @Test
    fun `default actionAfterPlay is PlayNext`() {
        assertThat(Prefs.actionAfterPlay).isEqualTo(ActionAfterPlay.PlayNext)
    }

    @Test
    fun `default playSpeed is X1`() {
        assertThat(Prefs.defaultPlaySpeed).isEqualTo(PlaySpeed.X1)
    }

    @Test
    fun `default danmakuScale is 1_75`() {
        assertThat(Prefs.defaultDanmakuScale).isEqualTo(1.75f)
    }

    @Test
    fun `default danmakuOpacity is 0_7`() {
        assertThat(Prefs.defaultDanmakuOpacity).isEqualTo(0.7f)
    }

    @Test
    fun `default danmakuArea is 0_5`() {
        assertThat(Prefs.defaultDanmakuArea).isEqualTo(0.5f)
    }

    @Test
    fun `default danmakuMask is false`() {
        assertThat(Prefs.defaultDanmakuMask).isFalse()
    }

    @Test
    fun `default subtitleFontSize is 24`() {
        assertThat(Prefs.defaultSubtitleFontSize).isEqualTo(24)
    }

    @Test
    fun `default subtitleBackgroundOpacity is 0_4`() {
        assertThat(Prefs.defaultSubtitleBackgroundOpacity).isEqualTo(0.4f)
    }

    @Test
    fun `default subtitleBottomPadding is 12`() {
        assertThat(Prefs.defaultSubtitleBottomPadding).isEqualTo(12)
    }

    @Test
    fun `default showVideoInfo is true`() {
        assertThat(Prefs.showVideoInfo).isTrue()
    }

    @Test
    fun `default showPersistentSeek is false`() {
        assertThat(Prefs.showPersistentSeek).isFalse()
    }

    @Test
    fun `default density is 2f`() {
        assertThat(Prefs.density).isEqualTo(2f)
    }

    @Test
    fun `default homeLeftNavItem is Home`() {
        assertThat(Prefs.homeLeftNavItem).isEqualTo(LeftNaviItem.Home)
    }

    @Test
    fun `default firstHomeTopNavItem is Dynamics`() {
        assertThat(Prefs.firstHomeTopNavItem).isEqualTo(HomeTopNavItem.Dynamics)
    }

    @Test
    fun `default firstPersonalTopNavItem is ToView`() {
        assertThat(Prefs.firstPersonalTopNavItem).isEqualTo(PersonalTopNavItem.ToView)
    }

    @Test
    fun `default showHotword is true`() {
        assertThat(Prefs.showHotword).isTrue()
    }

    @Test
    fun `default themeMode is FollowSystem`() {
        assertThat(Prefs.themeMode).isEqualTo(ThemeMode.FollowSystem)
    }

    @Test
    fun `default cacheThreshold is 0`() {
        assertThat(Prefs.cacheThreshold).isEqualTo(0)
    }

    @Test
    fun `default cacheAutoClean is true`() {
        assertThat(Prefs.cacheAutoClean).isTrue()
    }

    @Test
    fun `default crashReportEnabled is false`() {
        assertThat(Prefs.crashReportEnabled).isFalse()
    }

    @Test
    fun `default enableSoftwareVideoDecoder is false`() {
        assertThat(Prefs.enableSoftwareVideoDecoder).isFalse()
    }

    @Test
    fun `default enableFfmpegAudioRenderer is false`() {
        assertThat(Prefs.enableFfmpegAudioRenderer).isFalse()
    }

    @Test
    fun `default playerCustomShortcuts is empty`() {
        assertThat(Prefs.playerCustomShortcuts).isEmpty()
    }

    @Test
    fun `default tokenExpiredDate is epoch`() {
        assertThat(Prefs.tokenExpiredDate.time).isEqualTo(0L)
    }

    @Test
    fun `default danmakuTypes contains All Rolling Top Bottom`() {
        assertThat(Prefs.defaultDanmakuTypes).containsExactly(
            DanmakuType.All,
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom
        )
    }

    // ===== 读写测试 =====

    @Test
    fun `write and read basic types`() = runBlocking {
        Prefs.isLogin = true
        Prefs.uid = 12345L
        Prefs.sessData = "test_sessdata"
        Prefs.biliJct = "test_bili_jct"
        Prefs.incognitoMode = true
        Prefs.density = 2.5f
        Prefs.cacheThreshold = 1000

        awaitAsyncWrite()

        assertThat(Prefs.isLogin).isTrue()
        assertThat(Prefs.uid).isEqualTo(12345L)
        assertThat(Prefs.sessData).isEqualTo("test_sessdata")
        assertThat(Prefs.biliJct).isEqualTo("test_bili_jct")
        assertThat(Prefs.incognitoMode).isTrue()
        assertThat(Prefs.density).isEqualTo(2.5f)
        assertThat(Prefs.cacheThreshold).isEqualTo(1000)
    }

    @Test
    fun `write and read enum types`() = runBlocking {
        Prefs.apiType = ApiType.App
        Prefs.defaultQuality = Resolution.R4K
        Prefs.defaultVideoCodec = VideoCodec.HEVC
        Prefs.defaultAudio = Audio.AHiRes
        Prefs.actionAfterPlay = ActionAfterPlay.Exit
        Prefs.defaultPlaySpeed = PlaySpeed.X2
        Prefs.themeMode = ThemeMode.Dark
        Prefs.homeLeftNavItem = LeftNaviItem.Search

        awaitAsyncWrite()

        assertThat(Prefs.apiType).isEqualTo(ApiType.App)
        assertThat(Prefs.defaultQuality).isEqualTo(Resolution.R4K)
        assertThat(Prefs.defaultVideoCodec).isEqualTo(VideoCodec.HEVC)
        assertThat(Prefs.defaultAudio).isEqualTo(Audio.AHiRes)
        assertThat(Prefs.actionAfterPlay).isEqualTo(ActionAfterPlay.Exit)
        assertThat(Prefs.defaultPlaySpeed).isEqualTo(PlaySpeed.X2)
        assertThat(Prefs.themeMode).isEqualTo(ThemeMode.Dark)
        assertThat(Prefs.homeLeftNavItem).isEqualTo(LeftNaviItem.Search)
    }

    @Test
    fun `write and read date type`() = runBlocking {
        val testDate = java.util.Date(1700000000000L)
        Prefs.tokenExpiredDate = testDate

        awaitAsyncWrite()

        assertThat(Prefs.tokenExpiredDate.time).isEqualTo(1700000000000L)
    }

    @Test
    fun `write and read danmaku types list`() = runBlocking {
        val types = listOf(DanmakuType.Top, DanmakuType.Bottom)
        Prefs.defaultDanmakuTypes = types

        awaitAsyncWrite()

        assertThat(Prefs.defaultDanmakuTypes).containsExactly(DanmakuType.Top, DanmakuType.Bottom)
    }

    @Test
    fun `write persists to DataStore and survives reinit`() = runBlocking {
        Prefs.uid = 99999L
        Prefs.isLogin = true
        awaitAsyncWrite()

        // 重置并重新初始化（模拟应用重启）
        Prefs.resetForTesting()
        Prefs.init(dataStore)

        assertThat(Prefs.uid).isEqualTo(99999L)
        assertThat(Prefs.isLogin).isTrue()
    }

    // ===== buvid 自动生成测试 =====

    @Test
    fun `buvid is auto generated when missing`() {
        val buvid = Prefs.buvid
        assertThat(buvid).startsWith("XY")
        assertThat(buvid.length).isEqualTo(37)
    }

    @Test
    fun `buvid3 is auto generated when missing`() {
        val buvid3 = Prefs.buvid3
        assertThat(buvid3).endsWith("infoc")
    }

    @Test
    fun `custom buvid is preserved across reinit`() = runBlocking {
        Prefs.buvid = "custom_buvid_value"
        awaitAsyncWrite()

        Prefs.resetForTesting()
        Prefs.init(dataStore)

        assertThat(Prefs.buvid).isEqualTo("custom_buvid_value")
    }

    // ===== init 约束测试 =====

    @Test
    fun `init throws when called twice without reset`() = runBlocking {
        try {
            Prefs.init(dataStore)
            assert(false) { "应该抛出 IllegalStateException" }
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("已经初始化")
        }
    }

    @Test
    fun `clear resets all prefs to defaults`() = runBlocking {
        Prefs.isLogin = true
        Prefs.uid = 999L
        Prefs.density = 5f
        awaitAsyncWrite()

        Prefs.clear()
        awaitAsyncWrite()

        assertThat(Prefs.isLogin).isFalse()
        assertThat(Prefs.uid).isEqualTo(0L)
        assertThat(Prefs.density).isEqualTo(2f)
    }

    // ===== flowOf 测试 =====

    @Test
    fun `flowOf returns StateFlow for existing key`() {
        val flow = Prefs.flowOf(PrefKeys.isLogin)
        assertThat(flow).isNotNull()
    }

    @Test
    fun `flowOf returns null for unregistered key`() {
        val unregisteredKey = androidx.datastore.preferences.core.booleanPreferencesKey("nonexistent_key")
        val flow = Prefs.flowOf(unregisteredKey)
        assertThat(flow).isNull()
    }

    // ===== 枚举安全解析测试 =====

    @Test
    fun `Resolution fromCode returns default for invalid`() {
        assertThat(Resolution.fromCode(99999)).isEqualTo(Resolution.R1080P)
    }

    @Test
    fun `VideoCodec fromCode returns default for invalid`() {
        assertThat(VideoCodec.fromCode(99999)).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `Audio fromCode returns default for invalid`() {
        assertThat(Audio.fromCode(99999)).isEqualTo(Audio.A192K)
    }

    @Test
    fun `ActionAfterPlay fromCode returns default for invalid`() {
        assertThat(ActionAfterPlay.fromCode(99999)).isEqualTo(ActionAfterPlay.PlayNext)
    }

    @Test
    fun `PlaySpeed fromCode returns default for invalid`() {
        assertThat(PlaySpeed.fromCode(99999)).isEqualTo(PlaySpeed.X1)
    }

    @Test
    fun `ApiType fromOrdinal returns default for invalid`() {
        assertThat(ApiType.fromOrdinal(99999)).isEqualTo(ApiType.Web)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns default for invalid`() {
        assertThat(LeftNaviItem.fromOrdinal(99999)).isEqualTo(LeftNaviItem.Home)
    }

    @Test
    fun `PersonalTopNavItem fromOrdinal returns default for invalid`() {
        assertThat(PersonalTopNavItem.fromOrdinal(99999)).isEqualTo(PersonalTopNavItem.ToView)
    }

    @Test
    fun `ThemeMode fromOrdinal returns default for invalid`() {
        assertThat(ThemeMode.fromOrdinal(99999)).isEqualTo(ThemeMode.FollowSystem)
    }

    @Test
    fun `VideoCodec fromCodecString matches by prefix`() {
        assertThat(VideoCodec.fromCodecString("avc1.640028")).isEqualTo(VideoCodec.AVC)
        assertThat(VideoCodec.fromCodecString("hev1.1.6.L120.B0")).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `VideoCodec fromCodecString returns null for unknown`() {
        assertThat(VideoCodec.fromCodecString("unknown")).isNull()
    }

    @Test
    fun `VideoCodec fromCodecId returns AVC for unknown`() {
        assertThat(VideoCodec.fromCodecId(999)).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `PlaySpeed fromSpeed matches correctly`() {
        assertThat(PlaySpeed.fromSpeed(1f)).isEqualTo(PlaySpeed.X1)
        assertThat(PlaySpeed.fromSpeed(2f)).isEqualTo(PlaySpeed.X2)
        assertThat(PlaySpeed.fromSpeed(0.5f)).isEqualTo(PlaySpeed.X0_5)
    }

    @Test
    fun `PlaySpeed fromSpeed returns X1 for invalid`() {
        assertThat(PlaySpeed.fromSpeed(999f)).isEqualTo(PlaySpeed.X1)
    }

    @Test
    fun `HomeTopNavItem fromCode handles invalid`() {
        assertThat(HomeTopNavItem.fromCode(999)).isEqualTo(HomeTopNavItem.Dynamics)
    }

    /**
     * 等待异步写入完成。
     *
     * Prefs 的写入是异步的（launchPersist 在 IO 协程执行），
     * 需要短暂等待确保 DataStore 持久化完成。
     */
    private suspend fun awaitAsyncWrite() {
        delay(200)
    }
}
