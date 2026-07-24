package dev.frost819.newbv.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExtendsTest {

    @Test
    fun `formatMinSec formats milliseconds correctly`() {
        assertEquals("00:00", 0L.formatMinSec())
        assertEquals("00:01", 1000L.formatMinSec())
        assertEquals("00:59", 59000L.formatMinSec())
        assertEquals("01:00", 60000L.formatMinSec())
        assertEquals("01:30", 90000L.formatMinSec())
        assertEquals("10:00", 600000L.formatMinSec())
        assertEquals("59:59", 3599000L.formatMinSec())
        assertEquals("60:00", 3600000L.formatMinSec())
        assertEquals("99:59", 5999000L.formatMinSec())
    }

    @Test
    fun `formatMinSec returns dots for negative values`() {
        assertEquals("...", (-1L).formatMinSec())
        assertEquals("...", (-1000L).formatMinSec())
    }
}
