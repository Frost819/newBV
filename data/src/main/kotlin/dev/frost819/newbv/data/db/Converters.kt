package dev.frost819.newbv.data.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room 类型转换器。
 *
 * 将 [Date] 与 [Long]（时间戳毫秒）互转，用于在 SQLite 中持久化日期字段。
 * 注册到 [AppDatabase] 的 `@TypeConverters` 注解后，所有实体中的 Date 字段自动适用。
 */
object Converters {
    /** 时间戳毫秒转 [Date]，`null` 入参返回 `null`。 */
    @TypeConverter
    fun timestampToDate(value: Long?): Date? = value?.let { Date(it) }

    /** [Date] 转时间戳毫秒，`null` 入参返回 `null`。 */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
