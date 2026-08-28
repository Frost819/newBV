package dev.frost819.newbv.data.repository

import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SearchHistoryRepository] 的默认实现。
 *
 * 使用 [SearchHistoryDao] 持久化搜索历史，受 [Prefs.incognitoMode] 控制。
 * 无痕模式下不记录搜索历史。
 *
 * @param searchHistoryDao Room DAO
 */
@Singleton
class SearchHistoryRepositoryImpl
    @Inject
    constructor(
        private val searchHistoryDao: SearchHistoryDao,
    ) : SearchHistoryRepository {
        override suspend fun getHistories(count: Int): List<SearchHistoryEntity> = searchHistoryDao.getHistories(count)

        override suspend fun addHistory(keyword: String) {
            if (Prefs.incognitoMode) return
            val existing = searchHistoryDao.findHistory(keyword)
            if (existing != null) {
                existing.searchDate = Date()
                searchHistoryDao.update(existing)
            } else {
                searchHistoryDao.insert(SearchHistoryEntity(keyword = keyword))
            }
        }

        override suspend fun deleteHistory(keyword: String) {
            searchHistoryDao.findHistory(keyword)?.let { searchHistoryDao.delete(it) }
        }

        override suspend fun clearAll() {
            searchHistoryDao.deleteAll()
        }
    }
