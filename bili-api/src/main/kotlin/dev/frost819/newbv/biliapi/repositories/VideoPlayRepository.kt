package dev.frost819.newbv.biliapi.repositories

import bilibili.app.playerunite.v1.PlayerGrpcKt
import bilibili.app.playerunite.v1.playViewUniteReq
import bilibili.community.service.dm.v1.DMGrpcKt
import bilibili.community.service.dm.v1.dmSegMobileReq
import bilibili.community.service.dm.v1.dmViewReq
import bilibili.pgc.gateway.player.v2.playViewReq
import bilibili.playershared.videoVod
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.CodeType
import dev.frost819.newbv.biliapi.entity.PlayData
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskType
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMeta
import dev.frost819.newbv.biliapi.entity.danmaku.toDanmakuMeta
import dev.frost819.newbv.biliapi.entity.video.HeartbeatVideoType
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import bilibili.pgc.gateway.player.v2.PlayURLGrpcKt as PgcPlayURLGrpcKt

class VideoPlayRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val playerStub
        get() =
            runCatching {
                PlayerGrpcKt.PlayerCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()
    private val pgcPlayUrlStub
        get() =
            runCatching {
                PgcPlayURLGrpcKt.PlayURLCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()
    private val danmakuStub
        get() =
            runCatching {
                DMGrpcKt.DMCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    suspend fun getPlayData(
        aid: Long,
        cid: Long,
        preferApiType: ApiType,
    ): PlayData =
        when (preferApiType) {
            ApiType.Web -> {
                val playUrlData =
                    BiliHttpApi
                        .getVideoPlayUrl(
                            av = aid,
                            cid = cid,
                            fnval = 4048,
                            qn = 127,
                            fnver = 0,
                            fourk = 1,
                        ).getResponseData()
                PlayData.fromPlayUrlData(playUrlData)
            }

            ApiType.App -> {
                withContext(Dispatchers.IO) {
                    val codecTypes =
                        listOf(
                            CodeType.Code264,
                            CodeType.Code265,
                            CodeType.CodeAv1,
                        )
                    val replies =
                        codecTypes
                            .map { codecType ->
                                async {
                                    val playUniteReply =
                                        runCatching {
                                            playerStub?.playViewUnite(
                                                playViewUniteReq {
                                                    vod =
                                                        videoVod {
                                                            this.aid = aid
                                                            this.cid = cid
                                                            fnval = 4048
                                                            qn = 127
                                                            fnver = 0
                                                            fourk = true
                                                            forceHost = 2
                                                            preferCodecType = codecType.toPlayerSharedCodeType()
                                                        }
                                                },
                                            ) ?: throw IllegalStateException("Player stub is not initialized")
                                        }.onFailure {
                                            // dont throw
                                            runCatching { handleGrpcException(it) }
                                                .onFailure {
                                                    println(
                                                        "get play data failed: " +
                                                            "[aid=$aid, cid=$cid, codec=$codecType, api=$preferApiType]",
                                                    )
                                                    it.printStackTrace()
                                                }
                                        }.getOrNull()
                                    playUniteReply
                                }
                            }.awaitAll()
                    val result =
                        replies
                            .map {
                                it?.let { PlayData.fromPlayViewUniteReply(it) }
                            }.reduce { acc, playData ->
                                acc?.let { playData?.let { acc + playData } ?: acc } ?: playData
                            } ?: throw IllegalStateException("All codec types are failed to get play data")
                    result
                }
            }
        }

    suspend fun getPgcPlayData(
        aid: Long?,
        cid: Long?,
        epid: Int,
        preferCodec: CodeType = CodeType.NoCode,
        preferApiType: ApiType,
    ): PlayData {
        println(
            "get pgc play data: " +
                "[aid=$aid, cid=$cid, epid=$epid, preferCodec=$preferCodec, preferApiType=$preferApiType]",
        )
        return when (preferApiType) {
            ApiType.Web -> {
                val playUrlData =
                    BiliHttpApi
                        .getPgcVideoPlayUrlV2(
                            av = aid,
                            cid = cid,
                            epid = epid,
                            fnval = 4048,
                            qn = 127,
                            fnver = 0,
                            fourk = 1,
                        ).getResponseData()

                PlayData.fromPlayUrlV2Data(playUrlData)
            }

            ApiType.App -> {
                withContext(Dispatchers.IO) {
                    val codecTypes =
                        listOf(
                            CodeType.Code264,
                            CodeType.Code265,
                            CodeType.CodeAv1,
                        )
                    val replies =
                        codecTypes
                            .map { codecType ->
                                val req =
                                    playViewReq {
                                        this.epid = epid.toLong()
                                        cid?.let { this.cid = it }
                                        qn = 127
                                        fnver = 0
                                        fnval = 4048
                                        fourk = true
                                        forceHost = 0
                                        download = 0
                                        preferCodecType = codecType.toPgcPlayUrlCodeType()
                                    }
                                async {
                                    val playReply =
                                        runCatching {
                                            pgcPlayUrlStub?.playView(req)
                                                ?: throw IllegalStateException("Pgc play url stub is not initialized")
                                        }.onFailure {
                                            // dont throw
                                            runCatching { handleGrpcException(it) }
                                                .onFailure {
                                                    println(
                                                        "get pgc play data failed: " +
                                                            "[aid=$aid, cid=$cid, epid=$epid, codec=$codecType]",
                                                    )
                                                    it.printStackTrace()
                                                }
                                        }.getOrNull()
                                    playReply
                                }
                            }.awaitAll()
                    val result =
                        replies
                            .map {
                                it?.let { PlayData.fromPgcPlayViewReply(it) }
                            }.reduce { acc, playData ->
                                acc?.let { playData?.let { acc + playData } ?: acc } ?: playData
                            } ?: throw IllegalStateException("All codec types are failed to get play data")
                    result
                }
            }
        }
    }

    suspend fun getSubtitle(
        aid: Long,
        cid: Long,
        preferApiType: ApiType,
    ): List<Subtitle> =
        when (preferApiType) {
            ApiType.Web -> {
                val response =
                    BiliHttpApi
                        .getVideoMoreInfo(
                            avid = aid,
                            cid = cid,
                        ).getResponseData()
                response.subtitle
                    ?.subtitles
                    ?.map { Subtitle.fromSubtitleItem(it) }
                    ?: emptyList()
            }

            ApiType.App -> {
                val dmViewReply =
                    runCatching {
                        danmakuStub?.dmView(
                            dmViewReq {
                                pid = aid
                                oid = cid
                                type = 1
                            },
                        )
                    }.onFailure { handleGrpcException(it) }.getOrThrow()
                dmViewReply
                    ?.subtitle
                    ?.subtitlesList
                    ?.map { Subtitle.fromSubtitleItem(it) }
                    ?: emptyList()
            }
        }

    suspend fun sendHeartbeat(
        aid: Long,
        cid: Long,
        time: Int,
        type: HeartbeatVideoType = HeartbeatVideoType.Video,
        subType: Int? = null,
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType,
    ) {
        val result =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.sendHeartbeat(
                        avid = aid,
                        cid = cid,
                        playedTime = time,
                        type = type.value,
                        subType = subType,
                        epid = epid,
                        sid = seasonId,
                        csrf = authRepository.biliJct,
                    )

                ApiType.App ->
                    BiliHttpApi.sendHeartbeatApp(
                        avid = aid,
                        cid = cid,
                        playedTime = time,
                        type = type.value,
                        subType = subType,
                        epid = epid,
                        sid = seasonId,
                        mid = authRepository.mid,
                        accessKey = authRepository.accessToken,
                    )
            }
        println("send heartbeat result: $result")
    }

    /**
     * 获取弹幕元数据（分段配置）。
     *
     * 固定走 Web dm/view 通道：App gRPC 的 DmViewReply 不含 dmSge 字段，
     * 且该接口未登录可用，单一数据源最简（参考 blbl 项目做法）。
     *
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     * @return 弹幕元数据（分段大小、分段总数、弹幕是否关闭）
     * @throws Exception 网络失败或响应解析失败时抛出，由调用方决定兜底策略
     */
    suspend fun getDanmakuMeta(
        aid: Long,
        cid: Long,
    ): DanmakuMeta = BiliHttpApi.getDanmakuView(cid = cid, avid = aid).toDanmakuMeta()

    /**
     * 获取指定 6 分钟分段的弹幕。
     *
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     * @param segmentIndex 从 1 开始的分段索引
     * @param preferApiType 优先使用的接口类型
     * @return 该分段的弹幕列表
     */
    suspend fun getDanmakuSegment(
        aid: Long,
        cid: Long,
        segmentIndex: Int,
        preferApiType: ApiType,
    ): List<DanmakuData> =
        when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi.getDanmakuSeg(
                    cid = cid,
                    avid = aid,
                    segmentIndex = segmentIndex,
                )

            ApiType.App ->
                withContext(Dispatchers.IO) {
                    val reply =
                        runCatching {
                            danmakuStub?.dmSegMobile(
                                dmSegMobileReq {
                                    pid = aid
                                    oid = cid
                                    type = 1
                                    this.segmentIndex = segmentIndex.toLong()
                                },
                            ) ?: throw IllegalStateException("Danmaku stub is not initialized")
                        }.onFailure { handleGrpcException(it) }.getOrThrow()
                    reply.elemsList.map { DanmakuData.fromDanmakuElem(it) }
                }
        }

    suspend fun getDanmakuMask(
        aid: Long,
        cid: Long,
        preferApiType: ApiType,
    ): DanmakuMask? {
        val danmakuMaskUrl =
            when (preferApiType) {
                ApiType.Web -> {
                    val response =
                        BiliHttpApi
                            .getVideoMoreInfo(
                                avid = aid,
                                cid = cid,
                            ).getResponseData()
                    response.dmMask?.maskUrl
                }

                ApiType.App -> {
                    val dmViewReply =
                        runCatching {
                            danmakuStub?.dmView(
                                dmViewReq {
                                    pid = aid
                                    oid = cid
                                    type = 1
                                },
                            )
                        }.onFailure { handleGrpcException(it) }.getOrThrow()
                    dmViewReply?.mask?.maskUrl
                }
            } ?: return null

        val maskUrl =
            when (preferApiType) {
                ApiType.Web -> danmakuMaskUrl.replace("mobmask", "webmask")
                ApiType.App -> danmakuMaskUrl.replace("webmask", "mobmask")
            }
        val danmakuMaskType =
            when (preferApiType) {
                ApiType.Web -> DanmakuMaskType.WebMask
                ApiType.App -> DanmakuMaskType.MobMask
            }
        // 直接拿流，不缓冲到 ByteArray
        val maskStream = BiliHttpApi.downloadAsStream(maskUrl)
        return DanmakuMask.fromStream(maskStream, danmakuMaskType)
    }

    suspend fun getVideoShot(
        aid: Long,
        cid: Long,
        preferApiType: ApiType,
    ): VideoShot? {
        val videoShortResponse =
            when (preferApiType) {
                ApiType.Web -> BiliHttpApi.getWebVideoShot(aid = aid, cid = cid)
                ApiType.App -> BiliHttpApi.getAppVideoShot(aid = aid, cid = cid)
            }
        val videoShot = VideoShot.fromVideoShot(videoShortResponse.getResponseData())
        return videoShot
    }
}
