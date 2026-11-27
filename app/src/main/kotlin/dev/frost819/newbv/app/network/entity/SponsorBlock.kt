package dev.frost819.newbv.app.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SponsorBlock（bsbsb.top）哈希前缀查询响应中的单个视频条目。
 *
 * 服务端按 videoID 的 SHA-256 前缀返回该前缀下所有视频的片段，
 * 客户端需按 [videoID] 本地筛选目标视频。
 *
 * @param videoID 视频 BV 号
 * @param segments 该视频的片段列表
 */
@Serializable
data class SponsorHashVideo(
    val videoID: String = "",
    val segments: List<SponsorSegmentDto> = emptyList(),
)

/**
 * SponsorBlock 片段条目（HTTP 层）。
 *
 * 字段与 bsbsb.top API 响应一致；时间单位为秒（浮点）。
 *
 * @param segment [start秒, end秒] 二元组
 * @param uuid 片段唯一 ID（API 字段名为大写 `UUID`）
 * @param category 分类 API 名
 * @param actionType 动作类型：`skip`（跳过）/ `mute`（静音）
 * @param cid 关联分 P 的 CID 字符串（bsbsb.top 扩展字段，多 P 视频区分用）
 * @param locked 服务端审核锁定标记（0/1）
 * @param votes 社区票数
 * @param videoDuration 提交片段时的视频总时长（秒）
 */
@Serializable
data class SponsorSegmentDto(
    val segment: List<Double> = emptyList(),
    @SerialName("UUID") val uuid: String = "",
    val category: String = "",
    val actionType: String = "skip",
    val cid: String = "",
    val locked: Int = 0,
    val votes: Int = 0,
    val videoDuration: Double = 0.0,
) {
    /**
     * 片段起点毫秒；segment 缺失起点时为 0。
     */
    val startMs: Long
        get() = ((segment.firstOrNull() ?: 0.0) * 1000).toLong()

    /**
     * 片段终点毫秒；segment 缺失终点时为 0（归一化阶段会被过滤）。
     */
    val endMs: Long
        get() = ((segment.getOrNull(1) ?: 0.0) * 1000).toLong()
}
