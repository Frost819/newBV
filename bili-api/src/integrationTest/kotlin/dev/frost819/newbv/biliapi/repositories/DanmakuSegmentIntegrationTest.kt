package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Paths
import java.util.Properties

/**
 * 弹幕分段加载接口的集成测试。
 *
 * 覆盖：
 * - API 层：[BiliHttpApi.getDanmakuView]（dm/view 元数据）、[BiliHttpApi.getDanmakuSeg]（分段数据）
 * - Repository 层：[VideoPlayRepository.getDanmakuMeta]、[VideoPlayRepository.getDanmakuSegment]（Web + App 双通道）
 *
 * 测试视频 BV1rHbY6MEB9：时长长、弹幕多，跨多个分段。
 * 依赖真实 B 站凭证和网络（见 local.properties 的 test.* 配置）。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DanmakuSegmentIntegrationTest {
    companion object {
        /** 测试视频：长视频、弹幕量大，至少跨越 2 个分段。 */
        private const val TEST_BV = "BV1rHbY6MEB9"

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

        @JvmStatic
        @BeforeAll
        fun setup() {
            BiliHttpApi.init(
                buvid3 = BUVID,
                sessData = SESSDATA,
                biliJct = BILI_JCT,
                mid = UID,
                accessToken = ACCESS_TOKEN,
            )
        }
    }

    private val authRepository = AuthRepository()
    private val channelRepository = ChannelRepository()
    private val videoPlayRepository = VideoPlayRepository(authRepository, channelRepository)

    private var aid: Long = 0L
    private var cid: Long = 0L

    /** 解析测试视频的 aid/cid，并初始化 gRPC 通道。 */
    @BeforeAll
    fun resolveVideo() {
        channelRepository.initDefaultChannel(ACCESS_TOKEN, BUVID)
        runBlocking {
            val videoInfo = BiliHttpApi.getVideoInfo(bv = TEST_BV).getResponseData()
            aid = videoInfo.aid
            cid = videoInfo.cid
        }
        println("test video: bv=$TEST_BV aid=$aid cid=$cid")
        assertThat(aid).isGreaterThan(0L)
        assertThat(cid).isGreaterThan(0L)
    }

    @Test
    fun `getDanmakuView returns segment config`() {
        assertDoesNotThrow {
            runBlocking {
                val reply = BiliHttpApi.getDanmakuView(cid = cid, avid = aid)
                val pageSize = reply.dmSge.pageSize
                val total = reply.dmSge.total
                println(
                    "dm/view: state=${reply.state} pageSize=${pageSize}ms total=$total count=${reply.count}",
                )
                // 弹幕未关闭
                assertThat(reply.state).isEqualTo(0)
                // 分段大小合法（通常 6 分钟，不做硬性相等断言，以服务端为准）
                assertThat(pageSize).isGreaterThan(0L)
                // 长视频应跨越多个分段
                assertThat(total).isAtLeast(2)
                assertThat(reply.count).isGreaterThan(0L)
            }
        }
    }

    @Test
    fun `getDanmakuSeg returns danmaku for segment 1 and 2 in expected time range`() {
        assertDoesNotThrow {
            runBlocking {
                val pageSizeSec = BiliHttpApi.getDanmakuView(cid = cid, avid = aid).dmSge.pageSize / 1000f

                val seg1 = BiliHttpApi.getDanmakuSeg(cid = cid, avid = aid, segmentIndex = 1)
                val seg2 = BiliHttpApi.getDanmakuSeg(cid = cid, avid = aid, segmentIndex = 2)
                println("seg1 size=${seg1.size} seg2 size=${seg2.size}")
                assertThat(seg1).isNotEmpty()
                assertThat(seg2).isNotEmpty()

                // 各分段弹幕时间应落在对应窗口内（留 30 秒容差）
                assertThat(seg1.minOf { it.time }).isAtLeast(0f)
                assertThat(seg1.maxOf { it.time }).isLessThan(pageSizeSec + 30f)
                assertThat(seg2.minOf { it.time }).isAtLeast(pageSizeSec - 30f)

                // 弹幕字段完整性
                val first = seg1.first()
                assertThat(first.text).isNotEmpty()
                assertThat(first.dmid).isGreaterThan(0L)
            }
        }
    }

    @Test
    fun `getDanmakuMeta returns segment config from repository`() {
        assertDoesNotThrow {
            runBlocking {
                val meta = videoPlayRepository.getDanmakuMeta(aid = aid, cid = cid)
                println(
                    "meta: segmentSizeMs=${meta.segmentSizeMs} segTotal=${meta.segTotal} " +
                        "closed=${meta.closed} count=${meta.count}",
                )
                assertThat(meta.segmentSizeMs).isGreaterThan(0L)
                assertThat(meta.segTotal).isAtLeast(2)
                assertThat(meta.closed).isFalse()
                assertThat(meta.count).isGreaterThan(0L)
            }
        }
    }

    @Test
    fun `getDanmakuSegment with web api returns danmaku`() {
        assertDoesNotThrow {
            runBlocking {
                val segments =
                    videoPlayRepository.getDanmakuSegment(
                        aid = aid,
                        cid = cid,
                        segmentIndex = 1,
                        preferApiType = ApiType.Web,
                    )
                println("web segment 1 size=${segments.size}")
                assertThat(segments).isNotEmpty()
            }
        }
    }

    @Test
    fun `getDanmakuSegment with app api returns danmaku`() {
        assertDoesNotThrow {
            runBlocking {
                val segments =
                    videoPlayRepository.getDanmakuSegment(
                        aid = aid,
                        cid = cid,
                        segmentIndex = 1,
                        preferApiType = ApiType.App,
                    )
                println("app segment 1 size=${segments.size}")
                assertThat(segments).isNotEmpty()
            }
        }
    }
}
