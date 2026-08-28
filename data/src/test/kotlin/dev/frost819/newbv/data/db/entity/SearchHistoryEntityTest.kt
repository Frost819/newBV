package dev.frost819.newbv.data.db.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Date

/**
 * [SearchHistoryEntity] data class 的单元测试。
 *
 * 验证构造默认值、equals/hashCode、copy、destructuring 及可变字段修改。
 */
class SearchHistoryEntityTest {
    @Test
    fun `construct with keyword only uses defaults for id and searchDate`() {
        val entity = SearchHistoryEntity(keyword = "test")
        assertThat(entity.id).isNull()
        assertThat(entity.keyword).isEqualTo("test")
        assertThat(entity.searchDate).isNotNull()
    }

    @Test
    fun `construct with all fields`() {
        val date = Date(1700000000000L)
        val entity = SearchHistoryEntity(id = 1, keyword = "hello", searchDate = date)
        assertThat(entity.id).isEqualTo(1)
        assertThat(entity.keyword).isEqualTo("hello")
        assertThat(entity.searchDate).isEqualTo(date)
    }

    @Test
    fun `searchDate is mutable`() {
        val entity = SearchHistoryEntity(keyword = "test")
        val originalDate = entity.searchDate
        entity.searchDate = Date(999999999999L)
        assertThat(entity.searchDate).isNotEqualTo(originalDate)
        assertThat(entity.searchDate.time).isEqualTo(999999999999L)
    }

    @Test
    fun `equals returns true for same values`() {
        val date = Date(1000L)
        val a = SearchHistoryEntity(id = 1, keyword = "test", searchDate = date)
        val b = SearchHistoryEntity(id = 1, keyword = "test", searchDate = date)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals returns false for different keyword`() {
        val a = SearchHistoryEntity(id = 1, keyword = "test1")
        val b = SearchHistoryEntity(id = 1, keyword = "test2")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for different id`() {
        val a = SearchHistoryEntity(id = 1, keyword = "test")
        val b = SearchHistoryEntity(id = 2, keyword = "test")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for different searchDate`() {
        val a = SearchHistoryEntity(id = 1, keyword = "test", searchDate = Date(1000))
        val b = SearchHistoryEntity(id = 1, keyword = "test", searchDate = Date(2000))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals returns false for null vs entity`() {
        val entity = SearchHistoryEntity(keyword = "test")
        assertThat(entity).isNotEqualTo(null)
    }

    @Test
    fun `hashCode is consistent for same values`() {
        val date = Date(1000L)
        val a = SearchHistoryEntity(id = 1, keyword = "test", searchDate = date)
        val b = SearchHistoryEntity(id = 1, keyword = "test", searchDate = date)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `copy creates equal entity`() {
        val original = SearchHistoryEntity(id = 1, keyword = "test", searchDate = Date(1000))
        val copied = original.copy()
        assertThat(copied).isEqualTo(original)
    }

    @Test
    fun `copy with modified keyword`() {
        val original = SearchHistoryEntity(id = 1, keyword = "test")
        val copied = original.copy(keyword = "modified")
        assertThat(copied.keyword).isEqualTo("modified")
        assertThat(copied.id).isEqualTo(1)
    }

    @Test
    fun `copy with modified id`() {
        val original = SearchHistoryEntity(id = null, keyword = "test")
        val copied = original.copy(id = 5)
        assertThat(copied.id).isEqualTo(5)
        assertThat(copied.keyword).isEqualTo("test")
    }

    @Test
    fun `component functions return correct values`() {
        val entity = SearchHistoryEntity(id = 1, keyword = "test", searchDate = Date(1000))
        val (id, keyword, searchDate) = entity
        assertThat(id).isEqualTo(1)
        assertThat(keyword).isEqualTo("test")
        assertThat(searchDate).isEqualTo(Date(1000))
    }

    @Test
    fun `toString contains keyword`() {
        val entity = SearchHistoryEntity(keyword = "myKeyword")
        assertThat(entity.toString()).contains("myKeyword")
    }
}
