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
    ): PopularVideoData {
        return when (preferApiType) {
            ApiType.Web -> {
                val response =
                    BiliHttpApi.getPopularVideoData(
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
                val reply =
                    popularStub?.index(
                        popularResultReq {
                            idx = page.nextAppIndex.toLong()
                        },
                    )
                val list =
                    reply?.itemsList
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
    }

    suspend fun getRecommendVideos(
        page: RecommendPage = RecommendPage(),
        preferApiType: ApiType,
    ): RecommendData {
        val items =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.getFeedRcmd(
                        idx = page.nextWebIdx,
                    )
                        .getResponseData().item
                        .map { UgcItem.fromRcmdItem(it) }

                // 推荐流没有对应 RPC，与原版一致走 App HTTP feed/index；
                // 不能复用 Popular.Index，否则推荐和热门内容相同
                ApiType.App ->
                    BiliHttpApi.getFeedIndex(
                        idx = page.nextAppIdx,
                        accessKey = authRepository.accessToken,
                    )
                        .getResponseData().items
                        .filter { it.cardGoto == "av" }
                        .map { UgcItem.fromRcmdItem(it) }
            }
        val nextPage =
            when (preferApiType) {
                ApiType.Web ->
                    RecommendPage(
                        nextWebIdx = page.nextWebIdx + 1,
                    )

                ApiType.App ->
                    RecommendPage(
                        nextAppIdx = items.lastOrNull()?.idx?.plus(1) ?: page.nextAppIdx,
                    )
            }
        return RecommendData(
            items = items,
            nextPage = nextPage,
        )
    }
}
