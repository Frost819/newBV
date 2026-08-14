package dev.frost819.newbv.biliapi.websocket

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.InteractType
import dev.frost819.newbv.biliapi.http.entity.live.InteractWordEvent
import dev.frost819.newbv.biliapi.http.entity.live.LiveEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.http.entity.live.WatchedChangeEvent
import dev.frost819.newbv.biliapi.http.util.brotliDecompress
import dev.frost819.newbv.biliapi.http.util.zlibDecompress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 直播弹幕 WebSocket 连接管理器。
 *
 * 使用 OkHttp WebSocket 直接连接 B 站直播弹幕服务器，
 * 接收实时弹幕事件。包含自动重连机制。
 * 调用方通过取消收集 [Flow] 来断开连接。
 */
object LiveDataWebSocket {
    private const val OP_HEARTBEAT = 2
    private const val OP_MESSAGE = 5
    private const val OP_AUTH = 7
    private const val OP_AUTH_REPLY = 8

    private val json = Json { ignoreUnknownKeys = true }

    private val wsClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    private val heartbeatScheduler by lazy {
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "ldw-heartbeat").apply { isDaemon = true }
        }
    }

    private const val webUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"

    private var seq = 1

    /**
     * 连接直播间弹幕 WebSocket，持续接收事件直到 [Flow] 被取消。
     *
     * 包含自动重连机制：连接断开后自动重新获取 token 并重连。
     *
     * @param roomId 直播间房间号
     */
    fun connectLiveEvent(roomId: Int): Flow<LiveEvent> =
        callbackFlow {
            var reconnectDelay = 3000L
            var currentWs: WebSocket? = null

            suspend fun connectOnce() {
                val danmuResponse = BiliLiveHttpApi.getLiveDanmuInfo(roomId)
                val danmuInfo =
                    danmuResponse.data
                        ?: throw IllegalStateException(
                            "getLiveDanmuInfo returned null (code=${danmuResponse.code}, msg=${danmuResponse.message})",
                        )

                val hosts = danmuInfo.hostList.filter { it.wssPort > 0 }
                if (hosts.isEmpty()) throw IllegalStateException("No valid WebSocket host in host_list")
                val host = hosts.firstOrNull { it.host == "broadcastlv.chat.bilibili.com" } ?: hosts.first()

                val port = host.wssPort
                val url = "wss://${host.host}:$port/sub"

                val uid = BiliHttpApi.mid ?: 0L

                val authBody =
                    buildJsonObject {
                        put("uid", uid)
                        put("roomid", roomId)
                        put("protover", 3)
                        put("platform", "web")
                        put("type", 2)
                        put("key", danmuInfo.token)
                    }.toString().toByteArray(Charsets.UTF_8)

                val authPacket = buildPacket(OP_AUTH, authBody)
                val heartbeatPacket = buildPacket(OP_HEARTBEAT, "[object Object]".toByteArray(Charsets.UTF_8))

                val request =
                    Request.Builder()
                        .url(url)
                        .header("User-Agent", webUserAgent)
                        .header("Referer", "https://live.bilibili.com/")
                        .header("Origin", "https://www.bilibili.com")
                        .build()

                var heartbeatTask: ScheduledFuture<*>? = null

                val listener =
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            @Suppress("SpreadOperator")
                            webSocket.send(ByteString.of(*authPacket))
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            bytes: ByteString,
                        ) {
                            val data = bytes.toByteArray()
                            try {
                                val events = handlePacketBytes(data)
                                for (event in events) {
                                    if (event is AuthSuccessSignal) {
                                        heartbeatTask?.cancel(true)
                                        heartbeatTask =
                                            heartbeatScheduler.scheduleWithFixedDelay({
                                                @Suppress("SpreadOperator")
                                                webSocket.send(ByteString.of(*heartbeatPacket))
                                            }, 30, 30, TimeUnit.SECONDS)
                                    } else {
                                        trySend(event)
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            webSocket.close(code, reason)
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            heartbeatTask?.cancel(true)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            heartbeatTask?.cancel(true)
                        }
                    }

                currentWs = wsClient.newWebSocket(request, listener)

                while (isActive) {
                    delay(2000)
                }

                currentWs?.close(1000, "bye")
                heartbeatTask?.cancel(true)
            }

            val reconnectJob =
                launch {
                    while (isActive) {
                        try {
                            connectOnce()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                        }
                        if (!isActive) break
                        delay(reconnectDelay)
                        reconnectDelay = (reconnectDelay * 2).coerceAtMost(15_000L)
                    }
                }

            awaitClose {
                reconnectJob.cancel()
                currentWs?.close(1000, "bye")
            }
        }

    private fun buildPacket(
        op: Int,
        body: ByteArray,
    ): ByteArray {
        val total = 16 + body.size
        val buf = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(total)
        buf.putShort(16)
        buf.putShort(1)
        buf.putInt(op)
        buf.putInt(seq++)
        buf.put(body)
        return buf.array()
    }

    /**
     * 解析 WebSocket 帧中的所有协议包，返回事件列表。
     *
     * 对于认证响应（OP_AUTH_REPLY），返回 [AuthSuccessSignal] 信号。
     * 对于普通消息（OP_MESSAGE），解压后解析 CMD 事件。
     */
    private fun handlePacketBytes(bytes: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        var off = 0
        while (off + 16 <= bytes.size) {
            val packetLen = readInt(bytes, off)
            if (packetLen <= 0 || off + packetLen > bytes.size) break
            val headerLen = readShort(bytes, off + 4)
            val ver = readShort(bytes, off + 6)
            val op = readInt(bytes, off + 8)
            val bodyOff = off + headerLen
            val bodyLen = (packetLen - headerLen).coerceAtLeast(0)
            val body =
                if (bodyLen > 0 && bodyOff + bodyLen <= bytes.size) {
                    bytes.copyOfRange(bodyOff, bodyOff + bodyLen)
                } else {
                    ByteArray(0)
                }

            when (op) {
                OP_MESSAGE -> {
                    val payload =
                        when (ver) {
                            0, 1 -> body
                            2 -> body.zlibDecompress()
                            3 -> body.brotliDecompress()
                            else -> null
                        }
                    if (payload != null) {
                        if (ver == 2 || ver == 3) {
                            result.addAll(handlePacketBytes(payload))
                        } else {
                            handleJsonMessage(payload)?.let { result.add(it) }
                        }
                    }
                }

                OP_AUTH_REPLY -> {
                    val text = body.toString(Charsets.UTF_8).trim()
                    val code =
                        runCatching {
                            json.parseToJsonElement(text).jsonObject["code"]?.jsonPrimitive?.int ?: -1
                        }.getOrDefault(-1)
                    if (code == 0) {
                        result.add(AuthSuccessSignal)
                    }
                }

                OP_HEARTBEAT -> {
                }
            }

            off += packetLen
        }
        return result
    }

    private fun handleJsonMessage(body: ByteArray): LiveEvent? {
        val strData = body.toString(Charsets.UTF_8).trim()
        if (strData.isBlank()) return null
        val dataJson = runCatching { json.parseToJsonElement(strData).jsonObject }.getOrNull() ?: return null
        val cmd = dataJson["cmd"]?.jsonPrimitive?.content ?: return null

        if (cmd.startsWith("DANMU_MSG")) {
            return runCatching {
                val danmakuContent = dataJson["info"]!!.jsonArray[1].jsonPrimitive.content
                val senderMid = dataJson["info"]!!.jsonArray[2].jsonArray[0].jsonPrimitive.long
                val senderUsername = dataJson["info"]!!.jsonArray[2].jsonArray[1].jsonPrimitive.content
                var medalLevel: Int? = null
                var medalName: String? = null
                runCatching {
                    medalLevel = dataJson["info"]?.jsonArray?.get(3)?.jsonArray?.get(0)?.jsonPrimitive?.int
                    medalName = dataJson["info"]?.jsonArray?.get(3)?.jsonArray?.get(1)?.jsonPrimitive?.content
                }
                DanmakuEvent(
                    content = danmakuContent,
                    mid = senderMid,
                    username = senderUsername,
                    medalName = medalName,
                    medalLevel = medalLevel,
                )
            }.getOrNull()
        }

        return runCatching {
            when (cmd) {
                "INTERACT_WORD" -> {
                    val data = dataJson["data"]!!.jsonObject
                    val uid = data["uid"]!!.jsonPrimitive.long
                    val uname = data["uname"]?.jsonPrimitive?.content ?: ""
                    val msgType = data["msg_type"]?.jsonPrimitive?.int ?: 1
                    InteractType.fromCode(msgType)?.let {
                        InteractWordEvent(uid = uid, uname = uname, interactType = it)
                    }
                }

                "ONLINE_RANK_COUNT" -> {
                    val data = dataJson["data"]?.jsonObject
                    OnlineRankCountEvent(count = data?.get("count")?.jsonPrimitive?.int ?: 0)
                }

                "WATCHED_CHANGE" -> {
                    val data = dataJson["data"]?.jsonObject
                    WatchedChangeEvent(
                        num = data?.get("num")?.jsonPrimitive?.int ?: 0,
                        textLarge = data?.get("text_large")?.jsonPrimitive?.content ?: "",
                        textSmall = data?.get("text_small")?.jsonPrimitive?.content ?: "",
                    )
                }

                else -> null
            }
        }.getOrNull()
    }

    private fun readInt(
        bytes: ByteArray,
        offset: Int,
    ): Int = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int

    private fun readShort(
        bytes: ByteArray,
        offset: Int,
    ): Int = ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF

    /** 认证成功的内部信号事件，不对外暴露。 */
    private object AuthSuccessSignal : LiveEvent
}
