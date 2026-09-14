package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.AreaLiveListResult
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.PlayCodec
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.http.entity.live.RoomInitData
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
import dev.frost819.newbv.biliapi.util.BiliLogger

/**
 * 直播数据仓库。
 *
 * 聚合 B 站直播 Web 端 API，提供首页推荐、分区列表、直播间信息、流地址获取等能力。
 * 直播功能完全使用 Web 端 API，无需 App 端接口降级。
 */
class LiveRepository {
    private val logger = BiliLogger

    /**
     * 获取直播首页模块化列表。
     */
    suspend fun getLiveList(): LiveListResponse = BiliLiveHttpApi.getLiveList().getResponseData()

    /**
     * 获取推荐直播间列表。
     */
    suspend fun getLiveRecommend(): LiveRecommendResponse = BiliLiveHttpApi.getLiveRecommend().getResponseData()

    /**
     * 获取用户关注的主播正在直播的房间列表。
     *
     * 返回当前正在直播的关注房间，含在线人数、分区、封面等信息。
     */
    suspend fun getFollowLive(): FollowLiveResponse = BiliLiveHttpApi.getFollowLive().getResponseData()

    /**
     * 获取两级分区列表。
     */
    suspend fun getLiveAreaList(): List<LiveAreaParent> = BiliLiveHttpApi.getLiveAreaList().getResponseData()

    /**
     * 获取分区直播分页列表。
     *
     * 端点: `GET /room/v1/Area/getRoomList`
     * 鉴权: 无需 WBI 签名，Cookie 可选
     *
     * @param parentAreaId 父分区 ID
     * @param areaId 子分区 ID（0 = 全部）
     * @param page 页码，从 1 开始
     * @param sortType 排序方式：`online`=热门, `sort_type_2221`=综合, `sort_type_2223`=新番
     */
    suspend fun getAreaLiveList(
        parentAreaId: Int,
        areaId: Int,
        page: Int,
        sortType: String = "online",
    ): AreaLiveListResult {
        val pageSize = 30
        val list =
            BiliLiveHttpApi
                .getAreaLiveList(parentAreaId, areaId, page, pageSize, sortType)
                .getResponseData()
        val hasMore = list.size >= pageSize
        return AreaLiveListResult(list = list, hasMore = hasMore)
    }

    /**
     * 直播间短号→长号转换。
     */
    suspend fun getRoomInit(roomId: Int): RoomInitData = BiliLiveHttpApi.getRoomInit(roomId).getResponseData()

    /**
     * 获取直播间完整信息。
     */
    suspend fun getRoomInfo(roomId: Int): RoomInfoData = BiliLiveHttpApi.getRoomInfo(roomId).getResponseData()

    /**
     * 获取直播流信息（多线路 + 可用画质）。
     *
     * 一次请求同时解析出：
     * - [LivePlayInfo.qualities]：直播间实际可用的画质列表（qn, 描述），按 qn 降序
     * - [LivePlayInfo.lines]：同一编码下的多条 CDN 线路（host + base_url + extra），供用户手动切换
     *
     * v2 接口失败时降级到简单单画质接口 [BiliLiveHttpApi.getLiveStreamUrl]，仅返回单线路。
     *
     * @param roomId 真实房间号（长号）
     * @param qn 画质（0 = 自动）
     */
    suspend fun getLivePlayInfo(
        roomId: Int,
        qn: Int = 0,
    ): LivePlayInfo =
        runCatching {
            val playInfo = BiliLiveHttpApi.getRoomPlayInfoV2(roomId, qn).getResponseData()
            resolvePlayInfo(playInfo)
        }.onFailure {
            logger.warn { "getLivePlayInfo failed: ${it.message}, trying fallback" }
        }.getOrElse {
            runCatching {
                val simpleUrl = BiliLiveHttpApi.getLiveStreamUrl(roomId, qn).getResponseData()
                val url = simpleUrl.durl.firstOrNull()?.url
                LivePlayInfo(
                    currentQn = simpleUrl.currentQuality,
                    qualities = emptyList(),
                    lines = url?.let { listOf(LivePlayLine(order = 1, url = it)) } ?: emptyList(),
                )
            }.onFailure { e ->
                logger.warn { "getLivePlayInfo fallback failed: ${e.message}" }
            }.getOrDefault(LivePlayInfo(currentQn = 0, qualities = emptyList(), lines = emptyList()))
        }

    /**
     * 获取直播流地址（取第一条线路）。
     *
     * @param roomId 真实房间号（长号）
     * @param qn 画质（0 = 自动）
     * @return 流地址 URL，或 null 表示无可用流
     */
    suspend fun getLiveStreamUrl(
        roomId: Int,
        qn: Int = 0,
    ): String? = getLiveStreamInfo(roomId, qn).url

    /**
     * 获取直播流地址和当前画质（取第一条线路）。
     *
     * @param roomId 真实房间号（长号）
     * @param qn 画质（0 = 自动）
     * @return [LiveStreamInfo]，包含流 URL 和当前画质 qn
     */
    suspend fun getLiveStreamInfo(
        roomId: Int,
        qn: Int = 0,
    ): LiveStreamInfo {
        val playInfo = getLivePlayInfo(roomId, qn)
        return LiveStreamInfo(
            url = playInfo.lines.firstOrNull()?.url,
            currentQn = playInfo.currentQn,
        )
    }

    /**
     * 从 v2 流地址响应中解析出可用画质与多条线路。
     *
     * 策略：优先 http_stream（FLV），fallback 到 http_hls。
     * FLV 是渐进式流，不存在分片窗口滑动问题，不会触发 BehindLiveWindowException，
     * 且启动延迟更低、稳定性更好。HLS 作为兜底。
     * 两种协议内均优先 AVC 编码，并枚举所选编码下的全部 [PlayCodec.urlInfo] 生成线路。
     */
    internal fun resolvePlayInfo(data: RoomPlayInfoV2Data): LivePlayInfo {
        val playUrl = data.playUrlInfo.playUrl
        val qnDescMap = playUrl.qnDesc.associate { it.qn to it.desc }
        val acceptQns =
            playUrl.stream
                .flatMap { it.format }
                .flatMap { it.codec }
                .flatMap { it.acceptQn }
                .distinct()
        val qualities =
            acceptQns.sortedByDescending { it }.mapNotNull { qn ->
                qnDescMap[qn]?.let { desc -> qn to desc }
            }

        val codec =
            pickBestCodec(data)
                ?: return LivePlayInfo(currentQn = 0, qualities = qualities, lines = emptyList())

        // 同一编码下的每个 url_info 是一条独立 CDN 线路，按顺序去重后编号（从 1 开始）
        val lines =
            codec.urlInfo
                .mapNotNull { info -> (info.host + codec.baseUrl + info.extra).takeIf { it.isNotBlank() } }
                .distinct()
                .mapIndexed { index, url -> LivePlayLine(order = index + 1, url = url) }

        return LivePlayInfo(currentQn = codec.currentQn, qualities = qualities, lines = lines)
    }

    /**
     * 选择最佳编码。
     *
     * 优先级：http_stream/flv → http_stream/fmp4 → http_stream/ts →
     * http_hls/fmp4 → http_hls/ts → http_hls/flv，每个格式均优先 AVC。
     * 均无匹配时回退到第一个可用编码。
     */
    private fun pickBestCodec(data: RoomPlayInfoV2Data): PlayCodec? {
        val candidates =
            listOf(
                "http_stream" to listOf("flv", "fmp4", "ts"),
                "http_hls" to listOf("fmp4", "ts", "flv"),
            )
        val streams = data.playUrlInfo.playUrl.stream
        for ((protocol, formats) in candidates) {
            val stream = streams.find { it.protocolName == protocol } ?: continue
            for (formatName in formats) {
                val format = stream.format.find { it.formatName == formatName } ?: continue
                val codec = format.codec.find { it.codecName == "avc" } ?: format.codec.firstOrNull()
                if (codec != null) return codec
            }
        }
        val firstStream = streams.firstOrNull() ?: return null
        val firstFormat = firstStream.format.firstOrNull() ?: return null
        return firstFormat.codec.firstOrNull()
    }

    /**
     * 获取可用画质列表。
     *
     * 从 [getLivePlayInfo] 提取，失败时返回空列表。
     */
    suspend fun getAvailableQualities(roomId: Int): List<Pair<Int, String>> = getLivePlayInfo(roomId, 0).qualities
}

/**
 * 直播线路。
 *
 * @param order 线路序号（从 1 开始，与 UI 展示一致）
 * @param url 完整流地址（host + base_url + extra）
 */
data class LivePlayLine(
    val order: Int,
    val url: String,
)

/**
 * 直播流播放信息。
 *
 * @param currentQn 当前实际画质 qn
 * @param qualities 直播间可用画质列表（qn, 描述），按 qn 降序
 * @param lines 同一编码下的多条 CDN 线路，供用户手动切换
 */
data class LivePlayInfo(
    val currentQn: Int,
    val qualities: List<Pair<Int, String>>,
    val lines: List<LivePlayLine>,
)

/**
 * 直播流信息（单线路，向后兼容）。
 *
 * @param url 流地址 URL，null 表示无可用流
 * @param currentQn 当前实际画质 qn
 */
data class LiveStreamInfo(
    val url: String?,
    val currentQn: Int,
)
