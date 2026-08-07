package dev.frost819.newbv.app.network.entity

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [GithubRelease] 序列化/反序列化的单元测试。
 */
class GithubReleaseTest {

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    @Test
    fun deserialize_minimalRelease() {
        val releaseJson = """
        {
            "assets": [],
            "assets_url": "https://api.github.com/repos/test/test/releases/1/assets",
            "body": "Release notes",
            "created_at": "2024-01-01T00:00:00Z",
            "draft": false,
            "html_url": "https://github.com/test/test/releases/tag/v1.0",
            "id": 1,
            "name": "v1.0",
            "prerelease": false,
            "published_at": "2024-01-01T00:00:00Z",
            "tag_name": "v1.0",
            "tarball_url": "https://api.github.com/repos/test/test/tarball/v1.0",
            "target_commitish": "main",
            "upload_url": "https://uploads.github.com/repos/test/test/releases/1/assets",
            "url": "https://api.github.com/repos/test/test/releases/1",
            "zipball_url": "https://api.github.com/repos/test/test/zipball/v1.0"
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)

        assertThat(release.name).isEqualTo("v1.0")
        assertThat(release.tagName).isEqualTo("v1.0")
        assertThat(release.body).isEqualTo("Release notes")
        assertThat(release.prerelease).isFalse()
        assertThat(release.isPreRelease).isFalse()
        assertThat(release.assets).isEmpty()
    }

    @Test
    fun deserialize_preRelease() {
        val releaseJson = """
        {
            "assets": [],
            "assets_url": "",
            "body": "",
            "created_at": "",
            "draft": false,
            "html_url": "",
            "id": 2,
            "name": "v2.0-alpha",
            "prerelease": true,
            "published_at": "",
            "tag_name": "v2.0-alpha",
            "tarball_url": "",
            "target_commitish": "",
            "upload_url": "",
            "url": "",
            "zipball_url": ""
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)

        assertThat(release.prerelease).isTrue()
        assertThat(release.isPreRelease).isTrue()
        assertThat(release.name).isEqualTo("v2.0-alpha")
    }

    @Test
    fun deserialize_withAssets() {
        val releaseJson = """
        {
            "assets": [
                {
                    "browser_download_url": "https://github.com/test/test/releases/download/v1.0/BV_1.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "download_count": 100,
                    "id": 100,
                    "name": "BV_1.apk",
                    "size": 42000000,
                    "state": "uploaded",
                    "url": "https://api.github.com/repos/test/test/releases/assets/100"
                }
            ],
            "assets_url": "",
            "body": "",
            "created_at": "",
            "draft": false,
            "html_url": "",
            "id": 1,
            "name": "v1.0",
            "prerelease": false,
            "published_at": "",
            "tag_name": "v1.0",
            "tarball_url": "",
            "target_commitish": "",
            "upload_url": "",
            "url": "",
            "zipball_url": ""
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)

        assertThat(release.assets).hasSize(1)
        val asset = release.assets[0]
        assertThat(asset.name).isEqualTo("BV_1.apk")
        assertThat(asset.browserDownloadUrl).isEqualTo("https://github.com/test/test/releases/download/v1.0/BV_1.apk")
        assertThat(asset.size).isEqualTo(42000000)
    }

    @Test
    fun deserialize_listOfReleases() {
        val listJson = """
        [
            {
                "assets": [],
                "assets_url": "",
                "body": "",
                "created_at": "",
                "draft": false,
                "html_url": "",
                "id": 1,
                "name": "v1.0",
                "prerelease": false,
                "published_at": "",
                "tag_name": "v1.0",
                "tarball_url": "",
                "target_commitish": "",
                "upload_url": "",
                "url": "",
                "zipball_url": ""
            },
            {
                "assets": [],
                "assets_url": "",
                "body": "",
                "created_at": "",
                "draft": false,
                "html_url": "",
                "id": 2,
                "name": "v2.0",
                "prerelease": true,
                "published_at": "",
                "tag_name": "v2.0",
                "tarball_url": "",
                "target_commitish": "",
                "upload_url": "",
                "url": "",
                "zipball_url": ""
            }
        ]
        """.trimIndent()

        val releases = json.decodeFromString<List<GithubRelease>>(listJson)

        assertThat(releases).hasSize(2)
        assertThat(releases[0].name).isEqualTo("v1.0")
        assertThat(releases[1].isPreRelease).isTrue()
    }

    @Test
    fun deserialize_unknownKeys_ignored() {
        val releaseJson = """
        {
            "assets": [],
            "assets_url": "",
            "body": "",
            "created_at": "",
            "draft": false,
            "html_url": "",
            "id": 1,
            "name": "v1.0",
            "prerelease": false,
            "published_at": "",
            "tag_name": "v1.0",
            "tarball_url": "",
            "target_commitish": "",
            "upload_url": "",
            "url": "",
            "zipball_url": "",
            "unknown_field": "should be ignored"
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)
        assertThat(release.name).isEqualTo("v1.0")
    }

    // ── Serialization round-trip ──────────────────────────────────────

    @Test
    fun `serialize and deserialize roundtrip preserves all fields`() {
        val original = GithubRelease(
            assets = listOf(
                GithubRelease.Asset(
                    browserDownloadUrl = "https://example.com/download.apk",
                    contentType = "application/vnd.android.package-archive",
                    downloadCount = 500,
                    id = 200,
                    name = "app-release.apk",
                    size = 50000000,
                    state = "uploaded",
                    url = "https://api.github.com/repos/test/test/releases/assets/200",
                ),
            ),
            assetsUrl = "https://api.github.com/assets",
            body = "Release body text",
            createdAt = "2024-06-01T00:00:00Z",
            draft = false,
            htmlUrl = "https://github.com/test/test/releases/tag/v3.0",
            id = 10,
            name = "v3.0",
            prerelease = false,
            publishedAt = "2024-06-02T00:00:00Z",
            tagName = "v3.0",
            tarballUrl = "https://api.github.com/tarball/v3.0",
            targetCommitish = "main",
            uploadUrl = "https://uploads.github.com/repos/test/test/releases/10/assets",
            url = "https://api.github.com/repos/test/test/releases/10",
            zipballUrl = "https://api.github.com/zipball/v3.0",
        )

        val serialized = json.encodeToString(GithubRelease.serializer(), original)
        val deserialized = json.decodeFromString<GithubRelease>(serialized)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `Asset serialization roundtrip preserves all fields`() {
        val original = GithubRelease.Asset(
            browserDownloadUrl = "https://example.com/file.apk",
            contentType = "application/octet-stream",
            downloadCount = 42,
            id = 999,
            name = "test.apk",
            size = 12345,
            state = "open",
            url = "https://api.github.com/assets/999",
        )

        val serialized = json.encodeToString(GithubRelease.Asset.serializer(), original)
        val deserialized = json.decodeFromString<GithubRelease.Asset>(serialized)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `deserialize with multiple assets`() {
        val releaseJson = """
        {
            "assets": [
                {
                    "browser_download_url": "https://example.com/app-release.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "download_count": 100,
                    "id": 1,
                    "name": "app-release.apk",
                    "size": 50000000,
                    "state": "uploaded",
                    "url": "https://api.github.com/assets/1"
                },
                {
                    "browser_download_url": "https://example.com/app-alpha.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "download_count": 50,
                    "id": 2,
                    "name": "app-alpha.apk",
                    "size": 48000000,
                    "state": "uploaded",
                    "url": "https://api.github.com/assets/2"
                }
            ],
            "assets_url": "",
            "body": "",
            "created_at": "",
            "draft": false,
            "html_url": "",
            "id": 1,
            "name": "v1.0",
            "prerelease": false,
            "published_at": "",
            "tag_name": "v1.0",
            "tarball_url": "",
            "target_commitish": "",
            "upload_url": "",
            "url": "",
            "zipball_url": ""
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)

        assertThat(release.assets).hasSize(2)
        assertThat(release.assets[0].name).isEqualTo("app-release.apk")
        assertThat(release.assets[1].name).isEqualTo("app-alpha.apk")
        assertThat(release.assets[0].downloadCount).isEqualTo(100)
        assertThat(release.assets[1].downloadCount).isEqualTo(50)
    }

    @Test
    fun `isPreRelease reflects prerelease field`() {
        val nonPrerelease = GithubRelease(
            assets = emptyList(), assetsUrl = "", body = "", createdAt = "",
            draft = false, htmlUrl = "", id = 1, name = "", prerelease = false,
            publishedAt = "", tagName = "", tarballUrl = "", targetCommitish = "",
            uploadUrl = "", url = "", zipballUrl = "",
        )
        assertThat(nonPrerelease.isPreRelease).isFalse()

        val prerelease = nonPrerelease.copy(prerelease = true)
        assertThat(prerelease.isPreRelease).isTrue()
    }

    @Test
    fun `Asset fields mapped correctly from JSON`() {
        val releaseJson = """
        {
            "assets": [
                {
                    "browser_download_url": "https://example.com/download",
                    "content_type": "application/zip",
                    "download_count": 0,
                    "id": 555,
                    "name": "asset.zip",
                    "size": 1024,
                    "state": "uploaded",
                    "url": "https://api.github.com/assets/555"
                }
            ],
            "assets_url": "", "body": "", "created_at": "", "draft": false,
            "html_url": "", "id": 1, "name": "", "prerelease": false,
            "published_at": "", "tag_name": "", "tarball_url": "",
            "target_commitish": "", "upload_url": "", "url": "", "zipball_url": ""
        }
        """.trimIndent()

        val release = json.decodeFromString<GithubRelease>(releaseJson)
        val asset = release.assets[0]

        assertThat(asset.browserDownloadUrl).isEqualTo("https://example.com/download")
        assertThat(asset.contentType).isEqualTo("application/zip")
        assertThat(asset.downloadCount).isEqualTo(0)
        assertThat(asset.id).isEqualTo(555)
        assertThat(asset.name).isEqualTo("asset.zip")
        assertThat(asset.size).isEqualTo(1024)
        assertThat(asset.state).isEqualTo("uploaded")
        assertThat(asset.url).isEqualTo("https://api.github.com/assets/555")
    }
}
