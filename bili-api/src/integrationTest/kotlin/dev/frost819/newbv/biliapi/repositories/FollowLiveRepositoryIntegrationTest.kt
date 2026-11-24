package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [LiveRepository.getFollowLive] 的集成测试（真实网络 + 登录态）。
 *
 * 覆盖 issue #337：`GetWebList` 在 `hit_ab=false` 下每页只返回 10 条，必须翻页才能拿全
 * 正在直播的关注。验证首页条数、`hasMore` 与 `total` 的一致性、翻页不重复、越界页为空。
 *
 * 凭证来自 `local.properties`（`test.sessdata` 等）；当账号正在直播的关注不足 11 个时，
 * 翻页相关断言会自动跳过（{@link org.junit.jupiter.api.Assumptions}）。
 */
class FollowLiveRepositoryIntegrationTest {
    companion object {
        private const val PAGE_SIZE = 10

        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }

        private val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        private val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        private val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        private val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""
        private val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
    }

    private val repository = LiveRepository()

    init {
        BiliHttpApi.init(
            buvid3 = BUVID,
            sessData = SESSDATA,
            biliJct = BILI_JCT,
            mid = UID,
            accessToken = ACCESS_TOKEN,
        )
    }

    @Test
    fun `first page stays within page size and keeps real covers`() =
        runBlocking {
            val first = repository.getFollowLive(page = 1)
            assumeTrue(first.rooms.isNotEmpty(), "关注中没有正在直播的主播，跳过")

            assertThat(first.rooms.size).isAtMost(PAGE_SIZE)
            // hit_ab=false 才带真实封面；整页封面都为空说明服务端行为变了
            assertThat(
                first.rooms.any { it.coverFromUser.isNotBlank() || it.keyframe.isNotBlank() },
            ).isTrue()
        }

    @Test
    fun `hasMore matches total live follow count`() =
        runBlocking {
            val first = repository.getFollowLive(page = 1)
            assumeTrue(first.total > 0, "服务端未返回正在直播的关注人数，跳过")

            assertThat(first.hasMore).isEqualTo(first.total > PAGE_SIZE)
        }

    @Test
    fun `second page continues without duplicates`() =
        runBlocking {
            val first = repository.getFollowLive(page = 1)
            assumeTrue(first.hasMore, "正在直播的关注不足一页，跳过翻页断言")

            val second = repository.getFollowLive(page = 2)

            assertThat(second.rooms).isNotEmpty()
            val firstRoomIds = first.rooms.map { it.roomId }.toSet()
            assertThat(second.rooms.none { it.roomId in firstRoomIds }).isTrue()
        }

    @Test
    fun `page after the last page is empty`() =
        runBlocking {
            val first = repository.getFollowLive(page = 1)
            assumeTrue(first.total > 0, "服务端未返回正在直播的关注人数，跳过")

            val lastPage = (first.total + PAGE_SIZE - 1) / PAGE_SIZE
            val next = repository.getFollowLive(page = lastPage + 1)

            assertThat(next.rooms).isEmpty()
            assertThat(next.hasMore).isFalse()
        }
}
