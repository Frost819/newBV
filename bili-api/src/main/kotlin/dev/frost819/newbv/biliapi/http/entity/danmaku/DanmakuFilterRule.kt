package dev.frost819.newbv.biliapi.http.entity.danmaku

import kotlinx.serialization.Serializable

/**
 * 用户云端弹幕屏蔽规则（GET /x/dm/filter/user）。
 *
 * @property type 规则类型：0=关键词 1=正则 2=用户
 * @property filter 规则内容；type=2 时为 UID 的 crc32 hex（部分版本返回原始 UID）
 * @property opened 是否启用
 */
@Serializable
data class DanmakuFilterRule(
    val id: Long = 0,
    val mid: Long = 0,
    val type: Int = 0,
    val filter: String = "",
    val comment: String = "",
    val opened: Boolean = true,
)

/** /x/dm/filter/user 的 data 本体。 */
@Serializable
data class DanmakuFilterRuleData(
    val rule: List<DanmakuFilterRule> = emptyList(),
)
