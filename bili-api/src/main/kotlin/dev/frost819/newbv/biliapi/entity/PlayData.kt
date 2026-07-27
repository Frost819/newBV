package dev.frost819.newbv.biliapi.entity

import bilibili.app.playerunite.v1.PlayViewUniteReply
import bilibili.pgc.gateway.player.v2.dashVideoOrNull
import bilibili.pgc.gateway.player.v2.dolbyOrNull
import bilibili.pgc.gateway.player.v2.segmentVideoOrNull
import bilibili.playershared.dashVideoOrNull
import bilibili.playershared.dolbyOrNull
import bilibili.playershared.lossLessItemOrNull
import bilibili.playershared.segmentVideoOrNull

data class PlayData(
    val dashVideos: List<DashVideo>,
    val dashAudios: List<DashAudio>,
    val dolby: DashAudio? = null,
    val flac: DashAudio? = null,
    val codec: Map<Int, List<String>> = emptyMap(),
    val needPay: Boolean = false,
) {
    companion object {
        fun fromPlayViewUniteReply(playViewUniteReply: PlayViewUniteReply): PlayData {
            val vodInfo = playViewUniteReply.vodInfo

            // 过滤出有 dashVideo 的流
            val dashVideoStreams = vodInfo.streamListList.filter { it.dashVideoOrNull != null }

            // 过滤出有 segmentVideo 的流（试看流）
            val segmentVideoStreams = vodInfo.streamListList.filter { it.segmentVideoOrNull != null }

            val audioList = vodInfo.dashAudioList
            val dolbyItem = vodInfo.dolbyOrNull?.audioList?.firstOrNull()
            val lossLessItem = vodInfo.lossLessItemOrNull?.audio.takeIf { it?.id != 0 }

            // 处理 dashVideo
            val dashVideos =
                dashVideoStreams.map {
                    DashVideo(
                        quality = it.streamInfo.quality,
                        baseUrl = it.dashVideo.baseUrl,
                        bandwidth = it.dashVideo.bandwidth,
                        codecId = it.dashVideo.codecid,
                        width = it.dashVideo.width,
                        height = it.dashVideo.height,
                        frameRate = it.dashVideo.frameRate,
                        backUrl = it.dashVideo.backupUrlList,
                        codecs = CodeType.fromCodecId(it.dashVideo.codecid).str,
                    )
                }.toMutableList()

            val isPreview = dashVideos.isEmpty() && segmentVideoStreams.isNotEmpty()

            // 当 dashVideo 不存在时，使用 segmentVideo（试看流）的 durl 填充
            if (isPreview) {
                segmentVideoStreams.forEach { stream ->
                    // 使用 segmentVideo 中的第一个 durl 作为主流
                    val firstSegment = stream.segmentVideo.segmentList.firstOrNull()
                    if (firstSegment != null) {
                        dashVideos.add(
                            DashVideo(
                                quality = stream.streamInfo.quality,
                                // 使用 durl 的 url
                                baseUrl = firstSegment.url,
                                // segmentVideo 中没有 bandwidth 信息
                                bandwidth = 0,
                                codecId = stream.streamInfo.quality,
                                // segmentVideo 中没有宽高信息
                                width = 0,
                                height = 0,
                                frameRate = "",
                                // 使用 durl 的备用 URL
                                backUrl = firstSegment.backupUrlList,
                                codecs = CodeType.fromCodecId(stream.streamInfo.quality).str,
                            ),
                        )
                    }
                }
            }

            val dashAudios =
                audioList.map {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrlList,
                    )
                }

            val dolby =
                dolbyItem?.let {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrlList,
                    )
                }

            val flac =
                lossLessItem?.let {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrlList,
                    )
                }

            // 生成 codec 映射（优先使用 dashVideo，如果没有则使用 segmentVideo）
            val codecs =
                if (dashVideoStreams.isNotEmpty()) {
                    dashVideoStreams.associate {
                        it.streamInfo.quality to listOf(CodeType.fromCodecId(it.dashVideo.codecid).str)
                    }
                } else {
                    segmentVideoStreams.associate {
                        it.streamInfo.quality to listOf(CodeType.fromCodecId(it.streamInfo.quality).str)
                    }
                }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codecs,
                needPay = isPreview,
            )
        }

        fun fromPgcPlayViewReply(pgcPlayViewReply: bilibili.pgc.gateway.player.v2.PlayViewReply): PlayData {
            val vodInfo = pgcPlayViewReply.videoInfo

            // 过滤出有 dashVideo 的流
            val dashVideoStreams = vodInfo.streamListList.filter { it.dashVideoOrNull != null }

            // 过滤出有 segmentVideo 的流（试看流）
            val segmentVideoStreams = vodInfo.streamListList.filter { it.segmentVideoOrNull != null }

            val audioList = vodInfo.dashAudioList
            val dolbyItem = vodInfo.dolbyOrNull?.audio
            val isPreview = pgcPlayViewReply.business.isPreview

            // 处理 dashVideo
            val dashVideos =
                dashVideoStreams.map {
                    DashVideo(
                        quality = it.info.quality,
                        baseUrl = it.dashVideo.baseUrl,
                        bandwidth = it.dashVideo.bandwidth,
                        codecId = it.dashVideo.codecid,
                        width = it.dashVideo.width,
                        height = it.dashVideo.height,
                        frameRate = it.dashVideo.frameRate,
                        backUrl = it.dashVideo.backupUrlList,
                        codecs = CodeType.fromCodecId(it.dashVideo.codecid).str,
                    )
                }.toMutableList()

            // 当 dashVideo 不存在时，使用 segmentVideo（试看流）的 durl 填充
            if (dashVideos.isEmpty() && segmentVideoStreams.isNotEmpty()) {
                segmentVideoStreams.forEach { stream ->
                    // 使用 segmentVideo 中的第一个分段作为主流
                    val firstSegment = stream.segmentVideo.segmentList.firstOrNull()
                    if (firstSegment != null) {
                        dashVideos.add(
                            DashVideo(
                                quality = stream.info.quality,
                                // 使用 durl 的 url
                                baseUrl = firstSegment.url,
                                // segmentVideo 中没有 bandwidth 信息
                                bandwidth = 0,
                                codecId = stream.info.quality,
                                // segmentVideo 中没有宽高信息
                                width = 0,
                                height = 0,
                                frameRate = "",
                                // 使用 durl 的备用 URL
                                backUrl = firstSegment.backupUrlList,
                                codecs = CodeType.fromCodecId(stream.info.quality).str,
                            ),
                        )
                    }
                }
            }

            val dashAudios =
                audioList.map {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrlList,
                    )
                }

            val dolby =
                dolbyItem?.let {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.codecid,
                        backUrl = it.backupUrlList,
                    )
                }

            // 生成 codec 映射（优先使用 dashVideo，如果没有则使用 segmentVideo）
            val codecs =
                if (dashVideoStreams.isNotEmpty()) {
                    dashVideoStreams.associate {
                        it.info.quality to listOf(CodeType.fromCodecId(it.dashVideo.codecid).str)
                    }
                } else {
                    segmentVideoStreams.associate {
                        it.info.quality to listOf(CodeType.fromCodecId(it.info.quality).str)
                    }
                }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = null,
                codec = codecs,
                needPay = isPreview,
            )
        }

        fun fromPlayUrlData(playUrlData: dev.frost819.newbv.biliapi.http.entity.video.PlayUrlData): PlayData {
            val hasDash = playUrlData.dash != null
            val isPreview = !hasDash && playUrlData.durl.isNotEmpty()

            val audios = playUrlData.dash?.audio
            val dolbyItem = playUrlData.dash?.dolby?.audio?.firstOrNull()
            val flacItem = playUrlData.dash?.flac?.audio
            val codec =
                playUrlData.supportFormats
                    .mapNotNull { it.codecs?.let { c -> it.quality to c } }
                    .toMap()

            val dashVideos =
                if (hasDash) {
                    playUrlData.dash!!.video.map {
                        DashVideo(
                            quality = it.id,
                            baseUrl = it.baseUrl,
                            bandwidth = it.bandwidth,
                            codecId = it.id,
                            width = it.width,
                            height = it.height,
                            frameRate = it.frameRate,
                            backUrl = it.backupUrl,
                            codecs = it.codecs,
                        )
                    }
                } else {
                    // 付费视频未付费状态下没有dash，只有试看流durl，转成DASH结构
                    playUrlData.durl.map {
                        DashVideo(
                            quality = playUrlData.quality,
                            baseUrl = it.url,
                            backUrl = it.backupUrl,
                            codecId = playUrlData.videoCodecId,
                            // durl 模式下没有这些信息，给默认值
                            bandwidth = 0, width = 0, height = 0, frameRate = "", codecs = "",
                        )
                    }
                }
            val dashAudios =
                audios?.map {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrl,
                    )
                } ?: emptyList()
            val dolby =
                dolbyItem?.let {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrl,
                    )
                }
            val flac =
                flacItem?.let {
                    DashAudio(
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        backUrl = it.backupUrl,
                    )
                }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codec,
                needPay = isPreview,
            )
        }

        fun fromPlayUrlV2Data(playUrlV2Data: dev.frost819.newbv.biliapi.http.entity.video.PlayUrlV2Data): PlayData {
            return fromPlayUrlData(playUrlV2Data.videoInfo)
        }
    }

    operator fun plus(other: PlayData): PlayData {
        return PlayData(
            dashVideos =
                (dashVideos + other.dashVideos)
                    .distinctBy { "${it.codecId}_${it.quality}" }
                    .sortedByDescending { it.quality },
            dashAudios =
                (dashAudios + other.dashAudios)
                    .distinctBy { it.codecId }
                    .sortedByDescending { it.codecId },
            dolby = dolby ?: other.dolby,
            flac = flac ?: other.flac,
            codec =
                codec.map {
                    it.key to
                        (it.value + other.codec[it.key].orEmpty())
                            .distinct()
                            .filter { it != "none" }
                }.toMap(),
            needPay = needPay || other.needPay,
        )
    }
}

/**
 * @param quality 视频分辨率
 * @param baseUrl 主线流
 * @param bandwidth 码率
 * @param codecId 编码ID
 * @param width 视频宽度
 * @param height 视频高度
 * @param frameRate 帧率
 * @param backUrl 备用流
 * @param codecs 编码格式 仅 Web 接口有该值
 */
data class DashVideo(
    val quality: Int,
    val baseUrl: String,
    val bandwidth: Int,
    val codecId: Int,
    val width: Int,
    val height: Int,
    val frameRate: String,
    val backUrl: List<String>,
    val codecs: String? = null,
)

/**
 * @param baseUrl 主线流
 * @param bandwidth 码率
 * @param codecId 编码ID
 * @param backUrl 备用流
 */
data class DashAudio(
    val baseUrl: String,
    val bandwidth: Int,
    val codecId: Int,
    val backUrl: List<String>,
)
