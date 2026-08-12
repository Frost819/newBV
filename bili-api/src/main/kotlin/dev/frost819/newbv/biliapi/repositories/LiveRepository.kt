package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.AreaLiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.PlayCodec
import dev.frost819.newbv.biliapi.http.entity.live.PlayStream
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.http.entity.live.RoomInitData
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 直播数据仓库。
 *
 * 聚合 B 站直播 Web 端 API，提供首页推荐、分区列表、直播间信息、流地址获取等能力。
 * 直播功能完全使用 Web 端 API，无需 App 端接口降级。
 */
class LiveRepository {
    private val logger = KotlinLogging.logger { }

    /**
     * 获取直播首页模块化列表。
     */
    suspend fun getLiveList(): LiveListResponse = BiliLiveHttpApi.getLiveList().getResponseData()

    /**
     * 获取推荐直播间列表。
     */
    suspend fun getLiveRecommend(): LiveRecommendResponse = BiliLiveHttpApi.getLiveRecommend().getResponseData()

    /**
     * 获取两级分区列表。
     */
    suspend fun getLiveAreaList(): List<LiveAreaParent> = BiliLiveHttpApi.getLiveAreaList().getResponseData()

    /**
     * 获取分区直播分页列表。
     *
     * @param parentAreaId 父分区 ID
     * @param areaId 子分区 ID（0 = 全部）
     * @param page 页码，从 1 开始
     * @param sortType 排序方式
     */
    suspend fun getAreaLiveList(
        parentAreaId: Int,
        areaId: Int,
        page: Int,
        sortType: String = "sort_type_2221",
    ): AreaLiveListResponse =
        BiliLiveHttpApi.getAreaLiveList(parentAreaId, areaId, page, sortType)
            .getResponseData()

    /**
     * 直播间短号→长号转换。
     */
    suspend fun getRoomInit(roomId: Int): RoomInitData = BiliLiveHttpApi.getRoomInit(roomId).getResponseData()

    /**
     * 获取直播间完整信息。
     */
    suspend fun getRoomInfo(roomId: Int): RoomInfoData = BiliLiveHttpApi.getRoomInfo(roomId).getResponseData()

    /**
     * 获取直播流地址（优先 HLS，fallback 到 FLV）。
     *
     * @param roomId 真实房间号（长号）
     * @param qn 画质（0 = 自动）
     * @return 流地址 URL，或 null 表示无可用流
     */
    suspend fun getLiveStreamUrl(
        roomId: Int,
        qn: Int = 0,
    ): String? {
        return runCatching {
            val playInfo = BiliLiveHttpApi.getRoomPlayInfoV2(roomId, qn).getResponseData()
            resolveStreamUrl(playInfo)
        }.onFailure {
            logger.warn { "getRoomPlayInfoV2 failed: ${it.message}, trying fallback" }
        }.getOrElse {
            runCatching {
                val simpleUrl = BiliLiveHttpApi.getLiveStreamUrl(roomId, qn).getResponseData()
                simpleUrl.durl.firstOrNull()?.url
            }.onFailure { e ->
                logger.warn { "getLiveStreamUrl fallback failed: ${e.message}" }
            }.getOrNull()
        }
    }

    /**
     * 从 v2 流地址响应中解析出最佳流 URL。
     *
     * 策略：优先 HLS（fmp4 > ts），优先 AVC，fallback 到 http_stream/flv。
     */
    internal fun resolveStreamUrl(data: RoomPlayInfoV2Data): String? {
        val streams = data.playUrlInfo.playUrl.stream

        // 优先 HLS
        val hlsStream = streams.find { it.protocolName == "http_hls" }
        if (hlsStream != null) {
            val url = extractUrlFromStream(hlsStream, preferredFormat = "fmp4", fallbackFormat = "ts")
            if (url != null) return url
        }

        // fallback http_stream
        val httpStream = streams.find { it.protocolName == "http_stream" }
        if (httpStream != null) {
            val url = extractUrlFromStream(httpStream, preferredFormat = "flv", fallbackFormat = null)
            if (url != null) return url
        }

        // 兜底：取第一个可用
        return streams.firstOrNull()?.format?.firstOrNull()?.codec?.firstOrNull()?.let { codec ->
            buildUrl(codec)
        }
    }

    /**
     * 从 Stream 中按优先级提取流 URL。
     */
    private fun extractUrlFromStream(
        stream: PlayStream,
        preferredFormat: String,
        fallbackFormat: String?,
    ): String? {
        // 优先找 preferred format
        val targetFormat =
            stream.format.find { it.formatName == preferredFormat }
                ?: fallbackFormat?.let { ff -> stream.format.find { it.formatName == ff } }
                ?: stream.format.firstOrNull()
                ?: return null

        // 优先 AVC
        val codec =
            targetFormat.codec.find { it.codecName == "avc" }
                ?: targetFormat.codec.firstOrNull()
                ?: return null

        return buildUrl(codec)
    }

    /**
     * 拼接完整流 URL：host + base_url + extra。
     */
    private fun buildUrl(codec: PlayCodec): String? {
        val urlInfo = codec.urlInfo.firstOrNull() ?: return null
        return urlInfo.host + codec.baseUrl + urlInfo.extra
    }

    /**
     * 获取可用画质列表。
     */
    suspend fun getAvailableQualities(roomId: Int): List<Pair<Int, String>> {
        return runCatching {
            val playInfo = BiliLiveHttpApi.getRoomPlayInfoV2(roomId, 0).getResponseData()
            playInfo.playUrlInfo.playUrl.qnDesc.map { it.qn to it.desc }
        }.onFailure {
            logger.warn { "getAvailableQualities failed: ${it.message}" }
        }.getOrDefault(emptyList())
    }
}
