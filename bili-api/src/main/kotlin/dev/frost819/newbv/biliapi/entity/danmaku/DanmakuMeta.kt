package dev.frost819.newbv.biliapi.entity.danmaku

import bilibili.community.service.dm.v1.DmWebViewReply

/**
 * 弹幕元数据（来自 dm/view 接口）。
 *
 * 用于弹幕分段加载：分段大小以服务端返回的 [segmentSizeMs] 为准（通常 6 分钟），
 * 不在客户端硬编码；[segTotal] 用于越界保护。
 *
 * 同时携带 B 站用户弹幕配置与云屏蔽配置，供本地屏蔽逻辑消费。
 *
 * @property segmentSizeMs 每段时长（毫秒）
 * @property segTotal 分段总数；0 表示未知（不做越界限制）
 * @property closed 该视频是否已关闭弹幕
 * @property count 弹幕总数（服务端统计，仅供参考）
 * @property filterConfig B 站返回的用户弹幕过滤配置（屏蔽开关、AI 云屏蔽、举报屏蔽词等）
 */
data class DanmakuMeta(
    val segmentSizeMs: Long,
    val segTotal: Int,
    val closed: Boolean,
    val count: Long,
    val filterConfig: BiliDanmakuFilterConfig = BiliDanmakuFilterConfig.DEFAULT,
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
 * B 站用户弹幕过滤配置。
 *
 * 映射自 dm/view 返回的 `playerConfig`（DanmuWebPlayerConfig）、
 * `flag`（DanmakuFlagConfig）与 `reportFilterContent` 三个字段，
 * 统一封装后供本地引擎 Filter 链消费。
 *
 * @property blockTop 顶部弹幕开关（实测 true 表示未屏蔽，官方端未开屏蔽时服务端也下发 true）
 * @property blockScroll 滚动弹幕开关（同上）
 * @property blockBottom 底端弹幕开关（同上）
 * @property blockColor 彩色弹幕开关（同上）
 * @property blockSpecial 特殊弹幕开关（同上）
 * @property aiSwitch 是否开启 AI 智能云屏蔽
 * @property aiLevel AI 云屏蔽等级（服务端 1-10 尺度，与弹幕 weight 同源；0 表示未设置）
 * @property aiLevelV2 AI 云屏蔽 V2 等级（尺度未验证，仅作 aiLevel 缺失时的兜底）
 * @property reportFilterContent 由用户举报形成的当前 cid 维度屏蔽词
 * @property hasValidConfig 是否真正携带了有效配置（protobuf 中 `playerConfig` 或 `flag` 字段存在）
 * @property hasPlayerConfig 是否携带 `playerConfig`（block* 与 aiLevelV2 仅在此时有效）
 */
data class BiliDanmakuFilterConfig(
    val blockTop: Boolean = false,
    val blockScroll: Boolean = false,
    val blockBottom: Boolean = false,
    val blockColor: Boolean = false,
    val blockSpecial: Boolean = false,
    val aiSwitch: Boolean = false,
    val aiLevel: Int = 0,
    val aiLevelV2: Int = 0,
    val reportFilterContent: List<String> = emptyList(),
    val hasValidConfig: Boolean = false,
    val hasPlayerConfig: Boolean = false,
) {
    companion object {
        /** 兜底值：所有屏蔽关闭、无屏蔽词。 */
        val DEFAULT = BiliDanmakuFilterConfig()
    }
}

/**
 * 将 Web dm/view protobuf 响应转换为 [DanmakuMeta]。
 *
 * 对应端点：GET /x/v2/dm/web/view（见 docs/bilibili-API-collect-master/docs/danmaku/danmaku_proto.md）
 *
 * @receiver dm/view 的 protobuf 响应
 * @return 提取分段配置与过滤配置后的元数据；`dmSge` 缺失时 [DanmakuMeta.segmentSizeMs] 为 0，由调用方兜底
 */
fun DmWebViewReply.toDanmakuMeta(): DanmakuMeta =
    DanmakuMeta(
        segmentSizeMs = dmSge.pageSize,
        segTotal = dmSge.total.toInt(),
        closed = state == 1,
        count = count,
        filterConfig = toBiliDanmakuFilterConfig(),
    )

/** 从 dm/view protobuf 响应提取 [BiliDanmakuFilterConfig]。 */
private fun DmWebViewReply.toBiliDanmakuFilterConfig(): BiliDanmakuFilterConfig {
    val hasPlayerConfig = hasPlayerConfig()
    val hasFlag = hasFlag()
    val playerConfig = if (hasPlayerConfig) getPlayerConfig() else null
    val flag = if (hasFlag) getFlag() else null
    return BiliDanmakuFilterConfig(
        blockTop = playerConfig?.blocktop == true,
        blockScroll = playerConfig?.blockscroll == true,
        blockBottom = playerConfig?.blockbottom == true,
        blockColor = playerConfig?.blockcolor == true,
        blockSpecial = playerConfig?.blockspecial == true,
        aiSwitch = (flag?.recSwitch ?: 0) == 1 || playerConfig?.aiSwitch == true,
        aiLevel = (flag?.recFlag?.takeIf { it > 0 } ?: playerConfig?.aiLevel) ?: 0,
        aiLevelV2 = playerConfig?.aiLevelV2 ?: 0,
        reportFilterContent =
            if (reportFilterContentCount > 0) {
                buildList {
                    for (i in 0 until reportFilterContentCount) add(getReportFilterContent(i))
                }
            } else {
                emptyList()
            },
        hasValidConfig = hasPlayerConfig || hasFlag,
        hasPlayerConfig = hasPlayerConfig,
    )
}
