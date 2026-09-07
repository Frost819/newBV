package dev.frost819.newbv.app.ui.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [Routes] 路由定义的单元测试。
 *
 * 验证所有 `@Serializable` 路由的序列化/反序列化正确性，
 * 确保 Navigation-Compose 类型安全路由可正常工作。
 */
class RoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    // ── object 路由 ──────────────────────────────────────────────────

    @Test
    fun `HomeRoute serializes and deserializes`() {
        val encoded = json.encodeToString(HomeRoute)
        val decoded = json.decodeFromString<HomeRoute>(encoded)
        assertThat(decoded).isEqualTo(HomeRoute)
    }

    @Test
    fun `SearchResultRoute serializes and deserializes`() {
        val route = SearchResultRoute(keyword = "test")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<SearchResultRoute>(encoded)
        assertThat(decoded.keyword).isEqualTo("test")
    }

    @Test
    fun `SettingsRoute serializes and deserializes`() {
        val encoded = json.encodeToString(SettingsRoute)
        val decoded = json.decodeFromString<SettingsRoute>(encoded)
        assertThat(decoded).isEqualTo(SettingsRoute)
    }

    @Test
    fun `UserSwitchRoute serializes and deserializes`() {
        val encoded = json.encodeToString(UserSwitchRoute)
        val decoded = json.decodeFromString<UserSwitchRoute>(encoded)
        assertThat(decoded).isEqualTo(UserSwitchRoute)
    }

    @Test
    fun `LoginRoute serializes and deserializes`() {
        val encoded = json.encodeToString(LoginRoute)
        val decoded = json.decodeFromString<LoginRoute>(encoded)
        assertThat(decoded).isEqualTo(LoginRoute)
    }

    // ── data class 路由 ──────────────────────────────────────────────

    @Test
    fun `VideoDetailRoute serializes with required fields`() {
        val route = VideoDetailRoute(aid = 12345L)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<VideoDetailRoute>(encoded)
        assertThat(decoded.aid).isEqualTo(12345L)
        assertThat(decoded.epid).isNull()
    }

    @Test
    fun `VideoDetailRoute serializes with optional epid`() {
        val route = VideoDetailRoute(aid = 12345L, epid = 67890L)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<VideoDetailRoute>(encoded)
        assertThat(decoded.aid).isEqualTo(12345L)
        assertThat(decoded.epid).isEqualTo(67890L)
    }

    @Test
    fun `VideoPlayerRoute serializes with all fields`() {
        val route =
            VideoPlayerRoute(
                aid = 100L,
                cid = 200L,
                title = "Test Video",
                cover = "https://example.com/cover.jpg",
            )
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<VideoPlayerRoute>(encoded)
        assertThat(decoded.aid).isEqualTo(100L)
        assertThat(decoded.cid).isEqualTo(200L)
        assertThat(decoded.title).isEqualTo("Test Video")
        assertThat(decoded.cover).isEqualTo("https://example.com/cover.jpg")
    }

    @Test
    fun `SeasonPlayerRoute serializes correctly`() {
        val route = SeasonPlayerRoute(epid = 1L, sid = 2L, title = "Anime")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<SeasonPlayerRoute>(encoded)
        assertThat(decoded.epid).isEqualTo(1L)
        assertThat(decoded.sid).isEqualTo(2L)
        assertThat(decoded.title).isEqualTo("Anime")
    }

    @Test
    fun `LivePlayerRoute serializes correctly`() {
        val route = LivePlayerRoute(roomId = 999L, title = "Live Stream")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<LivePlayerRoute>(encoded)
        assertThat(decoded.roomId).isEqualTo(999L)
        assertThat(decoded.title).isEqualTo("Live Stream")
    }

    @Test
    fun `UserSpaceRoute serializes correctly`() {
        val route = UserSpaceRoute(mid = 123456L)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<UserSpaceRoute>(encoded)
        assertThat(decoded.mid).isEqualTo(123456L)
    }

    @Test
    fun `PgcFeatureRoute serializes correctly`() {
        val route = PgcFeatureRoute(seasonId = 42L)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<PgcFeatureRoute>(encoded)
        assertThat(decoded.seasonId).isEqualTo(42L)
    }

    // ── 默认值验证 ───────────────────────────────────────────────────

    @Test
    fun `VideoDetailRoute default epid is null`() {
        val route = VideoDetailRoute(aid = 1L)
        assertThat(route.epid).isNull()
    }

    @Test
    fun `VideoPlayerRoute defaults are correct`() {
        val route = VideoPlayerRoute(aid = 1L, cid = 2L)
        assertThat(route.epid).isNull()
        assertThat(route.title).isEmpty()
        assertThat(route.cover).isEmpty()
    }

    @Test
    fun `SeasonPlayerRoute defaults are correct`() {
        val route = SeasonPlayerRoute(epid = 1L, sid = 2L)
        assertThat(route.title).isEmpty()
        assertThat(route.cover).isEmpty()
    }

    @Test
    fun `LivePlayerRoute defaults are correct`() {
        val route = LivePlayerRoute(roomId = 1L)
        assertThat(route.title).isEmpty()
        assertThat(route.cover).isEmpty()
    }
}
