package dev.frost819.newbv.biliapi.websocket

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@org.junit.jupiter.api.Tag("integration")
internal class LiveDataWebSocketTest {
    @Test
    fun connectLiveEvent() {
        runBlocking {
            LiveDataWebSocket.connectLiveEvent(5555).collect { event ->
                println(event)
            }
            for (i in 1..10) {
                delay(1_000)
            }
        }
    }
}
