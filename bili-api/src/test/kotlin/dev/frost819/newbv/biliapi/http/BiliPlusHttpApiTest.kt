package dev.frost819.newbv.biliapi.http

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BiliPlusHttpApiTest {

    @Test
    fun `get season video view`() = runBlocking {
        val result = BiliPlusHttpApi.view(955635143)
        println(result)
    }

    @Test
    fun `get normal video view`() = runBlocking {
        val result = BiliPlusHttpApi.view(955635143)
        println(result)
    }

    @Test
    fun `get not found video view`() = runBlocking {
        val result = BiliPlusHttpApi.view(1)
        println(result)
    }

    @Test
    fun `get season id by avid`() = runBlocking {
        val seasonId = BiliPlusHttpApi.getSeasonIdByAvid(314583081)
        println(seasonId)
    }
}