package dev.frost819.newbv.biliapi.entity.comment

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
}
