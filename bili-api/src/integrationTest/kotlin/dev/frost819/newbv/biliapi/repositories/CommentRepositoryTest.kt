package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [CommentRepository] 的集成测试。
 *
 * 验证评论获取（Web HTTP + App gRPC）、楼中楼回复获取（Web + gRPC）
 * 和评论点赞操作。查询类接口断言正常返回数据；点赞为互动类操作仅断言返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class CommentRepositoryTest {
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

        private const val TEST_AID = 993403941L
    }

    private val authRepository = AuthRepository()
    private val channelRepository = ChannelRepository()
    private val commentRepository = CommentRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(ACCESS_TOKEN, BUVID)
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    @Test
    fun `get comments with web api`() {
        runBlocking {
            // 查询类：断言返回评论数据
            val result =
                commentRepository.getComments(
                    aid = TEST_AID,
                    sort = 1,
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            println("web comments count: ${result.comments.size}")
            println("web comments hasMore: ${result.hasMore}")
            assertThat(result.comments).isNotEmpty()
            result.comments.take(3).forEach { println("  - ${it.message}") }
        }
    }

    @Test
    fun `get comments with grpc`() {
        runBlocking {
            val result =
                commentRepository.getComments(
                    aid = TEST_AID,
                    sort = 1,
                    page = 1,
                    preferApiType = ApiType.App,
                )
            println("grpc comments count: ${result.comments.size}")
            println("grpc comments hasMore: ${result.hasMore}")
            assertThat(result.comments).isNotEmpty()
            result.comments.take(3).forEach { println("  - ${it.message}") }
        }
    }

    @Test
    fun `get replies with web api`() {
        runBlocking {
            val comments =
                commentRepository.getComments(
                    aid = TEST_AID,
                    sort = 1,
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            val rootRpid = comments.comments.firstOrNull()?.rpid
            if (rootRpid == null) {
                println("no comments found to test replies")
                return@runBlocking
            }
            val result =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = rootRpid,
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            println("web replies count: ${result.comments.size}")
            // 查询类：楼中楼可能为空（根评论无回复），断言接口正常返回即可
            assertThat(result.comments).isNotNull()
        }
    }

    @Test
    fun `get replies with grpc`() {
        runBlocking {
            val comments =
                commentRepository.getComments(
                    aid = TEST_AID,
                    sort = 1,
                    page = 1,
                    preferApiType = ApiType.App,
                )
            val rootRpid = comments.comments.firstOrNull()?.rpid
            if (rootRpid == null) {
                println("no comments found to test replies")
                return@runBlocking
            }
            val result =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = rootRpid,
                    page = 1,
                    preferApiType = ApiType.App,
                )
            println("grpc replies count: ${result.comments.size}")
            assertThat(result.comments).isNotNull()
        }
    }

    @Test
    fun `toggle comment like with web api`() {
        runBlocking {
            // 互动类：先赞后取消还原状态，repository 失败会抛异常
            val comments =
                commentRepository.getComments(
                    aid = TEST_AID,
                    sort = 1,
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            val rootRpid = comments.comments.firstOrNull()?.rpid
            if (rootRpid == null) {
                println("no comments found to test like")
                return@runBlocking
            }
            commentRepository.toggleCommentLike(
                aid = TEST_AID,
                rpid = rootRpid,
                like = true,
                preferApiType = ApiType.Web,
            )
            println("liked comment $rootRpid successfully")
            commentRepository.toggleCommentLike(
                aid = TEST_AID,
                rpid = rootRpid,
                like = false,
                preferApiType = ApiType.Web,
            )
            println("unliked comment $rootRpid successfully")
        }
    }
}
