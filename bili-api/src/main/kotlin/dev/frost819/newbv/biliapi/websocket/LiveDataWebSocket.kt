package dev.frost819.newbv.biliapi.websocket

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.DanmakuEvent
import dev.frost819.newbv.biliapi.http.entity.live.HostListItem
import dev.frost819.newbv.biliapi.http.entity.live.InteractType
import dev.frost819.newbv.biliapi.http.entity.live.InteractWordEvent
import dev.frost819.newbv.biliapi.http.entity.live.LiveEvent
import dev.frost819.newbv.biliapi.http.entity.live.OnlineRankCountEvent
import dev.frost819.newbv.biliapi.http.entity.live.WatchedChangeEvent
import dev.frost819.newbv.biliapi.http.util.brotliDecompress
import dev.frost819.newbv.biliapi.http.util.zlibDecompress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
 *
 * **设计要点**（参考 blbl 项目 `LiveMessageClient`）：
 * - token 只在首次连接时通过 `getLiveDanmuInfo` 获取一次，后续重连复用缓存 token，
 *   避免频繁调用 WBI 签名接口触发风控。
 * - 重连时轮换 host，避免一直连接同一个已断开的服务器。
 * - 指数退避：1s → 2s → 4s → 8s → 10s 封顶。
 * - 6 秒 auth 超时检测，超时触发重连。
 * - 如果 auth 返回非 0 code（token 可能过期），下次重连时重新获取 token。
 *
 * 调用方通过取消收集 [Flow] 来断开连接。
 */
object LiveDataWebSocket {
    private const val OP_HEARTBEAT = 2
    private const val OP_MESSAGE = 5
    private const val OP_AUTH = 7
    private const val OP_AUTH_REPLY = 8

    private val logger = KotlinLogging.logger { }

    private val json = Json { ignoreUnknownKeys = true }

    private val wsClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    private val scheduler by lazy {
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "ldw-scheduler").apply { isDaemon = true }
        }
    }

    private const val webUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"

    private var seq = 1

    /**
     * 连接直播间弹幕 WebSocket，持续接收事件直到 [Flow] 被取消。
     *
     * 包含自动重连机制：连接断开后自动使用缓存的 token 重新连接，
     * 无需重新调用 `getLiveDanmuInfo` API。
     *
     * @param roomId 直播间房间号（真实房间号）
     */
    fun connectLiveEvent(roomId: Int): Flow<LiveEvent> =
        callbackFlow {
            var reconnectAttempt = 0
            var currentWs: WebSocket? = null
            var heartbeatTask: ScheduledFuture<*>? = null
            var authTimeoutTask: ScheduledFuture<*>? = null

            // 缓存的 token + hosts，只在首次连接或 token 失效时获取
            var cachedToken: String? = null
            var cachedHosts: List<HostListItem> = emptyList()
            var hostIndex = 0
            var needRefreshToken = false

            // 断连信号，用于通知重连循环当前连接已断开
            var disconnectSignal = CompletableDeferred<Unit>()

            /**
             * 通过 HTTP API 获取弹幕 token 和 host 列表。
             */
            suspend fun fetchDanmuInfo() {
                val danmuResponse = BiliLiveHttpApi.getLiveDanmuInfo(roomId)
                val danmuInfo =
                    danmuResponse.data
                        ?: throw IllegalStateException(
                            "getLiveDanmuInfo returned null (code=${danmuResponse.code}, msg=${danmuResponse.message})",
                        )

                val hosts = danmuInfo.hostList.filter { it.wssPort > 0 }
                if (hosts.isEmpty()) throw IllegalStateException("No valid WebSocket host in host_list")

                cachedToken = danmuInfo.token
                cachedHosts = hosts
                needRefreshToken = false
                logger.info { "Fetched danmu info: ${hosts.size} hosts, token length=${danmuInfo.token.length}" }
            }

            /**
             * 使用缓存的 token 和指定 host 建立 WebSocket 连接。
             * 连接断开时完成 [signal] 以通知重连循环。
             */
            fun connectCurrentHost(signal: CompletableDeferred<Unit>) {
                val token = cachedToken
                val hosts = cachedHosts
                if (token == null || hosts.isEmpty()) {
                    signal.complete(Unit)
                    return
                }

                val host = hosts[hostIndex % hosts.size]
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
                        put("key", token)
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

                var authed = false

                val listener =
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            logger.info { "WebSocket opened to ${host.host}:$port" }
                            @Suppress("SpreadOperator")
                            webSocket.send(ByteString.of(*authPacket))

                            // 6 秒 auth 超时检测
                            authTimeoutTask?.cancel(false)
                            authTimeoutTask =
                                scheduler.schedule({
                                    if (!authed) {
                                        logger.warn { "Auth timeout (6s), closing connection" }
                                        webSocket.close(1000, "auth timeout")
                                    }
                                }, 6, TimeUnit.SECONDS)
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
                                        authed = true
                                        reconnectAttempt = 0
                                        authTimeoutTask?.cancel(false)
                                        authTimeoutTask = null
                                        heartbeatTask?.cancel(true)
                                        heartbeatTask =
                                            scheduler.scheduleWithFixedDelay({
                                                @Suppress("SpreadOperator")
                                                webSocket.send(ByteString.of(*heartbeatPacket))
                                            }, 30, 30, TimeUnit.SECONDS)
                                        logger.info { "Auth success, heartbeat started" }
                                    } else if (event is AuthFailedSignal) {
                                        needRefreshToken = true
                                        logger.warn {
                                            "Auth failed (code=${event.code}), will refresh token"
                                        }
                                        webSocket.close(1000, "auth failed")
                                    } else {
                                        trySend(event)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.warn(e) { "Failed to parse WebSocket message" }
                            }
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.info { "WebSocket closing: code=$code, reason=$reason" }
                            webSocket.close(code, reason)
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.info { "WebSocket closed: code=$code, reason=$reason" }
                            heartbeatTask?.cancel(true)
                            authTimeoutTask?.cancel(false)
                            signal.complete(Unit)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            logger.warn(t) { "WebSocket failure" }
                            heartbeatTask?.cancel(true)
                            authTimeoutTask?.cancel(false)
                            signal.complete(Unit)
                        }
                    }

                currentWs = wsClient.newWebSocket(request, listener)
            }

            val reconnectJob =
                launch {
                    // 首次获取 token
                    try {
                        fetchDanmuInfo()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to fetch initial danmu info" }
                        return@launch
                    }

                    while (isActive) {
                        disconnectSignal = CompletableDeferred()

                        try {
                            connectCurrentHost(disconnectSignal)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.warn(e) { "Failed to connect" }
                        }

                        // 等待连接断开
                        disconnectSignal.await()

                        if (!isActive) break

                        // 如果 auth 从未成功（token 可能过期），下次重连重新获取 token
                        if (needRefreshToken) {
                            logger.info { "Re-fetching danmu info (token may have expired)" }
                            runCatching { fetchDanmuInfo() }
                                .onFailure { logger.warn(it) { "Failed to re-fetch danmu info" } }
                        }

                        // 指数退避：1s, 2s, 4s, 8s, 10s, 10s, ...
                        val delaySec = (1L shl reconnectAttempt.coerceAtMost(4)).coerceAtMost(10)
                        reconnectAttempt++
                        hostIndex = (hostIndex + 1) % cachedHosts.size

                        logger.info { "Reconnecting in ${delaySec}s (attempt=$reconnectAttempt, hostIndex=$hostIndex)" }
                        delay(delaySec * 1000)
                    }
                }

            awaitClose {
                reconnectJob.cancel()
                currentWs?.close(1000, "bye")
                heartbeatTask?.cancel(true)
                authTimeoutTask?.cancel(false)
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
                    } else {
                        logger.warn { "Auth failed: code=$code, body=$text" }
                        result.add(AuthFailedSignal(code))
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

    /** 认证失败的内部信号事件，不对外暴露。用于通知重连逻辑需要重新获取 token。 */
    private data class AuthFailedSignal(val code: Int) : LiveEvent
}
