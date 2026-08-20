package dev.frost819.newbv.core.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * [CrashUploader] 的插桩测试。
 *
 * 验证 JSON 文件创建、上传开关逻辑、未发送日志扫描。
 * HTTP 上传部分因需真实 Worker 端点不纳入断言（上传失败静默忽略）。
 */
@RunWith(AndroidJUnit4::class)
class CrashUploaderTest {

    private lateinit var context: Context
    private lateinit var logDir: File
    private lateinit var uploader: CrashUploader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        logDir = File(context.filesDir, CrashHandler.LOG_DIR)
        logDir.deleteRecursively()
        logDir.mkdirs()

        uploader = CrashUploader(context, authToken = "test-token")
    }

    @After
    fun teardown() {
        logDir.deleteRecursively()
    }

    @Test
    fun canUpload_returns_false_when_disabled() {
        uploader.enabled = false
        assertThat(uploader.canUpload()).isFalse()
    }

    @Test
    fun canUpload_returns_false_when_token_blank() {
        uploader.enabled = true
        val blankTokenUploader = CrashUploader(context, authToken = "")
        blankTokenUploader.enabled = true
        assertThat(blankTokenUploader.canUpload()).isFalse()
    }

    @Test
    fun canUpload_returns_true_when_enabled_and_token_present() {
        uploader.enabled = true
        assertThat(uploader.canUpload()).isTrue()
    }

    @Test
    fun uploadCrash_is_noop_when_disabled() {
        uploader.enabled = false
        val deviceInfo = DeviceInfo(
            appVersion = "1.0",
            appVersionCode = 1,
            androidVersion = "12",
            androidSdk = 31,
            device = "test",
            model = "TestModel",
            manufacturer = "TestManufacturer",
        )

        uploader.uploadCrash(deviceInfo, Thread.currentThread(), RuntimeException("test"), "logcat")

        val jsonFiles = logDir.listFiles { it.name.endsWith(".json") }
        assertThat(jsonFiles.isNullOrEmpty()).isTrue()
    }

    @Test
    fun uploadCrash_saves_json_file_when_enabled() {
        uploader.enabled = true
        val deviceInfo = DeviceInfo(
            appVersion = "1.0",
            appVersionCode = 42,
            androidVersion = "12",
            androidSdk = 31,
            device = "test",
            model = "TestModel",
            manufacturer = "TestManufacturer",
        )

        uploader.uploadCrash(
            deviceInfo,
            Thread.currentThread(),
            RuntimeException("test crash"),
            "fake logcat output",
        )

        val jsonFiles = logDir.listFiles { it.name.endsWith(".json") }
        assertThat(jsonFiles).isNotNull()
        assertThat(jsonFiles!!.size).isAtLeast(1)

        val jsonContent = jsonFiles.first().readText()
        assertThat(jsonContent).contains("\"versionName\":\"1.0\"")
        assertThat(jsonContent).contains("\"manufacturer\":\"TestManufacturer\"")
        assertThat(jsonContent).contains("\"model\":\"TestModel\"")
        assertThat(jsonContent).contains("\"type\":\"java.lang.RuntimeException\"")
        assertThat(jsonContent).contains("\"message\":\"test crash\"")
        assertThat(jsonContent).contains("fake logcat output")
    }

    @Test
    fun uploadPendingCrashLogs_is_noop_when_disabled() {
        uploader.enabled = false

        val jsonFile = File(logDir, "logs_crash_2024-01-01_00:00:00.json")
        jsonFile.writeText("""{"id":"test","versionCode":1}""")

        uploader.uploadPendingCrashLogs()

        assertThat(jsonFile.exists()).isTrue()
    }

    @Test
    fun uploadPendingCrashLogs_finds_json_files_with_correct_prefix() {
        uploader.enabled = true

        val crashJson = File(logDir, "logs_crash_2024-01-01_00:00:00.json")
        crashJson.writeText("""{"id":"test","versionCode":1}""")

        val nonCrashJson = File(logDir, "other_data.json")
        nonCrashJson.writeText("""{"data":"not a crash"}""")

        uploader.uploadPendingCrashLogs()

        // Upload will fail (no real server), but the method should not crash
        // and the crash JSON file should still be present for retry
        assertThat(crashJson.exists()).isTrue()
        assertThat(nonCrashJson.exists()).isTrue()
    }
}
