package dev.frost819.newbv.biliapi.repositories

import bilibili.app.view.v1.ViewGrpcKt
import bilibili.app.view.v1.viewReq
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.biliapi.entity.video.season.SeasonDetail
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.util.BiliAppConf
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
                ViewGrpcKt.ViewCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    suspend fun getVideoDetail(
        aid: Long,
        preferApiType: ApiType,
        bvid: String = "",
    ): VideoDetail =
        when (preferApiType) {
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
                                    preferApiType = ApiType.Web,
                                )
                            }.onFailure {
                            }.getOrDefault(false)
                        }

                    val isCoined =
                        async {
                            runCatching {
                                coinRepository.checkVideoCoined(
                                    aid = aid,
                                    preferApiType = ApiType.Web,
                                )
                            }.onFailure {
                            }.getOrDefault(false)
                        }

                    val historyAndPlayerIcon =
                        async {
                            runCatching {
                                val videoModeInfo =
                                    BiliHttpApi
                                        .getVideoMoreInfo(
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

    suspend fun getUgcPages(
        aid: Long,
        preferApiType: ApiType,
    ): List<VideoPage> =
        try {
            when (preferApiType) {
                ApiType.Web -> {
                    val detail =
                        BiliHttpApi
                            .getVideoInfo(
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

    /**
     * 获取视频同时观看人数的可展示文案。
     *
     * - Web：GET /x/player/online/total（无需 WBI 签名、无需登录），
     *   按 UP 主 show_switch 开关选择 total/count，关闭时返回 null
     * - App：GET https://app.bilibili.com/x/v2/view/video/online（appkey+sign 自动签名），
     *   返回服务端预格式化的 total_text（如 "8.8万+人在看"），归一化时剥离"人在看"后缀
     *
     * @param aid 视频 AV 号
     * @param cid 分 P CID
     * @param preferApiType 接口类型
     * @param bvid 视频 BV 号（可选，仅 Web 路径使用）
     * @return 可展示的纯人数文案（如 "9.4万+"，不含后缀）；UP 主关闭展示或文案为空时返回 null
     * @throws IllegalStateException 接口返回非 0 错误码或数据缺失
     */
    suspend fun getOnlineTotalText(
        aid: Long,
        cid: Long,
        preferApiType: ApiType,
        bvid: String = "",
    ): String? =
        when (preferApiType) {
            ApiType.Web ->
                withContext(Dispatchers.IO) {
                    BiliHttpApi
                        .getVideoOnlineTotal(
                            avid = aid,
                            bvid = bvid.ifEmpty { null },
                            cid = cid,
                        ).getResponseData()
                        .displayText()
                }

            ApiType.App ->
                withContext(Dispatchers.IO) {
                    // App 端返回带"人在看"后缀的完整文案（如 "1000+人在看"），
                    // 剥离后缀与 Web 端契约对齐，由 UI 层统一拼接"人正在看"
                    BiliHttpApi
                        .getAppVideoOnlineTotal(
                            aid = aid,
                            cid = cid,
                        ).getResponseData()
                        .online.totalText
                        .takeIf { it.isNotBlank() }
                        ?.removeSuffix("人在看")
                        ?.takeIf { it.isNotBlank() }
                }
        }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType,
    ): SeasonDetail {
        return when (preferApiType) {
            ApiType.Web -> {
                val webSeasonData =
                    BiliHttpApi
                        .getWebSeasonInfo(
                            epId = epid,
                            seasonId = seasonId,
                        ).getResponseData()
                val seasonDetail = SeasonDetail.fromSeasonData(webSeasonData)
                val firstEp = webSeasonData.episodes.firstOrNull() ?: return seasonDetail

                val playerIcon =
                    runCatching {
                        val videoModeInfo =
                            BiliHttpApi
                                .getVideoMoreInfo(
                                    avid = firstEp.aid,
                                    cid = firstEp.cid,
                                ).getResponseData()
                        val playerIcon = VideoDetail.PlayerIcon.fromPlayerIcon(videoModeInfo.playerIcon)
                        playerIcon
                    }.onFailure {
                    }.getOrDefault(null)
                seasonDetail.playerIcon = playerIcon
                seasonDetail
            }

            ApiType.App -> {
                val appSeasonData =
                    BiliHttpApi
                        .getAppSeasonInfo(
                            seasonId = seasonId,
                            epId = epid,
                            mobiApp = BiliAppConf.MOBI_APP,
                            accessKey = authRepository.accessToken ?: "",
                        ).getResponseData()
                SeasonDetail.fromSeasonData(appSeasonData)
            }
        }
    }
}
