package dev.frost819.newbv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * [SearchHistoryDao] 的插桩测试。
 *
 * 在真实 SQLite 上验证 SQL 正确性：插入、查询、删除、按时间倒序。
 */
@RunWith(AndroidJUnit4::class)
class SearchHistoryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: SearchHistoryDao

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.searchHistoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_getAll() =
        runTest {
            dao.insert(SearchHistoryEntity(keyword = "test1"))

            val result = dao.getAll()

            assertThat(result).hasSize(1)
            assertThat(result[0].keyword).isEqualTo("test1")
        }

    @Test
    fun insert_multiple_and_getAll() =
        runTest {
            dao.insert(
                SearchHistoryEntity(keyword = "test1"),
                SearchHistoryEntity(keyword = "test2"),
                SearchHistoryEntity(keyword = "test3"),
            )

            val result = dao.getAll()

            assertThat(result).hasSize(3)
            assertThat(result.map { it.keyword }).containsExactly("test1", "test2", "test3")
        }

    @Test
    fun getHistories_returns_by_dateDesc() =
        runTest {
            val old = SearchHistoryEntity(keyword = "old", searchDate = Date(1000))
            val mid = SearchHistoryEntity(keyword = "mid", searchDate = Date(2000))
            val recent = SearchHistoryEntity(keyword = "recent", searchDate = Date(3000))
            dao.insert(old, mid, recent)

            val result = dao.getHistories(10)

            assertThat(result).hasSize(3)
            assertThat(result[0].keyword).isEqualTo("recent")
            assertThat(result[1].keyword).isEqualTo("mid")
            assertThat(result[2].keyword).isEqualTo("old")
        }

    @Test
    fun getHistories_respects_count_limit() =
        runTest {
            dao.insert(
                SearchHistoryEntity(keyword = "test1"),
                SearchHistoryEntity(keyword = "test2"),
                SearchHistoryEntity(keyword = "test3"),
            )

            val result = dao.getHistories(2)

            assertThat(result).hasSize(2)
        }

    @Test
    fun findHistory_returns_match() =
        runTest {
            dao.insert(SearchHistoryEntity(keyword = "hello"))

            val result = dao.findHistory("hello")

            assertThat(result).isNotNull()
            assertThat(result!!.keyword).isEqualTo("hello")
        }

    @Test
    fun findHistory_returns_null_when_not_found() =
        runTest {
            val result = dao.findHistory("nonexistent")

            assertThat(result).isNull()
        }

    @Test
    fun delete_removes_specific_history() =
        runTest {
            val history = SearchHistoryEntity(keyword = "test")
            dao.insert(history)
            val inserted = dao.getAll().first()

            dao.delete(inserted)

            assertThat(dao.getAll()).isEmpty()
        }

    @Test
    fun deleteAll_clears_all() =
        runTest {
            dao.insert(
                SearchHistoryEntity(keyword = "test1"),
                SearchHistoryEntity(keyword = "test2"),
            )

            dao.deleteAll()

            assertThat(dao.getAll()).isEmpty()
        }

    @Test
    fun update_changes_searchDate() =
        runTest {
            val history = SearchHistoryEntity(keyword = "test", searchDate = Date(1000))
            dao.insert(history)
            val inserted = dao.getAll().first()

            inserted.searchDate = Date(5000)
            dao.update(inserted)

            val result = dao.getHistories(1)
            assertThat(result[0].searchDate.time).isEqualTo(5000)
        }

    @Test
    fun getHistories_on_empty_table_returns_empty() =
        runTest {
            val result = dao.getHistories(10)

            assertThat(result).isEmpty()
        }
}
