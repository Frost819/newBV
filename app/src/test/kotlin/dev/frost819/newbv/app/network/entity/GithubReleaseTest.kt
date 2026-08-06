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
}
