package dev.frost819.newbv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 应用主数据库。
 *
 * 持久化搜索历史与多账户信息，由 Hilt 注入，禁止手动构建单例。
 * 数据库版本 4（相对原版 BV 的版本 3 新增了 newBV 的 schema 起始）。
 *
 * 实体列表：
 * - [SearchHistoryEntity]：搜索历史
 * - [UserEntity]：账户信息
 *
 * Schema 导出位置：`data/schemas/`（由 KSP `room.schemaLocation` 配置）。
 */
@Database(
    entities = [SearchHistoryEntity::class, UserEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** 搜索历史 DAO。 */
    abstract fun searchHistoryDao(): SearchHistoryDao

    /** 账户 DAO。 */
    abstract fun userDao(): UserDao

    companion object {
        /** 数据库文件名。 */
        const val DATABASE_NAME = "AppDatabase.db"
    }
}
