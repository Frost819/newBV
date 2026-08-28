package dev.frost819.newbv.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.AppDatabase
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [UserDao] 的 Robolectric 单元测试。
 *
 * 使用 Room 内存数据库验证账户 DAO 的全部操作，
 * 重点覆盖 [AppDatabaseRobolectricTest] 未涉及的边界条件：
 * 字段值完整性、多用户查找选择性、vararg 批量操作、
 * 重复 UID、不存在的记录更新/删除安全性、特殊字符持久化等。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.userDao()
    }

    @After
    fun teardown() = database.close()

    // ===== getAll =====

    @Test
    fun `getAll returns empty list on empty table`() =
        runTest {
            val result = dao.getAll()

            assertThat(result).isEmpty()
        }

    @Test
    fun `getAll returns all users with correct field values`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "alice", avatar = "url1", auth = "auth1", lock = "l1"),
                UserEntity(uid = 2L, username = "bob", avatar = "url2", auth = "auth2"),
            )

            val result = dao.getAll()

            assertThat(result).hasSize(2)
            val alice = result.find { it.username == "alice" }!!
            assertThat(alice.uid).isEqualTo(1L)
            assertThat(alice.avatar).isEqualTo("url1")
            assertThat(alice.auth).isEqualTo("auth1")
            assertThat(alice.lock).isEqualTo("l1")

            val bob = result.find { it.username == "bob" }!!
            assertThat(bob.uid).isEqualTo(2L)
            assertThat(bob.lock).isEmpty()
        }

    // ===== findUserByUid =====

    @Test
    fun `findUserByUid among multiple users returns correct one`() =
        runTest {
            dao.insert(
                UserEntity(uid = 100L, username = "u1", avatar = "a1", auth = "x1"),
                UserEntity(uid = 200L, username = "u2", avatar = "a2", auth = "x2"),
                UserEntity(uid = 300L, username = "u3", avatar = "a3", auth = "x3"),
            )

            val result = dao.findUserByUid(200L)

            assertThat(result).isNotNull()
            assertThat(result!!.username).isEqualTo("u2")
            assertThat(result.avatar).isEqualTo("a2")
            assertThat(result.auth).isEqualTo("x2")
        }

    @Test
    fun `findUserByUid with large uid value`() =
        runTest {
            val largeUid = Long.MAX_VALUE
            dao.insert(UserEntity(uid = largeUid, username = "big", avatar = "", auth = ""))

            val result = dao.findUserByUid(largeUid)

            assertThat(result).isNotNull()
            assertThat(result!!.uid).isEqualTo(largeUid)
            assertThat(result.username).isEqualTo("big")
        }

    @Test
    fun `findUserByUid with zero uid returns correct user`() =
        runTest {
            dao.insert(UserEntity(uid = 0L, username = "zero", avatar = "", auth = ""))

            val result = dao.findUserByUid(0L)

            assertThat(result).isNotNull()
            assertThat(result!!.username).isEqualTo("zero")
        }

    @Test
    fun `findUserByUid with negative uid returns correct user`() =
        runTest {
            dao.insert(UserEntity(uid = -1L, username = "negative", avatar = "", auth = ""))

            val result = dao.findUserByUid(-1L)

            assertThat(result).isNotNull()
            assertThat(result!!.uid).isEqualTo(-1L)
        }

    // ===== insert =====

    @Test
    fun `insert multiple users via vararg in single call`() =
        runTest {
            dao.insert(
                UserEntity(uid = 10L, username = "a", avatar = "", auth = ""),
                UserEntity(uid = 20L, username = "b", avatar = "", auth = ""),
                UserEntity(uid = 30L, username = "c", avatar = "", auth = ""),
            )

            val all = dao.getAll()
            assertThat(all).hasSize(3)
            assertThat(all.map { it.uid }).containsExactly(10L, 20L, 30L)
        }

    @Test
    fun `insert assigns auto-generated sequential ids`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "first", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "second", avatar = "", auth = ""),
            )

            val all = dao.getAll()
            assertThat(all[0].id).isEqualTo(1)
            assertThat(all[1].id).isEqualTo(2)
        }

    @Test
    fun `insert duplicate uid creates separate records`() =
        runTest {
            dao.insert(
                UserEntity(uid = 42L, username = "first", avatar = "a1", auth = "x1"),
                UserEntity(uid = 42L, username = "second", avatar = "a2", auth = "x2"),
            )

            val all = dao.getAll()

            assertThat(all).hasSize(2)
            assertThat(all[0].uid).isEqualTo(42L)
            assertThat(all[1].uid).isEqualTo(42L)
            assertThat(all[0].id).isNotEqualTo(all[1].id)
            assertThat(all.map { it.username }).containsExactly("first", "second")
        }

    @Test
    fun `user with empty string fields persists correctly`() =
        runTest {
            dao.insert(UserEntity(uid = 1L, username = "", avatar = "", auth = "", lock = ""))

            val found = dao.findUserByUid(1L)!!

            assertThat(found.username).isEmpty()
            assertThat(found.avatar).isEmpty()
            assertThat(found.auth).isEmpty()
            assertThat(found.lock).isEmpty()
        }

    @Test
    fun `user with special characters in auth field persists correctly`() =
        runTest {
            val specialAuth = """{"sessdata":"abc\r\n","bili_jct":"x\"y","token":"日本語"}"""
            dao.insert(UserEntity(uid = 1L, username = "special", avatar = "http://a.b/c?d=1&e=2", auth = specialAuth))

            val found = dao.findUserByUid(1L)!!

            assertThat(found.auth).isEqualTo(specialAuth)
            assertThat(found.avatar).isEqualTo("http://a.b/c?d=1&e=2")
        }

    // ===== delete =====

    @Test
    fun `delete multiple users via vararg removes all specified`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "del1", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "del2", avatar = "", auth = ""),
                UserEntity(uid = 3L, username = "keep", avatar = "", auth = ""),
            )

            val user1 = dao.findUserByUid(1L)!!
            val user2 = dao.findUserByUid(2L)!!
            dao.delete(user1, user2)

            val remaining = dao.getAll()
            assertThat(remaining).hasSize(1)
            assertThat(remaining[0].username).isEqualTo("keep")
        }

    @Test
    fun `delete non-existent user does not throw`() =
        runTest {
            val nonExistent = UserEntity(id = 999, uid = 999L, username = "ghost", avatar = "", auth = "")

            dao.delete(nonExistent)

            assertThat(dao.getAll()).isEmpty()
        }

    @Test
    fun `getAll after partial delete returns remaining users`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "a", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "b", avatar = "", auth = ""),
                UserEntity(uid = 3L, username = "c", avatar = "", auth = ""),
            )

            val toDelete = dao.findUserByUid(2L)!!
            dao.delete(toDelete)

            val remaining = dao.getAll()
            assertThat(remaining).hasSize(2)
            assertThat(remaining.map { it.uid }).containsExactly(1L, 3L)
        }

    @Test
    fun `delete all users one by one then getAll returns empty`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "a", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "b", avatar = "", auth = ""),
            )

            dao.getAll().forEach { dao.delete(it) }

            assertThat(dao.getAll()).isEmpty()
        }

    // ===== update =====

    @Test
    fun `update non-existent user does not throw and does not insert`() =
        runTest {
            val nonExistent = UserEntity(id = 999, uid = 999L, username = "phantom", avatar = "", auth = "")

            dao.update(nonExistent)

            assertThat(dao.getAll()).isEmpty()
        }

    @Test
    fun `update preserves id while changing all mutable fields`() =
        runTest {
            dao.insert(UserEntity(uid = 500L, username = "old", avatar = "old_url", auth = "old_auth"))

            val inserted = dao.findUserByUid(500L)!!
            val originalId = inserted.id
            inserted.username = "new"
            inserted.avatar = "new_url"
            inserted.auth = "new_auth"
            inserted.lock = "4321"
            dao.update(inserted)

            val updated = dao.findUserByUid(500L)!!
            assertThat(updated.id).isEqualTo(originalId)
            assertThat(updated.username).isEqualTo("new")
            assertThat(updated.avatar).isEqualTo("new_url")
            assertThat(updated.auth).isEqualTo("new_auth")
            assertThat(updated.lock).isEqualTo("4321")
        }

    @Test
    fun `update does not change total record count`() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "a", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "b", avatar = "", auth = ""),
            )

            val entity = dao.findUserByUid(1L)!!
            entity.username = "updated"
            dao.update(entity)

            assertThat(dao.getAll()).hasSize(2)
        }

    @Test
    fun `update changing lock from empty to non-empty persists correctly`() =
        runTest {
            dao.insert(UserEntity(uid = 1L, username = "user", avatar = "", auth = ""))

            val entity = dao.findUserByUid(1L)!!
            assertThat(entity.lock).isEmpty()
            entity.lock = "secret"
            dao.update(entity)

            val found = dao.findUserByUid(1L)!!
            assertThat(found.lock).isEqualTo("secret")
        }

    @Test
    fun `update changing lock from non-empty to empty persists correctly`() =
        runTest {
            dao.insert(UserEntity(uid = 1L, username = "user", avatar = "", auth = "", lock = "password"))

            val entity = dao.findUserByUid(1L)!!
            assertThat(entity.lock).isEqualTo("password")
            entity.lock = ""
            dao.update(entity)

            val found = dao.findUserByUid(1L)!!
            assertThat(found.lock).isEmpty()
        }

    // ===== combined workflow =====

    @Test
    fun `insert find update delete full lifecycle`() =
        runTest {
            // Given: insert a user
            dao.insert(UserEntity(uid = 777L, username = "lifecycle", avatar = "avatar", auth = "auth"))

            // When: find it
            val found = dao.findUserByUid(777L)
            assertThat(found).isNotNull()
            assertThat(found!!.username).isEqualTo("lifecycle")

            // When: update it
            found.username = "updated"
            found.avatar = "new_avatar"
            dao.update(found)
            val updated = dao.findUserByUid(777L)!!
            assertThat(updated.username).isEqualTo("updated")
            assertThat(updated.avatar).isEqualTo("new_avatar")

            // When: delete it
            dao.delete(updated)
            assertThat(dao.findUserByUid(777L)).isNull()
            assertThat(dao.getAll()).isEmpty()
        }
}
