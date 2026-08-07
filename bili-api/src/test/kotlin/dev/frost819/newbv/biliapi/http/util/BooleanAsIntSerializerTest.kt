package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [BooleanAsIntSerializer] 的单元测试。
 *
 * 验证 Int(0/1) 与 Boolean 之间的序列化/反序列化。
 */
class BooleanAsIntSerializerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    data class TestObject(
        @kotlinx.serialization.Serializable(with = BooleanAsIntSerializer::class)
        val flag: Boolean,
    )

    @Test
    fun `deserialize int 1 as true`() {
        val obj = json.decodeFromString<TestObject>("""{"flag":1}""")
        assertThat(obj.flag).isTrue()
    }

    @Test
    fun `deserialize int 0 as false`() {
        val obj = json.decodeFromString<TestObject>("""{"flag":0}""")
        assertThat(obj.flag).isFalse()
    }

    @Test
    fun `deserialize int 2 as true (non-zero is true)`() {
        val obj = json.decodeFromString<TestObject>("""{"flag":2}""")
        assertThat(obj.flag).isTrue()
    }

    @Test
    fun `serialize true as int 1`() {
        val jsonStr = json.encodeToString(TestObject.serializer(), TestObject(flag = true))
        assertThat(jsonStr).contains("\"flag\":1")
    }

    @Test
    fun `serialize false as int 0`() {
        val jsonStr = json.encodeToString(TestObject.serializer(), TestObject(flag = false))
        assertThat(jsonStr).contains("\"flag\":0")
    }

    @Test
    fun `round-trip preserves value`() {
        val original = TestObject(flag = true)
        val encoded = json.encodeToString(TestObject.serializer(), original)
        val decoded = json.decodeFromString<TestObject>(encoded)
        assertThat(decoded.flag).isEqualTo(original.flag)
    }

    @Test
    fun `deserialize boolean fallback when not int`() {
        // When the value is a boolean (not int), the serializer should fall back
        val obj = json.decodeFromString<TestObject>("""{"flag":true}""")
        assertThat(obj.flag).isTrue()
    }
}
