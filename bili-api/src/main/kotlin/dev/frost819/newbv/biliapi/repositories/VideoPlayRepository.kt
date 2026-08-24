package dev.frost819.newbv.biliapi.repositories

import bilibili.app.playerunite.v1.PlayerGrpcKt
import bilibili.app.playerunite.v1.playViewUniteReq
import bilibili.community.service.dm.v1.DMGrpcKt
import bilibili.community.service.dm.v1.dmViewReq
import bilibili.pgc.gateway.player.v2.playViewReq
import bilibili.playershared.videoVod
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.CodeType
import dev.frost819.newbv.biliapi.entity.PlayData
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskType
import dev.frost819.newbv.biliapi.entity.video.HeartbeatVideoType
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
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
        preferApiType: ApiType = ApiType.Web,
    ): PlayData {
        return when (preferApiType) {
            ApiType.Web -> {
                val playUrlData =
                    BiliHttpApi.getVideoPlayUrl(
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
                        codecTypes.map { codecType ->
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
                        replies.map {
                            it?.let { PlayData.fromPlayViewUniteReply(it) }
                        }.reduce { acc, playData ->
                            acc?.let { playData?.let { acc + playData } ?: acc } ?: playData
                        } ?: throw IllegalStateException("All codec types are failed to get play data")
                    result
                }
            }
        }
    }

    suspend fun getPgcPlayData(
        aid: Long?,
        cid: Long?,
        epid: Int,
        preferCodec: CodeType = CodeType.NoCode,
        preferApiType: ApiType = ApiType.Web,
    ): PlayData {
        println(
            "get pgc play data: " +
                "[aid=$aid, cid=$cid, epid=$epid, preferCodec=$preferCodec, preferApiType=$preferApiType]",
        )
        return when (preferApiType) {
            ApiType.Web -> {
                val playUrlData =
                    BiliHttpApi.getPgcVideoPlayUrlV2(
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
                        codecTypes.map { codecType ->
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
                        replies.map {
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
        preferApiType: ApiType = ApiType.Web,
    ): List<Subtitle> {
        return when (preferApiType) {
            ApiType.Web -> {
                val response =
                    BiliHttpApi.getVideoMoreInfo(
                        avid = aid,
                        cid = cid,
                    ).getResponseData()
                response.subtitle?.subtitles
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
                dmViewReply?.subtitle?.subtitlesList
                    ?.map { Subtitle.fromSubtitleItem(it) }
                    ?: emptyList()
            }
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
        preferApiType: ApiType = ApiType.Web,
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

    suspend fun getDanmakuMask(
        aid: Long,
        cid: Long,
        preferApiType: ApiType = ApiType.Web,
    ): DanmakuMask? {
        val danmakuMaskUrl =
            when (preferApiType) {
                ApiType.Web -> {
                    val response =
                        BiliHttpApi.getVideoMoreInfo(
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
        preferApiType: ApiType = ApiType.Web,
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
