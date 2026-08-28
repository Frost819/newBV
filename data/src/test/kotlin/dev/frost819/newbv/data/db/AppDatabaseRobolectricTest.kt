package dev.frost819.newbv.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * [AppDatabase] 及其 DAO 的 Robolectric 集成测试。
 *
 * 使用 Room 内存数据库验证 SearchHistoryDao 与 UserDao 的全部 CRUD 操作，
 * 同时覆盖 Room 生成的 [AppDatabase_Impl] 与 DAO 实现类的字节码。
 *
 * 由于 Room 生成的实现类需要 Android SQLite 环境，使用 Robolectric 提供的
 * ShadowSQLite 在 JVM 上运行真实 SQL。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseRobolectricTest {
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ===== SearchHistoryDao =====

    @Test
    fun insertSearchHistory_andFind() =
        runBlocking {
            database.searchHistoryDao().insert(SearchHistoryEntity(keyword = "test"))

            val found = database.searchHistoryDao().findHistory("test")
            assertThat(found).isNotNull()
            assertThat(found!!.keyword).isEqualTo("test")
            assertThat(found.id).isNotNull()
        }

    @Test
    fun insertMultipleSearchHistories_andGetAll() =
        runBlocking {
            database.searchHistoryDao().insert(
                SearchHistoryEntity(keyword = "a"),
                SearchHistoryEntity(keyword = "b"),
                SearchHistoryEntity(keyword = "c"),
            )

            val all = database.searchHistoryDao().getAll()
            assertThat(all).hasSize(3)
        }

    @Test
    fun getHistories_returnsOrderedByDateDesc() =
        runBlocking {
            val early = SearchHistoryEntity(keyword = "early", searchDate = Date(1000))
            val late = SearchHistoryEntity(keyword = "late", searchDate = Date(5000))
            database.searchHistoryDao().insert(early, late)

            val result = database.searchHistoryDao().getHistories(10)
            assertThat(result).hasSize(2)
            assertThat(result[0].keyword).isEqualTo("late")
            assertThat(result[1].keyword).isEqualTo("early")
        }

    @Test
    fun getHistories_respectsLimit() =
        runBlocking {
            for (i in 1..5) {
                database.searchHistoryDao().insert(SearchHistoryEntity(keyword = "kw$i"))
            }

            val result = database.searchHistoryDao().getHistories(3)
            assertThat(result).hasSize(3)
        }

    @Test
    fun getHistories_returnsEmptyWhenNoData() =
        runBlocking {
            val result = database.searchHistoryDao().getHistories(10)
            assertThat(result).isEmpty()
        }

    @Test
    fun findHistory_returnsNullWhenNotFound() =
        runBlocking {
            val result = database.searchHistoryDao().findHistory("nonexistent")
            assertThat(result).isNull()
        }

    @Test
    fun updateSearchHistory_changesDate() =
        runBlocking {
            val entity = SearchHistoryEntity(keyword = "test", searchDate = Date(1000))
            database.searchHistoryDao().insert(entity)

            val inserted = database.searchHistoryDao().findHistory("test")!!
            val newDate = Date(999999999999L)
            inserted.searchDate = newDate
            database.searchHistoryDao().update(inserted)

            val updated = database.searchHistoryDao().findHistory("test")!!
            assertThat(updated.searchDate).isEqualTo(newDate)
        }

    @Test
    fun deleteSearchHistory_removesRecord() =
        runBlocking {
            val entity = SearchHistoryEntity(keyword = "test")
            database.searchHistoryDao().insert(entity)

            val inserted = database.searchHistoryDao().findHistory("test")!!
            database.searchHistoryDao().delete(inserted)

            assertThat(database.searchHistoryDao().findHistory("test")).isNull()
        }

    @Test
    fun deleteAllSearchHistory_clearsAll() =
        runBlocking {
            database.searchHistoryDao().insert(
                SearchHistoryEntity(keyword = "a"),
                SearchHistoryEntity(keyword = "b"),
            )

            database.searchHistoryDao().deleteAll()

            assertThat(database.searchHistoryDao().getAll()).isEmpty()
        }

    // ===== UserDao =====

    @Test
    fun insertUser_andFindByUid() =
        runBlocking {
            val user =
                UserEntity(
                    uid = 12345L,
                    username = "testuser",
                    avatar = "http://example.com/a.png",
                    auth = """{"sessdata":"abc"}""",
                )
            database.userDao().insert(user)

            val found = database.userDao().findUserByUid(12345L)
            assertThat(found).isNotNull()
            assertThat(found!!.username).isEqualTo("testuser")
            assertThat(found.avatar).isEqualTo("http://example.com/a.png")
            assertThat(found.auth).isEqualTo("""{"sessdata":"abc"}""")
            assertThat(found.lock).isEmpty()
            assertThat(found.id).isNotNull()
        }

    @Test
    fun insertMultipleUsers_andGetAll() =
        runBlocking {
            database.userDao().insert(
                UserEntity(uid = 1L, username = "u1", avatar = "", auth = ""),
                UserEntity(uid = 2L, username = "u2", avatar = "", auth = ""),
            )

            val all = database.userDao().getAll()
            assertThat(all).hasSize(2)
        }

    @Test
    fun findUserByUid_returnsNullWhenNotFound() =
        runBlocking {
            val result = database.userDao().findUserByUid(99999L)
            assertThat(result).isNull()
        }

    @Test
    fun updateUser_changesFields() =
        runBlocking {
            database.userDao().insert(
                UserEntity(uid = 100L, username = "old", avatar = "old_url", auth = "old_auth"),
            )

            val inserted = database.userDao().findUserByUid(100L)!!
            inserted.username = "new"
            inserted.avatar = "new_url"
            inserted.auth = "new_auth"
            inserted.lock = "1234"
            database.userDao().update(inserted)

            val updated = database.userDao().findUserByUid(100L)!!
            assertThat(updated.username).isEqualTo("new")
            assertThat(updated.avatar).isEqualTo("new_url")
            assertThat(updated.auth).isEqualTo("new_auth")
            assertThat(updated.lock).isEqualTo("1234")
        }

    @Test
    fun deleteUser_removesRecord() =
        runBlocking {
            database.userDao().insert(
                UserEntity(uid = 200L, username = "toremove", avatar = "", auth = ""),
            )

            val inserted = database.userDao().findUserByUid(200L)!!
            database.userDao().delete(inserted)

            assertThat(database.userDao().findUserByUid(200L)).isNull()
        }

    @Test
    fun getAllUsers_returnsEmptyWhenNoData() =
        runBlocking {
            val result = database.userDao().getAll()
            assertThat(result).isEmpty()
        }

    // ===== Converters integration =====

    @Test
    fun dateConverter_persistsAndRestoresCorrectly() =
        runBlocking {
            val date = Date(1700000000000L)
            database.searchHistoryDao().insert(
                SearchHistoryEntity(keyword = "datetest", searchDate = date),
            )

            val found = database.searchHistoryDao().findHistory("datetest")!!
            assertThat(found.searchDate.time).isEqualTo(1700000000000L)
        }

    @Test
    fun userEntity_withLock_persistsCorrectly() =
        runBlocking {
            database.userDao().insert(
                UserEntity(uid = 300L, username = "locked", avatar = "", auth = "", lock = "9999"),
            )

            val found = database.userDao().findUserByUid(300L)!!
            assertThat(found.lock).isEqualTo("9999")
        }

    // ===== AppDatabase =====

    @Test
    fun databaseName_isCorrect() {
        assertThat(AppDatabase.DATABASE_NAME).isEqualTo("AppDatabase.db")
    }

    @Test
    fun searchHistoryDao_returnsSameInstance() {
        val dao1 = database.searchHistoryDao()
        val dao2 = database.searchHistoryDao()
        assertThat(dao1).isSameInstanceAs(dao2)
    }

    @Test
    fun userDao_returnsSameInstance() {
        val dao1 = database.userDao()
        val dao2 = database.userDao()
        assertThat(dao1).isSameInstanceAs(dao2)
    }
}
