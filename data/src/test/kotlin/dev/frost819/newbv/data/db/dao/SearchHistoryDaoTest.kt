package dev.frost819.newbv.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.AppDatabase
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * [SearchHistoryDao] 的 Robolectric 单元测试。
 *
 * 使用 Room 内存数据库验证搜索历史 DAO 的全部操作，
 * 重点覆盖 [AppDatabaseRobolectricTest] 未涉及的边界条件：
 * 空表查询、零/超大 limit、重复关键词、vararg 批量删除、
 * 特殊字符关键词、不存在的记录更新/删除安全性等。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SearchHistoryDao

    @Before
    fun setup() {
        ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.searchHistoryDao()
    }

    @After
    fun teardown() = database.close()

    // ===== getAll =====

    @Test
    fun `getAll returns empty list on empty table`() = runTest {
        val result = dao.getAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getAll returns all records with correct field values`() = runTest {
        val date1 = Date(1000L)
        val date2 = Date(2000L)
        dao.insert(
            SearchHistoryEntity(keyword = "alpha", searchDate = date1),
            SearchHistoryEntity(keyword = "beta", searchDate = date2),
        )

        val result = dao.getAll()

        assertThat(result).hasSize(2)
        val keywords = result.map { it.keyword }
        assertThat(keywords).containsExactly("alpha", "beta")
        val dates = result.map { it.searchDate }
        assertThat(dates).containsExactly(date1, date2)
    }

    @Test
    fun `getAll returns records in insertion order when no explicit ordering`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "first"),
            SearchHistoryEntity(keyword = "second"),
            SearchHistoryEntity(keyword = "third"),
        )

        val result = dao.getAll()

        assertThat(result).hasSize(3)
        assertThat(result[0].keyword).isEqualTo("first")
        assertThat(result[1].keyword).isEqualTo("second")
        assertThat(result[2].keyword).isEqualTo("third")
    }

    // ===== getHistories =====

    @Test
    fun `getHistories with zero count returns empty list`() = runTest {
        dao.insert(SearchHistoryEntity(keyword = "test"))

        val result = dao.getHistories(0)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getHistories with count larger than table size returns all records`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "a", searchDate = Date(1000)),
            SearchHistoryEntity(keyword = "b", searchDate = Date(2000)),
        )

        val result = dao.getHistories(100)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getHistories returns newest first when timestamps differ`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "oldest", searchDate = Date(1000)),
            SearchHistoryEntity(keyword = "middle", searchDate = Date(5000)),
            SearchHistoryEntity(keyword = "newest", searchDate = Date(9000)),
        )

        val result = dao.getHistories(10)

        assertThat(result).hasSize(3)
        assertThat(result[0].keyword).isEqualTo("newest")
        assertThat(result[1].keyword).isEqualTo("middle")
        assertThat(result[2].keyword).isEqualTo("oldest")
    }

    @Test
    fun `getHistories preserves searchDate through Date converter round-trip`() = runTest {
        val preciseDate = Date(1700000000123L)
        dao.insert(SearchHistoryEntity(keyword = "precise", searchDate = preciseDate))

        val result = dao.getHistories(1)

        assertThat(result[0].searchDate.time).isEqualTo(1700000000123L)
    }

    // ===== findHistory =====

    @Test
    fun `findHistory with unicode keyword returns correct record`() = runTest {
        dao.insert(SearchHistoryEntity(keyword = "哔哩哔哩"))

        val result = dao.findHistory("哔哩哔哩")

        assertThat(result).isNotNull()
        assertThat(result!!.keyword).isEqualTo("哔哩哔哩")
    }

    @Test
    fun `findHistory with empty string keyword returns correct record`() = runTest {
        dao.insert(SearchHistoryEntity(keyword = ""))

        val result = dao.findHistory("")

        assertThat(result).isNotNull()
        assertThat(result!!.keyword).isEmpty()
    }

    @Test
    fun `findHistory with special characters keyword returns correct record`() = runTest {
        val keyword = "test' OR 1=1; --"
        dao.insert(SearchHistoryEntity(keyword = keyword))

        val result = dao.findHistory(keyword)

        assertThat(result).isNotNull()
        assertThat(result!!.keyword).isEqualTo(keyword)
    }

    @Test
    fun `findHistory is case sensitive`() = runTest {
        dao.insert(SearchHistoryEntity(keyword = "Test"))

        val result = dao.findHistory("test")

        assertThat(result).isNull()
    }

    // ===== insert =====

    @Test
    fun `insert assigns auto-generated sequential ids`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "first"),
            SearchHistoryEntity(keyword = "second"),
        )

        val all = dao.getAll()
        assertThat(all[0].id).isEqualTo(1)
        assertThat(all[1].id).isEqualTo(2)
    }

    @Test
    fun `insert duplicate keyword creates separate records`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "dup", searchDate = Date(1000)),
            SearchHistoryEntity(keyword = "dup", searchDate = Date(2000)),
        )

        val all = dao.getAll()

        assertThat(all).hasSize(2)
        assertThat(all[0].keyword).isEqualTo("dup")
        assertThat(all[1].keyword).isEqualTo("dup")
        assertThat(all[0].id).isNotEqualTo(all[1].id)
    }

    // ===== delete =====

    @Test
    fun `delete multiple entities via vararg removes all specified`() = runTest {
        val entity1 = SearchHistoryEntity(keyword = "del1")
        val entity2 = SearchHistoryEntity(keyword = "del2")
        val entity3 = SearchHistoryEntity(keyword = "keep")
        dao.insert(entity1, entity2, entity3)

        val inserted1 = dao.findHistory("del1")!!
        val inserted2 = dao.findHistory("del2")!!
        dao.delete(inserted1, inserted2)

        val remaining = dao.getAll()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].keyword).isEqualTo("keep")
    }

    @Test
    fun `delete non-existent entity does not throw`() = runTest {
        val nonExistent = SearchHistoryEntity(id = 999, keyword = "ghost")

        dao.delete(nonExistent)

        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun `delete one entity when multiple exist removes only that record`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "a"),
            SearchHistoryEntity(keyword = "b"),
            SearchHistoryEntity(keyword = "c"),
        )

        val toDelete = dao.findHistory("b")!!
        dao.delete(toDelete)

        val remaining = dao.getAll()
        assertThat(remaining).hasSize(2)
        assertThat(remaining.map { it.keyword }).containsExactly("a", "c")
    }

    // ===== deleteAll =====

    @Test
    fun `deleteAll on empty table does not throw`() = runTest {
        dao.deleteAll()

        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun `deleteAll removes all records leaving table empty`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "x"),
            SearchHistoryEntity(keyword = "y"),
            SearchHistoryEntity(keyword = "z"),
        )

        dao.deleteAll()

        assertThat(dao.getAll()).isEmpty()
        assertThat(dao.getHistories(10)).isEmpty()
    }

    // ===== update =====

    @Test
    fun `update non-existent entity does not throw and does not insert`() = runTest {
        val nonExistent = SearchHistoryEntity(id = 999, keyword = "phantom", searchDate = Date(1000))

        dao.update(nonExistent)

        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun `update preserves id and keyword while changing date`() = runTest {
        dao.insert(SearchHistoryEntity(keyword = "original", searchDate = Date(1000)))

        val inserted = dao.findHistory("original")!!
        val originalId = inserted.id
        inserted.searchDate = Date(999999999999L)
        dao.update(inserted)

        val updated = dao.findHistory("original")!!
        assertThat(updated.id).isEqualTo(originalId)
        assertThat(updated.keyword).isEqualTo("original")
        assertThat(updated.searchDate.time).isEqualTo(999999999999L)
    }

    @Test
    fun `update does not change total record count`() = runTest {
        dao.insert(
            SearchHistoryEntity(keyword = "a", searchDate = Date(1000)),
            SearchHistoryEntity(keyword = "b", searchDate = Date(2000)),
        )

        val entity = dao.findHistory("a")!!
        entity.searchDate = Date(9999)
        dao.update(entity)

        assertThat(dao.getAll()).hasSize(2)
    }

    // ===== combined workflow =====

    @Test
    fun `insert find update delete full lifecycle`() = runTest {
        // Given: insert a record
        dao.insert(SearchHistoryEntity(keyword = "lifecycle", searchDate = Date(1000)))

        // When: find it
        val found = dao.findHistory("lifecycle")
        assertThat(found).isNotNull()
        assertThat(found!!.keyword).isEqualTo("lifecycle")

        // When: update it
        found.searchDate = Date(5000)
        dao.update(found)
        val updated = dao.findHistory("lifecycle")!!
        assertThat(updated.searchDate.time).isEqualTo(5000)

        // When: delete it
        dao.delete(updated)
        assertThat(dao.findHistory("lifecycle")).isNull()
        assertThat(dao.getAll()).isEmpty()
    }
}
