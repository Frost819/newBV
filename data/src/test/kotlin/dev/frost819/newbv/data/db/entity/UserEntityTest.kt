package dev.frost819.newbv.data.db.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UserEntity] data class 的单元测试。
 *
 * 验证构造默认值、equals/hashCode、copy、destructuring、可变字段修改及 lock 默认值。
 */
class UserEntityTest {
    @Test
    fun `construct with required fields only uses defaults for id and lock`() {
        val entity = UserEntity(uid = 123L, username = "user", avatar = "url", auth = "{}")
        assertThat(entity.id).isNull()
        assertThat(entity.uid).isEqualTo(123L)
        assertThat(entity.username).isEqualTo("user")
        assertThat(entity.avatar).isEqualTo("url")
        assertThat(entity.auth).isEqualTo("{}")
        assertThat(entity.lock).isEmpty()
    }

    @Test
    fun `construct with all fields`() {
        val entity =
            UserEntity(
                id = 1,
                uid = 456L,
                username = "test",
                avatar = "http://example.com/a.png",
                auth = """{"sessdata":"abc"}""",
                lock = "1234",
            )
        assertThat(entity.id).isEqualTo(1)
        assertThat(entity.uid).isEqualTo(456L)
        assertThat(entity.username).isEqualTo("test")
        assertThat(entity.avatar).isEqualTo("http://example.com/a.png")
        assertThat(entity.auth).isEqualTo("""{"sessdata":"abc"}""")
        assertThat(entity.lock).isEqualTo("1234")
    }

    @Test
    fun `username is mutable`() {
        val entity = UserEntity(uid = 1L, username = "old", avatar = "", auth = "")
        entity.username = "new"
        assertThat(entity.username).isEqualTo("new")
    }

    @Test
    fun `avatar is mutable`() {
        val entity = UserEntity(uid = 1L, username = "", avatar = "old", auth = "")
        entity.avatar = "new"
        assertThat(entity.avatar).isEqualTo("new")
    }

    @Test
    fun `auth is mutable`() {
        val entity = UserEntity(uid = 1L, username = "", avatar = "", auth = "old")
        entity.auth = "new"
        assertThat(entity.auth).isEqualTo("new")
    }

    @Test
    fun `lock is mutable`() {
        val entity = UserEntity(uid = 1L, username = "", avatar = "", auth = "")
        entity.lock = "password"
        assertThat(entity.lock).isEqualTo("password")
    }

    @Test
    fun `equals returns true for same values`() {
        val a = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x", lock = "l")
        val b = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x", lock = "l")
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals returns false for different uid`() {
        val a = UserEntity(id = 1, uid = 100L, username = "u", avatar = "", auth = "")
        val b = UserEntity(id = 1, uid = 200L, username = "u", avatar = "", auth = "")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for different username`() {
        val a = UserEntity(id = 1, uid = 100L, username = "u1", avatar = "", auth = "")
        val b = UserEntity(id = 1, uid = 100L, username = "u2", avatar = "", auth = "")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for different lock`() {
        val a = UserEntity(id = 1, uid = 100L, username = "u", avatar = "", auth = "", lock = "1")
        val b = UserEntity(id = 1, uid = 100L, username = "u", avatar = "", auth = "", lock = "2")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for null`() {
        val entity = UserEntity(uid = 1L, username = "", avatar = "", auth = "")
        assertThat(entity).isNotEqualTo(null)
    }

    @Test
    fun `hashCode is consistent for same values`() {
        val a = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x", lock = "l")
        val b = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x", lock = "l")
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `copy creates equal entity`() {
        val original = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x")
        val copied = original.copy()
        assertThat(copied).isEqualTo(original)
    }

    @Test
    fun `copy with modified username`() {
        val original = UserEntity(id = 1, uid = 100L, username = "old", avatar = "", auth = "")
        val copied = original.copy(username = "new")
        assertThat(copied.username).isEqualTo("new")
        assertThat(copied.uid).isEqualTo(100L)
    }

    @Test
    fun `copy with modified lock`() {
        val original = UserEntity(uid = 1L, username = "", avatar = "", auth = "")
        val copied = original.copy(lock = "9999")
        assertThat(copied.lock).isEqualTo("9999")
    }

    @Test
    fun `component functions return correct values`() {
        val entity = UserEntity(id = 1, uid = 100L, username = "u", avatar = "a", auth = "x", lock = "l")
        assertThat(entity.component1()).isEqualTo(1)
        assertThat(entity.component2()).isEqualTo(100L)
        assertThat(entity.component3()).isEqualTo("u")
        assertThat(entity.component4()).isEqualTo("a")
        assertThat(entity.component5()).isEqualTo("x")
        assertThat(entity.component6()).isEqualTo("l")
    }

    @Test
    fun `toString contains username`() {
        val entity = UserEntity(uid = 1L, username = "myUser", avatar = "", auth = "")
        assertThat(entity.toString()).contains("myUser")
    }
}
