package dev.frost819.newbv.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 视频同时观看人数（在线人数）。
 *
 * 对应接口：GET /x/player/online/total
 * 文档：docs/bilibili-API-collect-master/docs/video/online.md
 *
 * @property total 所有终端总计人数文本（服务端预格式化，如 `"9.4万+"`）
 * @property count web 端实时在线人数文本
 * @property showSwitch 数据显示控制（UP 主设置的展示开关）
 */
@Serializable
data class OnlineTotal(
    val total: String = "",
    val count: String = "",
    @SerialName("show_switch")
    val showSwitch: ShowSwitch = ShowSwitch(),
) {
    /**
     * 按 UP 主展示开关计算应显示的人数文案。
     *
     * 优先展示所有终端总计人数 [total]，其次展示 web 端实时人数 [count]，
     * 均被关闭或为空时返回 null（不显示）。
     *
     * @return 可展示的人数文本，如 `"9.4万+"`；不展示时为 null
     */
    fun displayText(): String? =
        when {
            showSwitch.total && total.isNotBlank() -> total
            showSwitch.count && count.isNotBlank() -> count
            else -> null
        }

    /**
     * 数据显示控制（由 UP 主在投稿后台设置）。
     *
     * @property total 是否展示所有终端总计人数
     * @property count 是否展示 web 端实时在线人数
     */
    @Serializable
    data class ShowSwitch(
        val total: Boolean = true,
        val count: Boolean = true,
    )
}
