package dev.frost819.newbv.app.entity.player

/**
 * 空降片段的动作类型。
 *
 * @property value SponsorBlock API 的 actionType 字符串
 */
enum class SponsorActionType(
    val value: String,
) {
    /** 自动跳转：进入片段起点时 seek 到片段终点。 */
    SKIP("skip"),

    /** 自动静音：片段内静音，越过片段终点后恢复音量。 */
    MUTE("mute"),
    ;

    companion object {
        /**
         * 从 API 字符串解析动作类型。
         *
         * @return 对应类型；未知 actionType（如 full/poi 等）返回 null，调用方应丢弃该片段
         */
        fun fromValue(value: String): SponsorActionType? = entries.firstOrNull { it.value == value }
    }
}

/**
 * 空降片段（SponsorBlock 社区贡献的跳过区间）领域实体。
 *
 * 时间为毫秒，与播放器进度单位一致。
 *
 * @param uuid 片段唯一 ID（消费一次的依据）
 * @param category 分类 API 名（[SponsorCategories] 常量之一）
 * @param actionType 片段动作（跳过 / 静音）
 * @param startMs 片段起点（毫秒）
 * @param endMs 片段终点（毫秒）
 * @param cid 关联分 P 的 CID 字符串（多 P 视频过滤用；空表示未标注）
 * @param votes 社区票数
 * @param locked 服务端审核锁定标记
 */
data class SponsorSegment(
    val uuid: String,
    val category: String,
    val actionType: SponsorActionType,
    val startMs: Long,
    val endMs: Long,
    val cid: String = "",
    val votes: Int = 0,
    val locked: Boolean = false,
)

/**
 * 空降片段分类定义。
 *
 * 与 SponsorBlock 社区分类一一对应；[OPTIONS] 的顺序即设置页展示顺序。
 * 颜色沿用社区惯例配色，用于进度条片段标记。
 */
object SponsorCategories {
    const val SPONSOR = "sponsor"
    const val INTRO = "intro"
    const val OUTRO = "outro"
    const val SELF_PROMO = "selfpromo"
    const val INTERACTION = "interaction"
    const val PREVIEW = "preview"
    const val MUSIC_OFFTOPIC = "music_offtopic"
    const val FILLER = "filler"
    const val EXCLUSIVE_ACCESS = "exclusive_access"

    /**
     * 单个分类的展示配置。
     *
     * @property category API 分类名
     * @property displayName 中文展示名
     * @property color 进度条标记颜色（ARGB）
     * @property defaultEnabled 默认是否启用
     */
    data class Option(
        val category: String,
        val displayName: String,
        val color: Long,
        val defaultEnabled: Boolean,
    )

    /** 全部可配置分类（poi_highlight / padding 等少数类别无跳过价值，不提供）。 */
    val OPTIONS: List<Option> =
        listOf(
            Option(SPONSOR, "赞助/恰饭", 0xFF00D400, defaultEnabled = true),
            Option(INTRO, "过场/开场动画", 0xFF00FFFF, defaultEnabled = false),
            Option(OUTRO, "鸣谢/结束画面", 0xFF0202ED, defaultEnabled = false),
            Option(SELF_PROMO, "无偿/自我推广", 0xFFFFFF00, defaultEnabled = false),
            Option(INTERACTION, "三连/互动提醒", 0xFFCC00FF, defaultEnabled = false),
            Option(PREVIEW, "回顾/概要", 0xFF008FD6, defaultEnabled = false),
            Option(MUSIC_OFFTOPIC, "音乐:非音乐部分", 0xFFFF9900, defaultEnabled = false),
            Option(FILLER, "离题闲聊/玩笑", 0xFF7300FF, defaultEnabled = false),
            Option(EXCLUSIVE_ACCESS, "付费/专属内容", 0xFF008A5C, defaultEnabled = false),
        )

    /** 默认启用的分类：仅"恰饭"。开场/片尾可能承载正片内容，误伤率高，默认关闭。 */
    val DEFAULT_ENABLED: Set<String> =
        OPTIONS.filter { it.defaultEnabled }.map { it.category }.toSet()

    /**
     * 分类的中文展示名；未知分类原样返回。
     */
    fun displayNameOf(category: String): String =
        OPTIONS.firstOrNull { it.category == category }?.displayName ?: category

    /**
     * 分类的进度条标记颜色（ARGB Long）；未知分类回退恰饭绿色。
     */
    fun colorOf(category: String): Long =
        OPTIONS.firstOrNull { it.category == category }?.color ?: 0xFF00D400

    /**
     * 反序列化启用的分类列表。
     *
     * 空串/null 语义为「全部关闭」；首次使用前的默认值由 Prefs 存储默认值
     * （仅恰饭）负责，不在本函数中兜底，避免「全部关闭」被重置回默认。
     *
     * @param raw [Prefs.sponsorCategories] 存储的逗号连接字符串
     * @return 启用的分类名列表（保持存储顺序，去重）
     */
    fun parse(raw: String?): List<String> =
        raw
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    /**
     * 序列化启用的分类列表为存储字符串。
     *
     * @param categories 启用的分类名列表；空列表序列化为空串（语义 = 全部关闭）
     */
    fun serialize(categories: List<String>): String = categories.joinToString(",")
}
