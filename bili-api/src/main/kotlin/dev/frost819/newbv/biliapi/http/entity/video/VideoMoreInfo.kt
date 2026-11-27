package dev.frost819.newbv.biliapi.http.entity.video

import dev.frost819.newbv.biliapi.http.entity.user.LevelInfo
import dev.frost819.newbv.biliapi.http.entity.user.Vip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * 视频更多信息
 *
 * @param aid avid
 * @param bvid bvid
 * @param allowBp
 * @param noShare
 * @param cid cid
 * @param maxLimit
 * @param pageNo
 * @param hasNext
 * @param ipInfo IP 地址信息
 * @param loginMid 当前用户 mid
 * @param loginMidHash 当前用户 mid hash
 * @param isOwner
 * @param name 用户昵称
 * @param permission 用户权限，以逗号分隔
 * @param levelInfo 用户等级信息
 * @param vip 大会员信息
 * @param answerStatue
 * @param blockTime
 * @param role
 * @param lastPlayTime 最后播放进度，单位毫秒
 * @param lastPlayCid 最后播放分 P 的 cid
 * @param nowTime 当天时间，单位秒
 * @param onlineCount 所有终端总计在线人数
 * @param dmMask
 * @param subtitle
 * @param playerIcon
 * @param viewPoints 章节信息（视频分段点，仅部分视频有）
 * @param isUgcPayPreview
 * @param previewToast
 * @param pcdnLoader
 * @param options
 * @param guideAttention
 * @param jumpCard
 * @param operationCard
 * @param onlineSwitch
 * @param fawkes
 * @param showSwitch
 * @param toastBlock
 */
@Serializable
data class VideoMoreInfo(
    val aid: Long,
    val bvid: String,
    @SerialName("allow_bp")
    val allowBp: Boolean,
    @SerialName("no_share")
    val noShare: Boolean,
    val cid: Long,
    @SerialName("max_limit")
    val maxLimit: Int,
    @SerialName("page_no")
    val pageNo: Int,
    @SerialName("has_next")
    val hasNext: Boolean,
    @SerialName("ip_info")
    val ipInfo: IpInfo,
    @SerialName("login_mid")
    val loginMid: Long,
    @SerialName("login_mid_hash")
    val loginMidHash: String,
    @SerialName("is_owner")
    val isOwner: Boolean,
    val name: String,
    val permission: String,
    @SerialName("level_info")
    val levelInfo: LevelInfo,
    val vip: Vip,
    @SerialName("answer_status")
    val answerStatue: Int,
    @SerialName("block_time")
    val blockTime: Int,
    val role: String,
    @SerialName("last_play_time")
    val lastPlayTime: Int,
    @SerialName("last_play_cid")
    val lastPlayCid: Long,
    @SerialName("now_time")
    val nowTime: Int,
    @SerialName("online_count")
    val onlineCount: Int,
    @SerialName("dm_mask")
    val dmMask: DmMask? = null,
    val subtitle: Subtitle? = null,
    @SerialName("player_icon")
    val playerIcon: PlayerIcon? = null,
    @SerialName("view_points")
    val viewPoints: List<ViewPoint>? = null,
    @SerialName("is_ugc_pay_preview")
    val isUgcPayPreview: Boolean,
    @SerialName("preview_toast")
    val previewToast: String,
    @SerialName("pcdn_loader")
    val pcdnLoader: PcdnLoader? = null,
    val options: Options,
    @SerialName("guide_attention")
    val guideAttention: JsonArray,
    @SerialName("jump_card")
    val jumpCard: JsonArray,
    @SerialName("operation_card")
    val operationCard: JsonArray,
    @SerialName("online_switch")
    val onlineSwitch: OnlineSwitch,
    val fawkes: Fawkes,
    @SerialName("show_switch")
    val showSwitch: ShowSwitch,
    // @SerialName("bgm_info")
    // val bgmInfo: Any
    @SerialName("toast_block")
    val toastBlock: Boolean,
) {
    /**
     * IP 信息
     *
     * @param ip IP 地址
     * @param zoneIp
     * @param zoneId
     * @param country 国家
     * @param province 省份
     * @param city 城市
     */
    @Serializable
    data class IpInfo(
        val ip: String,
        @SerialName("zone_ip")
        val zoneIp: String,
        @SerialName("zone_id")
        val zoneId: Int,
        val country: String,
        val province: String,
        val city: String,
    )

    /**
     * @param cid
     * @param plat
     * @param fps
     * @param time
     * @param maskUrl
     */
    @Serializable
    data class DmMask(
        val cid: Long,
        val plat: Int,
        val fps: Int,
        val time: Int,
        @SerialName("mask_url")
        val maskUrl: String,
    )

    /**
     * @param allowSubmit 允许提交字幕
     * @param lan
     * @param lanDoc
     * @param subtitles
     */
    @Serializable
    data class Subtitle(
        @SerialName("allow_submit")
        val allowSubmit: Boolean,
        val lan: String,
        @SerialName("lan_doc")
        val lanDoc: String,
        val subtitles: List<SubtitleItem> = emptyList(),
    )

    /**
     * 字幕信息
     *
     * @param id
     * @param lan 字幕语言代号，例如zh-Hans
     * @param lanDoc 字幕语言名称
     * @param isLock
     * @param subtitleUrl
     * @param type
     * @param idStr
     * @param aiType
     * @param aiStatus
     */
    @Serializable
    data class SubtitleItem(
        val id: Long,
        val lan: String,
        @SerialName("lan_doc")
        val lanDoc: String,
        @SerialName("is_lock")
        val isLock: Boolean,
        @SerialName("subtitle_url")
        val subtitleUrl: String,
        val type: Int,
        @SerialName("id_str")
        val idStr: String,
        @SerialName("ai_type")
        val aiType: Int,
        @SerialName("ai_status")
        val aiStatus: Int,
    )

    /**
     * 章节信息（view_points 单项）。
     *
     * 实测 `from`/`to` 为整数下发的秒值，用 Double 兼容潜在的小数秒。
     * 字段均给默认值：个别条目缺字段不应让整个 [VideoMoreInfo] 解析失败
     * （该响应同时承载字幕/蒙版/历史进度等数据）。
     *
     * @param from 章节起点，单位秒
     * @param to 章节终点，单位秒
     * @param content 章节标题
     * @param imgUrl 章节配图 URL
     */
    @Serializable
    data class ViewPoint(
        val from: Double = 0.0,
        val to: Double = 0.0,
        val content: String = "",
        @SerialName("imgUrl")
        val imgUrl: String? = null,
    )

    /**
     * @param url1
     * @param hash1
     * @param url2
     * @param hash2
     * @param ctime
     */
    @Serializable
    data class PlayerIcon(
        val url1: String,
        val hash1: String,
        val url2: String,
        val hash2: String,
        val ctime: Int,
    )

    /**
     * @param flv
     * @param dash
     */
    @Serializable
    data class PcdnLoader(
        val flv: PcdnLoaderItem,
        val dash: PcdnLoaderItem,
    ) {
        /**
         * @param group
         * @param labels
         */
        @Serializable
        data class PcdnLoaderItem(
            val group: String? = null,
            val labels: Labels,
        ) {
            /**
             * @param pcdnVideoType
             * @param pcdnStage
             * @param pcdnGroup
             */
            @Serializable
            data class Labels(
                @SerialName("pcdn_video_type")
                val pcdnVideoType: String,
                @SerialName("pcdn_stage")
                val pcdnStage: String,
                @SerialName("pcdn_group")
                val pcdnGroup: String,
            )
        }
    }

    /**
     * @param is360
     * @param withoutVip
     */
    @Serializable
    data class Options(
        @SerialName("is_360")
        val is360: Boolean,
        @SerialName("without_vip")
        val withoutVip: Boolean,
    )

    /**
     * @param enableGrayDashPlayback
     * @param newBroadcast
     * @param realtimeDm
     * @param subtitleSubmitSwitch
     */
    @Serializable
    data class OnlineSwitch(
        @SerialName("enable_gray_dash_playback")
        val enableGrayDashPlayback: String,
        @SerialName("new_broadcast")
        val newBroadcast: String,
        @SerialName("realtime_dm")
        val realtimeDm: String,
        @SerialName("subtitle_submit_switch")
        val subtitleSubmitSwitch: String,
    )

    /**
     * @param configVersion
     * @param ffVersion
     */
    @Serializable
    data class Fawkes(
        @SerialName("config_version")
        val configVersion: Int,
        @SerialName("ff_version")
        val ffVersion: Int,
    )

    /**
     * @param longProgress
     */
    @Serializable
    data class ShowSwitch(
        @SerialName("long_progress")
        val longProgress: Boolean,
    )
}
