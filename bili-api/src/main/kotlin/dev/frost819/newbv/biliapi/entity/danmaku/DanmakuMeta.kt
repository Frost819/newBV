package dev.frost819.newbv.biliapi.entity.danmaku

import bilibili.community.service.dm.v1.DmWebViewReply

/**
 * 弹幕元数据（来自 dm/view 接口）。
 *
 * 用于弹幕分段加载：分段大小以服务端返回的 [segmentSizeMs] 为准（通常 6 分钟），
 * 不在客户端硬编码；[segTotal] 用于越界保护。
 *
 * @property segmentSizeMs 每段时长（毫秒）
 * @property segTotal 分段总数；0 表示未知（不做越界限制）
 * @property closed 该视频是否已关闭弹幕
 * @property count 弹幕总数（服务端统计，仅供参考）
 */
data class DanmakuMeta(
    val segmentSizeMs: Long,
    val segTotal: Int,
    val closed: Boolean,
    val count: Long,
) {
    companion object {
        /** dm/view 不可用或字段缺失时的兜底值：按 6 分钟分段、总数未知。 */
        val DEFAULT =
            DanmakuMeta(
                segmentSizeMs = 360_000L,
                segTotal = 0,
                closed = false,
                count = 0,
            )
    }
}

/**
 * 将 Web dm/view protobuf 响应转换为 [DanmakuMeta]。
 *
 * 对应端点：GET /x/v2/dm/web/view（见 docs/bilibili-API-collect-master/docs/danmaku/danmaku_proto.md）
 *
 * @receiver dm/view 的 protobuf 响应
 * @return 提取分段配置后的元数据；`dmSge` 缺失时 [DanmakuMeta.segmentSizeMs] 为 0，由调用方兜底
 */
fun DmWebViewReply.toDanmakuMeta(): DanmakuMeta =
    DanmakuMeta(
        segmentSizeMs = dmSge.pageSize,
        segTotal = dmSge.total.toInt(),
        closed = state == 1,
        count = count,
    )
