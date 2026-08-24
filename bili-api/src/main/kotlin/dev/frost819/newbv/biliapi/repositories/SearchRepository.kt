package dev.frost819.newbv.biliapi.repositories

import bilibili.app.interfaces.v1.suggestionResult3Req
import bilibili.pagination.pagination
import bilibili.polymer.app.search.v1.Item
import bilibili.polymer.app.search.v1.SearchAllResponse
import bilibili.polymer.app.search.v1.SearchByTypeRequest
import bilibili.polymer.app.search.v1.searchAllRequest
import bilibili.polymer.app.search.v1.searchByTypeRequest
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.search.Hotword
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.util.smartDate

class SearchRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val searchSuggestStub
        get() =
            runCatching {
                bilibili.app.interfaces.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    private val searchResultStub
        get() =
            runCatching {
                bilibili.polymer.app.search.v1.SearchGrpcKt.SearchCoroutineStub(
                    channelRepository.requireDefaultChannel(),
                )
            }.getOrNull()

    /*private val searchStub
        get() = runCatching {
            SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20,
        preferApiType: ApiType
    ): SearchData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.search(
                    keyword = keyword,
                    page = page,
                    pageSize = pageSize,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                SearchData.fromSearchResponse(data)
            }

            ApiType.App -> {
                val reply = searchStub?.searchV2(searchV2Req {
                    this.keyword = keyword
                    this.page = page
                    this.pageSize = pageSize
                })
                SearchData.fromSearchResponse(reply!!)
            }
        }
    }*/

    suspend fun getSearchHotwords(
        limit: Int = 30,
        preferApiType: ApiType,
    ): List<Hotword> {
        return when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi.getWebSearchSquare(limit = limit)
                    .getResponseData().trending.list
                    .map { Hotword.fromHttpWebHotword(it) }

            ApiType.App ->
                BiliHttpApi.getSearchTrendRank(limit = limit)
                    .getResponseData().list
                    .map { Hotword.fromHttpAppSearchTrendingHotword(it) }
        }
    }

    /**
     * 全量搜索（返回所有类型结果）。
     *
     * Web 走 HTTP `/x/web-interface/wbi/search/all/v2`；App 走 gRPC `Search.SearchAll`。
     * 主要映射视频、番剧/影视、用户、直播间四类结果，其余卡片类型忽略。
     *
     * @param keyword 搜索关键词
     * @param page 页码（从 1 开始）
     * @param preferApiType 首选接口类型
     * @throws IllegalStateException 当 App 模式返回异常或所需类型卡片缺失时
     */
    suspend fun searchAll(
        keyword: String,
        page: Int = 1,
        preferApiType: ApiType,
    ): SearchAllResult {
        return when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.searchAll(
                        keyword = keyword,
                        page = page,
                    ).getResponseData()
                SearchAllResult.fromWeb(data)
            }

            ApiType.App -> {
                val reply =
                    runCatching {
                        searchResultStub?.searchAll(
                            searchAllRequest {
                                this.keyword = keyword
                                pagination =
                                    pagination {
                                        next = page.toString()
                                    }
                            },
                        ) ?: throw IllegalStateException("App gRPC search stub is not initialized")
                    }.onFailure { handleGrpcException(it) }.getOrThrow()
                SearchAllResult.fromGrpc(reply)
            }
        }
    }

    suspend fun getSearchSuggest(
        keyword: String,
        preferApiType: ApiType,
    ): List<String> {
        return when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi.getKeywordSuggest(
                    term = keyword,
                    buvid = authRepository.buvid ?: "",
                ).suggests.map { it.value }

            // TODO 返回的关键词提示中可能包含通过avid/bvid/专栏id等的直达跳转结果项，需要过滤掉或进行单独处理
            ApiType.App ->
                searchSuggestStub?.suggest3(
                    suggestionResult3Req {
                        this.keyword = keyword
                    },
                )?.listList?.map { it.keyword } ?: emptyList()
        }
    }

    /**
     * 按分类进行搜索
     *
     * app 端的接口无法对视频投稿结果进行筛选搜索
     */
    suspend fun searchType(
        keyword: String,
        type: SearchType,
        tid: Int?,
        order: SearchFilterOrderType,
        duration: SearchFilterDuration,
        page: SearchTypePage,
        preferApiType: ApiType,
    ): SearchTypeResult {
        return when (preferApiType) {
            ApiType.Web -> {
                val response =
                    BiliHttpApi.searchType(
                        keyword = keyword,
                        type = type.httpTypeParam,
                        page = page.nextPageForWeb,
                        tid = tid,
                        order = order.httpOrderParam,
                        duration = duration.httpDurationParam,
                    ).getResponseData()
                SearchTypeResult.fromSearchTypeResult(response)
            }

            ApiType.App -> {
                val searchTypeReply =
                    runCatching {
                        val searchTypeRequest =
                            searchByTypeRequest {
                                this.keyword = keyword
                                this.type = type.grpcTypeParam
                                categorySort = order.grpcOrderParam
                                userType = SearchByTypeRequest.UserType.ALL
                                userSort = SearchByTypeRequest.UserSort.USER_SORT_DEFAULT
                                pagination =
                                    pagination {
                                        next = page.nextPageForApp
                                    }
                            }
                        searchResultStub?.searchByType(searchTypeRequest)
                            ?: throw IllegalStateException("Search result stub is not initialized")
                    }.onFailure { handleGrpcException(it) }.getOrThrow()
                SearchTypeResult.fromSearchTypeResult(searchTypeReply)
            }
        }
    }
}

data class SearchTypePage(
    val nextPageForWeb: Int = 1,
    val nextPageForApp: String = "",
)

enum class SearchType(
    val httpTypeParam: String,
    val grpcTypeParam: Int,
) {
    Video(httpTypeParam = "video", grpcTypeParam = 10),
    MediaBangumi(httpTypeParam = "media_bangumi", grpcTypeParam = 7),
    MediaFt(httpTypeParam = "media_ft", grpcTypeParam = 8),
    BiliUser(httpTypeParam = "bili_user", grpcTypeParam = 2),
    LiveRoom(httpTypeParam = "live_room", grpcTypeParam = 4),
    // Article grpcTypeParam = 6
}

enum class SearchFilterOrderType(
    val httpOrderParam: String?,
    val grpcOrderParam: SearchByTypeRequest.CategorySort,
) {
    ComprehensiveSort(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_DEFAULT,
    ),
    MostClicks(
        httpOrderParam = "click",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_CLICK_COUNT,
    ),
    LatestPublish(
        httpOrderParam = "pubdate",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_PUBLISH_TIME,
    ),
    MostDanmaku(
        httpOrderParam = "dm",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED,
    ),
    MostFavorites(
        httpOrderParam = "stow",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED,
    ),
    MostComment(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_COMMENT_COUNT,
    ),
    MostLikes(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_LIKE_COUNT,
    ),
    ;

    companion object {
        val webFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostDanmaku, MostFavorites)
        val allFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostComment, MostLikes)
    }
}

enum class SearchFilterDuration(
    val httpDurationParam: Int?,
    // val grpcOrderParam: SearchByTypeRequest.
) {
    All(null),
    LessThan10Minutes(1),
    Between10And30Minutes(2),
    Between30And60Minutes(3),
    MoreThan60Minutes(4),
}

data class SearchTypeResult(
    val videos: List<Video> = emptyList(),
    val pgcs: List<Pgc> = emptyList(),
    val users: List<User> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),
    val page: SearchTypePage,
    val hasMore: Boolean = true,
) {
    companion object {
        fun fromSearchTypeResult(
            result: dev.frost819.newbv.biliapi.http.entity.search.SearchResultData,
        ): SearchTypeResult {
            val hasMore = result.page < result.numPages
            return when (result.searchTypeResults.firstOrNull()) {
                is dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult -> {
                    SearchTypeResult(
                        videos =
                            result.searchTypeResults.map {
                                Video.fromSearchVideoResult(
                                    it as dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult,
                                )
                            },
                        page = SearchTypePage(nextPageForWeb = result.page + 1),
                        hasMore = hasMore,
                    )
                }

                is dev.frost819.newbv.biliapi.http.entity.search.SearchMediaResult -> {
                    SearchTypeResult(
                        pgcs =
                            result.searchTypeResults.map {
                                Pgc.fromSearchPgcResult(
                                    it as dev.frost819.newbv.biliapi.http.entity.search.SearchMediaResult,
                                )
                            },
                        page = SearchTypePage(nextPageForWeb = result.page + 1),
                        hasMore = hasMore,
                    )
                }

                is dev.frost819.newbv.biliapi.http.entity.search.SearchBiliUserResult -> {
                    SearchTypeResult(
                        users =
                            result.searchTypeResults.map {
                                User.fromSearchUserResult(
                                    it as dev.frost819.newbv.biliapi.http.entity.search.SearchBiliUserResult,
                                )
                            },
                        page = SearchTypePage(nextPageForWeb = result.page + 1),
                        hasMore = hasMore,
                    )
                }

                is dev.frost819.newbv.biliapi.http.entity.search.SearchLiveRoomResult -> {
                    SearchTypeResult(
                        liveRooms =
                            result.searchTypeResults.map {
                                LiveRoom.fromSearchLiveRoomResult(
                                    it as dev.frost819.newbv.biliapi.http.entity.search.SearchLiveRoomResult,
                                )
                            },
                        page = SearchTypePage(nextPageForWeb = result.page + 1),
                        hasMore = hasMore,
                    )
                }

                else -> {
                    SearchTypeResult(
                        page = SearchTypePage(nextPageForWeb = result.page + 1),
                        hasMore = hasMore,
                    )
                }
            }
        }

        fun fromSearchTypeResult(result: bilibili.polymer.app.search.v1.SearchByTypeResponse): SearchTypeResult {
            return when (result.itemsList.firstOrNull()?.cardItemCase) {
                bilibili.polymer.app.search.v1.Item.CardItemCase.AV -> {
                    SearchTypeResult(
                        videos = result.itemsList.map { Video.fromSearchVideoCard(it) },
                        page = SearchTypePage(nextPageForApp = result.pagination.next),
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.BANGUMI -> {
                    SearchTypeResult(
                        pgcs = result.itemsList.map { Pgc.fromSearchPgcCard(it) },
                        page = SearchTypePage(nextPageForApp = result.pagination.next),
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.AUTHOR -> {
                    SearchTypeResult(
                        users = result.itemsList.map { User.fromSearchUserCard(it) },
                        page = SearchTypePage(nextPageForApp = result.pagination.next),
                    )
                }

                else -> {
                    SearchTypeResult(page = SearchTypePage(nextPageForApp = result.pagination.next))
                }
            }
        }
    }

    interface SearchTypeResultItem

    data class Video(
        val aid: Long,
        val bvid: String,
        val title: String,
        val cover: String,
        val author: String,
        val mid: Long,
        val duration: Int,
        val play: Int,
        val danmaku: Int,
        val pubTime: String? = null,
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchVideoResult(video: dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult) =
                Video(
                    aid = video.aid,
                    bvid = video.bvid,
                    title = video.title,
                    cover = "https:${video.pic}",
                    author = video.author,
                    mid = video.mid,
                    duration = convertStringTimeToSeconds(video.duration),
                    play = video.play ?: 0,
                    danmaku = video.danmaku,
                    pubTime = video.pubDate.smartDate,
                )

            fun fromSearchVideoCard(video: bilibili.polymer.app.search.v1.Item) =
                Video(
                    aid = video.param.toLong(),
                    bvid = video.av.share.video.bvid,
                    title = video.av.title,
                    cover = video.av.cover,
                    author = video.av.author,
                    mid = video.av.mid,
                    duration = convertStringTimeToSeconds(video.av.duration),
                    play = video.av.play,
                    danmaku = video.av.danmaku,
                )
        }
    }

    data class Pgc(
        val title: String,
        val cover: String,
        val star: Float,
        val seasonId: Int,
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchPgcResult(pgc: dev.frost819.newbv.biliapi.http.entity.search.SearchMediaResult) =
                Pgc(
                    title = pgc.title,
                    cover = pgc.cover,
                    star = pgc.mediaScore.score,
                    seasonId = pgc.seasonId,
                )

            fun fromSearchPgcCard(pgc: bilibili.polymer.app.search.v1.Item) =
                Pgc(
                    title = pgc.bangumi.title,
                    cover = pgc.bangumi.cover,
                    star = pgc.bangumi.rating.toFloat(),
                    seasonId = pgc.bangumi.seasonId.toInt(),
                )
        }
    }

    data class User(
        val mid: Long,
        val name: String,
        val avatar: String,
        val sign: String,
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchUserResult(user: dev.frost819.newbv.biliapi.http.entity.search.SearchBiliUserResult) =
                User(
                    mid = user.mid,
                    name = user.uname,
                    avatar = "https:${user.upic}",
                    sign = user.usign,
                )

            fun fromSearchUserCard(user: bilibili.polymer.app.search.v1.Item) =
                User(
                    mid = user.param.toLong(),
                    name = user.author.title,
                    avatar = user.author.cover,
                    sign = user.author.sign,
                )
        }
    }

    data class LiveRoom(
        val roomId: Long,
        val title: String,
        val uname: String,
        val uid: Long,
        val cover: String,
        val userCover: String,
        val face: String,
        val areaName: String,
        val online: Int,
        val liveStatus: Int,
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchLiveRoomResult(room: dev.frost819.newbv.biliapi.http.entity.search.SearchLiveRoomResult) =
                LiveRoom(
                    roomId = room.roomid,
                    title = room.title,
                    uname = room.uname,
                    uid = room.uid,
                    cover = room.cover.toHttpsUrl(),
                    userCover = room.userCover.toHttpsUrl(),
                    face = room.uface.toHttpsUrl(),
                    areaName = room.cateName,
                    online = room.online,
                    liveStatus = room.liveStatus,
                )
        }
    }
}

private fun String.toHttpsUrl(): String = if (startsWith("//")) "https:$this" else this

private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}

/**
 * 全量搜索（`Search.SearchAll` / HTTP `/search/all/v2`）的结果。
 *
 * 聚合视频、番剧/影视、用户、直播间四类主要结果，忽略其余卡片类型。
 *
 * @property keyword 搜索关键词
 * @property videos 视频结果
 * @property pgcs 番剧/影视结果
 * @property users 用户结果
 * @property liveRooms 直播间结果
 * @property page 当前页码
 * @property pages 总页数
 * @property hasMore 是否还有更多
 */
data class SearchAllResult(
    val keyword: String = "",
    val videos: List<SearchTypeResult.Video> = emptyList(),
    val pgcs: List<SearchTypeResult.Pgc> = emptyList(),
    val users: List<SearchTypeResult.User> = emptyList(),
    val liveRooms: List<SearchTypeResult.LiveRoom> = emptyList(),
    val page: Int = 1,
    val pages: Int = 0,
    val hasMore: Boolean = false,
) {
    companion object {
        /** 从 Web HTTP `/search/all/v2` 响应转换。 */
        fun fromWeb(data: dev.frost819.newbv.biliapi.http.entity.search.SearchResultData): SearchAllResult {
            val videos = mutableListOf<SearchTypeResult.Video>()
            val pgcs = mutableListOf<SearchTypeResult.Pgc>()
            data.searchTypeResults.forEach {
                when (it) {
                    is dev.frost819.newbv.biliapi.http.entity.search.SearchVideoResult ->
                        videos.add(SearchTypeResult.Video.fromSearchVideoResult(it))
                    is dev.frost819.newbv.biliapi.http.entity.search.SearchMediaResult ->
                        pgcs.add(SearchTypeResult.Pgc.fromSearchPgcResult(it))
                    else -> Unit
                }
            }
            return SearchAllResult(
                keyword = data.suggestKeyword,
                videos = videos,
                pgcs = pgcs,
                page = data.page,
                pages = data.numPages,
                hasMore = data.page < data.numPages,
            )
        }

        /** 从 App gRPC `Search.SearchAll` 响应转换。 */
        fun fromGrpc(reply: SearchAllResponse): SearchAllResult {
            val videos = mutableListOf<SearchTypeResult.Video>()
            val pgcs = mutableListOf<SearchTypeResult.Pgc>()
            val users = mutableListOf<SearchTypeResult.User>()
            val liveRooms = mutableListOf<SearchTypeResult.LiveRoom>()
            reply.itemList.forEach { item ->
                when (item.cardItemCase) {
                    Item.CardItemCase.AV ->
                        videos.add(SearchTypeResult.Video.fromSearchVideoCard(item))
                    Item.CardItemCase.BANGUMI ->
                        pgcs.add(SearchTypeResult.Pgc.fromSearchPgcCard(item))
                    Item.CardItemCase.AUTHOR ->
                        users.add(SearchTypeResult.User.fromSearchUserCard(item))
                    else -> Unit
                }
            }
            val totalPages = reply.pagination.next.toIntOrNull() ?: 0
            return SearchAllResult(
                keyword = reply.keyword,
                videos = videos,
                pgcs = pgcs,
                users = users,
                liveRooms = liveRooms,
                page = totalPages.coerceAtLeast(1),
                hasMore = totalPages > 0,
            )
        }
    }
}
