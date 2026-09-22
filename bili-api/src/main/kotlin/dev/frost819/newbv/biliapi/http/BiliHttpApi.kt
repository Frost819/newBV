package dev.frost819.newbv.biliapi.http

import bilibili.community.service.dm.v1.DmSegMobileReply
import bilibili.community.service.dm.v1.DmWebViewReply
import com.tfowl.ktor.client.plugins.JsoupPlugin
import dev.frost819.newbv.biliapi.entity.SpiData
import dev.frost819.newbv.biliapi.entity.SpiResult
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.http.BiliHttpApi.getRegionDynamic
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.BiliResponseWithoutData
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuData
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuResponse
import dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData
import dev.frost819.newbv.biliapi.http.entity.history.HistoryData
import dev.frost819.newbv.biliapi.http.entity.home.RcmdIndexData
import dev.frost819.newbv.biliapi.http.entity.home.RcmdTopData
import dev.frost819.newbv.biliapi.http.entity.index.IndexResultData
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedData
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcFeedV3Data
import dev.frost819.newbv.biliapi.http.entity.pgc.PgcWebInitialStateData
import dev.frost819.newbv.biliapi.http.entity.region.RegionDynamic
import dev.frost819.newbv.biliapi.http.entity.region.RegionDynamicList
import dev.frost819.newbv.biliapi.http.entity.region.RegionFeedRcmd
import dev.frost819.newbv.biliapi.http.entity.region.RegionLocs
import dev.frost819.newbv.biliapi.http.entity.search.AppSearchSquareData
import dev.frost819.newbv.biliapi.http.entity.search.KeywordSuggest
import dev.frost819.newbv.biliapi.http.entity.search.SearchResultData
import dev.frost819.newbv.biliapi.http.entity.search.SearchTendingData
import dev.frost819.newbv.biliapi.http.entity.search.WebSearchSquareData
import dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData
import dev.frost819.newbv.biliapi.http.entity.season.FollowingSeasonAppData
import dev.frost819.newbv.biliapi.http.entity.season.FollowingSeasonWebData
import dev.frost819.newbv.biliapi.http.entity.season.SeasonFollowData
import dev.frost819.newbv.biliapi.http.entity.season.WebSeasonData
import dev.frost819.newbv.biliapi.http.entity.toview.ToViewData
import dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData
import dev.frost819.newbv.biliapi.http.entity.user.FollowAction
import dev.frost819.newbv.biliapi.http.entity.user.FollowActionSource
import dev.frost819.newbv.biliapi.http.entity.user.MyInfoData
import dev.frost819.newbv.biliapi.http.entity.user.RelationData
import dev.frost819.newbv.biliapi.http.entity.user.RelationStat
import dev.frost819.newbv.biliapi.http.entity.user.UserCardData
import dev.frost819.newbv.biliapi.http.entity.user.UserFollowData
import dev.frost819.newbv.biliapi.http.entity.user.UserInfoData
import dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfo
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfoListData
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteItemIdListResponse
import dev.frost819.newbv.biliapi.http.entity.user.favorite.UserFavoriteFoldersData
import dev.frost819.newbv.biliapi.http.entity.user.garb.Equip
import dev.frost819.newbv.biliapi.http.entity.user.garb.EquipPart
import dev.frost819.newbv.biliapi.http.entity.video.AddCoin
import dev.frost819.newbv.biliapi.http.entity.video.CheckSentCoin
import dev.frost819.newbv.biliapi.http.entity.video.CheckVideoFavoured
import dev.frost819.newbv.biliapi.http.entity.video.OneClickTripleAction
import dev.frost819.newbv.biliapi.http.entity.video.OnlineTotal
import dev.frost819.newbv.biliapi.http.entity.video.OnlineTotalApp
import dev.frost819.newbv.biliapi.http.entity.video.PlayUrlData
import dev.frost819.newbv.biliapi.http.entity.video.PlayUrlV2Data
import dev.frost819.newbv.biliapi.http.entity.video.PopularVideoData
import dev.frost819.newbv.biliapi.http.entity.video.RelatedVideosResponse
import dev.frost819.newbv.biliapi.http.entity.video.SetVideoFavorite
import dev.frost819.newbv.biliapi.http.entity.video.Tag
import dev.frost819.newbv.biliapi.http.entity.video.TagDetail
import dev.frost819.newbv.biliapi.http.entity.video.TagTopVideosResponse
import dev.frost819.newbv.biliapi.http.entity.video.Timeline
import dev.frost819.newbv.biliapi.http.entity.video.TimelineAppData
import dev.frost819.newbv.biliapi.http.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.http.entity.video.VideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoMoreInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoShot
import dev.frost819.newbv.biliapi.http.entity.web.NavResponseData
import dev.frost819.newbv.biliapi.http.plugins.BiliUserAgent
import dev.frost819.newbv.biliapi.http.util.BiliAppConf
import dev.frost819.newbv.biliapi.http.util.encApiSign
import dev.frost819.newbv.biliapi.http.util.injectCookies
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Parameters
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.jsoup.nodes.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

@Suppress("SpellCheckingInspection")
object BiliHttpApi {
    /** 单次 HTTP 请求的整体超时（毫秒），防止慢速滴流响应永久挂起。 */
    private const val REQUEST_TIMEOUT_MS = 20_000L

    /** 建立连接超时（毫秒）。 */
    private const val CONNECT_TIMEOUT_MS = 10_000L

    /** 单次 socket 读写超时（毫秒）。 */
    private const val SOCKET_TIMEOUT_MS = 20_000L

    private var endPoint: String = "api.bilibili.com"
    private var clientInstance: HttpClient? = null
    private val client: HttpClient
        get() = clientInstance ?: createClient().also { clientInstance = it }

    /**
     * 在后台协程预创建 Ktor HttpClient，与 UI 初始化并行执行。
     * 创建完成后赋值给 [clientInstance]，使首次网络请求时 [client] 直接可用，避免串行等待。
     */
    fun warmUpClient() {
        if (clientInstance == null) {
            CoroutineScope(Dispatchers.IO).launch {
                if (clientInstance == null) {
                    clientInstance = createClient()
                }
            }
        }
    }

    private val json =
        Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    var wbiImgKey: String? = null
    var wbiSubKey: String? = null
    private var wbiLastRefreshDate = 0L

    var buvid3: String = ""

    /**
     * 设备 cookie 字符串（buvid3 + b_nut 等），由 SPI 流程获取并持久化。
     *
     * 由 [fetchBuvid3FromSpi] 设置，由 [injectCookies] 注入到所有 Web 请求中。
     * 只包含设备标识类 cookie（不含 SESSDATA/bili_jct 等登录凭证）。
     */
    var deviceCookies: String = ""

    var sessData: String = ""
    var biliJct: String = ""
    var mid: Long? = null
    var accessToken: String = ""

    fun init(
        buvid3: String,
        deviceCookies: String = "",
        sessData: String = "",
        biliJct: String = "",
        mid: Long? = null,
        accessToken: String = "",
    ) {
        this.buvid3 = buvid3
        this.deviceCookies = deviceCookies
        this.sessData = sessData
        this.biliJct = biliJct
        this.mid = mid
        this.accessToken = accessToken

        CoroutineScope(Dispatchers.IO).launch {
            updateWbi()
        }
        warmUpClient()
    }

    /**
     * 通过 SPI 接口获取 B 站注册的 buvid3 和配套设备 cookie。
     *
     * 本地随机生成的 buvid3 未在 B 站注册，投币等敏感操作会返回 -401（非法访问）。
     * 1. 调用 `/x/frontend/finger/spi` 获取 B 站颁发的 buvid3（响应体）
     * 2. 携带 buvid3 请求 `https://www.bilibili.com/` 获取配套设备 cookie（Set-Cookie 头）
     * 调用方负责持久化返回的 [SpiResult]，避免每次启动都重新获取。
     *
     * @return [SpiResult] 包含 buvid3 和设备 cookie 字符串，失败时返回 null。
     */
    suspend fun fetchBuvid3FromSpi(): SpiResult? =
        runCatching {
            // 1. 获取 SPI buvid3
            val spiResponse = client.get("/x/frontend/finger/spi") {}
            val spiData = spiResponse.body<BiliResponse<SpiData>>()
            if (spiData.code != 0 || spiData.data?.b3.isNullOrBlank()) return@runCatching null
            buvid3 = spiData.data!!.b3!!

            // 2. 携带 buvid3 请求 www.bilibili.com 获取 b_nut 等设备 cookie
            val biliResponse =
                client.get("https://www.bilibili.com/") {}
            deviceCookies = "buvid3=$buvid3" +
                parseSetCookies(biliResponse.headers.getAll("Set-Cookie"))
                    .let { if (it.isNotBlank()) "; $it" else "" }
            SpiResult(buvid3 = buvid3, deviceCookies = deviceCookies)
        }.onFailure {
            // SPI 获取失败时保留本地生成的 buvid3（不影响非敏感接口）
        }.getOrNull()

    /**
     * 将 Set-Cookie 头列表解析为 `name=value; name=value` 格式的字符串。
     * 只保留 buvid3 和 b_nut（过滤 path/expires 等属性）。
     */
    private fun parseSetCookies(setCookies: List<String>?): String {
        if (setCookies.isNullOrEmpty()) return ""
        val cookies = mutableListOf<String>()
        for (header in setCookies) {
            val pair = header.substringBefore(";")
            val name = pair.substringBefore("=", "").trim()
            if (name in setOf("buvid3", "b_nut")) {
                cookies.add(pair.trim())
            }
        }
        return cookies.joinToString("; ")
    }

    private fun createClient(): HttpClient =
        HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) { json(json) }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            install(HttpRequestRetry) { retryOnException(maxRetries = 2) }
            // OkHttp 引擎默认无整体超时，慢速滴流的响应可能永久挂起；
            // 这里加整体请求超时，配合 OkHttp 的连接/读取超时兜底
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
            install(JsoupPlugin)
            defaultRequest {
                url {
                    host = endPoint
                    protocol = URLProtocol.HTTPS
                }
            }
        }.apply {
            encApiSign() // 1. 先注册（LIFO → 后执行）：负责签名
            injectCookies() // 2. 后注册（LIFO → 先执行）：cookie 注入在签名之前
        }

    /**
     * 获取热门视频列表
     */
    suspend fun getPopularVideoData(
        pageNumber: Int = 1,
        pageSize: Int = 20,
    ): BiliResponse<PopularVideoData> =
        client
            .get("/x/web-interface/popular") {
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
            }.body()

    /**
     * 获取视频详细信息
     */
    suspend fun getVideoInfo(
        av: Long? = null,
        bv: String? = null,
    ): BiliResponse<VideoInfo> =
        client
            .get("/x/web-interface/view") {
                parameter("aid", av)
                parameter("bvid", bv)
            }.body()

    /**
     * 获取视频超详细信息
     *
     * 端点：GET /x/web-interface/wbi/view/detail（路径含 wbi，由 encApiSign 拦截器自动 WBI 签名）
     * 鉴权：无需登录也可获取，但未登录时可能返回不完整数据，retry 后可成功
     */
    suspend fun getVideoDetail(
        av: Long? = null,
        bv: String? = null,
    ): BiliResponse<VideoDetail> =
        client
            .get("/x/web-interface/wbi/view/detail") {
                parameter("aid", av)
                parameter("bvid", bv)
                header("Referer", "https://www.bilibili.com")
            }.body()

    /**
     * 获取视频流
     */
    suspend fun getVideoPlayUrl(
        av: Long? = null,
        bv: String? = null,
        cid: Long,
        qn: Int? = 80,
        fnval: Int? = 1,
        fnver: Int? = 0,
        fourk: Int? = 1,
        session: String? = null,
        otype: String = "json",
        type: String = "",
        platform: String = "oc",
    ): BiliResponse<PlayUrlData> {
        val response =
            client.get("/x/player/playurl") {
                require(av != null || bv != null) { "av and bv cannot be null at the same time" }
                parameter("avid", av)
                parameter("bvid", bv)
                parameter("cid", cid)
                parameter("qn", qn)
                parameter("fnval", fnval)
                parameter("fnver", fnver)
                parameter("fourk", fourk)
                parameter("session", session)
                parameter("otype", otype)
                parameter("type", type)
                parameter("platform", platform)
                if (sessData.isEmpty()) {
                    parameter("web_location", "1315873")
                    parameter("gaia_source", "pre-load")
                    parameter("isGaiaAvoided", "true")
                    parameter("try_look", "1")
                }
            }
        val rawText = response.bodyAsText()
        val parsed = json.decodeFromString<JsonObject>(rawText)
        val data = parsed["data"]
        if (data is JsonObject && "v_voucher" in data) {
            throw dev.frost819.newbv.biliapi.http.entity.RiskControlException(
                "触发风控，请稍后再试或更换接口类型",
            )
        }
        return json.decodeFromString(rawText)
    }

    /**
     * 获取剧集视频流
     */
    suspend fun getPgcVideoPlayUrl(
        av: Long? = null,
        bv: String? = null,
        epid: Int? = null,
        cid: Long? = null,
        qn: Int? = null,
        fnval: Int? = null,
        fnver: Int? = null,
        fourk: Int? = null,
        session: String? = null,
        supportMultiAudio: Boolean? = null,
        drmTechType: Int? = null,
        fromClient: String? = null,
    ): BiliResponse<PlayUrlData> =
        client
            .get("/pgc/player/web/playurl") {
                require(av != null || bv != null) { "av and bv cannot be null at the same time" }
                require(epid != null || cid != null) { "epid and cid cannot be null at the same time" }
                av?.let { parameter("avid", it) }
                bv?.let { parameter("bvid", it) }
                epid?.let { parameter("ep_id", it) }
                cid?.let { parameter("cid", it) }
                qn?.let { parameter("qn", it) }
                fnval?.let { parameter("fnval", it) }
                fnver?.let { parameter("fnver", it) }
                fourk?.let { parameter("fourk", it) }
                session?.let { parameter("session", it) }
                supportMultiAudio?.let { parameter("support_multi_audio", it) }
                drmTechType?.let { parameter("drm_tech_type", it) }
                fromClient?.let { parameter("from_client", it) }
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 获取剧集视频流 v2
     */
    suspend fun getPgcVideoPlayUrlV2(
        av: Long? = null,
        bv: String? = null,
        epid: Int? = null,
        cid: Long? = null,
        qn: Int? = null,
        fnval: Int? = null,
        fnver: Int? = null,
        fourk: Int? = null,
        session: String? = null,
        supportMultiAudio: Boolean? = null,
        drmTechType: Int? = null,
        fromClient: String? = null,
    ): BiliResponse<PlayUrlV2Data> =
        client
            .get("/pgc/player/web/v2/playurl") {
                av?.let { parameter("avid", it) }
                bv?.let { parameter("bvid", it) }
                epid?.let { parameter("ep_id", it) }
                cid?.let { parameter("cid", it) }
                qn?.let { parameter("qn", it) }
                fnval?.let { parameter("fnval", it) }
                fnver?.let { parameter("fnver", it) }
                fourk?.let { parameter("fourk", it) }
                session?.let { parameter("session", it) }
                supportMultiAudio?.let { parameter("support_multi_audio", it) }
                drmTechType?.let { parameter("drm_tech_type", it) }
                fromClient?.let { parameter("from_client", it) }
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 通过[cid]获取视频弹幕
     */
    suspend fun getDanmakuXml(cid: Long): DanmakuResponse {
        val xmlChannel =
            client
                .get("/x/v1/dm/list.so") {
                    parameter("oid", cid)
                }.bodyAsChannel()

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc =
            withContext(Dispatchers.IO) {
                dBuilder.parse(xmlChannel.toInputStream())
            }
        doc.documentElement.normalize()

        val chatServer = doc.getElementsByTagName("chatserver").item(0).textContent
        val chatId =
            doc
                .getElementsByTagName("chatid")
                .item(0)
                .textContent
                .toLong()
        val maxLimit =
            doc
                .getElementsByTagName("maxlimit")
                .item(0)
                .textContent
                .toInt()
        val state =
            doc
                .getElementsByTagName("state")
                .item(0)
                .textContent
                .toInt()
        val realName =
            doc
                .getElementsByTagName("real_name")
                .item(0)
                .textContent
                .toInt()
        val source =
            runCatching {
                doc.getElementsByTagName("source").item(0).textContent
            }.getOrDefault("")

        val data = mutableListOf<DanmakuData>()
        val danmakuNodes = doc.getElementsByTagName("d")

        for (i in 0 until danmakuNodes.length) {
            val danmakuNode = danmakuNodes.item(i)
            val p = danmakuNode.attributes.item(0).textContent
            val text = danmakuNode.textContent
            data.add(DanmakuData.fromString(p, text))
        }

        return DanmakuResponse(chatServer, chatId, maxLimit, state, realName, source, data)
    }

    /**
     * 获取视频弹幕分段（Web protobuf 接口）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/danmaku/danmaku_proto.md
     * 端点：GET /x/v2/dm/wbi/web/seg.so（路径含 wbi，由 encApiSign 拦截器自动 WBI 签名）
     * 鉴权：SESSDATA Cookie（injectCookies）；半匿名时仅返回部分弹幕，属文档预期行为
     *
     * @param cid 视频 CID（oid）
     * @param avid 稿件 avid（pid）
     * @param segmentIndex 分段索引，从 1 开始，每 6 分钟一段
     */
    suspend fun getDanmakuSeg(
        cid: Long,
        avid: Long,
        segmentIndex: Int,
    ): List<DanmakuData> {
        val bytes =
            client
                .get("/x/v2/dm/wbi/web/seg.so") {
                    parameter("type", 1)
                    parameter("oid", cid)
                    parameter("pid", avid)
                    parameter("segment_index", segmentIndex)
                }.readRawBytes()
        return DmSegMobileReply.parseFrom(bytes).elemsList.map { DanmakuData.fromDanmakuElem(it) }
    }

    /**
     * 获取弹幕元数据（Web protobuf 接口）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/danmaku/danmaku_proto.md
     * 端点：GET /x/v2/dm/web/view（非 WBI 端点，无需签名，与原版 playurl 策略一致以降低风控风险）
     * 鉴权：Cookie（injectCookies）；未登录可正常获取元数据
     *
     * 返回的分段配置（pageSize/total）用于弹幕分段加载，不硬编码 6 分钟。
     * 注意：App gRPC 的 DmViewReply 不含 dmSge 字段，故元数据固定走 Web 通道。
     *
     * @param cid 视频 CID（oid）
     * @param avid 稿件 avid（pid）
     */
    suspend fun getDanmakuView(
        cid: Long,
        avid: Long,
    ): DmWebViewReply {
        val bytes =
            client
                .get("/x/v2/dm/web/view") {
                    parameter("type", 1)
                    parameter("oid", cid)
                    parameter("pid", avid)
                }.readRawBytes()
        return DmWebViewReply.parseFrom(bytes)
    }

    /**
     * 获取动态列表
     *
     * @param type 返回数据额类型 all:全部 video:视频投稿 pgc:追番追剧 article：专栏
     * @param offset 请求第2页及其之后时填写，填写上一次请求获得的offset
     */
    suspend fun getDynamicList(
        timezoneOffset: Int = -480,
        type: String = "all",
        page: Int = 1,
        offset: String? = null,
    ): BiliResponse<DynamicData> =
        client
            .get("/x/polymer/web-dynamic/v1/feed/all") {
                parameter("timezone_offset", timezoneOffset)
                parameter("type", type)
                parameter("page", page)
                offset?.let { parameter("offset", offset) }
            }.body()

    /**
     * 获取用户[uid]的详细信息。
     *
     * 使用 WBI 签名端点 `/x/space/wbi/acc/info`，旧版 `/x/space/acc/info` 已被 B 站废弃。
     * 路径中的 `wbi` 会自动触发 [encApiSign] 拦截器添加 `w_rid`/`wts` 签名参数。
     */
    suspend fun getUserInfo(uid: Long): BiliResponse<UserInfoData> =
        client
            .get("/x/space/wbi/acc/info") {
                parameter("mid", uid)
            }.body()

    /**
     * 获取用户[uid]的卡片信息
     *
     * @param uid 用户id
     * @param photo 是否请求用户主页头图
     */
    suspend fun getUserCardInfo(
        uid: Long,
        photo: Boolean = false,
    ): BiliResponse<UserCardData> =
        client
            .get("/x/web-interface/card") {
                parameter("mid", uid)
                parameter("photo", photo)
            }.body()

    /**
     * 通过[sessData]获取用户个人信息
     */
    suspend fun getUserSelfInfo(): BiliResponse<MyInfoData> = client.get("/x/space/myinfo").body()

    /**
     * 获取截止至目标id[max]和目标时间[viewAt]历史记录
     *
     * @param business 分类 貌似无效
     * @param pageSize 页面大小
     */
    suspend fun getHistories(
        max: Long = 0,
        business: String = "",
        viewAt: Long = 0,
        pageSize: Int = 20,
    ): BiliResponse<HistoryData> =
        client
            .get("/x/web-interface/history/cursor") {
                parameter("max", max)
                parameter("business", business)
                parameter("view_at", viewAt)
                parameter("ps", pageSize)
            }.body()

    /**
     * 获取稍后再看列表
     */

    suspend fun getToView(accessKey: String? = null): BiliResponse<ToViewData> =
        client
            .get("/x/v2/history/toview") {
                checkToken(accessKey)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    /**
     * 添加视频到稍后再看
     */
    suspend fun addToView(
        avid: Long? = null,
        bvid: String? = null,
        csrf: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/v2/history/toview/add") {
                    require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                avid?.let { append("aid", "$it") }
                                bvid?.let { append("bvid", it) }
                                append("csrf", csrf)
                            },
                        ),
                    )
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    suspend fun addToViewWithAccessKey(
        avid: Long? = null,
        bvid: String? = null,
        accessKey: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/v2/history/toview/add") {
                    require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                avid?.let { append("aid", "$it") }
                                bvid?.let { append("bvid", it) }
                                append("access_key", accessKey)
                            },
                        ),
                    )
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 移除稍后再看的视频
     */
    suspend fun delToView(
        viewed: Boolean = false,
        avid: Long? = null,
        csrf: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/v2/history/toview/del") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("viewed", "${if (viewed) 1 else 0}")
                                avid?.let { append("aid", "$it") }
                                append("csrf", csrf)
                            },
                        ),
                    )
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    suspend fun delToViewWithAccessKey(
        viewed: Boolean = false,
        avid: Long? = null,
        accessKey: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/v2/history/toview/del") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("viewed", "${if (viewed) 1 else 0}")
                                avid?.let { append("aid", "$it") }
                                append("access_key", accessKey)
                            },
                        ),
                    )
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 获取与视频[avid]或[bvid]有关的相关推荐视频
     */
    suspend fun getRelatedVideos(
        avid: Long? = null,
        bvid: String? = null,
    ): RelatedVideosResponse =
        client
            .get("/x/web-interface/archive/related") {
                require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                parameter("aid", avid)
                parameter("bvid", bvid)
            }.body()

    /**
     * 获取收藏夹[mediaId]的元数据
     */
    suspend fun getFavoriteFolderInfo(
        mediaId: Long,
        accessKey: String? = null,
    ): BiliResponse<FavoriteFolderInfo> =
        client
            .get("/x/v3/fav/folder/info") {
                checkToken(accessKey)
                parameter("media_id", mediaId)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    /**
     * 获取用户[mid]的所有收藏夹信息
     *
     * @param type 目标内容属性 默认为全部 0：全部 2：视频稿件
     * @param rid 目标内容id 视频稿件：视频稿件avid
     */
    suspend fun getAllFavoriteFoldersInfo(
        mid: Long,
        type: Int = 0,
        rid: Long? = null,
        accessKey: String? = null,
    ): BiliResponse<UserFavoriteFoldersData> =
        client
            .get("/x/v3/fav/folder/created/list-all") {
                checkToken(accessKey)
                parameter("up_mid", mid)
                parameter("type", type)
                parameter("rid", rid)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    /**
     * 获取收藏夹[mediaId]的详细内容
     *
     * @param tid 分区tid 默认为全部分区 0：全部分区
     * @param keyword 搜索关键字
     * @param order 排序方式 按收藏时间:mtime 按播放量: view 按投稿时间：pubtime
     * @param type 查询范围 0：当前收藏夹（对应media_id） 1：全部收藏夹
     * @param pageSize 每页数量 定义域：1-20
     * @param pageNumber 页码 默认为1
     * @param platform 平台标识 可为web（影响内容列表类型）
     */
    suspend fun getFavoriteList(
        mediaId: Long,
        tid: Int = 0,
        keyword: String? = null,
        order: String? = null,
        type: Int = 0,
        pageSize: Int = 20,
        pageNumber: Int = 1,
        platform: String? = null,
        accessKey: String? = null,
    ): BiliResponse<FavoriteFolderInfoListData> =
        client
            .get("/x/v3/fav/resource/list") {
                checkToken(accessKey)
                parameter("media_id", mediaId)
                parameter("tid", tid)
                parameter("keyword", keyword)
                parameter("order", order)
                parameter("type", type)
                parameter("ps", pageSize)
                parameter("pn", pageNumber)
                parameter("platform", platform)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    /**
     * 获取收藏夹[mediaId]的全部内容id
     */
    suspend fun getFavoriteIdList(
        mediaId: Long,
        platform: String? = null,
        accessKey: String? = null,
    ): FavoriteItemIdListResponse =
        client
            .get("/x/v3/fav/resource/ids") {
                checkToken(accessKey)
                parameter("media_id", mediaId)
                parameter("platform", platform)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    /**
     * 上报视频播放心跳
     *
     * @param avid 稿件avid avid与bvid任选一个
     * @param bvid 稿件bvid avid与bvid任选一个
     * @param cid 视频cid 用于识别分P
     * @param epid 番剧epid
     * @param sid 番剧ssid
     * @param mid 当前用户mid
     * @param playedTime 视频播放进度 单位为秒 默认为0
     * @param realtime 总计播放时间 单位为秒
     * @param startTs 开始播放时刻 时间戳
     * @param type 视频类型 3：投稿视频 4：剧集 10：课程
     * @param subType 剧集副类型 当type=4时本参数有效 1：番剧 2：电影 3：纪录片 4：国创 5：电视剧 7：综艺
     * @param dt 2
     * @param playType 播放动作 0：播放中 1：开始播放 2：暂停 3：继续播放
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendHeartbeat(
        avid: Long? = null,
        bvid: String? = null,
        cid: Long? = null,
        epid: Int? = null,
        sid: Int? = null,
        mid: Long? = null,
        playedTime: Int? = null,
        realtime: Int? = null,
        startTs: Long? = null,
        type: Int? = null,
        subType: Int? = null,
        dt: Int? = null,
        playType: Int? = null,
        csrf: String? = null,
    ): String =
        client
            .post("/x/click-interface/web/heartbeat") {
                require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                setBody(
                    FormDataContent(
                        Parameters.build {
                            avid?.let { append("aid", "$it") }
                            bvid?.let { append("bvid", it) }
                            cid?.let { append("cid", "$it") }
                            epid?.let { append("epid", "$it") }
                            sid?.let { append("sid", "$it") }
                            mid?.let { append("mid", "$it") }
                            playedTime?.let { append("played_time", "$it") }
                            realtime?.let { append("realtime", "$it") }
                            startTs?.let { append("start_ts", "$it") }
                            type?.let { append("type", "$it") }
                            subType?.let { append("sub_type", "$it") }
                            dt?.let { append("dt", "$it") }
                            playType?.let { append("play_type", "$it") }
                            csrf?.let { append("csrf", it) }
                        },
                    ),
                )
            }.bodyAsText()

    suspend fun sendHeartbeatApp(
        avid: Long? = null,
        bvid: String? = null,
        cid: Long? = null,
        epid: Int? = null,
        sid: Int? = null,
        mid: Long? = null,
        playedTime: Int? = null,
        realtime: Int? = null,
        startTs: Long? = null,
        type: Int? = null,
        subType: Int? = null,
        dt: Int? = null,
        playType: Int? = null,
        accessKey: String? = null,
    ): String =
        client
            .post("/x/v2/history/report") {
                require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                setBody(
                    FormDataContent(
                        Parameters.build {
                            avid?.let { append("aid", "$it") }
                            bvid?.let { append("bvid", it) }
                            cid?.let { append("cid", "$it") }
                            epid?.let { append("epid", "$it") }
                            sid?.let { append("sid", "$it") }
                            mid?.let { append("mid", "$it") }
                            playedTime?.let { append("progress", "$it") }
                            realtime?.let { append("realtime", "$it") }
                            startTs?.let { append("start_ts", "$it") }
                            type?.let { append("type", "$it") }
                            subType?.let { append("sub_type", "$it") }
                            dt?.let { append("dt", "$it") }
                            playType?.let { append("play_type", "$it") }
                            accessKey?.let { append("access_key", it) }
                        },
                    ),
                )
            }.bodyAsText()

    /**
     * 获取视频主评论列表。
     *
     * Web 请求使用 SESSDATA，App 请求通过 [accessKey] 鉴权。两种请求返回相同的 JSON
     * 结构，便于 Repository 统一解析。
     *
     * @param aid 视频 AV 号
     * @param sort 排序方式，0 为时间，1 为热度，2 为回复数
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，接口上限为 20
     * @param accessKey App access_key，为空时使用 Web 请求
     */
    suspend fun getVideoComments(
        aid: Long,
        sort: Int = 1,
        page: Int = 1,
        pageSize: Int = 20,
        accessKey: String? = null,
    ): BiliResponse<JsonObject> =
        client
            .get("/x/v2/reply") {
                parameter("type", 1)
                parameter("oid", aid)
                parameter("sort", sort)
                parameter("pn", page.coerceAtLeast(1))
                parameter("ps", pageSize.coerceIn(1, 20))
                parameter("nohot", 1)
                accessKey?.takeIf { it.isNotBlank() }?.let { parameter("access_key", it) }
            }.body()

    /**
     * 获取视频评论的楼中楼。
     *
     * @param aid 视频 AV 号
     * @param rootRpid 根评论 ID
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，接口上限为 49
     * @param accessKey App access_key，为空时使用 Web 请求
     */
    suspend fun getVideoCommentReplies(
        aid: Long,
        rootRpid: Long,
        page: Int = 1,
        pageSize: Int = 20,
        accessKey: String? = null,
    ): BiliResponse<JsonObject> =
        client
            .get("/x/v2/reply/reply") {
                parameter("type", 1)
                parameter("oid", aid)
                parameter("root", rootRpid)
                parameter("pn", page.coerceAtLeast(1))
                parameter("ps", pageSize.coerceIn(1, 49))
                accessKey?.takeIf { it.isNotBlank() }?.let { parameter("access_key", it) }
            }.body()

    /**
     * 点赞或取消点赞评论。
     *
     * @param aid 视频 AV 号
     * @param rpid 评论 ID
     * @param like 是否点赞
     * @param csrf Web 请求的 bili_jct
     * @param accessKey App access_key
     */
    suspend fun updateCommentLiked(
        aid: Long,
        rpid: Long,
        like: Boolean,
        csrf: String? = null,
        accessKey: String? = null,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/v2/reply/action") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("type", "1")
                                append("oid", aid.toString())
                                append("rpid", rpid.toString())
                                append("action", if (like) "1" else "0")
                                csrf?.takeIf { it.isNotBlank() }?.let { append("csrf", it) }
                                accessKey?.takeIf { it.isNotBlank() }?.let { append("access_key", it) }
                            },
                        ),
                    )
                    header("Referer", "https://www.bilibili.com")
                }.body<BiliResponseWithoutData>()
        return (response.code == 0) to response.message
    }

    /**
     * 获取视频[avid]的[cid]视频更多信息，例如播放进度
     */
    suspend fun getVideoMoreInfo(
        avid: Long,
        cid: Long,
    ): BiliResponse<VideoMoreInfo> =
        client
            .get("/x/player/wbi/v2") {
                parameter("aid", avid)
                parameter("cid", cid)
            }.body()

    /**
     * 获取视频同时观看人数（在线人数）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/online.md
     * 鉴权：无需登录，无需 WBI 签名（路径不含 wbi，签名拦截器不会处理）
     *
     * @param avid 稿件 avid，与 [bvid] 至少提供一个
     * @param bvid 稿件 bvid，与 [avid] 至少提供一个
     * @param cid 视频 CID，用于选择目标分 P
     */
    suspend fun getVideoOnlineTotal(
        avid: Long? = null,
        bvid: String? = null,
        cid: Long,
    ): BiliResponse<OnlineTotal> =
        client
            .get("/x/player/online/total") {
                require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                parameter("aid", avid)
                parameter("bvid", bvid)
                parameter("cid", cid)
            }.body()

    /**
     * 获取视频同时观看人数（在线人数）— App 端。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/online.md
     * 鉴权：appkey + sign（host 为 app.bilibili.com，签名拦截器自动追加）
     *
     * @param aid 稿件 avid
     * @param cid 视频 CID，用于选择目标分 P
     */
    suspend fun getAppVideoOnlineTotal(
        aid: Long,
        cid: Long,
    ): BiliResponse<OnlineTotalApp> =
        client
            .get("https://app.bilibili.com/x/v2/view/video/online") {
                parameter("aid", aid)
                parameter("cid", cid)
                parameter("ts", 0)
            }.body()

    /**
     * 为视频[avid]或[bvid]点赞或取消赞
     *
     * @param like 是否点赞
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoLike(
        avid: Long? = null,
        bvid: String? = null,
        like: Boolean = true,
        csrf: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("/x/web-interface/archive/like") {
                    require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                avid?.let { append("aid", "$it") }
                                bvid?.let { append("bvid", it) }
                                append("like", "${if (like) 1 else 2}")
                                append("csrf", csrf)
                            },
                        ),
                    )
                    header("referer", "https://www.bilibili.com")
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 为视频[avid]点赞或取消赞（App 端）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/action.md §点赞视频（APP端）
     * 鉴权：access_key
     *
     * @param avid 稿件 avid
     * @param like true 点赞 / false 取消赞
     * @param accessKey App access_key
     */
    suspend fun sendVideoLikeApp(
        avid: Long,
        like: Boolean,
        accessKey: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("https://app.bilibili.com/x/v2/view/like") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("access_key", accessKey)
                                append("aid", "$avid")
                                append("like", "${if (like) 0 else 1}")
                            },
                        ),
                    )
                }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 检查视频[avid]或[bvid]是否已点赞
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/action.md §判断视频近期是否被点赞（双端）
     * 鉴权：Cookie(SESSDATA) 或 APP(access_key)
     */
    suspend fun checkVideoLiked(
        avid: Long? = null,
        bvid: String? = null,
        accessKey: String? = null,
    ): Boolean {
        val response =
            client
                .get("/x/web-interface/archive/has/like") {
                    require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                    avid?.let { parameter("aid", it) }
                    bvid?.let { parameter("bvid", it) }
                    accessKey?.let { parameter("access_key", it) }
                }.body<BiliResponse<Int>>()
        return runCatching {
            response.getResponseData() == 1
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]或[bvid]点赞或取消赞
     *
     * @param like 是否顺便点赞
     * @param multiply 投币数量
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoCoin(
        avid: Long? = null,
        bvid: String? = null,
        multiply: Int = 1,
        like: Boolean = false,
        csrf: String,
    ): Pair<Boolean, String> {
        require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
        val response =
            client
                .post("/x/web-interface/coin/add") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                avid?.let { append("aid", "$it") }
                                bvid?.let { append("bvid", it) }
                                append("multiply", "$multiply")
                                append("select_like", "${if (like) 1 else 0}")
                                append("cross_domain", "true")
                                append("source", "web_normal")
                                append("ga", "1")
                                append("spmid", "333.788.0.0")
                                append("csrf", csrf)
                            },
                        ),
                    )
                    header("Referer", "https://www.bilibili.com")
                }.body<BiliResponse<AddCoin>>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 检查视频[avid]或[bvid]是否已投币
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/action.md §判断视频是否被投币（双端）
     * 鉴权：Cookie(SESSDATA) 或 APP(access_key)
     */
    suspend fun checkVideoSentCoin(
        avid: Long? = null,
        bvid: String? = null,
        accessKey: String? = null,
    ): Boolean {
        val response =
            client
                .get("/x/web-interface/archive/coins") {
                    require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                    avid?.let { parameter("aid", it) }
                    bvid?.let { parameter("bvid", it) }
                    accessKey?.let { parameter("access_key", it) }
                }.body<BiliResponse<CheckSentCoin>>()
        return runCatching {
            response.getResponseData().multiply != 0
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]投币（App 端）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/action.md §投币视频（APP端）
     * 鉴权：access_key
     *
     * @param avid 稿件 avid
     * @param multiply 投币数量（1-2）
     * @param like 是否同时点赞
     * @param accessKey App access_key
     */
    suspend fun sendVideoCoinApp(
        avid: Long,
        multiply: Int = 1,
        like: Boolean = false,
        accessKey: String,
    ): Pair<Boolean, String> {
        val response =
            client
                .post("https://app.bilibili.com/x/v2/view/coin/add") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("access_key", accessKey)
                                append("aid", "$avid")
                                append("multiply", "$multiply")
                                append("select_like", "${if (like) 1 else 0}")
                            },
                        ),
                    )
                }.body<BiliResponse<AddCoin>>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 为视频[avid]添加到[addMediaIds]或从[delMediaIds]移除
     */
    suspend fun setVideoToFavorite(
        avid: Long,
        type: Int = 2,
        addMediaIds: List<Long> = listOf(),
        delMediaIds: List<Long> = listOf(),
        accessKey: String? = null,
        csrf: String? = null,
    ) {
        checkToken(accessKey)
        val response =
            client
                .post("/x/v3/fav/resource/deal") {
                    require(addMediaIds.isNotEmpty() || delMediaIds.isNotEmpty()) {
                        "addMediaIds and delMediaIds cannot be empty at the same time"
                    }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("rid", "$avid")
                                append("type", "$type")
                                append("add_media_ids", addMediaIds.joinToString(separator = ","))
                                append("del_media_ids", delMediaIds.joinToString(separator = ","))
                                csrf?.let { append("csrf", it) }
                                accessKey?.let { append("access_key", it) }
                            },
                        ),
                    )
                    header("referer", "https://www.bilibili.com")
                }.body<BiliResponse<SetVideoFavorite>>()
        check(response.code == 0) { response.message }
    }

    /**
     * 检查视频[avid]是否已收藏
     */
    suspend fun checkVideoFavoured(
        avid: Long,
        accessKey: String? = null,
    ): Boolean {
        checkToken(accessKey)
        val response =
            client
                .get("/x/v2/fav/video/favoured") {
                    parameter("aid", avid)
                    accessKey?.let { parameter("access_key", it) }
                }.body<BiliResponse<CheckVideoFavoured>>()
        return runCatching {
            response.getResponseData().favoured
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]或[bvid]一键三连
     *
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoOneClickTripleAction(
        avid: Long? = null,
        bvid: String? = null,
        csrf: String,
    ): Triple<Boolean, String, OneClickTripleAction?> {
        require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
        val response =
            client
                .post("/x/web-interface/archive/like/triple") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                avid?.let { append("aid", "$it") }
                                bvid?.let { append("bvid", it) }
                                append("csrf", csrf)
                            },
                        ),
                    )
                    header("referer", "https://www.bilibili.com")
                }.body<BiliResponse<OneClickTripleAction>>()
        return Triple(response.code == 0, response.message, response.data)
    }

    /**
     * 为视频[avid]一键三连（App 端）。
     *
     * 对应文档：docs/bilibili-API-collect-master/docs/video/action.md §一键三连（APP端）
     * 同时点赞投币收藏视频，收藏于默认收藏夹中。鉴权：access_key
     *
     * @param avid 稿件 avid
     * @param accessKey App access_key
     */
    suspend fun sendVideoOneClickTripleActionApp(
        avid: Long,
        accessKey: String,
    ): Triple<Boolean, String, OneClickTripleAction?> {
        val response =
            client
                .post("https://app.bilibili.com/x/v2/view/like/triple") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("access_key", accessKey)
                                append("aid", "$avid")
                            },
                        ),
                    )
                }.body<BiliResponse<OneClickTripleAction>>()
        return Triple(response.code == 0, response.message, response.data)
    }

    /**
     * 获取用户[mid]投稿视频
     *
     * @param order 排序方式 默认为pubdate 最新发布：pubdate 最多播放：click 最多收藏：stow
     * @param tid 筛选目标分区 默认为0 0：不进行分区筛选 分区tid为所筛选的分区
     * @param keyword 关键词筛选 用于使用关键词搜索该UP主视频稿件
     * @param pageNumber 页码
     * @param pageSize 每页项数 最小1，最大50
     */
    suspend fun getWebUserSpaceVideos(
        mid: Long,
        order: String = "pubdate",
        tid: Int = 0,
        keyword: String? = null,
        pageNumber: Int = 1,
        pageSize: Int = 30,
    ): BiliResponse<WebSpaceVideoData> =
        client
            .get("/x/space/wbi/arc/search") {
                parameter("mid", mid)
                parameter("order", order)
                parameter("tid", tid)
                keyword?.let { parameter("keyword", it) }
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
                parameter("dm_img_list", "[]")
                parameter("dm_img_str", "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ")
                parameter(
                    "dm_cover_img_str",
                    "QU5HTEUgKEFNRCwgQU1EIFJhZGVvbiA3ODBNIEdyYXBoaWNzICgweDAwMDAxNUJGKSBEaXJlY3" +
                        "QzRDExIHZzXzVfMCBwc181XzAsIEQzRDExKUdvb2dsZSBJbmMuIChBTU",
                )
                parameter("dm_img_inter", "{\"ds\":[],\"wh\":[4769,2793,43],\"of\":[285,570,285]}")
                header("referer", "https://space.bilibili.com")
            }.body()

    suspend fun getAppUserSpaceVideos(
        mid: Long,
        lastAvid: Long,
        order: String = "pubdate",
        ts: Long,
        accessKey: String,
    ): BiliResponse<AppSpaceVideoData> =
        client
            .get("https://app.bilibili.com/x/v2/space/archive/cursor") {
                parameter("vmid", mid)
                parameter("aid", lastAvid)
                parameter("order", order)
                parameter("ts", ts)
                parameter("access_key", accessKey)
            }.body()

    /**
     * 获取剧集[seasonId]或[epId]的详细信息 (Web)，例如 ss24439 ep234533，传参仅需数字
     */
    suspend fun getWebSeasonInfo(
        seasonId: Int? = null,
        epId: Int? = null,
    ): BiliResponse<WebSeasonData> =
        client
            .get("/pgc/view/web/season") {
                require(seasonId != null || epId != null) { "seasonId and epId cannot be null at the same time" }
                seasonId?.let { parameter("season_id", it) }
                epId?.let { parameter("ep_id", it) }
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 获取剧集[seasonId]或[epId]的详细信息 (App)，例如 ss24439 ep234533，传参仅需数字
     */
    suspend fun getAppSeasonInfo(
        seasonId: Int? = null,
        epId: Int? = null,
        mobiApp: String,
        adExtra: String? = null,
        autoPlay: Int? = null,
        build: Int? = null,
        cLocale: String? = null,
        channel: String? = null,
        disableRcmd: Int? = null,
        fromAv: String? = null,
        fromSpmid: String? = null,
        isShowAllSeries: Int? = null,
        platform: String? = null,
        sLocale: String? = null,
        spmid: String? = null,
        statistics: String? = null,
        trackPath: String? = null,
        trackid: String? = null,
        ts: Int? = null,
        accessKey: String? = "",
    ): BiliResponse<AppSeasonData> =
        client
            .get("/pgc/view/v2/app/season") {
                require(seasonId != null || epId != null) { "seasonId and epId cannot be null at the same time" }
                seasonId?.let { parameter("season_id", it) }
                epId?.let { parameter("ep_id", it) }
                parameter("mobi_app", mobiApp)
                adExtra?.let { parameter("ad_extra", it) }
                autoPlay?.let { parameter("auto_play", it) }
                build?.let { parameter("build", it) }
                cLocale?.let { parameter("c_locale", it) }
                channel?.let { parameter("channel", it) }
                disableRcmd?.let { parameter("disable_rcmd", it) }
                fromAv?.let { parameter("from_av", it) }
                fromSpmid?.let { parameter("from_spmid", it) }
                isShowAllSeries?.let { parameter("is_show_all_series", it) }
                platform?.let { parameter("platform", it) }
                sLocale?.let { parameter("s_locale", it) }
                spmid?.let { parameter("spmid", it) }
                statistics?.let { parameter("statistics", it) }
                trackPath?.let { parameter("track_path", it) }
                trackid?.let { parameter("trackid", it) }
                ts?.let { parameter("ts", it) }
                accessKey?.let { parameter("access_key", accessKey) }
            }.body()

    /**
     * 添加番剧[seasonId]的追番
     */
    suspend fun addSeasonFollow(
        seasonId: Int,
        csrf: String,
    ): BiliResponse<SeasonFollowData> =
        client
            .post("/pgc/web/follow/add") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("season_id", "$seasonId")
                            append("csrf", csrf)
                        },
                    ),
                )
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 添加番剧[seasonId]的追番（App）
     */
    suspend fun addSeasonFollowApp(
        seasonId: Int,
        accessKey: String,
    ): BiliResponse<SeasonFollowData> =
        client
            .post("/pgc/app/follow/add") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("season_id", "$seasonId")
                            append("access_key", accessKey)
                        },
                    ),
                )
            }.body()

    /**
     * 取消番剧[seasonId]的追番
     */
    suspend fun delSeasonFollow(
        seasonId: Int,
        csrf: String,
    ): BiliResponse<SeasonFollowData> =
        client
            .post("/pgc/web/follow/del") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("season_id", "$seasonId")
                            append("csrf", csrf)
                        },
                    ),
                )
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 取消番剧[seasonId]的追番（App）
     */
    suspend fun delSeasonFollowApp(
        seasonId: Int,
        accessKey: String,
    ): BiliResponse<SeasonFollowData> =
        client
            .post("/pgc/app/follow/del") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("season_id", "$seasonId")
                            append("access_key", accessKey)
                        },
                    ),
                )
            }.body()

    /**
     * 单独获取剧集[seasonId]的用户信息[WebSeasonData.UserStatus]
     */
    suspend fun getSeasonUserStatus(seasonId: Int): BiliResponse<WebSeasonData.UserStatus> =
        client
            .get("/pgc/view/web/season/user/status") {
                parameter("season_id", seasonId)
                header("referer", "https://www.bilibili.com")
            }.body()

    /**
     * 获取视频[avid]/[bvid]的视频标签[Tag]
     */
    suspend fun getVideoTags(
        avid: Long? = null,
        bvid: String? = null,
    ): BiliResponse<List<Tag>> =
        client
            .get("/x/tag/archive/tags") {
                require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
                avid?.let { parameter("aid", it) }
                bvid?.let { parameter("bvid", it) }
            }.body()

    /**
     * 获取视频标签[tagId]的详细信息，包含相关标签和最新视频
     */
    suspend fun getTagDetail(
        tagId: Int,
        pageNumber: Int,
        pageSize: Int,
    ): BiliResponse<TagDetail> =
        client
            .get("/x/tag/detail") {
                parameter("tag_id", tagId)
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
            }.body()

    /**
     * 获取视频标签[tagId]的最热门的视频列表
     */
    suspend fun getTagTopVideos(
        tagId: Int,
        pageNumber: Int,
        pageSize: Int,
    ): TagTopVideosResponse =
        client
            .get("/x/web-interface/tag/top") {
                parameter("tid", tagId)
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
            }.body()

    /**
     * 获取剧集更新时间表
     *
     * @param type 番剧: 1 影视（貌似只有少数几个纪录片）: 3, 国创: 4
     */
    suspend fun getTimeline(
        type: Int,
        before: Int,
        after: Int,
    ): BiliResponse<List<Timeline>> =
        client
            .get("/pgc/web/timeline") {
                require(before in 0..7) { "before must in [0,7]" }
                require(after in 0..7) { "after must in [0,7]" }
                parameter("types", type)
                parameter("before", before)
                parameter("after", after)
            }.body()

    /**
     * 获取剧集更新时间表
     *
     * @param filterType 全部: 0 番剧: 1 我的追番: 2 国创: 3
     */
    suspend fun getTimeline(filterType: Int): BiliResponse<TimelineAppData> =
        client
            .get("/pgc/app/timeline") {
                parameter("filter_type", filterType)
                parameter("access_key", "")
            }.body()

    /**
     * 获取用户[mid]的关注列表，对于其他用户只能访问前5页
     */
    suspend fun getUserFollow(
        mid: Long,
        orderType: String? = null,
        pageSize: Int = 50,
        pageNumber: Int = 1,
        accessKey: String? = null,
    ): BiliResponse<UserFollowData> =
        client
            .get("/x/relation/followings") {
                checkToken(accessKey)
                parameter("vmid", mid)
                orderType?.let { parameter("order_type", orderType) }
                parameter("ps", pageSize)
                parameter("pn", pageNumber)
                accessKey?.let { parameter("access_key", accessKey) }
            }.body()

    /**
     * 更改与用户[mid]之间的相互关系[action]
     */
    suspend fun modifyFollow(
        mid: Long,
        action: FollowAction,
        actionSource: FollowActionSource,
        accessKey: String? = null,
        csrf: String? = null,
    ): BiliResponseWithoutData =
        client
            .post("/x/relation/modify") {
                checkToken(accessKey)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("fid", "$mid")
                            append("act", "${action.id}")
                            append("re_src", "${actionSource.id}")
                            accessKey?.let { append("access_key", accessKey) }
                            csrf?.let { append("csrf", csrf) }
                        },
                    ),
                )
            }.body()

    /**
     * 获取与用户[mid]的相互关系[RelationData]
     *
     * 有两个api，响应相同
     * - https://api.bilibili.com/x/space/acc/relation
     * - https://api.bilibili.com/x/web-interface/relation
     */
    suspend fun getRelations(
        mid: Long,
        accessKey: String? = null,
    ): BiliResponse<RelationData> =
        client
            .get("/x/space/wbi/acc/relation") {
                checkToken(accessKey)
                parameter("mid", mid)
                accessKey?.let { parameter("access_key", accessKey) }
            }.body()

    /**
     * 获取用户[mid]的关系统计（关注数，粉丝数，黑名单数）
     */
    suspend fun getRelationStat(
        mid: Long,
        accessKey: String? = null,
    ): BiliResponse<RelationStat> =
        client
            .get("x/relation/stat") {
                parameter("vmid", mid)
                accessKey?.let { parameter("access_key", accessKey) }
            }.body()

    /**
     * 获取搜索提示（Web）
     *
     * @param limit 返回数量
     * @param platform 平台标识
     */
    suspend fun getWebSearchSquare(
        limit: Int = 10,
        platform: String? = null,
    ): BiliResponse<WebSearchSquareData> =
        client
            .get("/x/web-interface/wbi/search/square") {
                parameter("limit", limit)
                platform?.let { parameter("platform", platform) }
            }.body()

    /**
     * 获取搜索提示（App）
     *
     * @param limit 返回数量，上限仅为 10
     * @param platform 平台标识
     */
    suspend fun getAppSearchSquare(
        limit: Int = 10,
        platform: String? = null,
        // accessKey: String = ""
    ): BiliResponse<List<AppSearchSquareData>> =
        client
            .get("https://app.bilibili.com/x/v2/search/square") {
                parameter("limit", limit)
                platform?.let { parameter("platform", platform) }
                parameter("build", BiliAppConf.APP_BUILD_CODE)
                // parameter("access_key", accessKey)
            }.body()

    /**
     * 获取搜索趋势（App）
     *
     * @param limit 返回数量
     */
    suspend fun getSearchTrendRank(limit: Int = 10): BiliResponse<SearchTendingData> =
        client
            .get("https://app.bilibili.com/x/v2/search/trending/ranking") {
                parameter("limit", limit)
                // platform?.let { parameter("platform", platform) }
                // parameter("build", BiliAppConf.APP_BUILD_CODE)
            }.body()

    /**
     * 获取搜索关键词建议
     *
     * 如果请求不带 [mainVer]，那返回的响应将只会包含 result，但不便于数据处理
     *
     * 如果请求中包含了 [highlight]，在返回的结果中 [KeywordSuggest.Result.tag] 的 name 会包含高亮的 html 标签
     */
    @OptIn(InternalAPI::class)
    suspend fun getKeywordSuggest(
        term: String,
        mainVer: String = "v1",
        highlight: String? = null,
        buvid: String,
    ): KeywordSuggest {
        // 需手动解析 json，因为返回的 Content-Type 为 null，会导致 Ktor 抛出异常
        // io.ktor.client.call.NoTransformationFoundException: Expected response body of the type 'class dev.frost819.newbv.biliapi.http.entity.search.KeywordSuggest (Kotlin reflection is not available)' but was 'class io.ktor.utils.io.ByteBufferChannel (Kotlin reflection is not available)'
        // In response from `https://s.search.bilibili.com/main/suggest?term=xxx`
        // Response status `200 `
        // Response header `ContentType: null`
        // Request header `Accept: application/json`
        val responseText =
            client
                .get("https://s.search.bilibili.com/main/suggest") {
                    parameter("term", term)
                    parameter("main_ver", mainVer)
                    highlight?.let { parameter("highlight", it) }
                    parameter("buvid", buvid)
                }.readRawBytes()
                .toString(Charsets.UTF_8)
        val keywordSuggest = json.decodeFromString<KeywordSuggest>(responseText)
        val result = json.decodeFromJsonElement<KeywordSuggest.Result>(keywordSuggest.result!!)
        keywordSuggest.suggests.addAll(result.tag)
        return keywordSuggest
    }

    /**
     * 综合搜索与[keyword]相关的结果
     */
    suspend fun searchAll(
        keyword: String,
        page: Int = 1,
        tid: Int? = null,
        order: String? = null,
        duration: Int? = null,
    ): BiliResponse<SearchResultData> =
        client
            .get("/x/web-interface/wbi/search/all/v2") {
                parameter("keyword", keyword)
                parameter("page", page)
                tid?.let { parameter("tids", it) }
                order?.let { parameter("order", it) }
                duration?.let { parameter("duration", it) }
            }.body()

    /**
     * 分类搜索与[keyword]相关的[type]类型的相关结果
     */
    suspend fun searchType(
        keyword: String,
        type: String,
        page: Int = 1,
        tid: Int? = null,
        order: String? = null,
        duration: Int? = null,
    ): BiliResponse<SearchResultData> =
        client
            .get("/x/web-interface/wbi/search/type") {
                parameter("keyword", keyword)
                parameter("search_type", type)
                parameter("page", page)
                tid?.let { parameter("tids", it) }
                order?.let { parameter("order", it) }
                duration?.let { parameter("duration", it) }
                header("referer", "https://search.bilibili.com/")
            }.body()

    /** 获取番剧首页数据 */
    suspend fun getPgcWebInitialStateData(pgcType: PgcType): PgcWebInitialStateData {
        val path = pgcType.name.lowercase()
        val htmlDocuments = client.get("https://www.bilibili.com/$path").body<Document>()

        val dataScriptTagContent =
            htmlDocuments
                .body()
                .select("script")
                .find {
                    it.html().contains("__INITIAL_STATE__")
                }?.html() ?: throw IllegalStateException("initial state data cannot be null")
        val dataJson =
            dataScriptTagContent.split("__INITIAL_STATE__=", ";(function()")[1]
        val initinalData =
            runCatching {
                json.decodeFromString<PgcWebInitialStateData>(dataJson)
            }.onFailure {
                println("parse initial state data failed: ${it.stackTraceToString()}")
            }.getOrNull() ?: throw IllegalStateException("parse initial state data failed")
        return initinalData
    }

    /**
     * 获取 PGC 猜你喜欢
     *
     * 返回数据的前几条内包含每小时更新的分类排行榜
     */
    suspend fun getPgcFeedV3(
        name: String = "anime",
        cursor: Int = 0,
    ): BiliResponse<PgcFeedV3Data> =
        client
            .get("/pgc/page/web/v3/feed") {
                parameter("name", name)
                parameter("coursor", cursor)
            }.body()

    /**
     * 获取 PGC 猜你喜欢
     */
    suspend fun getPgcFeed(
        name: String = "movie",
        cursor: Int = 0,
    ): BiliResponse<PgcFeedData> =
        client
            .get("/pgc/page/web/feed") {
                parameter("name", name)
                parameter("coursor", cursor)
                parameter("new_cursor_status", true)
            }.body()

    /**
     * 获取用户[mid]的追剧列表
     *
     * @param type 追剧类型
     * @param status 追剧状态
     * @param pageNumber 页码
     * @param pageSize 每页数量 [1, 30]
     * @param mid 用户id
     */
    suspend fun getFollowingSeasons(
        type: Int,
        status: Int,
        pageNumber: Int = 1,
        pageSize: Int = 15,
        mid: Long,
    ): BiliResponse<FollowingSeasonWebData> =
        client
            .get("/x/space/bangumi/follow/list") {
                parameter("type", type)
                parameter("follow_status", status)
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
                parameter("vmid", mid)
            }.body()

    /**
     * 获取用户的追剧列表
     *
     * @param type 追剧类型
     * @param status 追剧状态
     * @param pageNumber 页码
     * @param pageSize 每页数量 [1, 30]
     * @param build App build code
     */
    suspend fun getFollowingSeasons(
        type: String,
        status: Int,
        pageNumber: Int = 1,
        pageSize: Int = 15,
        build: Int,
        accessKey: String,
    ): BiliResponse<FollowingSeasonAppData> =
        client
            .get("/pgc/app/follow/v2/$type") {
                parameter("status", status)
                parameter("pn", pageNumber)
                parameter("ps", pageSize)
                parameter("build", build)
                parameter("access_key", accessKey)
            }.body()

    /**
     * 获取导航栏用户信息
     *
     * 内含 wbi keys
     */
    suspend fun getWebInterfaceNav(): BiliResponse<NavResponseData> = client.get("/x/web-interface/nav").body()

    /**
     * 更新 wbi keys
     */
    suspend fun updateWbi() {
        val now = System.currentTimeMillis()
        val needToUpdate =
            wbiImgKey == null ||
                wbiSubKey == null ||
                (now - wbiLastRefreshDate > 2 * 60 * 60 * 1000L)

        if (!needToUpdate) {
            println("Skip update wbi keys")
            return
        }

        runCatching {
            val wbiData = getWebInterfaceNav().data!!.wbiImg
            wbiImgKey = wbiData.getImgKey()
            wbiSubKey = wbiData.getSubKey()
            println("Update wbi keys: $wbiImgKey, $wbiSubKey")
            wbiLastRefreshDate = now
        }.onFailure {
            println("Update wbi data failed: ${it.stackTraceToString()}")
        }
    }

    /**
     * 获取首页视频推荐列表（Web）
     */
    suspend fun getFeedRcmd(
        freshType: Int = 4,
        pageSize: Int = 30,
        idx: Int = 1,
    ): BiliResponse<RcmdTopData> =
        client
            .get("/x/web-interface/wbi/index/top/feed/rcmd") {
                parameter("fresh_type", freshType)
                parameter("ps", pageSize)
                parameter("fresh_idx", idx)
                parameter("fresh_idx_1h", idx)
            }.body()

    /**
     * 获取首页视频推荐列表（App）
     */
    suspend fun getFeedIndex(
        idx: Int = 0,
        accessKey: String? = null,
    ): BiliResponse<RcmdIndexData> =
        client
            .get("https://app.bilibili.com/x/v2/feed/index") {
                parameter("idx", idx)
                accessKey?.let { parameter("access_key", it) }
            }.body()

    private suspend fun seasonIndexResult(
        seasonIndexType: SeasonIndexType,
        order: Int? = null,
        seasonVersion: Int? = null,
        spokenLanguageType: Int? = null,
        area: Int? = null,
        isFinish: Int? = null,
        copyright: Int? = null,
        seasonStatus: Int? = null,
        seasonMonth: Int? = null,
        year: String? = null,
        releaseDate: String? = null,
        styleId: Int? = null,
        producerId: Int? = null,
        sort: Int? = null,
        page: Int? = null,
        pagesize: Int? = null,
        type: Int? = null,
    ): BiliResponse<IndexResultData> =
        client
            .get("/pgc/season/index/result") {
                parameter("st", seasonIndexType.id)
                order?.let { parameter("order", it) }
                seasonVersion?.let { parameter("season_version", it) }
                spokenLanguageType?.let { parameter("spoken_language_type", it) }
                area?.let { parameter("area", it) }
                isFinish?.let { parameter("is_finish", it) }
                copyright?.let { parameter("copyright", it) }
                seasonStatus?.let { parameter("season_status", it) }
                seasonMonth?.let { parameter("season_month", it) }
                year?.let { parameter("year", it) }
                releaseDate?.let { parameter("release_date", it) }
                styleId?.let { parameter("style_id", it) }
                producerId?.let { parameter("producer_id", it) }
                sort?.let { parameter("sort", it) }
                page?.let { parameter("page", it) }
                parameter("season_type", seasonIndexType.id)
                pagesize?.let { parameter("pagesize", it) }
                type?.let { parameter("type", it) }
            }.body()

    suspend fun seasonIndexAnimeResult(
        order: Int = 0,
        seasonVersion: Int = -1,
        spokenLanguageType: Int = -1,
        area: Int = -1,
        isFinish: Int = -1,
        copyright: Int = -1,
        seasonStatus: Int = -1,
        seasonMonth: Int = -1,
        year: String = "-1",
        styleId: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Anime,
        order = order,
        seasonVersion = seasonVersion,
        spokenLanguageType = spokenLanguageType,
        area = area,
        isFinish = isFinish,
        copyright = copyright,
        seasonStatus = seasonStatus,
        seasonMonth = seasonMonth,
        year = year,
        styleId = styleId,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun seasonIndexGuochuangResult(
        order: Int = 0,
        seasonVersion: Int = -1,
        isFinish: Int = -1,
        copyright: Int = -1,
        seasonStatus: Int = -1,
        year: String = "-1",
        styleId: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Guochuang,
        order = order,
        seasonVersion = seasonVersion,
        isFinish = isFinish,
        copyright = copyright,
        seasonStatus = seasonStatus,
        year = year,
        styleId = styleId,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun seasonIndexVarietyResult(
        order: Int = 0,
        seasonStatus: Int = -1,
        styleId: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Variety,
        order = order,
        seasonStatus = seasonStatus,
        styleId = styleId,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun seasonIndexMovieResult(
        order: Int = 0,
        area: Int = -1,
        styleId: Int = -1,
        releaseDate: String = "-1",
        seasonStatus: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Movie,
        order = order,
        area = area,
        styleId = styleId,
        releaseDate = releaseDate,
        seasonStatus = seasonStatus,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun seasonIndexTvResult(
        order: Int = 0,
        area: Int = -1,
        styleId: Int = -1,
        releaseDate: String = "-1",
        seasonStatus: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Tv,
        order = order,
        area = area,
        styleId = styleId,
        releaseDate = releaseDate,
        seasonStatus = seasonStatus,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun seasonIndexDocumentaryResult(
        order: Int = 0,
        area: Int = -1,
        styleId: Int = -1,
        producerId: Int = -1,
        releaseDate: String = "-1",
        seasonStatus: Int = -1,
        sort: Int = 0,
        page: Int = 1,
        pagesize: Int = 20,
        type: Int = 1,
    ) = seasonIndexResult(
        seasonIndexType = SeasonIndexType.Documentary,
        order = order,
        area = area,
        styleId = styleId,
        producerId = producerId,
        releaseDate = releaseDate,
        seasonStatus = seasonStatus,
        sort = sort,
        page = page,
        pagesize = pagesize,
        type = type,
    )

    suspend fun download(url: String): ByteArray = client.get(url).readRawBytes()

    suspend fun downloadAsStream(url: String): InputStream = client.get(url).bodyAsChannel().toInputStream()

    suspend fun getWebVideoShot(
        aid: Long? = null,
        bvid: String? = null,
        cid: Long? = null,
        needJsonArrayIndex: Boolean = false,
    ): BiliResponse<VideoShot> =
        client
            .get("/x/player/videoshot") {
                require(aid != null || bvid != null) { "av and bv cannot be null at the same time" }
                aid?.let { parameter("aid", it) }
                bvid?.let { parameter("bvid", it) }
                cid?.let { parameter("cid", it) }
                parameter("index", if (needJsonArrayIndex) 1 else 0)
            }.body()

    suspend fun getAppVideoShot(
        aid: Long,
        cid: Long,
    ): BiliResponse<VideoShot> =
        client
            .get("https://app.bilibili.com/x/v2/view/video/shot") {
                parameter("aid", aid)
                parameter("cid", cid)
                parameter("ts", 0)
            }.body()

    suspend fun getUserEquippedGarb(part: EquipPart): BiliResponse<Equip> =
        client
            .get("/x/garb/user/equip") {
                parameter("part", part.value)
            }.body()

    /**
     * 获取分区动态（App），包含顶部轮播图，大卡片活动推广位，和视频列表第一页
     */
    suspend fun getRegionDynamic(
        rid: Int,
        accessKey: String,
    ): BiliResponse<RegionDynamic> =
        client
            .get("https://app.bilibili.com/x/v2/region/dynamic") {
                parameter("access_key", accessKey)
                parameter("build", BiliAppConf.APP_BUILD_CODE)
                parameter("rid", rid)
            }.body()

    /**
     * 获取分区视频列表（App）,用于[getRegionDynamic]加载数据后下滑加载更多数据
     */
    suspend fun getRegionDynamicList(
        rid: Int,
        ctime: Long = 0,
        accessKey: String,
    ): BiliResponse<RegionDynamicList> =
        client
            .get("https://app.bilibili.com/x/v2/region/dynamic/list") {
                parameter("access_key", accessKey)
                parameter("build", BiliAppConf.APP_BUILD_CODE)
                parameter("rid", rid)
                parameter("ctime", ctime)
                parameter("pull", "false")
            }.body()

    //

    /**
     * 获取分区内各种插入的banner，例如顶部轮播图，还有插入的广告横幅（Web）
     *
     * id:
     * 4973  动画  douga
     * 4991  游戏  game
     * 5004  鬼畜  kichiku
     * 4979  音乐  music
     * 4985  舞蹈  dance
     * 5008  影视  cinephile
     * 5007  娱乐  ent
     * 4997  知识  knowledge
     * 4998  科技  tech
     * 5005  资讯  information
     * 5002  美食  food
     * 5001  生活  life
     * 5000  汽车  car
     * 5006  时尚  fashion
     * 4999  运动  sports
     * 5003  动物圈 animal
     */
    suspend fun getLocs(ids: List<Int>): RegionLocs =
        client
            .get("/x/web-show/res/locs") {
                parameter("ids", ids.joinToString(","))
            }.body()

    /**
     * 获取 UGC 分区推荐视频
     *
     * @param displayId 页数
     * @param requestCnt 每页数量
     * @param fromRegion 分区id
     */
    suspend fun getRegionFeedRcmd(
        displayId: Int,
        requestCnt: Int = 15,
        fromRegion: Int,
        device: String = "web",
        plat: Int = 30,
    ): BiliResponse<RegionFeedRcmd> =
        client
            .get("/x/web-interface/region/feed/rcmd") {
                parameter("display_id", displayId)
                parameter("request_cnt", requestCnt)
                parameter("from_region", fromRegion)
                parameter("device", device)
                parameter("plat", plat)
            }.body()
}

enum class SeasonIndexType(
    val id: Int,
) {
    Anime(1),
    Movie(2),
    Documentary(3),
    Guochuang(4),
    Tv(5),
    Variety(7),
    ;

    companion object {
        fun fromId(id: Int) = entries.first { it.id == id }
    }
}

private fun checkToken(accessKey: String?) {
    require(accessKey != null || BiliHttpApi.sessData.isNotBlank()) {
        "accessKey and sessData cannot be null at the same time"
    }
}
