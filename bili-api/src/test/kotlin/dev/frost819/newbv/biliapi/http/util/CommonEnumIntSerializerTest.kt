package dev.frost819.newbv.biliapi.http.util

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [CommonEnumIntSerializer] 的单元测试。
 *
 * 验证枚举与 Int 之间的序列化/反序列化及异常处理。
 */
class CommonEnumIntSerializerTest {
    enum class TestEnum(
        override val serialNumber: Int?,
    ) : SerialEnum {
        ALPHA(10),
        BETA(20),
        GAMMA(null),
    }

    enum class NoSerialEnum : SerialEnum {
        X,
        Y,
        ;

        override val serialNumber: Int? = null
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val serializer: CommonEnumIntSerializer<TestEnum> =
        CommonEnumIntSerializer(
            "TestEnum",
            arrayOf(TestEnum.ALPHA, TestEnum.BETA, TestEnum.GAMMA),
            arrayOf(10, 20, 2),
        )

    @Test
    fun `serialize enum to its serial number`() {
        val encoded = json.encodeToString(serializer, TestEnum.ALPHA)
        assertThat(encoded).isEqualTo("10")
    }

    @Test
    fun `deserialize int to correct enum value`() {
        val decoded = json.decodeFromString(serializer, "20")
        assertThat(decoded).isEqualTo(TestEnum.BETA)
    }

    @Test
    fun `serialize null serialNumber uses ordinal`() {
        val encoded = json.encodeToString(serializer, TestEnum.GAMMA)
        assertThat(encoded).isEqualTo("2")
    }

    @Test
    fun `deserialize ordinal for null serialNumber`() {
        val decoded = json.decodeFromString(serializer, "2")
        assertThat(decoded).isEqualTo(TestEnum.GAMMA)
    }

    @Test
    fun `round-trip preserves all values`() {
        for (enum in TestEnum.entries) {
            val encoded = json.encodeToString(serializer, enum)
            val decoded = json.decodeFromString(serializer, encoded)
            assertThat(decoded).isEqualTo(enum)
        }
    }

    @Test
    fun `init throws when choices and numbers size mismatch`() {
        assertThrows<IllegalArgumentException> {
            CommonEnumIntSerializer(
                "Bad",
                arrayOf(TestEnum.ALPHA, TestEnum.BETA),
                arrayOf(10),
            )
        }
    }

    @Test
    fun `init throws when duplicate serial numbers`() {
        assertThrows<IllegalArgumentException> {
            CommonEnumIntSerializer(
                "Bad",
                arrayOf(TestEnum.ALPHA, TestEnum.BETA),
                arrayOf(10, 10),
            )
        }
    }

    @Test
    fun `serial extension uses serialNumber when present`() {
        val numbers = TestEnum.entries.toTypedArray().serial()
        assertThat(numbers.toList()).isEqualTo(listOf(10, 20, 2))
    }

    @Test
    fun `serial extension falls back to ordinal when serialNumber is null`() {
        val numbers = NoSerialEnum.entries.toTypedArray().serial()
        assertThat(numbers.toList()).isEqualTo(listOf(0, 1))
    }

    @Test
    fun `deserialize throws on unknown serial number`() {
        val exception =
            assertThrows<IllegalStateException> {
                json.decodeFromString(serializer, "999")
            }
        assertThat(exception.message).contains("999 is not a valid serial value")
    }
}
