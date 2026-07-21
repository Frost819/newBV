package dev.frost819.newbv.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity

/**
 * 搜索历史数据访问对象。
 *
 * 提供搜索历史的增删改查，所有方法为 `suspend`，需在协程中调用。
 * 查询方法按 `search_date DESC` 排序，最近搜索在最前。
 */
@Dao
interface SearchHistoryDao {
    /** 查询全部搜索历史。 */
    @Query("SELECT * FROM search_history")
    suspend fun getAll(): List<SearchHistoryEntity>

    /**
     * 查询最近的 [count] 条搜索历史，按搜索时间倒序。
     *
     * @param count 返回的最大条数。
     */
    @Query("SELECT * FROM search_history ORDER BY search_date DESC LIMIT :count")
    suspend fun getHistories(count: Int): List<SearchHistoryEntity>

    /**
     * 按关键词精确查找历史记录。
     *
     * @param keyword 搜索关键词。
     * @return 命中的记录，无匹配返回 `null`。
     */
    @Query("SELECT * FROM search_history WHERE keyword = :keyword LIMIT 1")
    suspend fun findHistory(keyword: String): SearchHistoryEntity?

    /** 插入一条或多条搜索历史。 */
    @Insert
    suspend fun insert(vararg searchHistory: SearchHistoryEntity)

    /** 删除一条或多条搜索历史。 */
    @Delete
    suspend fun delete(vararg searchHistory: SearchHistoryEntity)

    /** 清空全部搜索历史。 */
    @Query("DELETE FROM search_history")
    suspend fun deleteAll()

    /** 更新一条搜索历史（如刷新搜索时间）。 */
    @Update
    suspend fun update(searchHistory: SearchHistoryEntity)
}
