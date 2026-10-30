package dev.frost819.newbv.biliapi.http.entity.danmaku

import bilibili.community.service.dm.v1.DanmakuElem
import bilibili.community.service.dm.v1.DmSegMobileReply

/**
 * 将 seg.so/gRPC 的 [DmSegMobileReply] 转为弹幕列表。
 *
 * 同时解析 `ai_flag`（dmid → AI 云屏蔽评分）；Web 通道实测恒不下发，评分均为 0。
 * Web（[dev.frost819.newbv.biliapi.http.BiliHttpApi]）与 App gRPC
 * （[dev.frost819.newbv.biliapi.repositories.VideoPlayRepository]）两条通道共用。
 */
internal fun DmSegMobileReply.toDanmakuDataList(): List<DanmakuData> {
    val aiFlagScores = aiFlag.dmFlagsList.associate { it.dmid to it.flag }
    return elemsList.map { DanmakuData.fromDanmakuElem(it, aiFlagScores[it.id] ?: 0) }
}

data class DanmakuResponse(
    val chatserver: String,
    val chatId: Long,
    val maxLimit: Int,
    val state: Int,
    val realName: Int,
    val source: String,
    val data: List<DanmakuData> = emptyList(),
)

data class DanmakuData(
    val time: Float,
    val type: Int,
    val size: Int,
    val color: Int,
    val timestamp: Int,
    val pool: Int,
    val midHash: String,
    val dmid: Long,
    /** 弹幕权重，名义区间 [1, 10]（真机实测存在 11），是 AI 云屏蔽的实际过滤依据 */
    val level: Int,
    val text: String,
    /** AI 云屏蔽评分（来自 seg 响应的 ai_flag）；Web 通道实测恒为 0，0 表示未下发评分 */
    val aiFlagScore: Int = 0,
) {
    companion object {
        fun fromString(
            p: String,
            text: String,
        ): DanmakuData {
            val data = p.split(",")
            return DanmakuData(
                time = data[0].toFloat(),
                type = data[1].toInt(),
                size = data[2].toInt(),
                color = data[3].toInt(),
                timestamp = data[4].toInt(),
                pool = data[5].toInt(),
                midHash = data[6],
                dmid = data[7].toLong(),
                level = data[8].toInt(),
                text = text,
            )
        }

        /**
         * 从 gRPC [DanmakuElem]（protobuf）转换弹幕数据。
         *
         * Web `seg.so` 与 App `DM.DmSegMobile` 双通道共用此映射。
         * 对应文档：docs/bilibili-API-collect-master/docs/danmaku/danmaku_proto.md
         */
        fun fromDanmakuElem(
            elem: DanmakuElem,
            aiFlagScore: Int = 0,
        ): DanmakuData =
            DanmakuData(
                // progress 为毫秒，time 为秒
                time = elem.progress / 1000f,
                type = elem.mode,
                size = elem.fontsize,
                color = elem.color.toInt(),
                // ctime 文档中已是秒级时间戳，无需除以 1000
                timestamp = elem.ctime.toInt(),
                pool = elem.pool,
                midHash = elem.midHash,
                dmid = elem.id,
                level = elem.weight,
                text = elem.content,
                aiFlagScore = aiFlagScore,
            )
    }
}
