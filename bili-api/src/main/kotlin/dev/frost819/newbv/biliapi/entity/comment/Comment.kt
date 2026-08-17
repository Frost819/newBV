package dev.frost819.newbv.biliapi.entity.comment

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 视频评论数据。
 *
 * 评论和楼中楼使用同一个模型，通过 [parentRpid] 与 [rootRpid] 表示层级关系。
 */
@Serializable
data class Comment(
    val rpid: Long,
    val oid: Long,
    val type: Int,
    val mid: Long,
    val rootRpid: Long,
    val parentRpid: Long,
    val userName: String,
    val avatar: String,
    val level: Int,
    val message: String,
    val pictures: List<String>,
    val ctime: Long,
    val likeCount: Long,
    val replyCount: Int,
    val isLiked: Boolean,
    val isUp: Boolean,
    val isExpanded: Boolean = false,
    val isLoadingReplies: Boolean = false,
    val replies: List<Comment> = emptyList(),
    val repliesError: Boolean = false,
) {
    /** 返回评论的展开/收起状态副本。 */
    fun withExpanded(expanded: Boolean): Comment = copy(isExpanded = expanded)

    /** 从 B 站 Web/App HTTP 评论 JSON 转换为统一模型。 */
    companion object {
        fun fromJson(
            json: JsonObject,
            oid: Long,
            defaultType: Int = 1,
        ): Comment {
            val member = json["member"] as? JsonObject
            val content = json["content"] as? JsonObject
            val control = json["reply_control"] as? JsonObject
            val levelInfo = member?.get("level_info") as? JsonObject
            val pictures =
                (content?.get("pictures") as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { picture ->
                        (picture as? JsonObject)?.get("img_src")?.jsonPrimitive?.contentOrNull
                    }
                    .orEmpty()
            val action =
                control?.get("action")?.jsonPrimitive?.intOrNull
                    ?: json["user_action"]?.jsonPrimitive?.intOrNull
                    ?: 0

            return Comment(
                rpid = json.long("rpid", json.long("id", 0L)),
                oid = json.long("oid", oid),
                type = json.int("type", defaultType),
                mid =
                    json.long(
                        "mid",
                        member?.long("mid", 0L) ?: 0L,
                    ),
                rootRpid = json.long("root", 0L),
                parentRpid = json.long("parent", 0L),
                userName = member?.string("uname") ?: "未知用户",
                avatar = member?.string("avatar") ?: member?.string("face") ?: "",
                level = levelInfo?.int("current_level", 0) ?: 0,
                message = content?.string("message") ?: json.string("content") ?: "",
                pictures = pictures,
                ctime = json.long("ctime", 0L),
                likeCount = json.long("like", 0L),
                replyCount =
                    json.int(
                        "rcount",
                        json.int("count", 0),
                    ),
                isLiked = action == 1,
                isUp =
                    control?.get("up_like")?.jsonPrimitive?.booleanOrNull == true ||
                        control?.get("is_up_top")?.jsonPrimitive?.booleanOrNull == true,
            )
        }
    }
}

/** 评论分页结果。 */
@Serializable
data class CommentPage(
    val comments: List<Comment>,
    val page: Int,
    val total: Int,
    val hasMore: Boolean,
)

private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

private fun JsonObject.long(
    name: String,
    default: Long,
): Long = this[name]?.jsonPrimitive?.longOrNull ?: default

private fun JsonObject.int(
    name: String,
    default: Int,
): Int = this[name]?.jsonPrimitive?.intOrNull ?: default
