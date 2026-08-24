package dev.frost819.newbv.biliapi.entity.comment

import bilibili.main.community.reply.v1.content
import bilibili.main.community.reply.v1.member
import bilibili.main.community.reply.v1.picture
import bilibili.main.community.reply.v1.replyControl
import bilibili.main.community.reply.v1.replyInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [Comment] JSON 转换测试。 */
class CommentTest {
    @Test
    fun `fromJson maps user content action and reply metadata`() {
        val json =
            Json.parseToJsonElement(
                """
                {
                  "rpid": 123,
                  "oid": 456,
                  "type": 1,
                  "mid": 789,
                  "root": 0,
                  "parent": 0,
                  "like": 12,
                  "rcount": 3,
                  "count": 5,
                  "ctime": 1700000000,
                  "member": {
                    "uname": "测试用户",
                    "avatar": "https://example.com/avatar.png",
                    "level_info": { "current_level": 6 }
                  },
                  "content": {
                    "message": "测试评论",
                    "pictures": [{ "img_src": "https://example.com/picture.png" }]
                  },
                  "reply_control": { "action": 1, "up_like": true }
                }
                """.trimIndent(),
            ).jsonObject

        val comment = Comment.fromJson(json, oid = 456)

        assertEquals(123L, comment.rpid)
        assertEquals("测试用户", comment.userName)
        assertEquals("测试评论", comment.message)
        assertEquals(6, comment.level)
        assertEquals(12L, comment.likeCount)
        assertEquals(3, comment.replyCount)
        assertEquals("https://example.com/picture.png", comment.pictures.single())
        assertTrue(comment.isLiked)
        assertTrue(comment.isUp)
    }

    @Test
    fun `fromJson uses safe defaults for incomplete response`() {
        val comment = Comment.fromJson(Json.parseToJsonElement("{}").jsonObject, oid = 456)

        assertEquals(456L, comment.oid)
        assertEquals("未知用户", comment.userName)
        assertEquals("", comment.message)
        assertFalse(comment.isLiked)
        assertEquals(0, comment.replyCount)
    }

    @Test
    fun `fromGrpc maps reply info into unified comment`() {
        val comment =
            Comment.fromGrpc(
                replyInfo {
                    id = 123L
                    oid = 456L
                    type = 1L
                    mid = 789L
                    root = 10L
                    parent = 11L
                    like = 12L
                    count = 3L
                    ctime = 1700000000L
                    member =
                        member {
                            name = "测试用户"
                            face = "https://example.com/avatar.png"
                            level = 6L
                        }
                    content =
                        content {
                            message = "测试评论"
                            pictures += picture { imgSrc = "https://example.com/picture.png" }
                        }
                    replyControl =
                        replyControl {
                            action = 1L
                            upLike = true
                        }
                },
                oid = 456L,
            )

        assertEquals(123L, comment.rpid)
        assertEquals(789L, comment.mid)
        assertEquals(10L, comment.rootRpid)
        assertEquals("测试用户", comment.userName)
        assertEquals("测试评论", comment.message)
        assertEquals(6, comment.level)
        assertTrue(comment.isLiked)
        assertTrue(comment.isUp)
    }
}
