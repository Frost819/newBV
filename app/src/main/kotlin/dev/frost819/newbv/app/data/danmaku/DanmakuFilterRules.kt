package dev.frost819.newbv.app.data.danmaku

import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuFilterRule
import java.util.zip.CRC32

/**
 * 账号云端弹幕屏蔽规则的编译结果（不可变）。
 *
 * 由 [DanmakuFilterRepository] 在 IO/Default 线程构建，点播过滤（Default）与
 * 直播弹幕回调（Main）并发读取，因此所有字段都必须只读。
 *
 * @property keywords 关键词规则（子串匹配）
 * @property regexes 正则规则（已编译，大小写不敏感）
 * @property blockedUserHashes 被屏蔽用户的 midHash（8 位小写 hex）
 * @property invalidRegexCount 编译失败的正则数量，仅用于日志
 */
internal class DanmakuFilterRules(
    val keywords: List<String> = emptyList(),
    val regexes: List<Regex> = emptyList(),
    val blockedUserHashes: Set<String> = emptySet(),
    val invalidRegexCount: Int = 0,
) {
    /** 无任何可用规则时快速跳过匹配。 */
    val isEmpty: Boolean
        get() = keywords.isEmpty() && regexes.isEmpty() && blockedUserHashes.isEmpty()

    /**
     * 判断弹幕是否命中屏蔽规则。
     *
     * @param midHash 发送者 midHash（已由接口下发；为空时跳过用户规则）
     * @param content 弹幕文本
     * @return true 表示应丢弃
     */
    fun matches(
        midHash: String,
        content: String,
    ): Boolean {
        if (blockedUserHashes.isNotEmpty() &&
            midHash.isNotEmpty() &&
            midHash.lowercase() in blockedUserHashes
        ) {
            return true
        }
        if (keywords.isNotEmpty() && keywords.any { content.contains(it) }) return true
        if (regexes.isNotEmpty() && regexes.any { it.containsMatchIn(content) }) return true
        return false
    }

    /**
     * 直播弹幕匹配：WS 回调只提供原始 mid，需按需计算 midHash。
     *
     * 没有用户规则时跳过 CRC32 计算（直播弹幕频率高）。
     */
    fun matchesLive(
        content: String,
        mid: Long,
    ): Boolean {
        if (isEmpty) return false
        val midHash = if (blockedUserHashes.isEmpty()) "" else crc32Hex(mid.toString())
        return matches(midHash = midHash, content = content)
    }

    companion object {
        /** 空规则集。 */
        val EMPTY = DanmakuFilterRules()
    }
}

/** 云端屏蔽规则类型：0=关键词 1=正则 2=用户。 */
private const val FILTER_TYPE_KEYWORD = 0
private const val FILTER_TYPE_REGEX = 1
private const val FILTER_TYPE_USER = 2

/** 单条规则文本长度上限，防止超长规则拖垮过滤线程。 */
private const val USER_RULE_MAX_LENGTH = 200

/** 8 位 hex 视为接口已下发的 midHash（见 [normalizeUserRule]）。 */
private val MID_HASH_PATTERN = Regex("^[0-9a-fA-F]{8}$")

/**
 * 将接口返回的规则编译为可执行匹配器。
 *
 * 文本超长或正则可编译失败时跳过该条，不中断其余规则。
 *
 * @param rules 接口返回的规则（调用方已按 opened 过滤）
 */
internal fun compileDanmakuFilterRules(rules: List<DanmakuFilterRule>): DanmakuFilterRules {
    val keywords = mutableListOf<String>()
    val regexes = mutableListOf<Regex>()
    val userHashes = mutableSetOf<String>()
    var invalidRegex = 0
    for (rule in rules) {
        val value = rule.filter.trim()
        if (value.isEmpty() || value.length > USER_RULE_MAX_LENGTH) continue
        when (rule.type) {
            FILTER_TYPE_KEYWORD -> keywords += value
            FILTER_TYPE_REGEX -> {
                val pattern =
                    if (value.length > 1 && value.startsWith("/") && value.endsWith("/")) {
                        value.substring(1, value.length - 1)
                    } else {
                        value
                    }
                runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }
                    .onSuccess { regexes += it }
                    .onFailure { invalidRegex++ }
            }

            FILTER_TYPE_USER -> userHashes += normalizeUserRule(value)
        }
    }
    return DanmakuFilterRules(keywords, regexes, userHashes, invalidRegex)
}

/**
 * 用户规则归一化为 midHash。
 *
 * 接口下发的 type=2 规则通常是 8 位 hex（midHash），部分版本返回原始 UID：
 * - 先按 8 位 hex 判定，避免「纯数字的 midHash」被当成 UID 二次 crc32 后永不命中；
 * - 其余纯数字按原始 UID 转 crc32；
 * - 其余视为已下发的 midHash（统一小写）。
 */
private fun normalizeUserRule(value: String): String =
    when {
        MID_HASH_PATTERN.matches(value) -> value.lowercase()
        value.all(Char::isDigit) -> crc32Hex(value)
        else -> value.lowercase()
    }

/** 计算 UID 的 midHash：crc32(十进制 UID) 的小写 8 位 hex。 */
internal fun crc32Hex(value: String): String {
    val crc = CRC32()
    crc.update(value.toByteArray(Charsets.US_ASCII))
    return crc.value.toString(16).padStart(8, '0')
}
