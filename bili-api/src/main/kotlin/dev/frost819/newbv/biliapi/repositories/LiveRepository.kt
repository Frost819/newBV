package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.BiliLiveHttpApi
import dev.frost819.newbv.biliapi.http.entity.live.AreaLiveListResult
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveListResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.PlayCodec
import dev.frost819.newbv.biliapi.http.entity.live.QnDesc
import dev.frost819.newbv.biliapi.http.entity.live.RoomInfoData
import dev.frost819.newbv.biliapi.http.entity.live.RoomInitData
import dev.frost819.newbv.biliapi.http.entity.live.RoomPlayInfoV2Data
import dev.frost819.newbv.biliapi.http.entity.user.UserCardData.UserCardInfo
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
     * HDR 容器偏好（选流与载体选取同源）：flv 被排除——B 站 HEVC-in-FLV 为私有
     * 扩展（codec_id=12），Media3 FlvExtractor 不支持（UnsupportedFormatException）。
     */
    private val hdrContainerPreference =
        listOf(
            "http_hls" to listOf("fmp4", "ts"),
            "http_stream" to listOf("fmp4"),
        )

    /** [hdrContainerPreference] 展开为 (protocol, format) 的有序列表，下标即容器优先级（越小越优先）。 */
    private val hdrContainerOrder =
        hdrContainerPreference.flatMap { (protocol, formats) -> formats.map { protocol to it } }

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
     * 获取主播用户卡片信息。
     *
     * `/room/v1/Room/get_info` 只返回主播 `uid`，不包含 `uname`/`face`，
     * 因此通过用户卡片接口补全主播名称与头像。该接口匿名可访问。
     *
     * @param mid 主播 UID（[RoomInfoData.uid]）
     */
    suspend fun getAnchorCard(mid: Long): UserCardInfo = BiliHttpApi.getUserCardInfo(uid = mid).getResponseData().card

    /**
     * 获取直播流信息（多线路 + 可用画质）。
     *
     * 一次请求同时解析出：
     * - [LivePlayInfo.qualities]：直播间实际可用画质档位（以 (qn, hdrType, eotf) 区分 SDR/HDR），按 qn 降序
     * - [LivePlayInfo.lines]：同一编码下的多条 CDN 线路（host + base_url + extra），供用户手动切换
     *
     * v2 接口失败时降级到简单单画质接口 [BiliLiveHttpApi.getLiveStreamUrl]，仅返回单线路。
     *
     * @param roomId 真实房间号（长号）
     * @param qualityKey 画质档位标识（[LiveQuality.key]，0 = 自动）
     */
    suspend fun getLivePlayInfo(
        roomId: Int,
        qualityKey: Int = 0,
    ): LivePlayInfo =
        runCatching {
            val playInfo = BiliLiveHttpApi.getRoomPlayInfoV2(roomId, LiveQuality.qnOf(qualityKey)).getResponseData()
            resolvePlayInfo(playInfo, qualityKey)
        }.onFailure {
            logger.warn { "getLivePlayInfo failed: ${it.message}, trying fallback" }
        }.getOrElse {
            runCatching {
                val simpleUrl =
                    BiliLiveHttpApi
                        .getLiveStreamUrl(roomId, LiveQuality.qnOf(qualityKey))
                        .getResponseData()
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
     * @param qualityKey 画质档位标识（[LiveQuality.key]，0 = 自动）
     * @return 流地址 URL，或 null 表示无可用流
     */
    suspend fun getLiveStreamUrl(
        roomId: Int,
        qualityKey: Int = 0,
    ): String? = getLiveStreamInfo(roomId, qualityKey).url

    /**
     * 获取直播流地址和当前画质（取第一条线路）。
     *
     * @param roomId 真实房间号（长号）
     * @param qualityKey 画质档位标识（[LiveQuality.key]，0 = 自动）
     * @return [LiveStreamInfo]，包含流 URL 和当前画质 qn
     */
    suspend fun getLiveStreamInfo(
        roomId: Int,
        qualityKey: Int = 0,
    ): LiveStreamInfo {
        val playInfo = getLivePlayInfo(roomId, qualityKey)
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
     *
     * HDR 处理（以 S 赛 4K 直播间真实响应为准）：
     * - HDR 流是同 format 下的额外编码节点（hdr_type 非 0，通常 HEVC + bt2020），
     *   其 accept_qn 与 SDR 重叠，档位须以 (qn, hdrType, eotf) 三元组区分。
     * - 候选集由各编码节点的 accept_qn 展开；g_qn_desc 仅提供描述文案与徽标，
     *   允许同 qn 重复条目（SDR/HLG/HDR10 并存），按三元组逐级匹配。
     * - 旧契约的 hdr_qn 指针（HDR 档独立于 accept_qn）作为补充候选保留兼容。
     * - 请求 HDR 档（[requestedKey] 携带 hdrType/eotf）时优先选携带 HDR 的编码节点，
     *   服务端降级为 SDR 时回退默认选流逻辑。
     *
     * @param requestedKey 本次请求的档位标识（[LiveQuality.key]，0 = 自动）
     */
    internal fun resolvePlayInfo(
        data: RoomPlayInfoV2Data,
        requestedKey: Int = 0,
    ): LivePlayInfo {
        val playUrl = data.playUrlInfo.playUrl
        // HDR 流只从可播放容器展开：flv 的 HEVC 是私有扩展（codec_id=12），Media3 不支持，
        // 若纳入会产出"选中后静默播 SDR"的档位行。容器序号与 pickBestCodec 的 HDR 偏好同源。
        //
        // 候选按三元组去重并携带展开它的来源编码节点：同一 rendition 常同时存在于 http_hls 与
        // http_stream，若按 qn 二次查找载体，同 qn 的 HDR10/HLG 档位会命中同一个节点（codecName/
        // codecs 串档，能力过滤取到错的 RFC 6381 串）。同三元组被多个容器展开时，保留容器序号
        // 更小（即播放时更可能被选中）的那个节点。
        val candidates = LinkedHashMap<Triple<Int, Int, Int>, QualityCandidate>()

        fun offerCandidate(
            qn: Int,
            hdrType: Int,
            eotf: Int,
            codec: PlayCodec,
            rank: Int,
        ) {
            // SDR 档不携带解码提示（维持既有行为）：来源节点仅在 HDR 档保留，
            // 使同 qn 由多个 SDR 编码节点展开的候选仍按三元组合并为一行
            val isHdrRow = hdrType != 0 || eotf != 0
            val key = Triple(qn, hdrType, eotf)
            val existing = candidates[key]
            if (existing == null || rank < existing.rank) {
                candidates[key] = QualityCandidate(qn, hdrType, eotf, codec.takeIf { isHdrRow }, rank)
            }
        }

        for (stream in playUrl.stream) {
            for (format in stream.format) {
                // 容器序号：越小越优先，< 0 表示不在 HDR 偏好容器内
                val rank = hdrContainerOrder.indexOf(stream.protocolName to format.formatName)
                for (codec in format.codec) {
                    if (codec.isHdrCarrier && rank < 0) continue
                    val eotf = codec.videoColorInfo?.eotf ?: 0
                    // LiveQuality.key 按 qn*100+hdrType*10+eotf 编码，hdrType/eotf 须为个位数，
                    // 越界会串档（qnOf=key/100 直接拼请求参数），在源头丢弃非法节点
                    if (codec.hdrType !in 0..9 || eotf !in 0..9) {
                        logger.warn {
                            "live: skip codec(${codec.codecName}) out-of-range hdr marker " +
                                "hdrType=${codec.hdrType}, eotf=$eotf"
                        }
                        continue
                    }
                    for (qn in codec.acceptQn) {
                        offerCandidate(qn, codec.hdrType, eotf, codec, rank)
                    }
                    // 旧契约：hdr_qn 指针指向的 HDR 档位（独立于 accept_qn 的 SDR 梯队）。
                    // 仅对 HDR 载体节点有效：SDR 节点带 hdr_qn 只会产出无载体的幽灵 HDR 行
                    // （无解码提示被能力过滤放行 → 选中后回退 SDR）
                    if (codec.isHdrCarrier) {
                        codec.hdrQn?.let { hdrQn ->
                            offerCandidate(hdrQn, codec.hdrType.takeIf { it != 0 } ?: 1, eotf, codec, rank)
                        }
                    }
                }
            }
        }
        val qualities =
            candidates.values
                .sortedWith(
                    compareByDescending<QualityCandidate> { it.qn }
                        .thenBy { it.hdrType }
                        .thenBy { it.eotf },
                ).mapNotNull { candidate ->
                    val qnDesc =
                        findQnDesc(playUrl.qnDesc, candidate.qn, candidate.hdrType, candidate.eotf)
                            ?: return@mapNotNull null
                    val carrier = candidate.codec
                    val isHdrRow = candidate.hdrType != 0 || candidate.eotf != 0
                    // 杜比视界属 HDR 格式：dolby_qn 是 qn 级指针，只认 HDR 行——若照旧对所有行
                    // 生效，同 qn 的 SDR 行会被一并标成杜比档，菜单里多出一条文案雷同的幽灵行
                    val isDolbyRow =
                        (carrier?.dolbyType ?: 0) != 0 || (isHdrRow && playUrl.dolbyQn == candidate.qn)
                    // HDR 档分辨率按 qn 档位推导：编码节点 media_info 描述的是当前 rendition，
                    // 不是该档实际能拉到的尺寸（4K 档若沿用 1080p 提示会漏过能力过滤）
                    val (resW, resH) = if (carrier != null) hdrResolution(candidate.qn) else (0 to 0)
                    LiveQuality(
                        qn = candidate.qn,
                        desc = qnDesc.desc,
                        hdrType = candidate.hdrType,
                        eotf = candidate.eotf,
                        hdrDesc = qnDesc.hdrDesc,
                        isDolby = isDolbyRow,
                        codecName = carrier?.codecName,
                        codecs = carrier?.videoCodecs?.base,
                        width = resW,
                        height = resH,
                        badge =
                            qnDesc
                                .mediaBaseDesc
                                ?.briefDesc
                                ?.badge
                                .orEmpty(),
                    )
                }

        val codec =
            pickBestCodec(data, requestedKey)
                ?: return LivePlayInfo(currentQn = 0, qualities = qualities, lines = emptyList())

        // 同一编码下的每个 url_info 是一条独立 CDN 线路，按顺序去重后编号（从 1 开始）
        val lines =
            codec.urlInfo
                .mapNotNull { info -> (info.host + codec.baseUrl + info.extra).takeIf { it.isNotBlank() } }
                .distinct()
                .mapIndexed { index, url -> LivePlayLine(order = index + 1, url = url) }

        val codecEotf = codec.videoColorInfo?.eotf ?: 0
        // 当前档位标识：优先用服务端权威三元组 expected_quality 校准，其次按编码节点
        // 三元组匹配；兜底取列表首项，保证 currentKey 一定存在于 qualities（不合成悬空 key）
        val expected = data.playUrlInfo.expectedQuality
        val currentKey =
            expected
                ?.let { e ->
                    qualities
                        .firstOrNull { it.qn == e.qn && it.hdrType == e.hdrType && it.eotf == e.eotf }
                        ?.key
                }
                ?: qualities
                    .firstOrNull { it.qn == codec.currentQn && it.hdrType == codec.hdrType && it.eotf == codecEotf }
                    ?.key
                ?: qualities.firstOrNull()?.key
                ?: 0
        return LivePlayInfo(
            currentQn = codec.currentQn,
            currentKey = currentKey,
            qualities = qualities,
            lines = lines,
        )
    }

    /**
     * 按 (qn, hdrType, eotf) 精确匹配描述条目，逐级回退到 (qn, hdrType)、qn 的 SDR 条目。
     *
     * g_qn_desc 允许同 qn 重复条目（SDR/HLG/HDR10 并存），不能按 qn 去重。
     */
    private fun findQnDesc(
        qnDesc: List<QnDesc>,
        qn: Int,
        hdrType: Int,
        eotf: Int,
    ): QnDesc? =
        qnDesc.firstOrNull { it.qn == qn && it.hdrType == hdrType && it.eotf == eotf }
            ?: qnDesc.firstOrNull { it.qn == qn && it.hdrType == hdrType }
            ?: qnDesc.firstOrNull { it.qn == qn && it.hdrType == 0 && it.eotf == 0 }
            ?: qnDesc.firstOrNull { it.qn == qn }

    /**
     * 选择最佳编码。
     *
     * 优先级：http_stream/flv → http_stream/fmp4 → http_stream/ts →
     * http_hls/fmp4 → http_hls/ts → http_hls/flv，每个格式均优先 AVC。
     * 均无匹配时回退到第一个可用编码。
     *
     * [requestedKey] 为 HDR 档（携带 hdrType/eotf 或被 dolby_qn 指向）时，
     * 查找携带 HDR 的编码节点（hdr_type 非 0 且 qn 命中），找不到（服务端降级）
     * 再执行默认逻辑。注意 HDR 的容器偏好与 SDR 不同：B 站 HEVC-in-FLV 是私有
     * 扩展（codec_id=12），Media3 FlvExtractor 不支持（UnsupportedFormatException），
     * 因此 HDR 只走 HLS（fmp4 → ts）与 http_stream/fmp4，不选 flv。
     */
    private fun pickBestCodec(
        data: RoomPlayInfoV2Data,
        requestedKey: Int = 0,
    ): PlayCodec? {
        val candidates =
            listOf(
                "http_stream" to listOf("flv", "fmp4", "ts"),
                "http_hls" to listOf("fmp4", "ts", "flv"),
            )
        val hdrCandidates = hdrContainerPreference
        val playUrl = data.playUrlInfo.playUrl
        val streams = playUrl.stream

        val requestedQn = LiveQuality.qnOf(requestedKey)
        val requestedEotf = LiveQuality.eotfOf(requestedKey)
        val hdrRequested =
            requestedKey > 0 &&
                (
                    LiveQuality.hdrTypeOf(requestedKey) != 0 ||
                        requestedEotf != 0 ||
                        playUrl.dolbyQn == requestedQn
                )
        if (hdrRequested) {
            for ((protocol, formats) in hdrCandidates) {
                val stream = streams.find { it.protocolName == protocol } ?: continue
                for (formatName in formats) {
                    val format = stream.format.find { it.formatName == formatName } ?: continue
                    // eotf 仅在编码节点声明了色彩信息时强校验，未声明视为通配
                    val codec =
                        format.codec.find {
                            it.isHdrCarrier &&
                                (
                                    requestedQn <= 0 ||
                                        requestedQn in it.acceptQn ||
                                        it.currentQn == requestedQn ||
                                        it.hdrQn == requestedQn
                                ) &&
                                (
                                    requestedEotf <= 0 ||
                                        (it.videoColorInfo?.eotf ?: requestedEotf) == requestedEotf
                                )
                        } ?: continue
                    return codec
                }
            }
        }

        for ((protocol, formats) in candidates) {
            val stream = streams.find { it.protocolName == protocol } ?: continue
            for (formatName in formats) {
                val format = stream.format.find { it.formatName == formatName } ?: continue
                val playable = format.codec.filterNot { it.isUnplayableIn(formatName) }
                val codec =
                    playable.find { it.codecName == "avc" && it.hdrType == 0 }
                        ?: playable.find { it.codecName == "avc" }
                        ?: playable.firstOrNull { it.hdrType == 0 }
                        ?: playable.firstOrNull()
                if (codec != null) return codec
            }
        }
        val firstStream = streams.firstOrNull() ?: return null
        val firstFormat = firstStream.format.firstOrNull() ?: return null
        return firstFormat.codec.firstOrNull { !it.isUnplayableIn(firstFormat.formatName) }
    }

    /**
     * 获取可用画质列表。
     *
     * 从 [getLivePlayInfo] 提取，失败时返回空列表。
     */
    suspend fun getAvailableQualities(roomId: Int): List<LiveQuality> = getLivePlayInfo(roomId, 0).qualities

    /** 按 qn 档位推导 HDR 档的分辨率（用于能力判定，非实际 rendition 尺寸）。 */
    private fun hdrResolution(qn: Int): Pair<Int, Int> =
        when {
            qn >= 20000 -> 3840 to 2160
            qn >= 15000 -> 2560 to 1440
            qn >= 400 -> 1920 to 1080
            qn >= 250 -> 1280 to 720
            qn >= 150 -> 854 to 480
            else -> 640 to 360
        }

    /**
     * 候选画质档位：三元组及其展开它的来源编码节点。
     *
     * @param qn 档位 qn
     * @param hdrType HDR 类型标记，0 = SDR
     * @param eotf 电光转换函数标记，0 = SDR
     * @param codec 来源编码节点，用于填充 HDR 档的解码提示；SDR 档为 null
     * @param rank 来源容器在 [hdrContainerOrder] 中的序号（越小越优先），
     *   同一三元组被多个容器展开时保留播放时更可能被选中的那个
     */
    private data class QualityCandidate(
        val qn: Int,
        val hdrType: Int,
        val eotf: Int,
        val codec: PlayCodec?,
        val rank: Int,
    )
}

/** 该编码节点是否携带 HDR 流（HDR10 用 hdrType，HLG 用 eotf，二者任一即视为 HDR 载体）。 */
private val PlayCodec.isHdrCarrier: Boolean
    get() = hdrType != 0 || (videoColorInfo?.eotf ?: 0) != 0

/**
 * HDR 载体在 FLV 容器中不可播放（HEVC-in-FLV 为私有扩展，Media3 不支持），
 * SDR 请求的兜底选流必须排除此组合，避免选中后解码失败。
 */
private fun PlayCodec.isUnplayableIn(formatName: String): Boolean = isHdrCarrier && formatName == "flv"

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
 * 直播画质档位。
 *
 * 档位以 (qn, hdrType, eotf) 三元组区分：HDR 开播后服务端在同一 qn 下同时下发
 * SDR 与 HDR 变体（g_qn_desc 出现同 qn 重复条目、HDR 编码节点 accept_qn 与 SDR 重叠），
 * 仅 qn 无法唯一标识档位。
 *
 * @param qn 画质 qn（服务端请求参数）
 * @param desc 档位描述（如 "原画"、"4K/HDR"）
 * @param hdrType HDR 类型标记，0 = SDR
 * @param eotf 电光转换函数标记，0 = SDR，1 = PQ(HDR10)，2 = HLG
 * @param hdrDesc HDR 徽标文案（如 "HDR"），SDR 为空串
 * @param isDolby 是否为杜比视界档（HDR 行被 playurl.dolby_qn 指向，或载体编码标记 dolby_type）
 * @param codecName 携带该 HDR 档的编码名（"avc"/"hevc"/"av1"），仅 HDR 档有值
 * @param codecs 携带该 HDR 档的 RFC 6381 编码串（如 `hvc1.2.4.L123`），仅 HDR 档有值
 * @param width 按 qn 档位推导的判定宽度（供解码能力判定，非流实际 rendition 尺寸），仅 HDR 档有值
 * @param height 按 qn 档位推导的判定高度（供解码能力判定，非流实际 rendition 尺寸），仅 HDR 档有值
 */
data class LiveQuality(
    val qn: Int,
    val desc: String,
    val hdrType: Int = 0,
    val eotf: Int = 0,
    val hdrDesc: String = "",
    val isDolby: Boolean = false,
    val codecName: String? = null,
    val codecs: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    /** 服务端下发的徽标文案（media_base_desc.brief_desc.badge，如 "HDR"），空串表示未下发。 */
    val badge: String = "",
) {
    /**
     * 档位唯一标识：qn * 100 + hdrType * 10 + eotf。
     * qn ≤ 30000、hdrType/eotf 为个位数（生成处已做范围守护，越界节点不会进入候选），
     * 不会溢出，且可无损还原。
     */
    val key: Int
        get() = qn * 100 + hdrType * 10 + eotf

    /** 是否为 HDR 档（含杜比视界）。 */
    val isHdr: Boolean
        get() = hdrType != 0 || eotf != 0 || isDolby

    /** UI 展示文案：服务端描述未含 HDR/杜比字样时追加徽标后缀（优先用 media_base_desc.badge），避免重复。 */
    val displayDesc: String
        get() =
            when {
                isDolby -> if (desc.contains("杜比")) desc else "$desc·杜比视界"
                isHdr ->
                    if (desc.contains("HDR", ignoreCase = true)) {
                        desc
                    } else {
                        "$desc·${badge.ifBlank { hdrDesc.ifBlank { "HDR" } }}"
                    }
                else -> desc
            }

    companion object {
        /** 从档位标识还原服务端 qn 参数。 */
        fun qnOf(key: Int): Int = key / 100

        /** 从档位标识还原 HDR 类型标记。 */
        fun hdrTypeOf(key: Int): Int = key / 10 % 10

        /** 从档位标识还原 EOTF 标记。 */
        fun eotfOf(key: Int): Int = key % 10
    }
}

/**
 * 直播流播放信息。
 *
 * @param currentQn 当前实际画质 qn（服务端返回）
 * @param currentKey 当前档位标识（[LiveQuality.key]），用于 UI 选中态与续播请求
 * @param qualities 直播间可用画质档位列表，按 qn 降序（同 qn 时 SDR 在前）
 * @param lines 同一编码下的多条 CDN 线路，供用户手动切换
 */
data class LivePlayInfo(
    val currentQn: Int,
    val currentKey: Int = 0,
    val qualities: List<LiveQuality>,
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
