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
     * @param page 页码，从 1 开始
     * @param preferApiType 首选接口类型
     */
    suspend fun getComments(
        aid: Long,
        sort: Int = 1,
        page: Int = 1,
        preferApiType: ApiType = ApiType.Web,
    ): CommentPage {
        return when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.getVideoComments(aid, sort, page, accessKey = null).getResponseData()
                parsePage(data, aid = aid, page = page, defaultType = 1)
            }

            ApiType.App -> {
                val reply =
                    replyStub?.mainList(
                        mainListReq {
                            oid = aid
                            type = 1
                            cursor =
                                cursorReq {
                                    next = if (page <= 1) 0 else page.toLong()
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
                    comments = reply.repliesList.map { Comment.fromGrpc(it, aid) },
                    page = page,
                    total = 0,
                    hasMore = !reply.cursor.isEnd,
                )
            }
        }
    }

    /**
     * 获取根评论下的楼中楼分页。
     *
     * @param aid 视频 AV 号
     * @param rootRpid 根评论 ID
     * @param page 页码，从 1 开始
     * @param preferApiType 首选接口类型
     */
    suspend fun getReplies(
        aid: Long,
        rootRpid: Long,
        page: Int = 1,
        preferApiType: ApiType = ApiType.Web,
    ): CommentPage {
        return when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.getVideoCommentReplies(
                        aid = aid,
                        rootRpid = rootRpid,
                        page = page,
                        accessKey = null,
                    ).getResponseData()
                parsePage(data, aid = aid, page = page, defaultType = 1)
            }

            ApiType.App -> {
                val reply =
                    replyStub?.detailList(
                        detailListReq {
                            oid = aid
                            type = 1
                            root = rootRpid
                            rpid = rootRpid
                            cursor = cursorReq { next = if (page <= 1) 0 else page.toLong() }
                        },
                    ) ?: throw IllegalStateException("App gRPC reply stub is not initialized")
                val root = reply.root
                CommentPage(
                    comments = root.repliesList.map { Comment.fromGrpc(it, aid) },
                    page = page,
                    total = 0,
                    hasMore = !reply.cursor.isEnd,
                )
            }
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
        preferApiType: ApiType = ApiType.Web,
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

    private fun accessKey(preferApiType: ApiType): String? =
        authRepository.accessToken.takeIf { preferApiType == ApiType.App && !it.isNullOrBlank() }

    private fun parsePage(
        data: JsonObject,
        aid: Long,
        page: Int,
        defaultType: Int,
    ): CommentPage {
        val pageObject = data["page"] as? JsonObject
        val replies =
            (data["replies"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject }
                ?.map { Comment.fromJson(it, oid = aid, defaultType = defaultType) }
                .orEmpty()
        val total =
            pageObject?.longValue("acount")
                ?: pageObject?.longValue("count")
                ?: 0L
        return CommentPage(
            comments = replies,
            page = page,
            total = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            hasMore = replies.isNotEmpty() && (total <= 0L || page * 20 < total),
        )
    }
}

private fun JsonObject.longValue(name: String): Long? = this[name]?.toString()?.trim('"')?.toLongOrNull()
