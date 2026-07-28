package dev.frost819.newbv.app.data

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
import java.util.Date

/**
 * [AuthData] 的单元测试。
 *
 * 验证 JSON 序列化/反序列化与 Prefs 双向同步。
 * Prefs 初始化一次，每个测试前 clear 重置。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthDataTest {

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_authdata", ".preferences_pb")
            file.deleteOnExit()
            testDataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized to avoid async write exceptions
        }
    }

    @BeforeEach
    fun clearPrefs() {
        runBlocking { Prefs.clear() }
    }

    @Test
    fun `toJson and fromJson roundtrip preserves all fields`() {
        val original = AuthData(
            uid = 12345L,
            uidCkMd5 = "ckmd5",
            sid = "sid-123",
            biliJct = "jct-token",
            sessData = "sess-data",
            tokenExpiredDate = 1700000000000L,
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )

        val json = original.toJson()
        val restored = AuthData.fromJson(json)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `toJson produces valid JSON with all fields`() {
        val authData = AuthData(
            uid = 100L,
            uidCkMd5 = "md5",
            sid = "sid",
            biliJct = "jct",
            sessData = "sess",
            tokenExpiredDate = 1700000000000L,
        )

        val json = authData.toJson()

        assertThat(json).contains("\"uid\":100")
        assertThat(json).contains("\"sessData\":\"sess\"")
        assertThat(json).contains("\"biliJct\":\"jct\"")
    }

    @Test
    fun `fromJson handles default empty accessToken and refreshToken`() {
        val json = """{"uid":1,"uidCkMd5":"m","sid":"s","biliJct":"j","sessData":"d","tokenExpiredDate":1700000000000}"""

        val authData = AuthData.fromJson(json)

        assertThat(authData.accessToken).isEmpty()
        assertThat(authData.refreshToken).isEmpty()
    }

    @Test
    fun `saveToPrefs writes all fields to Prefs`() {
        val authData = AuthData(
            uid = 999L,
            uidCkMd5 = "ckmd5",
            sid = "mysid",
            biliJct = "myjct",
            sessData = "mysess",
            tokenExpiredDate = 1700000000000L,
            accessToken = "mytoken",
            refreshToken = "myrefresh",
        )

        authData.saveToPrefs()

        assertThat(Prefs.isLogin).isTrue()
        assertThat(Prefs.uid).isEqualTo(999L)
        assertThat(Prefs.uidCkMd5).isEqualTo("ckmd5")
        assertThat(Prefs.sid).isEqualTo("mysid")
        assertThat(Prefs.biliJct).isEqualTo("myjct")
        assertThat(Prefs.sessData).isEqualTo("mysess")
        assertThat(Prefs.accessToken).isEqualTo("mytoken")
        assertThat(Prefs.refreshToken).isEqualTo("myrefresh")
        assertThat(Prefs.tokenExpiredDate.time).isEqualTo(1700000000000L)
    }

    @Test
    fun `fromPrefs reads all fields from Prefs`() {
        Prefs.uid = 555L
        Prefs.uidCkMd5 = "md5hash"
        Prefs.sid = "psid"
        Prefs.biliJct = "pjct"
        Prefs.sessData = "psess"
        Prefs.tokenExpiredDate = Date(1800000000000L)
        Prefs.accessToken = "ptoken"
        Prefs.refreshToken = "prefresh"

        val authData = AuthData.fromPrefs()

        assertThat(authData.uid).isEqualTo(555L)
        assertThat(authData.uidCkMd5).isEqualTo("md5hash")
        assertThat(authData.sid).isEqualTo("psid")
        assertThat(authData.biliJct).isEqualTo("pjct")
        assertThat(authData.sessData).isEqualTo("psess")
        assertThat(authData.tokenExpiredDate).isEqualTo(1800000000000L)
        assertThat(authData.accessToken).isEqualTo("ptoken")
        assertThat(authData.refreshToken).isEqualTo("prefresh")
    }

    @Test
    fun `saveToPrefs and fromPrefs roundtrip`() {
        val original = AuthData(
            uid = 777L,
            uidCkMd5 = "roundtrip",
            sid = "rsid",
            biliJct = "rjct",
            sessData = "rsess",
            tokenExpiredDate = 1900000000000L,
            accessToken = "rtoken",
            refreshToken = "rrefresh",
        )

        original.saveToPrefs()
        val restored = AuthData.fromPrefs()

        assertThat(restored).isEqualTo(original)
    }
}
