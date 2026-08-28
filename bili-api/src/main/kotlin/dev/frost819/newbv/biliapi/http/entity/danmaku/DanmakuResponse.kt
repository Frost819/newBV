package dev.frost819.newbv.biliapi.http.entity.danmaku

import bilibili.community.service.dm.v1.DanmakuElem

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
    val level: Int,
    val text: String,
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
        fun fromDanmakuElem(elem: DanmakuElem): DanmakuData =
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
            )
    }
}
