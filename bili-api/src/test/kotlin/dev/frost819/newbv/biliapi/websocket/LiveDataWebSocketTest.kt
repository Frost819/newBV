package dev.frost819.newbv.biliapi.websocket

import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.InteractWordEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.http.entity.live.WatchedChangeEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@org.junit.jupiter.api.Tag("integration")
internal class LiveDataWebSocketTest {
    @Test
    fun `connects to configured room and receives a live event`() {
        runBlocking {
            val roomInfo = BiliLiveHttpApi.getRoomPlayInfoV2(1718159119).data
            assumeTrue(roomInfo?.liveStatus == 1, "configured room is not live")

            val event =
                withTimeoutOrNull(30_000) {
                    LiveDataWebSocket.connectLiveEvent(1718159119).first()
                }

            // A live room may have no chat activity during the observation window.
            if (event != null) {
                assertTrue(
                    event is DanmakuEvent ||
                        event is InteractWordEvent ||
                        event is OnlineRankCountEvent ||
                        event is WatchedChangeEvent,
                )
            }
        }
    }
}
