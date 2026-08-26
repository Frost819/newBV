package dev.frost819.newbv.biliapi.http

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonStatus
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.biliapi.http.entity.user.FollowAction
import dev.frost819.newbv.biliapi.http.entity.user.FollowActionSource
import dev.frost819.newbv.biliapi.http.entity.user.garb.EquipPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.Properties

internal class BiliHttpApiTest {
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

        @JvmStatic
        @BeforeAll
        fun setup() {
            // 在这里执行初始化
            BiliHttpApi.init(
                buvid3 = BUVID,
                sessData = SESSDATA,
                biliJct = BILI_JCT,
                mid = UID,
                accessToken = ACCESS_TOKEN,
            )
            println("BiliHttpApi initialized with BUVID: $BUVID")
        }
    }

    @Test
    fun `println sessdata and bili_jct`() {
        println("SESSDATA: $SESSDATA")
        println("BILI_JCT: $BILI_JCT")
        assertThat(SESSDATA).isNotEmpty()
        assertThat(BILI_JCT).isNotEmpty()
    }

    @Test
    fun `get popular videos`() {
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getPopularVideoData()
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.list).isNotEmpty()
            }
        }
    }

    @Test
    fun `get video info`() {
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getVideoInfo(av = 170001)
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.aid).isEqualTo(170001)
            }
        }
    }

    @Test
    fun `get video info which is ugc season`() {
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getVideoInfo(av = 433139956)
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.aid).isEqualTo(433139956)
            }
        }
    }

    @Test
    fun `get video play url`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getVideoPlayUrl(
                        av = 648092492,
                        cid = 903675075,
                        fnval = 4048,
                        qn = 127,
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.quality).isGreaterThan(0)
            }
        }
    }

    @Test
    fun `get video online total`() {
        val aid = localProperties.getProperty("test.video.aid")?.toLongOrNull() ?: 993403941L
        val cid = localProperties.getProperty("test.video.cid")?.toLongOrNull() ?: 1051761130L
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getVideoOnlineTotal(avid = aid, cid = cid)
                println(response)
                assertThat(response.code).isEqualTo(0)
                val data = response.data
                assertThat(data).isNotNull()
                // total 或 count 至少有一个可展示（除非 UP 主关闭了全部开关）
                println("online total display text: ${data!!.displayText()}")
            }
        }
    }

    @Test
    fun `get app video online total`() {
        val aid = localProperties.getProperty("test.video.aid")?.toLongOrNull() ?: 993403941L
        val cid = localProperties.getProperty("test.video.cid")?.toLongOrNull() ?: 1051761130L
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getAppVideoOnlineTotal(aid = aid, cid = cid)
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                println("app online total text: ${response.data!!.online.totalText}")
            }
        }
    }

    @Test
    fun `get pgc video play url`() {
        runBlocking {
            val response =
                BiliHttpApi.getPgcVideoPlayUrl(
                    av = 672676070,
                    cid = 331748015,
                    fnval = 4048,
                    qn = 127,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.getResponseData().quality).isGreaterThan(0)
        }
    }

    @Test
    fun `get pgc video play url v2`() {
        runBlocking {
            val response =
                BiliHttpApi.getPgcVideoPlayUrlV2(
                    av = 672676070,
                    cid = 331748015,
                    fnval = 4048,
                    qn = 127,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.getResponseData().videoInfo.quality).isGreaterThan(0)
        }
    }

    @Test
    fun `get video danmaku from xml`() {
        assertDoesNotThrow {
            runBlocking {
                val response = BiliHttpApi.getDanmakuXml(cid = 903675075)
                println(response)
                assertThat(response.data).isNotEmpty()
            }
        }
    }

    @Test
    fun `get dynamic list with type all`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getDynamicList(
                        type = "article",
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.items).isNotEmpty()
            }
        }
    }

    @Test
    fun `get user info from Mr_He`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getUserInfo(
                        uid = 163637592,
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.mid).isEqualTo(163637592)
            }
        }
    }

    @Test
    fun `get user card info from Mr_He`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getUserCardInfo(
                        uid = 163637592,
                        photo = true,
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.card.mid.toLong()).isEqualTo(163637592L)
            }
        }
    }

    @Test
    fun `get self user info`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getUserSelfInfo()
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.mid).isEqualTo(UID)
            }
        }
    }

    @Test
    fun `get histories`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getHistories(
                        viewAt = 0,
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotNull()
                assertThat(response.data!!.list).isNotEmpty()
            }
        }
    }

    @Test
    fun `get related vidoes`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.getRelatedVideos(
                        avid = 170001,
                    )
                println(response)
                assertThat(response.code).isEqualTo(0)
                assertThat(response.data).isNotEmpty()
            }
        }
    }

    @Test
    fun `get favorite folder metadata from id 2333`() {
        runBlocking {
            val response =
                BiliHttpApi.getFavoriteFolderInfo(
                    mediaId = 2333,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.id).isEqualTo(2333L)
        }
    }

    @Test
    fun `get all favorite folders metadata`() {
        runBlocking {
            val response =
                BiliHttpApi.getAllFavoriteFoldersInfo(
                    mid = 2333,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
        }
    }

    @Test
    fun `get all favorite item ids`() {
        runBlocking {
            val response =
                BiliHttpApi.getFavoriteIdList(
                    mediaId = 2333,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }
    }

    @Test
    fun `get favorite list`() {
        runBlocking {
            val response =
                BiliHttpApi.getFavoriteList(
                    mediaId = 2333,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }
    }

    @Test
    fun `send heartbeat`() {
        assertDoesNotThrow {
            runBlocking {
                val response =
                    BiliHttpApi.sendHeartbeat(
                        avid = 170001,
                        cid = 280468,
                        playedTime = 23,
                    )
                println(response)
                assertThat(response).isNotEmpty()
            }
        }
    }

    @Test
    fun `get video more info`() {
        runBlocking {
            val response =
                BiliHttpApi.getVideoMoreInfo(
                    avid = 170001,
                    cid = 279786,
                )
            println("code: ${response.code}, message: ${response.message}")
            assertThat(response.code).isEqualTo(0)
            val moreInfo = response.getResponseData()
            println("lastPlayTime: ${moreInfo.lastPlayTime}")
            println("lastPlayCid: ${moreInfo.lastPlayCid}")
            assertThat(moreInfo.aid).isEqualTo(170001)
        }
    }

    @Test
    fun `send video like`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.sendVideoLike(
                        avid = 170001,
                        like = true,
                        csrf = BILI_JCT,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `check video is liked`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.checkVideoLiked(
                        avid = 170001,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `send video coin`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.sendVideoCoin(
                        avid = 170001,
                        csrf = BILI_JCT,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `check video coin`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.checkVideoSentCoin(
                        avid = 170001,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `add video to favorite`() {
        assertDoesNotThrow {
            runBlocking {
                val folders =
                    BiliHttpApi.getAllFavoriteFoldersInfo(
                        mid = UID,
                        type = 2,
                        rid = 170001,
                    ).getResponseData()
                val mediaId = folders.list.firstOrNull()?.id
                requireNotNull(mediaId) { "当前账号没有收藏夹，请先创建一个" }
                println("using favorite folder: $mediaId")
                println(
                    BiliHttpApi.setVideoToFavorite(
                        avid = 170001,
                        addMediaIds = listOf(mediaId),
                        csrf = BILI_JCT,
                    ),
                )
            }
        }
    }

    @Test
    fun `delete video from favorite`() {
        assertDoesNotThrow {
            runBlocking {
                val folders =
                    BiliHttpApi.getAllFavoriteFoldersInfo(
                        mid = UID,
                        type = 2,
                        rid = 170001,
                    ).getResponseData()
                val mediaId = folders.list.firstOrNull()?.id
                requireNotNull(mediaId) { "当前账号没有收藏夹，请先创建一个" }
                println("using favorite folder: $mediaId")
                println(
                    BiliHttpApi.setVideoToFavorite(
                        avid = 170001,
                        delMediaIds = listOf(mediaId),
                        csrf = BILI_JCT,
                    ),
                )
            }
        }
    }

    @Test
    fun `check is video in favorite`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.checkVideoFavoured(
                        avid = 170001,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `send one click triple action`() {
        assertDoesNotThrow {
            runBlocking {
                val result =
                    BiliHttpApi.sendVideoOneClickTripleAction(
                        avid = 170001,
                        csrf = BILI_JCT,
                    )
                println(result)
            }
        }
    }

    @Test
    fun `get user space videos`() =
        runBlocking {
            val response = BiliHttpApi.getWebUserSpaceVideos(mid = 1)
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.list).isNotNull()
            assertThat(response.data!!.list!!.vlist).isNotEmpty()
        }

    @Test
    fun `get web season info data`() {
        runBlocking {
            val response =
                BiliHttpApi.getWebSeasonInfo(
                    epId = 705917,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.getResponseData().episodes).isNotEmpty()
        }
    }

    @Test
    fun `get app season info data`() {
        runBlocking {
            val response =
                BiliHttpApi.getAppSeasonInfo(
                    epId = 752900,
                    seasonId = 45303,
                    mobiApp = "android_hd",
                    accessKey = ACCESS_TOKEN,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.seasonId).isEqualTo(45303)
        }
    }

    @Test
    fun `get user season status data`() {
        runBlocking {
            val response =
                BiliHttpApi.getSeasonUserStatus(
                    seasonId = 44152,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.getResponseData()).isNotNull()
        }
    }

    @Test
    fun `get video tags`() {
        runBlocking {
            val response =
                BiliHttpApi.getVideoTags(
                    avid = 170001,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!).isNotEmpty()
        }
    }

    @Test
    fun `get tag detail`() {
        runBlocking {
            val response =
                BiliHttpApi.getTagDetail(
                    tagId = 6020278,
                    pageNumber = 1,
                    pageSize = 20,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.info.tagId).isEqualTo(6020278)
        }
    }

    @Test
    fun `get tag popular videos`() {
        runBlocking {
            val response =
                BiliHttpApi.getTagTopVideos(
                    tagId = 6020278,
                    pageNumber = 1,
                    pageSize = 20,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotEmpty()
        }
    }

    @Test
    fun `get web timeline`() {
        runBlocking {
            val result =
                BiliHttpApi.getTimeline(
                    type = 1,
                    before = 7,
                    after = 7,
                )
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.getResponseData()).isNotEmpty()
        }
    }

    @Test
    fun `get app timeline`() {
        runBlocking {
            val result =
                BiliHttpApi.getTimeline(
                    filterType = 0,
                )
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.getResponseData().data).isNotEmpty()
        }
    }

    @Test
    fun `get follow list`() {
        runBlocking {
            val response =
                BiliHttpApi.getUserFollow(
                    mid = 3066511,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `add follow`() {
        runBlocking {
            val response =
                BiliHttpApi.modifyFollow(
                    mid = 3066511,
                    action = FollowAction.AddFollow,
                    actionSource = FollowActionSource.Space,
                    csrf = BILI_JCT,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
        }
    }

    @Test
    fun `delete follow`() {
        runBlocking {
            val response =
                BiliHttpApi.modifyFollow(
                    mid = 3066511,
                    action = FollowAction.DelFollow,
                    actionSource = FollowActionSource.Space,
                    csrf = BILI_JCT,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
        }
    }

    @Test
    fun `get user relations`() {
        runBlocking {
            val response =
                BiliHttpApi.getRelations(
                    mid = 11336264,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }
    }

    @Test
    fun `get user relation stat`() {
        runBlocking {
            val response =
                BiliHttpApi.getRelationStat(
                    mid = 11336264,
                )
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }
    }

    @Test
    fun `get web search hot words`() {
        runBlocking {
            val result = BiliHttpApi.getWebSearchSquare()
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.trending.list).isNotEmpty()
        }
    }

    @Test
    fun `get app search hot words`() {
        runBlocking {
            val result = BiliHttpApi.getAppSearchSquare()
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!).isNotEmpty()
        }
    }

    @Test
    fun `get app search trending ranking`() {
        runBlocking {
            val result = BiliHttpApi.getSearchTrendRank()
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get search keyword suggests`() {
        runBlocking {
            val result =
                BiliHttpApi.getKeywordSuggest(
                    term = "和奥托一起泡温泉",
                    buvid = BUVID,
                )
            println(result)
            assertThat(result.code).isIn(0..3)
            assertThat(result.suggests).isNotNull()
        }
    }

    @Test
    fun `search all`() {
        runBlocking {
            val result =
                BiliHttpApi.searchAll(
                    keyword = "007",
                )
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.result).isNotEmpty()
        }
    }

    @Test
    fun `search type`() {
        val types =
            listOf("video", "media_bangumi", "media_ft", "article", "topic", "bili_user")
        runBlocking {
            types.forEach { type ->
                runCatching {
                    BiliHttpApi.searchType(
                        keyword = "007",
                        type = type,
                    )
                }.onSuccess { result ->
                    println(result)
                    if (result.code == 0) {
                        assertThat(result.data).isNotNull()
                    } else {
                        println("search type [$type] returned code ${result.code}: ${result.message}")
                    }
                }.onFailure { println("search type [$type] failed: ${it.message}") }
            }
        }
    }

    @Test
    fun `get web initial state data`() {
        runBlocking {
            PgcType.entries.forEach { pgcType ->
                println("type: ${pgcType.name}")
                val result = BiliHttpApi.getPgcWebInitialStateData(pgcType)
                println(
                    result.toString().replace("\n", ""),
                )
                assertThat(result.modules.banner.items).isNotEmpty()
            }
        }
    }

    @Test
    fun `get pgc feed data`() {
        runBlocking {
            PgcType.entries.forEach { pgcType ->
                println("type: ${pgcType.name}")
                when (pgcType) {
                    PgcType.Anime, PgcType.GuoChuang -> {
                        val result =
                            BiliHttpApi.getPgcFeedV3(name = pgcType.name.lowercase())
                        println(
                            result.toString().replace("\n", ""),
                        )
                        assertThat(result.code).isEqualTo(0)
                        assertThat(result.data).isNotNull()
                        assertThat(result.data!!.items).isNotNull()
                    }

                    PgcType.Tv, PgcType.Movie, PgcType.Documentary, PgcType.Variety -> {
                        val result =
                            BiliHttpApi.getPgcFeed(name = pgcType.name.lowercase())
                        println(
                            result.toString().replace("\n", ""),
                        )
                        assertThat(result.code).isEqualTo(0)
                        assertThat(result.data).isNotNull()
                        assertThat(result.data!!.items).isNotNull()
                    }
                }
            }
        }
    }

    @Test
    fun `get web following season data`() {
        runBlocking {
            for (followingSeasonType in FollowingSeasonType.values()) {
                for (followingSeasonStatus in FollowingSeasonStatus.values()) {
                    println("type: $followingSeasonType, status: $followingSeasonStatus: ")
                    val response =
                        BiliHttpApi.getFollowingSeasons(
                            type = followingSeasonType.id,
                            status = followingSeasonStatus.id,
                            pageNumber = 1,
                            pageSize = 1,
                            mid = UID,
                        )
                    println(response)
                    assertThat(response.code).isEqualTo(0)
                    assertThat(response.data).isNotNull()
                }
            }
        }
    }

    @Test
    fun `get app following season data`() {
        runBlocking {
            for (followingSeasonType in FollowingSeasonType.values()) {
                for (followingSeasonStatus in FollowingSeasonStatus.values()) {
                    println("type: $followingSeasonType, status: $followingSeasonStatus: ")
                    val response =
                        BiliHttpApi.getFollowingSeasons(
                            type = followingSeasonType.paramName,
                            status = followingSeasonStatus.id,
                            pageNumber = 1,
                            pageSize = 1,
                            build = 6830300,
                            accessKey = ACCESS_TOKEN,
                        )
                    println(response)
                    assertThat(response.code).isEqualTo(0)
                    assertThat(response.getResponseData().total).isAtLeast(0)
                }
            }
        }
    }

    @Test
    fun `get web homepage recommend items`() {
        runBlocking {
            val result = BiliHttpApi.getFeedRcmd()
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.item).isNotEmpty()
        }
    }

    @Test
    fun `get app homepage recommend items`() {
        runBlocking {
            val result = BiliHttpApi.getFeedIndex()
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.items).isNotEmpty()
        }
    }

    @Test
    fun `get anime index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexAnimeResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get guochuang index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexGuochuangResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get movie index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexMovieResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get tv index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexTvResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get variety season index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexVarietyResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get documentary season index`() {
        runBlocking {
            val result = BiliHttpApi.seasonIndexDocumentaryResult()
            println(result.data?.list?.map { it.title })
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.list).isNotEmpty()
        }
    }

    @Test
    fun `get web video shot`() {
        runBlocking {
            val result = BiliHttpApi.getWebVideoShot(aid = 170001)
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.image).isNotEmpty()
        }
    }

    @Test
    fun `get app video shot`() {
        runBlocking {
            val result = BiliHttpApi.getAppVideoShot(aid = 170001, cid = 279786)
            println(result)
            assertThat(result.code).isEqualTo(0)
            assertThat(result.data).isNotNull()
            assertThat(result.data!!.image).isNotEmpty()
        }
    }

    @Test
    fun `get app region dynamic`() {
        runBlocking {
            val rids =
                listOf(
                    1, 13, 167, 3, 129, 4, 36, 188, 234, 223, 160,
                    211, 217, 119, 155, 202, 5, 181, 177, 23, 11,
                )
            rids
                .shuffled()
                .forEach { rid ->
                    println("rid $rid:")
                    val result =
                        BiliHttpApi.getRegionDynamic(
                            rid = rid,
                            accessKey = ACCESS_TOKEN,
                        )
                    println(result)
                    assertThat(result.code).isEqualTo(0)
                    assertThat(result.data).isNotNull()
                    assertThat(result.data!!.new).isNotEmpty()
                    delay((800L..2000L).random())
                }
        }
    }

    @Test
    fun `get app region dynamic list`() {
        runBlocking {
            val rids =
                listOf(
                    1, 13, 167, 3, 129, 4, 36, 188, 234, 223, 160,
                    211, 217, 119, 155, 202, 5, 181, 177, 23, 11,
                )
            rids
                .shuffled()
                .forEach { rid ->
                    println("rid $rid:")
                    val result =
                        BiliHttpApi.getRegionDynamicList(
                            rid = rid,
                            accessKey = ACCESS_TOKEN,
                        )
                    println(result)
                    assertThat(result.code).isEqualTo(0)
                    assertThat(result.data).isNotNull()
                    assertThat(result.data!!.new).isNotEmpty()
                    delay((800L..2000L).random())
                }
        }
    }

    @Test
    fun `get locs`() {
        runBlocking {
            val locIds =
                listOf(
                    4973, 4991, 5004, 4979, 4985, 5008, 5007, 4997,
                    4998, 5005, 5002, 5001, 5000, 5006, 4999, 5003,
                )

            locIds.chunked(3).forEach { locs ->
                println("${locs.joinToString(",")}:")
                val result =
                    BiliHttpApi.getLocs(
                        ids = locs,
                    )
                println(result)
                assertThat(result.code).isEqualTo(0)
                assertThat(result.data).isNotEmpty()
                delay((800L..2000L).random())
            }
        }
    }

    @Test
    fun `add to watch later`() =
        runBlocking {
            val result =
                BiliHttpApi.addToView(
                    avid = 170001,
                    csrf = BILI_JCT,
                )
            println(result)
            assertThat(result.first).isTrue()
        }

    @Test
    fun `delete from watch later`() =
        runBlocking {
            val addRes =
                BiliHttpApi.addToView(
                    avid = 170001,
                    csrf = BILI_JCT,
                )
            println(addRes)
            assertThat(addRes.first).isTrue()
            val result =
                BiliHttpApi.delToView(
                    viewed = false,
                    avid = 170001,
                    csrf = BILI_JCT,
                )
            println(result)
            assertThat(result.first).isTrue()
        }

    // ===== App 端接口 =====

    @Test
    fun `send video like with app api`() =
        runBlocking {
            val result = BiliHttpApi.sendVideoLikeApp(avid = 170001, like = true, accessKey = ACCESS_TOKEN)
            println(result)
            assertThat(result.first).isTrue()
        }

    @Test
    fun `send video coin with app api`() =
        runBlocking {
            val result =
                BiliHttpApi.sendVideoCoinApp(
                    avid = 170001,
                    multiply = 1,
                    like = false,
                    accessKey = ACCESS_TOKEN,
                )
            // 超过投币上限算正常（每天有上限），只验证接口能正常调用返回结果
            assertThat(result.second).isNotEmpty()
        }

    @Test
    fun `send video one click triple action with app api`() =
        runBlocking {
            val result = BiliHttpApi.sendVideoOneClickTripleActionApp(avid = 170001, accessKey = ACCESS_TOKEN)
            // 已三连过算正常，只验证接口能正常调用返回结果
            assertThat(result.second).isNotEmpty()
        }

    @Test
    fun `add and del to view with access key`() =
        runBlocking {
            val addResult = BiliHttpApi.addToViewWithAccessKey(avid = 170001, accessKey = ACCESS_TOKEN)
            println("add: $addResult")
            assertThat(addResult.first).isTrue()
            val delResult = BiliHttpApi.delToViewWithAccessKey(viewed = false, avid = 170001, accessKey = ACCESS_TOKEN)
            println("del: $delResult")
            assertThat(delResult.first).isTrue()
        }

    @Test
    fun `get app user space videos`() =
        runBlocking {
            val response = BiliHttpApi.getAppUserSpaceVideos(mid = UID, lastAvid = 0, ts = 0, accessKey = ACCESS_TOKEN)
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }

    @Test
    fun `send heartbeat with app api`() =
        runBlocking {
            val result =
                BiliHttpApi.sendHeartbeatApp(
                    avid = 170001,
                    cid = 280468,
                    playedTime = 10,
                    accessKey = ACCESS_TOKEN,
                )
            println(result)
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `get video comments`() =
        runBlocking {
            val response = BiliHttpApi.getVideoComments(aid = 170001, sort = 1, page = 1)
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }

    @Test
    fun `get video comment replies`() {
        runBlocking {
            val commentsResponse = BiliHttpApi.getVideoComments(aid = 170001, sort = 1, page = 1)
            assertThat(commentsResponse.code).isEqualTo(0)
            val data = requireNotNull(commentsResponse.data) { "comments data should not be null" }
            val replies = data["replies"]?.jsonArray
            assertThat(replies).isNotNull()
            assertThat(replies!!.size).isGreaterThan(0)
            val rootRpid = replies.first().jsonObject["rpid"]!!.jsonPrimitive.content.toLong()
            val response = BiliHttpApi.getVideoCommentReplies(aid = 170001, rootRpid = rootRpid, page = 1)
            println(response)
            assertThat(response.code).isEqualTo(0)
        }
    }

    @Test
    fun `update comment liked`() {
        runBlocking {
            val commentsResponse = BiliHttpApi.getVideoComments(aid = 170001, sort = 1, page = 1)
            assertThat(commentsResponse.code).isEqualTo(0)
            val data = requireNotNull(commentsResponse.data) { "comments data should not be null" }
            val replies = data["replies"]?.jsonArray
            assertThat(replies).isNotNull()
            assertThat(replies!!.size).isGreaterThan(0)
            val rootRpid = replies.first().jsonObject["rpid"]!!.jsonPrimitive.content.toLong()
            val likeResult = BiliHttpApi.updateCommentLiked(aid = 170001, rpid = rootRpid, like = true, csrf = BILI_JCT)
            println("like: $likeResult")
            assertThat(likeResult.first).isTrue()
            val unlikeResult =
                BiliHttpApi.updateCommentLiked(
                    aid = 170001,
                    rpid = rootRpid,
                    like = false,
                    csrf = BILI_JCT,
                )
            println("unlike: $unlikeResult")
            assertThat(unlikeResult.first).isTrue()
        }
    }

    // ===== 杂项接口 =====

    @Test
    fun `get user equipped garb`() =
        runBlocking {
            val response = BiliHttpApi.getUserEquippedGarb(part = EquipPart.CardBg)
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
        }

    @Test
    fun `get web interface nav`() =
        runBlocking {
            val response = BiliHttpApi.getWebInterfaceNav()
            println(response)
            assertThat(response.code).isEqualTo(0)
            assertThat(response.data).isNotNull()
            assertThat(response.data!!.isLogin).isTrue()
        }

    @Test
    fun `update wbi`() =
        runBlocking {
            BiliHttpApi.updateWbi()
            assertThat(BiliHttpApi.wbiImgKey).isNotNull()
            assertThat(BiliHttpApi.wbiImgKey).isNotEmpty()
            assertThat(BiliHttpApi.wbiSubKey).isNotNull()
            assertThat(BiliHttpApi.wbiSubKey).isNotEmpty()
        }

    @Test
    fun `fetch buvid3 from spi`() =
        runBlocking {
            val result = BiliHttpApi.fetchBuvid3FromSpi()
            assertThat(result).isNotNull()
            assertThat(result!!.buvid3).isNotEmpty()
        }

    @Test
    fun `download returns non-empty bytes`() =
        runBlocking {
            val data = BiliHttpApi.getVideoInfo(av = 170001).getResponseData()
            val picUrl = data.pic
            val bytes = BiliHttpApi.download(picUrl)
            assertThat(bytes).isNotEmpty()
        }

    @Test
    fun `download as stream returns non-null`() =
        runBlocking {
            val data = BiliHttpApi.getVideoInfo(av = 170001).getResponseData()
            val picUrl = data.pic
            val stream = BiliHttpApi.downloadAsStream(picUrl)
            val firstByte = stream.read()
            assertThat(firstByte).isNotEqualTo(-1)
        }
}
