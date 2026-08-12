package dev.frost819.newbv.biliapi.repositories

import bilibili.app.view.v1.ViewGrpcKt
import bilibili.app.view.v1.viewReq
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.entity.video.season.SeasonDetail
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository,
) {
    private val viewStub
        get() =
            runCatching {
                ViewGrpcKt.ViewCoroutineStub(channelRepository.defaultChannel!!)
            }.getOrNull()

    suspend fun getVideoDetail(
        aid: Long,
        preferApiType: ApiType = ApiType.Web,
        bvid: String = "",
    ): VideoDetail {
        return when (preferApiType) {
            ApiType.Web -> {
                withContext(Dispatchers.IO) {
                    val videoDetailWithoutUserActions =
                        async {
                            val response =
                                BiliHttpApi.getVideoDetail(
                                    av = aid,
                                    bv = bvid.ifEmpty { null },
                                )
                            val httpVideoDetail = response.getResponseData()
                            VideoDetail.fromVideoDetail(httpVideoDetail)
                        }

                    // check liked, favoured, coined status...
                    val isFavoured =
                        async {
                            runCatching {
                                favoriteRepository.checkVideoFavoured(
                                    aid = aid,
                                    preferApiType = ApiType.Web,
                                )
                            }.onFailure {
                            }.getOrDefault(false)
                        }

                    val isLiked =
                        async {
                            runCatching {
                                likeRepository.checkVideoLiked(
                                    aid = aid,
                                )
                            }.onFailure {
                            }.getOrDefault(false)
                        }

                    val isCoined =
                        async {
                            runCatching {
                                coinRepository.checkVideoCoined(
                                    aid = aid,
                                )
                            }.onFailure {
                            }.getOrDefault(false)
                        }

                    val historyAndPlayerIcon =
                        async {
                            runCatching {
                                val videoModeInfo =
                                    BiliHttpApi.getVideoMoreInfo(
                                        avid = aid,
                                        cid = videoDetailWithoutUserActions.await().cid,
                                    ).getResponseData()
                                val history =
                                    VideoDetail.History(
                                        progress = videoModeInfo.lastPlayTime / 1000,
                                        lastPlayedCid = videoModeInfo.lastPlayCid,
                                    )
                                history
                            }.onFailure {
                            }.getOrDefault(VideoDetail.History(0, 0))
                        }

                    videoDetailWithoutUserActions.await().let { detail ->
                        val newUserActions =
                            detail.userActions.copy(
                                favorite = isFavoured.await(),
                                like = isLiked.await(),
                                coin = isCoined.await(),
                            )
                        val newHistory = historyAndPlayerIcon.await()
                        detail.copy(
                            userActions = newUserActions,
                            history = newHistory,
                        )
                    }
                }
            }

            ApiType.App -> {
                val viewReply =
                    runCatching {
                        viewStub?.view(
                            viewReq {
                                this.aid = aid.toLong()
                            },
                        ) ?: throw IllegalStateException("Player stub is not initialized")
                    }.onFailure { handleGrpcException(it) }.getOrThrow()
                VideoDetail.fromViewReply(viewReply)
            }
        }
    }

    suspend fun getUgcPages(
        aid: Long,
        preferApiType: ApiType = ApiType.Web,
    ): List<VideoPage> {
        return try {
            when (preferApiType) {
                ApiType.Web -> {
                    val detail =
                        BiliHttpApi.getVideoInfo(
                            av = aid,
                        ).getResponseData()
                    detail.pages.map { VideoPage.fromVideoPage(it) }
                }

                ApiType.App -> {
                    val viewReply =
                        runCatching {
                            viewStub?.view(
                                viewReq {
                                    this.aid = aid
                                },
                            ) ?: throw IllegalStateException("Player stub is not initialized")
                        }.onFailure { handleGrpcException(it) }
                            .getOrThrow()

                    viewReply.pagesList.map { VideoPage.fromViewPage(it) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emptyList()
        }
    }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType = ApiType.Web,
    ): SeasonDetail {
        when (preferApiType) {
            ApiType.Web -> {
                val webSeasonData =
                    BiliHttpApi.getWebSeasonInfo(
                        epId = epid,
                        seasonId = seasonId,
                    ).getResponseData()
                val seasonDetail = SeasonDetail.fromSeasonData(webSeasonData)
                val firstEp = webSeasonData.episodes.firstOrNull() ?: return seasonDetail

                val playerIcon =
                    runCatching {
                        val videoModeInfo =
                            BiliHttpApi.getVideoMoreInfo(
                                avid = firstEp.aid,
                                cid = firstEp.cid,
                            ).getResponseData()
                        val playerIcon = VideoDetail.PlayerIcon.fromPlayerIcon(videoModeInfo.playerIcon)
                        playerIcon
                    }.onFailure {
                    }.getOrDefault(null)
                seasonDetail.playerIcon = playerIcon
                return seasonDetail
            }

            ApiType.App -> {
                val appSeasonData =
                    BiliHttpApi.getAppSeasonInfo(
                        epId = epid,
                        seasonId = seasonId,
                        mobiApp = "android_hd",
                        accessKey = authRepository.accessToken ?: "",
                    ).getResponseData()
                return SeasonDetail.fromSeasonData(appSeasonData)
            }
        }
    }
}
