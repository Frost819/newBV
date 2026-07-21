package dev.frost819.newbv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [UserDao] 的插桩测试。
 *
 * 在真实 SQLite 上验证 SQL 正确性：CRUD 操作。
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_getAll() = runTest {
        val user = UserEntity(
            uid = 1001L,
            username = "testUser",
            avatar = "https://example.com/avatar.jpg",
            auth = "{\"sessData\":\"abc\"}"
        )
        dao.insert(user)

        val result = dao.getAll()

        assertThat(result).hasSize(1)
        assertThat(result[0].uid).isEqualTo(1001L)
        assertThat(result[0].username).isEqualTo("testUser")
        assertThat(result[0].avatar).isEqualTo("https://example.com/avatar.jpg")
        assertThat(result[0].auth).isEqualTo("{\"sessData\":\"abc\"}")
    }

    @Test
    fun insert_multiple_users() = runTest {
        dao.insert(
            UserEntity(uid = 1L, username = "user1", avatar = "url1", auth = "auth1"),
            UserEntity(uid = 2L, username = "user2", avatar = "url2", auth = "auth2"),
            UserEntity(uid = 3L, username = "user3", avatar = "url3", auth = "auth3")
        )

        val result = dao.getAll()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.uid }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun findUserByUid_returns_match() = runTest {
        dao.insert(UserEntity(uid = 12345L, username = "found", avatar = "url", auth = "auth"))

        val result = dao.findUserByUid(12345L)

        assertThat(result).isNotNull()
        assertThat(result!!.username).isEqualTo("found")
    }

    @Test
    fun findUserByUid_returns_null_when_not_found() = runTest {
        val result = dao.findUserByUid(99999L)

        assertThat(result).isNull()
    }

    @Test
    fun delete_removes_user() = runTest {
        val user = UserEntity(uid = 100L, username = "delete", avatar = "url", auth = "auth")
        dao.insert(user)
        val inserted = dao.getAll().first()

        dao.delete(inserted)

        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun delete_specific_user_keeps_others() = runTest {
        dao.insert(
            UserEntity(uid = 1L, username = "keep", avatar = "url1", auth = "auth1"),
            UserEntity(uid = 2L, username = "delete", avatar = "url2", auth = "auth2")
        )
        val users = dao.getAll()
        val toDelete = users.find { it.uid == 2L }!!

        dao.delete(toDelete)

        val remaining = dao.getAll()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].uid).isEqualTo(1L)
    }

    @Test
    fun update_changes_username_and_avatar() = runTest {
        val user = UserEntity(uid = 100L, username = "oldName", avatar = "oldUrl", auth = "auth")
        dao.insert(user)
        val inserted = dao.getAll().first()

        inserted.username = "newName"
        inserted.avatar = "newUrl"
        dao.update(inserted)

        val result = dao.findUserByUid(100L)
        assertThat(result!!.username).isEqualTo("newName")
        assertThat(result.avatar).isEqualTo("newUrl")
    }

    @Test
    fun update_changes_auth_and_lock() = runTest {
        val user = UserEntity(uid = 100L, username = "user", avatar = "url", auth = "oldAuth")
        dao.insert(user)
        val inserted = dao.getAll().first()

        inserted.auth = "newAuth"
        inserted.lock = "1234"
        dao.update(inserted)

        val result = dao.findUserByUid(100L)
        assertThat(result!!.auth).isEqualTo("newAuth")
        assertThat(result.lock).isEqualTo("1234")
    }

    @Test
    fun default_lock_is_empty_string() = runTest {
        dao.insert(UserEntity(uid = 1L, username = "user", avatar = "url", auth = "auth"))

        val result = dao.findUserByUid(1L)
        assertThat(result!!.lock).isEmpty()
    }

    @Test
    fun getAll_on_empty_table_returns_empty() = runTest {
        val result = dao.getAll()

        assertThat(result).isEmpty()
    }
}
