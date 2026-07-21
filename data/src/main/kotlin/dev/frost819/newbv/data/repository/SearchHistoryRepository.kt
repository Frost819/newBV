package dev.frost819.newbv.data.repository

import dev.frost819.newbv.data.db.entity.SearchHistoryEntity

/**
 * 搜索历史仓库接口。
 *
 * 定义搜索历史的增删查抽象，实现由 app 层提供（注入 [SearchHistoryDao]）。
 * 搜索历史受无痕模式（[Prefs.incognitoMode]）控制，无痕模式下不记录。
 *
 * @see SearchHistoryEntity
 */
interface SearchHistoryRepository {

    /**
     * 查询最近的搜索历史。
     *
     * @param count 返回的最大条数。
     * @return 按搜索时间倒序的历史列表。
     */
    suspend fun getHistories(count: Int): List<SearchHistoryEntity>

    /**
     * 添加搜索历史。
     *
     * 若关键词已存在则更新搜索时间，否则插入新记录。
     * 无痕模式下不记录。
     *
     * @param keyword 搜索关键词。
     */
    suspend fun addHistory(keyword: String)

    /**
     * 删除指定关键词的搜索历史。
     *
     * @param keyword 待删除的关键词。
     */
    suspend fun deleteHistory(keyword: String)

    /** 清空全部搜索历史。 */
    suspend fun clearAll()
}
