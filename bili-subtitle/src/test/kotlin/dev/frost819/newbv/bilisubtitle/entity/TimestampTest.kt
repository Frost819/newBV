package dev.frost819.newbv.bilisubtitle.entity

import kotlin.test.Test

class TimestampTest {
    @Test
    fun `parse bcc time`() {
        val time = 7530.2f
        println(Timestamp.fromBccString(time))
    }

    @Test
    fun `parse srt time`() {
        val time = "01:03:17,775"
        println(Timestamp.fromSrtString(time))
    }
}