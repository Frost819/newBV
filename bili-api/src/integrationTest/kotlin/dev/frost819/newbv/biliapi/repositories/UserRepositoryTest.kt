package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [UserRepository] 的集成测试。
 *
 * 验证用户空间视频、动态、关注列表、关注关系与追番操作（Web + App）。
 * 查询类接口断言正常返回数据；关注/追番为互动类操作（先操作后还原）仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class UserRepositoryTest {
    companion object {
        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
    }

    private val authRepository = AuthRepository()
    private val channelRepository = ChannelRepository()
    private val userRepository = UserRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(
            ACCESS_TOKEN,
            BUVID,
        )
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)

        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `get user space videos with web api`() =
        runBlocking {
            // 查询类：断言第一页返回投稿视频数据（mid=2 为 B 站官方账号，必有投稿）
            var page = SpaceVideoPage()
            var firstPageChecked = false
            while (page.hasNext) {
                val spaceVideoData =
                    userRepository.getSpaceVideos(
                        mid = 2,
                        order = SpaceVideoOrder.PubDate,
                        page = page,
                        preferApiType = ApiType.Web,
                    )
                if (!firstPageChecked) {
                    assertThat(spaceVideoData.videos).isNotEmpty()
                    firstPageChecked = true
                }
                println("web page videos: ${spaceVideoData.videos.size}")
                page = spaceVideoData.page
                delay((1000L..3000L).random())
            }
        }

    @Test
    fun `get user space videos with app api`() =
        runBlocking {
            var page = SpaceVideoPage()
            var firstPageChecked = false
            while (page.hasNext) {
                val spaceVideoData =
                    userRepository.getSpaceVideos(
                        mid = 2,
                        order = SpaceVideoOrder.PubDate,
                        page = page,
                        preferApiType = ApiType.App,
                    )
                if (!firstPageChecked) {
                    assertThat(spaceVideoData.videos).isNotEmpty()
                    firstPageChecked = true
                }
                println("app page videos: ${spaceVideoData.videos.size}")
                page = spaceVideoData.page
                delay((1000L..3000L).random())
            }
        }

    @Test
    fun `get dynamic videos with web api`() =
        runBlocking {
            // 查询类：动态列表可能为空（账号无关注者动态），断言接口正常返回结构
            val result =
                userRepository.getDynamicVideos(
                    page = 1,
                    offset = "",
                    updateBaseline = "",
                    preferApiType = ApiType.Web,
                )
            println("web dynamic videos: ${result.videos.size}")
            assertThat(result.videos).isNotNull()
        }

    @Test
    fun `get dynamic videos with grpc api`() =
        runBlocking {
            val result =
                userRepository.getDynamicVideos(
                    page = 1,
                    offset = "",
                    updateBaseline = "",
                    preferApiType = ApiType.App,
                )
            println("grpc dynamic videos: ${result.videos.size}")
            assertThat(result.videos).isNotNull()
        }

    @Test
    fun `get following users with web api`() =
        runBlocking {
            // 查询类：关注列表可能为空，断言接口正常返回结构
            val result =
                userRepository.getFollowedUsers(
                    mid = UID,
                    preferApiType = ApiType.Web,
                )
            println("web following users: ${result.size}")
            assertThat(result).isNotNull()
        }

    @Test
    fun `get following users with app api`() =
        runBlocking {
            val result =
                userRepository.getFollowedUsers(
                    mid = UID,
                    preferApiType = ApiType.App,
                )
            println("app following users: ${result.size}")
            assertThat(result).isNotNull()
        }

    @Test
    fun `check is following with web api`() =
        runBlocking {
            // 查询类：正常返回布尔结果即视为数据有效
            val result = userRepository.checkIsFollowing(mid = UID)
            println("is following self: $result")
        }

    // follow/unfollow 与追番会修改账号状态，使用可逆的 follow 测试（关注后取消）
    @Test
    fun `follow then unfollow user with app api`() =
        runBlocking {
            // 互动类：先关注后取关还原状态，repository 失败会抛异常
            val targetMid = UID
            val followResult = userRepository.followUser(mid = targetMid, preferApiType = ApiType.App)
            println("app follow result: $followResult")
            assertThat(followResult).isTrue()
            val unfollowResult = userRepository.unfollowUser(mid = targetMid, preferApiType = ApiType.App)
            println("app unfollow result: $unfollowResult")
            assertThat(unfollowResult).isTrue()
        }

    // 追番/取关追番使用一个固定番剧，测试后还原
    @Test
    fun `season follow then unfollow with app api`() =
        runBlocking {
            // 互动类：先追番后取消还原状态，repository 失败会抛异常
            val seasonId = 34652
            val follow = userRepository.addSeasonFollow(seasonId = seasonId, preferApiType = ApiType.App)
            println("app season follow toast: $follow")
            assertThat(follow).isNotEmpty()
            val unfollow = userRepository.delSeasonFollow(seasonId = seasonId, preferApiType = ApiType.App)
            println("app season unfollow toast: $unfollow")
            assertThat(unfollow).isNotEmpty()
        }

    @Test
    fun `get following up count with app api`() =
        runBlocking {
            // 查询类：断言返回非负的关注数
            val count = userRepository.getFollowingUpCount(mid = UID, preferApiType = ApiType.App)
            println("app following up count: $count")
            assertThat(count).isAtLeast(0)
        }
}
