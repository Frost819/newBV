package dev.frost819.newbv.biliapi.repositories

import bilibili.main.community.reply.v1.ReplyGrpcKt
import bilibili.main.community.reply.v1.cursorReq
import bilibili.main.community.reply.v1.detailListReq
import bilibili.main.community.reply.v1.mainListReq
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.comment.Comment
import dev.frost819.newbv.biliapi.entity.comment.CommentPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * 视频评论仓库。
 *
 * 统一封装主评论、楼中楼和评论点赞操作。Web 请求使用 Cookie，App 请求使用
 * access_key；UI 层不直接依赖 B 站 JSON 结构。
 *
 * @param authRepository 当前登录凭证仓库
 */
class CommentRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val replyStub
        get() =
            runCatching {
                ReplyGrpcKt.ReplyCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    /**
     * 获取主评论分页。
     *
     * @param aid 视频 AV 号
     * @param sort 排序方式，0 为时间，1 为热度，2 为回复数
     * @param page 页码，从 1 开始（Web 通道使用）
     * @param nextCursor App gRPC 通道的下一页游标，首次请求传 null
     * @param preferApiType 首选接口类型
     */
    suspend fun getComments(
        aid: Long,
        sort: Int = 1,
        page: Int = 1,
        nextCursor: Long? = null,
        preferApiType: ApiType,
    ): CommentPage =
        when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.getVideoComments(aid, sort, page, accessKey = null).getResponseData()
                parsePage(data, aid = aid, page = page, defaultType = 1, isThread = false)
            }

            ApiType.App -> {
                val reply =
                    replyStub?.mainList(
                        mainListReq {
                            oid = aid
                            type = 1
                            cursor =
                                cursorReq {
                                    // gRPC 使用服务端返回的 next cursor 翻页，不能用本地页码代替
                                    next = nextCursor ?: 0L
                                    mode =
                                        if (sort == 0) {
                                            bilibili.main.community.reply.v1.Mode.MAIN_LIST_TIME
                                        } else {
                                            bilibili.main.community.reply.v1.Mode.MAIN_LIST_HOT
                                        }
                                }
                        },
                    ) ?: throw IllegalStateException("App gRPC reply stub is not initialized")
                CommentPage(
                    comments =
                        reply.repliesList
                            .map { Comment.fromGrpc(it, aid) }
                            .filter { it.rpid > 0L }
                            .distinctBy { it.rpid },
                    page = page,
                    total = 0,
                    hasMore = !reply.cursor.isEnd,
                    nextCursor = reply.cursor.next.takeIf { !reply.cursor.isEnd },
                )
            }
        }

    /**
     * 获取根评论下的楼中楼分页。
     *
     * @param aid 视频 AV 号
     * @param rootRpid 根评论 ID
     * @param page 页码，从 1 开始（Web 通道使用）
     * @param nextCursor App gRPC 通道的下一页游标，首次请求传 null
     * @param preferApiType 首选接口类型
     */
    suspend fun getReplies(
        aid: Long,
        rootRpid: Long,
        page: Int = 1,
        nextCursor: Long? = null,
        preferApiType: ApiType,
    ): CommentPage =
        when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi
                        .getVideoCommentReplies(
                            aid = aid,
                            rootRpid = rootRpid,
                            page = page,
                            accessKey = null,
                        ).getResponseData()
                parsePage(data, aid = aid, page = page, defaultType = 1, isThread = true)
            }

            ApiType.App -> {
                val reply =
                    replyStub?.detailList(
                        detailListReq {
                            oid = aid
                            type = 1
                            root = rootRpid
                            rpid = rootRpid
                            // gRPC 使用服务端返回的 next cursor 翻页，不能用本地页码代替
                            cursor = cursorReq { next = nextCursor ?: 0L }
                        },
                    ) ?: throw IllegalStateException("App gRPC reply stub is not initialized")
                val root = reply.root
                CommentPage(
                    comments =
                        root.repliesList
                            .map { Comment.fromGrpc(it, aid) }
                            .filter { it.rpid > 0L && it.rpid != rootRpid }
                            .distinctBy { it.rpid },
                    page = page,
                    total = 0,
                    hasMore = !reply.cursor.isEnd,
                    nextCursor = reply.cursor.next.takeIf { !reply.cursor.isEnd },
                )
            }
        }

    /**
     * 点赞或取消点赞评论。
     *
     * @param aid 视频 AV 号
     * @param rpid 评论 ID
     * @param like 是否点赞
     * @param preferApiType 首选接口类型
     * @throws IllegalStateException 当 B 站返回失败状态时抛出
     */
    suspend fun toggleCommentLike(
        aid: Long,
        rpid: Long,
        like: Boolean,
        preferApiType: ApiType,
    ) {
        val successMessage =
            BiliHttpApi.updateCommentLiked(
                aid = aid,
                rpid = rpid,
                like = like,
                csrf = authRepository.biliJct.takeIf { preferApiType == ApiType.Web },
                accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
            )
        check(successMessage.first) { successMessage.second.ifBlank { "评论点赞失败" } }
    }

    /**
     * 解析 Web 通道的评论 JSON。
     *
     * 主评论与楼中楼共用 `replies` 数组，但总数与页码字段不同：
     * - 主评论：`page.acount` 为总数，`page.count` 为兜底
     * - 楼中楼：`page.count` 为该根评论的二级评论总数
     *
     * @param isThread 是否为楼中楼分页
     */
    private fun parsePage(
        data: JsonObject,
        aid: Long,
        page: Int,
        defaultType: Int,
        isThread: Boolean,
    ): CommentPage {
        val pageObject = data["page"] as? JsonObject
        val repliesJson = data["replies"] as? JsonArray
        val rawCount = repliesJson?.count { it is JsonObject } ?: 0
        val comments =
            repliesJson
                ?.mapNotNull { it as? JsonObject }
                ?.map { Comment.fromJson(it, oid = aid, defaultType = defaultType) }
                .orEmpty()
                .filter { it.rpid > 0L }
                .distinctBy { it.rpid }
        val total =
            (
                if (isThread) {
                    pageObject?.longValue("count")
                } else {
                    pageObject?.longValue("acount") ?: pageObject?.longValue("count")
                }
            ) ?: 0L
        val pageNum = pageObject?.longValue("num")?.toInt()?.takeIf { it > 0 } ?: page
        val pageSize = pageObject?.longValue("size")?.toInt()?.takeIf { it > 0 } ?: DEFAULT_PAGE_SIZE
        return CommentPage(
            comments = comments,
            page = pageNum,
            total = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            hasMore = rawCount > 0 && (total <= 0L || pageNum.toLong() * pageSize < total),
        )
    }

    private companion object {
        /** 服务端未返回 `page.size` 时的默认每页数量。 */
        const val DEFAULT_PAGE_SIZE = 20
    }
}

private fun JsonObject.longValue(name: String): Long? = this[name]?.toString()?.trim('"')?.toLongOrNull()
