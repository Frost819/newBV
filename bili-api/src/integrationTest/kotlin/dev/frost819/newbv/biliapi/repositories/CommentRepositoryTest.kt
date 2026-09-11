package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.entity.comment.CommentPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * [CommentRepository] 的集成测试。
 *
 * 验证评论获取（Web HTTP + App gRPC）、楼中楼回复获取（Web + gRPC）、分页 / cursor
 * 翻页与排序，以及评论点赞操作。查询类接口断言分页不重复、排序生效；点赞为互动类
 * 操作先赞后取消还原状态。依赖真实 B 站凭证和网络。
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

        /**
         * 测试视频：`BV1GkYN6RERr`（aid=117244956772233）。
         *
         * 评论区数量充足，且热门排序首页存在回复数超过单页上限（20）的根评论，
         * 可同时覆盖主评论分页与楼中楼分页。
         */
        private const val TEST_AID = 117244956772233L

        /** 扫描主评论寻找有楼中楼回复的根评论时，最多翻页数。 */
        private const val MAX_ROOT_SCAN_PAGES = 5

        /** 触发楼中楼分页所需的最小根评论回复数（单页上限为 20）。 */
        private const val MIN_REPLY_COUNT_FOR_PAGING = 21
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

    // ------------------------------------------------------------------
    // 主评论
    // ------------------------------------------------------------------

    @Test
    fun `get comments web paginates without duplicates`() {
        runBlocking {
            val first = getWebComments(page = 1)
            assertThat(first.comments).isNotEmpty()
            assertThat(first.comments.map { it.rpid }).containsNoDuplicates()
            assertThat(first.total).isGreaterThan(0)

            if (!first.hasMore) {
                println("web comments only has one page: ${first.comments.size}")
                return@runBlocking
            }

            val second = getWebComments(page = 2)
            assertThat(second.page).isEqualTo(2)
            assertThat(second.comments).isNotEmpty()
            assertThat(second.comments.map { it.rpid }).containsNoDuplicates()

            val overlap = first.rpids() intersect second.rpids()
            println("web page1=${first.comments.size}, page2=${second.comments.size}, overlap=${overlap.size}")
            assertThat(overlap).isEmpty()
        }
    }

    @Test
    fun `get comments web supports hot and latest sort`() {
        runBlocking {
            val hot = commentRepository.getComments(TEST_AID, sort = 1, page = 1, preferApiType = ApiType.Web)
            val latest = commentRepository.getComments(TEST_AID, sort = 0, page = 1, preferApiType = ApiType.Web)

            assertThat(hot.comments).isNotEmpty()
            assertThat(latest.comments).isNotEmpty()
            assertThat(latest.comments.map { it.rpid }).containsNoDuplicates()
            // 两次排序都应为该视频评论区的内容
            assertThat(hot.comments.first().oid).isEqualTo(TEST_AID)
            assertThat(latest.comments.first().oid).isEqualTo(TEST_AID)
        }
    }

    @Test
    fun `get comments grpc paginates with cursor`() {
        runBlocking {
            val first = commentRepository.getComments(TEST_AID, sort = 1, page = 1, preferApiType = ApiType.App)
            assertThat(first.comments).isNotEmpty()
            assertThat(first.comments.map { it.rpid }).containsNoDuplicates()

            if (!first.hasMore) {
                println("grpc comments only has one page: ${first.comments.size}")
                return@runBlocking
            }

            // gRPC 翻页依赖服务端 cursor，而不是本地页码
            val cursor = first.nextCursor
            assertThat(cursor).isNotNull()
            val second =
                commentRepository.getComments(
                    TEST_AID,
                    sort = 1,
                    page = 2,
                    nextCursor = cursor,
                    preferApiType = ApiType.App,
                )
            assertThat(second.comments.map { it.rpid }).containsNoDuplicates()

            val overlap = first.rpids() intersect second.rpids()
            println("grpc page1=${first.comments.size}, page2=${second.comments.size}, overlap=${overlap.size}")
            assertThat(overlap).isEmpty()
        }
    }

    // ------------------------------------------------------------------
    // 楼中楼
    // ------------------------------------------------------------------

    @Test
    fun `get replies web paginates without duplicates`() {
        runBlocking {
            val root = findRootWithReplies(aid = TEST_AID, preferApiType = ApiType.Web)
            if (root == null) {
                println("no root comment with enough replies found, skip")
                return@runBlocking
            }

            val first =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = root.rpid,
                    page = 1,
                    preferApiType = ApiType.Web,
                )
            assertThat(first.comments).isNotEmpty()
            assertThat(first.comments.map { it.rpid }).containsNoDuplicates()
            assertThat(first.comments.map { it.rpid }).doesNotContain(root.rpid)
            assertThat(first.hasMore).isTrue()

            val second =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = root.rpid,
                    page = 2,
                    preferApiType = ApiType.Web,
                )
            assertThat(second.page).isEqualTo(2)
            assertThat(second.comments).isNotEmpty()
            assertThat(second.comments.map { it.rpid }).containsNoDuplicates()

            val overlap = first.rpids() intersect second.rpids()
            println(
                "web replies root=${root.rpid} page1=${first.comments.size}, " +
                    "page2=${second.comments.size}, overlap=${overlap.size}",
            )
            assertThat(overlap).isEmpty()
        }
    }

    @Test
    fun `get replies grpc paginates with cursor`() {
        runBlocking {
            val root = findRootWithReplies(aid = TEST_AID, preferApiType = ApiType.App)
            if (root == null) {
                println("no root comment with enough replies found, skip")
                return@runBlocking
            }

            val first =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = root.rpid,
                    page = 1,
                    preferApiType = ApiType.App,
                )
            assertThat(first.comments).isNotEmpty()
            assertThat(first.comments.map { it.rpid }).containsNoDuplicates()
            assertThat(first.comments.map { it.rpid }).doesNotContain(root.rpid)
            assertThat(first.hasMore).isTrue()

            val cursor = first.nextCursor
            assertThat(cursor).isNotNull()
            val second =
                commentRepository.getReplies(
                    aid = TEST_AID,
                    rootRpid = root.rpid,
                    page = 2,
                    nextCursor = cursor,
                    preferApiType = ApiType.App,
                )
            assertThat(second.comments).isNotEmpty()
            assertThat(second.comments.map { it.rpid }).containsNoDuplicates()

            val overlap = first.rpids() intersect second.rpids()
            println(
                "grpc replies root=${root.rpid} page1=${first.comments.size}, " +
                    "page2=${second.comments.size}, overlap=${overlap.size}",
            )
            assertThat(overlap).isEmpty()
        }
    }

    // ------------------------------------------------------------------
    // 评论点赞
    // ------------------------------------------------------------------

    @Test
    fun `toggle comment like with web api`() {
        runBlocking {
            // 互动类：先赞后取消还原状态，repository 失败会抛异常
            val comments = getWebComments(page = 1)
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

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private suspend fun getWebComments(page: Int): CommentPage =
        commentRepository.getComments(
            aid = TEST_AID,
            sort = 1,
            page = page,
            preferApiType = ApiType.Web,
        )

    /**
     * 翻页查找一个回复数足以触发分页（replyCount >= [MIN_REPLY_COUNT_FOR_PAGING]）的根评论。
     *
     * 找不到则返回 null，调用方应跳过楼中楼分页断言。
     */
    private suspend fun findRootWithReplies(
        aid: Long,
        preferApiType: ApiType,
    ): Comment? {
        var page = 1
        var cursor: Long? = null
        var scanned = 0
        while (scanned < MAX_ROOT_SCAN_PAGES) {
            val result =
                commentRepository.getComments(
                    aid = aid,
                    sort = 1,
                    page = page,
                    nextCursor = cursor,
                    preferApiType = preferApiType,
                )
            result.comments.firstOrNull { it.replyCount >= MIN_REPLY_COUNT_FOR_PAGING }?.let { return it }
            if (!result.hasMore) break
            cursor = result.nextCursor
            page++
            scanned++
        }
        return null
    }

    private fun CommentPage.rpids(): Set<Long> = comments.map { it.rpid }.toSet()
}
