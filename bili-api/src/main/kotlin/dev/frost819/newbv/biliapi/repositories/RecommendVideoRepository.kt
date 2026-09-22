package dev.frost819.newbv.biliapi.repositories

import bilibili.app.show.v1.PopularGrpcKt
import bilibili.app.show.v1.popularResultReq
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.home.RecommendData
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoData
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class RecommendVideoRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val popularStub
        get() =
            runCatching {
                PopularGrpcKt.PopularCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    suspend fun getPopularVideos(
        page: PopularVideoPage,
        preferApiType: ApiType,
    ): PopularVideoData =
        when (preferApiType) {
            ApiType.Web -> {
                val response =
                    BiliHttpApi
                        .getPopularVideoData(
                            pageSize = page.nextWebPageSize,
                            pageNumber = page.nextWebPageNumber,
                        ).getResponseData()
                val list = response.list.map { UgcItem.fromVideoInfo(it) }
                val nextPage =
                    PopularVideoPage(
                        nextWebPageSize = page.nextWebPageSize,
                        nextWebPageNumber = page.nextWebPageNumber + 1,
                    )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = response.noMore,
                )
            }

            ApiType.App -> {
                // 不主动回退 Web：App 通道未就绪时抛明确异常，
                // 由上层展示错误并允许重试，避免静默返回空列表造成"加载不完整"假象
                val stub =
                    popularStub
                        ?: throw IllegalStateException("App gRPC channel is not initialized")
                val reply =
                    stub.index(
                        popularResultReq {
                            idx = page.nextAppIndex.toLong()
                        },
                    )
                val list =
                    reply
                        .itemsList
                        ?.filter { it.itemCase == bilibili.app.card.v1.Card.ItemCase.SMALL_COVER_V5 }
                        ?.map { UgcItem.fromSmallCoverV5(it.smallCoverV5) }
                        ?: emptyList()
                val nextPage =
                    PopularVideoPage(
                        nextAppIndex = list.lastOrNull()?.idx ?: -1,
                    )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = nextPage.nextAppIndex == -1,
                )
            }
        }

    suspend fun getRecommendVideos(
        page: RecommendPage = RecommendPage(),
        preferApiType: ApiType,
    ): RecommendData {
        // App 接口的下一页游标必须基于原始列表（含被过滤掉的卡片）推进，
        // 否则整页卡片被过滤时会返回与上次相同的游标，导致重复请求同一页
        var appNextIdx: Int? = null
        val items =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi
                        .getFeedRcmd(
                            idx = page.nextWebIdx,
                        ).getResponseData()
                        .item
                        .map { UgcItem.fromRcmdItem(it) }

                // 推荐流没有对应 RPC，与原版一致走 App HTTP feed/index；
                // 不能复用 Popular.Index，否则推荐和热门内容相同
                ApiType.App -> {
                    val rawItems =
                        BiliHttpApi
                            .getFeedIndex(
                                idx = page.nextAppIdx,
                                accessKey = authRepository.accessToken,
                            ).getResponseData()
                            .items
                    appNextIdx = rawItems.lastOrNull()?.idx?.plus(1)
                    rawItems
                        .filter { it.cardGoto == "av" }
                        .map { UgcItem.fromRcmdItem(it) }
                }
            }
        val nextPage =
            when (preferApiType) {
                ApiType.Web ->
                    RecommendPage(
                        nextWebIdx = page.nextWebIdx + 1,
                    )

                ApiType.App ->
                    RecommendPage(
                        nextAppIdx = appNextIdx ?: page.nextAppIdx,
                    )
            }
        return RecommendData(
            items = items,
            nextPage = nextPage,
        )
    }
}
