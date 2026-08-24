package dev.frost819.newbv.biliapi.repositories

import bilibili.rpc.Status
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.HeartbeatVideoType
import dev.frost819.newbv.biliapi.grpc.utils.getDetail
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * [VideoPlayRepository] 的集成测试。
 *
 * 验证播放地址、PGC 播放、字幕、心跳、缩略图等（Web + App）。
 * 查询类接口断言正常返回数据（视频流非空等）；心跳为互动类操作仅断言接口返回正常。
 * 依赖真实 B 站凭证和网络。
 */
class VideoPlayRepositoryTest {
    companion object {
        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
    }

    private val authRepository = AuthRepository()
    private val channelRepository = ChannelRepository()
    private val videoPlayRepository = VideoPlayRepository(authRepository, channelRepository)

    init {
        channelRepository.initDefaultChannel(ACCESS_TOKEN, BUVID)
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
        authRepository.sessionData = SESSDATA
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.biliJct = BILI_JCT
    }

    /** 查询类断言：播放数据包含视频流（dash 或 durl）。 */
    private fun assertPlayData(
        data: dev.frost819.newbv.biliapi.entity.PlayData,
        aid: Long,
    ) {
        println(
            "aid=$aid dashVideos=${data.dashVideos.size} dashAudios=${data.dashAudios.size} needPay=${data.needPay}",
        )
        assertThat(data.dashVideos).isNotEmpty()
    }

    @Test
    fun `get flac video with grpc`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 993403941,
                    cid = 1051761130,
                    preferApiType = ApiType.App,
                )
            assertPlayData(result, 993403941)
        }

    @Test
    fun `get flac video with http`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 993403941,
                    cid = 1051761130,
                    preferApiType = ApiType.Web,
                )
            assertPlayData(result, 993403941)
        }

    @Test
    fun `get 8k video with grpc`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 934637444,
                    cid = 455439756,
                    preferApiType = ApiType.App,
                )
            assertPlayData(result, 934637444)
        }

    @Test
    fun `get 8k video with http`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 934637444,
                    cid = 455439756,
                    preferApiType = ApiType.Web,
                )
            assertPlayData(result, 934637444)
        }

    @Test
    fun `get multi part video with http`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 836207,
                    cid = 1215693,
                    preferApiType = ApiType.Web,
                )
            assertPlayData(result, 836207)
        }

    @Test
    fun `get multi part video with grpc`() =
        runBlocking {
            val result =
                videoPlayRepository.getPlayData(
                    aid = 836207,
                    cid = 1215693,
                    preferApiType = ApiType.App,
                )
            assertPlayData(result, 836207)
        }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `parse error status`() {
        // 纯解析测试：断言能解析出业务状态码 -400
        val errorBin =
            "CAISBC00MDQaRAondHlwZS5nb29nbGVhcGlzLmNvbS9iaWxpYmlsaS5ycGMuU3RhdHVzEhkI7Pz/////////ARIM5ZWl6YO95pyo5pyJ"
        val errorData = Base64.decode(errorBin)
        val status = Status.parseFrom(errorData).getDetail()
        println(status)
        assertThat(status).isNotNull()
    }

    @Test
    fun `get pgc video with grpc`() =
        runBlocking {
            val result =
                videoPlayRepository.getPgcPlayData(
                    aid = 210680503,
                    cid = 486114279,
                    epid = 469110,
                    preferApiType = ApiType.App,
                )
            assertPlayData(result, 210680503)
        }

    @Test
    fun `get pgc video with http`() =
        runBlocking {
            val result =
                videoPlayRepository.getPgcPlayData(
                    aid = 210680503,
                    cid = 486114279,
                    epid = 469110,
                    preferApiType = ApiType.Web,
                )
            assertPlayData(result, 210680503)
        }

    @Test
    fun `get paid pgc video with grpc`() =
        runBlocking {
            val result =
                videoPlayRepository.getPgcPlayData(
                    aid = 741219885,
                    cid = 1132332811,
                    epid = 750015,
                    preferApiType = ApiType.App,
                )
            assertPlayData(result, 741219885)
        }

    @Test
    fun `get paid pgc video with http`() =
        runBlocking {
            val result =
                videoPlayRepository.getPgcPlayData(
                    aid = 741219885,
                    cid = 1132332811,
                    epid = 750015,
                    preferApiType = ApiType.Web,
                )
            assertPlayData(result, 741219885)
        }

    @Test
    fun `get subtitle with web api`() =
        runBlocking {
            // 查询类：字幕可能为空（视频无字幕），断言接口正常返回结构
            val result =
                videoPlayRepository.getSubtitle(
                    aid = 913498989,
                    cid = 1203020250,
                    preferApiType = ApiType.Web,
                )
            println("web subtitles: ${result.size}")
            assertThat(result).isNotNull()
        }

    @Test
    fun `get subtitle with app api`() =
        runBlocking {
            val result =
                videoPlayRepository.getSubtitle(
                    aid = 913498989,
                    cid = 1203020250,
                    preferApiType = ApiType.App,
                )
            println("app subtitles: ${result.size}")
            assertThat(result).isNotNull()
        }

    @Test
    fun `send heartbeat with web api`() =
        runBlocking {
            // 互动类：repository 失败会抛异常，正常返回即接口成功
            val randomTime = (0..100).random()
            println("random time: $randomTime")
            videoPlayRepository.sendHeartbeat(
                aid = 170001,
                cid = 280468,
                time = randomTime,
                preferApiType = ApiType.Web,
            )
            videoPlayRepository.sendHeartbeat(
                aid = 476982015,
                cid = 1107179650,
                type = HeartbeatVideoType.Season,
                subType = 4,
                time = randomTime,
                epid = 706666,
                seasonId = 39707,
                preferApiType = ApiType.Web,
            )
        }

    @Test
    fun `send heartbeat with app api`() =
        runBlocking {
            val randomTime = (0..100).random()
            println("random time: $randomTime")
            videoPlayRepository.sendHeartbeat(
                aid = 170001,
                cid = 280468,
                time = randomTime,
                preferApiType = ApiType.App,
            )
            videoPlayRepository.sendHeartbeat(
                aid = 476982015,
                cid = 1107179650,
                type = HeartbeatVideoType.Season,
                subType = 4,
                time = randomTime,
                epid = 706666,
                seasonId = 39707,
                preferApiType = ApiType.App,
            )
        }

    @Test
    fun `get play url domain`() =
        runBlocking {
            // 查询类：断言每个接口类型返回视频流且 URL 可解析域名
            val getUrlDomain: (String) -> String = {
                val url = URL(it)
                "${url.protocol}://${url.host}"
            }
            ApiType.entries.forEach { apiType ->
                val result =
                    videoPlayRepository.getPlayData(
                        aid = 934637444,
                        cid = 455439756,
                        preferApiType = apiType,
                    )
                println("api type: $apiType, videos: ${result.dashVideos.size}")
                assertThat(result.dashVideos).isNotEmpty()
                result.dashVideos.forEach { video ->
                    println("video quality: ${video.quality}")
                    val videoUrls = mutableListOf<String>()
                    videoUrls.add(video.baseUrl)
                    videoUrls.addAll(video.backUrl)
                    videoUrls.forEach { println(getUrlDomain(it)) }
                }
            }
        }

    @Test
    fun `get video shots`() =
        runBlocking {
            // 查询类：断言缩略图数据非空
            ApiType.entries.forEach { apiType ->
                val result =
                    videoPlayRepository.getVideoShot(
                        aid = 170001,
                        cid = 279786,
                        preferApiType = apiType,
                    )
                println("api type: $apiType, shots: ${result?.times?.size ?: 0}")
                assertThat(result).isNotNull()
                assertThat(result?.times).isNotEmpty()
            }
        }

    @Test
    fun `play video three times without risk control`() =
        runBlocking {
            // 查询类：连续 3 次获取播放地址均成功
            val bvid = "BV1fuuc6tEhW"
            val info = BiliHttpApi.getVideoInfo(bv = bvid).getResponseData()
            println("aid=${info.aid}, cid=${info.cid}, title=${info.title}")
            repeat(3) { i ->
                println("=== Attempt ${i + 1} ===")
                val result =
                    videoPlayRepository.getPlayData(
                        aid = info.aid,
                        cid = info.cid,
                        preferApiType = ApiType.Web,
                    )
                println("  success: ${result.dashVideos.size} video streams")
                assertThat(result.dashVideos).isNotEmpty()
            }
        }

    @Test
    fun `get danmaku mask with web api`() =
        runBlocking {
            // BV14CeDzREjj 确定有弹幕蒙版
            val videoInfo = BiliHttpApi.getVideoInfo(bv = "BV14CeDzREjj").getResponseData()
            val aid = videoInfo.aid
            val cid = videoInfo.cid
            val result = videoPlayRepository.getDanmakuMask(aid = aid, cid = cid, preferApiType = ApiType.Web)
            assertThat(result).isNotNull()
            assertThat(result!!.segmentCount).isGreaterThan(0)
        }

    @Test
    fun `get danmaku mask with app api`() =
        runBlocking {
            val videoInfo = BiliHttpApi.getVideoInfo(bv = "BV14CeDzREjj").getResponseData()
            val aid = videoInfo.aid
            val cid = videoInfo.cid
            val result = videoPlayRepository.getDanmakuMask(aid = aid, cid = cid, preferApiType = ApiType.App)
            assertThat(result).isNotNull()
            assertThat(result!!.segmentCount).isGreaterThan(0)
        }
}
