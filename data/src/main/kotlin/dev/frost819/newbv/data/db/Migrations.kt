package dev.frost819.newbv.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库 4 → 5 迁移：移除已废弃的 `user.lock` 列。
 *
 * SQLite 3.35 之前的版本不支持 `ALTER TABLE ... DROP COLUMN`，
 * 为保证 Android 5.0+ 全版本可用，采用「建新表 → 搬数据 → 替换旧表」的方式。
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `uid` INTEGER NOT NULL,
                    `username` TEXT NOT NULL,
                    `avatar` TEXT NOT NULL,
                    `auth` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "INSERT INTO `user_new` (`id`, `uid`, `username`, `avatar`, `auth`) " +
                    "SELECT `id`, `uid`, `username`, `avatar`, `auth` FROM `user`",
            )
            db.execSQL("DROP TABLE `user`")
            db.execSQL("ALTER TABLE `user_new` RENAME TO `user`")
        }
    }
