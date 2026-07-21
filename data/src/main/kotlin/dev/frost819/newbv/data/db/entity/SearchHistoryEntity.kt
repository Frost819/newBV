package dev.frost819.newbv.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 搜索历史实体。
 *
 * 对应数据库表 `search_history`，记录用户搜索过的关键词及时间。
 * 查询时按 [searchDate] 倒序返回最近搜索。
 *
 * @property id 自增主键，插入时传 `null` 由数据库生成。
 * @property keyword 搜索关键词。
 * @property searchDate 搜索时间，新插入时默认为当前时间；更新历史时需重新赋值。
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "search_date") var searchDate: Date = Date(),
)
