package dev.frost819.newbv.app.network

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * [buildDownloadUrls] / [downloadSourceLabel] / [isApkLike] 的单元测试。
 *
 * 覆盖更新包多源回退的地址拼接顺序，以及镜像站返回错误页时对内容的拦截。
 */
class GithubApiDownloadSourceTest {
    private val assetUrl =
        "https://github.com/Frost819/newBV/releases/download/v1.2.2/newBV_232_release.apk"

    @Test
    fun `buildDownloadUrls keeps mirror order and appends direct url last`() {
        val urls = buildDownloadUrls(assetUrl)

        assertThat(urls).hasSize(DOWNLOAD_SOURCES.size)
        assertThat(urls.first()).isEqualTo(DOWNLOAD_SOURCES.first() + assetUrl)
        assertThat(urls.last()).isEqualTo(assetUrl)
        assertThat(urls.all { it.startsWith("https://") }).isTrue()
    }

    @Test
    fun `downloadSourceLabel uses mirror host and direct label`() {
        assertThat(downloadSourceLabel("https://gh-proxy.com/$assetUrl")).isEqualTo("gh-proxy.com")
        assertThat(downloadSourceLabel(assetUrl)).isEqualTo("GitHub 直连")
    }

    @Test
    fun `isApkLike accepts zip header`(
        @TempDir dir: Path,
    ) {
        val apk = dir.resolve("update.apk").toFile()
        apk.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00))

        assertThat(apk.isApkLike()).isTrue()
    }

    @Test
    fun `isApkLike rejects html error page`(
        @TempDir dir: Path,
    ) {
        val html = dir.resolve("error.html").toFile()
        html.writeText("<!DOCTYPE html><html><body>mirror blocked</body></html>")

        assertThat(html.isApkLike()).isFalse()
    }

    @Test
    fun `isApkLike rejects truncated file`(
        @TempDir dir: Path,
    ) {
        val truncated = dir.resolve("truncated.apk").toFile()
        truncated.writeBytes(byteArrayOf(0x50, 0x4B))

        assertThat(truncated.isApkLike()).isFalse()
    }
}
